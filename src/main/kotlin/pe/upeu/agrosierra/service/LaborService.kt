
package pe.upeu.agrosierra.service

import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.dominio.*
import pe.upeu.agrosierra.repositorio.*
import pe.upeu.agrosierra.seguridad.*
import pe.upeu.agrosierra.web.ApiException
import pe.upeu.agrosierra.web.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

@Service
class LaborService(
    private val labores: LaborRepository,
    private val lotes: LoteRepository,
    private val usuarios: UsuarioRepository
) {

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('LABOR_LEER')")
    fun listarPorLote(
        loteId: Long,
        pagina: Int,
        tamanio: Int
    ): PaginaRespuesta<LaborDto> {

        validarPagina(pagina, tamanio)

        buscarLote(loteId)

        return labores.findByLote_IdOrderByFechaDesc(
            loteId,
            PageRequest.of(pagina, tamanio)
        ).map { it.aDto() }.aRespuesta()
    }

    @Transactional
    @PreAuthorize("hasAuthority('LABOR_CREAR')")
    fun crear(
        solicitud: LaborSolicitud,
        auth: JwtAuthenticationToken
    ): LaborDto {

        val datos = validarDatos(solicitud)
        val lote = buscarLote(datos.loteId)

        verificarCampania(lote)
        verificarDosis(lote, datos)

        val usuario = usuarioActual(auth)

        val labor = LaborEntidad(
            lote = lote,
            tipo = datos.tipo,
            fecha = datos.fecha,
            responsable = usuario.username,
            cantidad = datos.cantidad,
            observacion = datos.observacion,
            registradaEn = LocalDateTime.now()
        )

        return labores.saveAndFlush(labor).aDto()
    }

    @Transactional
    @PreAuthorize("hasAuthority('LABOR_EDITAR')")
    fun actualizar(
        id: Long,
        solicitud: LaborSolicitud,
        auth: JwtAuthenticationToken
    ): LaborDto {

        val labor = buscarLabor(id)
        val usuario = usuarioActual(auth)

        val lote = labor.lote
            ?: throw ApiException(
                HttpStatus.NOT_FOUND,
                "El lote de la labor no existe"
            )

        verificarCampania(lote)

        // RN-02: restricción especial del técnico.
        if (usuario.rol?.codigo == "TECNICO") {

            val esPropia =
                labor.responsable == usuario.username

            val dentroDePlazo =
                !LocalDateTime.now().isAfter(
                    labor.registradaEn.plusHours(24)
                )

            if (!esPropia || !dentroDePlazo) {
                throw ApiException(
                    HttpStatus.FORBIDDEN,
                    "Un técnico solo puede corregir " +
                            "sus propias labores durante " +
                            "las primeras 24 horas"
                )
            }
        }

        val datos = validarDatos(solicitud)

        if (datos.loteId != lote.id) {
            throw ApiException(
                HttpStatus.BAD_REQUEST,
                "Datos invalidos",
                mapOf(
                    "loteId" to
                            "No se puede cambiar el lote de la labor"
                )
            )
        }

        verificarDosis(lote, datos)

        labor.tipo = datos.tipo
        labor.fecha = datos.fecha
        labor.cantidad = datos.cantidad
        labor.observacion = datos.observacion

        return labores.saveAndFlush(labor).aDto()
    }

    @Transactional
    @PreAuthorize("hasAuthority('LABOR_ELIMINAR')")
    fun eliminar(id: Long) {

        val labor = buscarLabor(id)

        labores.delete(labor)
    }

    private fun buscarLote(id: Long): LoteEntidad {
        return lotes.findById(id).orElseThrow {
            ApiException(
                HttpStatus.NOT_FOUND,
                "Lote no encontrado"
            )
        }
    }

    private fun buscarLabor(id: Long): LaborEntidad {
        return labores.findById(id).orElseThrow {
            ApiException(
                HttpStatus.NOT_FOUND,
                "Labor no encontrada"
            )
        }
    }

    private fun usuarioActual(
        auth: JwtAuthenticationToken
    ): UsuarioEntidad {

        val id = auth.token.subject
            ?.toLongOrNull()
            ?: throw ApiException(
                HttpStatus.UNAUTHORIZED,
                "Token inválido"
            )

        val usuario = usuarios.findById(id)
            .orElseThrow {
                ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no encontrado"
                )
            }

        if (usuario.activo != 1) {
            throw ApiException(
                HttpStatus.UNAUTHORIZED,
                "Usuario inactivo"
            )
        }

        return usuario
    }

    // RN-01
    private fun verificarCampania(lote: LoteEntidad) {
        if (lote.campaniaCerrada == 1) {
            throw ApiException(
                HttpStatus.CONFLICT,
                "No se permiten labores en una campaña cerrada"
            )
        }
    }

    // RN-04
    private fun verificarDosis(
        lote: LoteEntidad,
        datos: DatosLabor
    ) {
        if (datos.tipo != TipoLabor.FUMIGACION) return

        val maximo =
            lote.dosisMaximaPorHectarea * lote.hectareas

        if (datos.cantidad > maximo) {
            throw ApiException(
                HttpStatus.BAD_REQUEST,
                "Datos invalidos",
                mapOf(
                    "cantidad" to
                            "La cantidad supera la dosis " +
                            "máxima permitida de $maximo"
                )
            )
        }
    }

    private fun validarDatos(
        solicitud: LaborSolicitud
    ): DatosLabor {

        val errores = mutableMapOf<String, String>()

        val loteId = solicitud.loteId
        if (loteId == null || loteId <= 0) {
            errores["loteId"] = "Seleccione un lote válido"
        }

        val tipo = solicitud.tipo
        if (tipo == null) {
            errores["tipo"] = "Seleccione un tipo de labor"
        }

        val fecha = try {
            solicitud.fecha
                ?.takeIf { it.isNotBlank() }
                ?.let { LocalDate.parse(it) }
        } catch (e: DateTimeParseException) {
            null
        }

        if (fecha == null) {
            errores["fecha"] = "Use el formato AAAA-MM-DD"
        }

        val cantidad = solicitud.cantidad
        if (cantidad == null ||
            !cantidad.isFinite() ||
            cantidad <= 0.0
        ) {
            errores["cantidad"] =
                "La cantidad debe ser mayor que cero"
        }

        val observacion = solicitud.observacion?.trim()

        if (observacion.isNullOrBlank() ||
            observacion.length > 500
        ) {
            errores["observacion"] =
                "La observación debe tener entre 1 y 500 caracteres"
        }

        if (errores.isNotEmpty()) {
            throw ApiException(
                HttpStatus.BAD_REQUEST,
                "Datos invalidos",
                errores
            )
        }

        return DatosLabor(
            loteId = requireNotNull(loteId),
            tipo = requireNotNull(tipo),
            fecha = requireNotNull(fecha),
            cantidad = requireNotNull(cantidad),
            observacion = requireNotNull(observacion)
        )
    }

    private data class DatosLabor(
        val loteId: Long,
        val tipo: TipoLabor,
        val fecha: LocalDate,
        val cantidad: Double,
        val observacion: String
    )
}
