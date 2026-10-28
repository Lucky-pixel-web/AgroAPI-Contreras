
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

    // LISTAR LABORES DE UN LOTE
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

    // CREAR UNA NUEVA LABOR
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
            cantidadJornales = datos.cantidadJornales,
            observacion = datos.observacion,
            registradaEn = LocalDateTime.now()
        )

        return labores.saveAndFlush(labor).aDto()
    }

    // ACTUALIZAR UNA LABOR
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

        // RN-02: El técnico solo puede editar
        // sus propias labores durante 24 horas.
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
        labor.cantidadJornales = datos.cantidadJornales
        labor.observacion = datos.observacion

        return labores.saveAndFlush(labor).aDto()
    }

    // ELIMINAR UNA LABOR
    @Transactional
    @PreAuthorize("hasAuthority('LABOR_ELIMINAR')")
    fun eliminar(id: Long) {

        val labor = buscarLabor(id)
        labores.delete(labor)
    }

    // BUSCAR LOTE POR ID
    private fun buscarLote(id: Long): LoteEntidad {
        return lotes.findById(id).orElseThrow {
            ApiException(
                HttpStatus.NOT_FOUND,
                "Lote no encontrado"
            )
        }
    }

    // BUSCAR LABOR POR ID
    private fun buscarLabor(id: Long): LaborEntidad {
        return labores.findById(id).orElseThrow {
            ApiException(
                HttpStatus.NOT_FOUND,
                "Labor no encontrada"
            )
        }
    }

    // OBTENER USUARIO AUTENTICADO
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

    // RN-01: NO REGISTRAR NI MODIFICAR
    // LABORES EN CAMPAÑAS CERRADAS
    private fun verificarCampania(lote: LoteEntidad) {

        if (lote.campaniaCerrada == 1) {
            throw ApiException(
                HttpStatus.CONFLICT,
                "No se permiten labores en una campaña cerrada"
            )
        }
    }

    // RN-04: VALIDAR DOSIS MÁXIMA
    // PARA LABORES DE FUMIGACIÓN
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

    // VALIDAR TODOS LOS DATOS DEL FORMULARIO
    // INCLUYE CANTIDAD DE JORNALES (SC-C)
    private fun validarDatos(
        solicitud: LaborSolicitud
    ): DatosLabor {

        val errores = mutableMapOf<String, String>()

        // VALIDAR LOTE
        val loteId = solicitud.loteId

        if (loteId == null || loteId <= 0) {
            errores["loteId"] =
                "Seleccione un lote válido"
        }

        // VALIDAR TIPO DE LABOR
        val tipo = solicitud.tipo

        if (tipo == null) {
            errores["tipo"] =
                "Seleccione un tipo de labor"
        }

        // VALIDAR FECHA
        val fecha = try {
            solicitud.fecha
                ?.takeIf { it.isNotBlank() }
                ?.let { LocalDate.parse(it) }
        } catch (e: DateTimeParseException) {
            null
        }

        if (fecha == null) {
            errores["fecha"] =
                "Use el formato AAAA-MM-DD"
        }

        // VALIDAR CANTIDAD
        val cantidad = solicitud.cantidad

        if (
            cantidad == null ||
            !cantidad.isFinite() ||
            cantidad <= 0.0
        ) {
            errores["cantidad"] =
                "La cantidad debe ser mayor que cero"
        }

        // VALIDAR OBSERVACIÓN
        val observacion =
            solicitud.observacion?.trim()

        if (
            observacion.isNullOrBlank() ||
            observacion.length > 500
        ) {
            errores["observacion"] =
                "La observación debe tener entre 1 y 500 caracteres"
        }

        // SC-C: VALIDAR CANTIDAD DE JORNALES
        val cantidadJornales =
            solicitud.cantidadJornales

        if (
            cantidadJornales == null ||
            cantidadJornales < 1
        ) {
            errores["cantidadJornales"] =
                "La cantidad de jornales debe ser mayor que cero"
        }

        // RESPONDER SI EXISTEN ERRORES
        if (errores.isNotEmpty()) {
            throw ApiException(
                HttpStatus.BAD_REQUEST,
                "Datos invalidos",
                errores
            )
        }

        // DEVOLVER DATOS VALIDADOS
        return DatosLabor(
            loteId = requireNotNull(loteId),
            tipo = requireNotNull(tipo),
            fecha = requireNotNull(fecha),
            cantidad = requireNotNull(cantidad),
            observacion = requireNotNull(observacion),
            cantidadJornales = requireNotNull(cantidadJornales)
        )
    }

    // ESTRUCTURA INTERNA DE DATOS VALIDADOS
    private data class DatosLabor(
        val loteId: Long,
        val tipo: TipoLabor,
        val fecha: LocalDate,
        val cantidad: Double,
        val observacion: String,
        val cantidadJornales: Int
    )
}
