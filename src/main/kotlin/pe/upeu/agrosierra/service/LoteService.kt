
package pe.upeu.agrosierra.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.dominio.*
import pe.upeu.agrosierra.repositorio.*
import pe.upeu.agrosierra.web.ApiException
import pe.upeu.agrosierra.web.dto.*

@Service
class LoteService(
    private val lotes: LoteRepository,
    private val labores: LaborRepository
) {

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('LOTE_LEER')")
    fun listar(
        pagina: Int,
        tamanio: Int,
        valle: String?,
        campaniaCerrada: Boolean?
    ): PaginaRespuesta<LoteDto> {

        validarPagina(pagina, tamanio)

        val filtro = Specification<LoteEntidad> {
                root, _, cb ->

            val condiciones = mutableListOf(
                cb.conjunction()
            )

            if (!valle.isNullOrBlank()) {
                condiciones.add(
                    cb.equal(
                        cb.lower(root.get<String>("valle")),
                        valle.trim().lowercase()
                    )
                )
            }

            if (campaniaCerrada != null) {
                condiciones.add(
                    cb.equal(
                        root.get<Int>("campaniaCerrada"),
                        if (campaniaCerrada) 1 else 0
                    )
                )
            }

            cb.and(*condiciones.toTypedArray())
        }

        val pageable = PageRequest.of(
            pagina,
            tamanio,
            Sort.by("id").ascending()
        )

        return lotes.findAll(filtro, pageable)
            .map { it.aDto() }
            .aRespuesta()
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('LOTE_LEER')")
    fun detalle(id: Long): LoteDto {
        return buscarLote(id).aDto()
    }

    @Transactional
    @PreAuthorize("hasAuthority('CAMPANIA_CERRAR')")
    fun cerrarCampania(id: Long): LoteDto {

        val lote = buscarLote(id)

        if (lote.campaniaCerrada == 1) {
            throw ApiException(
                HttpStatus.CONFLICT,
                "La campaña ya está cerrada"
            )
        }

        val tieneCosecha =
            labores.existsByLote_IdAndTipo(
                id,
                TipoLabor.COSECHA
            )

        if (!tieneCosecha) {
            throw ApiException(
                HttpStatus.CONFLICT,
                "No se puede cerrar la campaña: " +
                        "el lote no tiene una labor de cosecha"
            )
        }

        lote.campaniaCerrada = 1

        return lotes.save(lote).aDto()
    }

    private fun buscarLote(id: Long): LoteEntidad {
        return lotes.findById(id).orElseThrow {
            ApiException(
                HttpStatus.NOT_FOUND,
                "No se encontró el lote solicitado"
            )
        }
    }
}

fun validarPagina(pagina: Int, tamanio: Int) {
    val errores = mutableMapOf<String, String>()

    if (pagina < 0) {
        errores["pagina"] = "La página no puede ser negativa"
    }

    if (tamanio !in 1..100) {
        errores["tamanio"] = "El tamaño debe estar entre 1 y 100"
    }

    if (errores.isNotEmpty()) {
        throw ApiException(
            HttpStatus.BAD_REQUEST,
            "Datos invalidos",
            errores
        )
    }
}
