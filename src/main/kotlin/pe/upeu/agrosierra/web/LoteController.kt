
package pe.upeu.agrosierra.web

import org.springframework.web.bind.annotation.*
import pe.upeu.agrosierra.service.LoteService
import pe.upeu.agrosierra.service.LaborService
import pe.upeu.agrosierra.web.dto.*

@RestController
@RequestMapping("/api/v1/lotes")
class LoteController(
    private val loteService: LoteService,
    private val laborService: LaborService
) {

    @GetMapping
    fun listar(
        @RequestParam(defaultValue = "0")
        pagina: Int,

        @RequestParam(defaultValue = "20")
        tamanio: Int,

        @RequestParam(required = false)
        valle: String?,

        @RequestParam(required = false)
        campaniaCerrada: Boolean?
    ): PaginaRespuesta<LoteDto> {
        return loteService.listar(
            pagina, tamanio, valle, campaniaCerrada
        )
    }

    @GetMapping("/{id}")
    fun detalle(
        @PathVariable id: Long
    ): LoteDto {
        return loteService.detalle(id)
    }

    @GetMapping("/{id}/labores")
    fun listarLabores(
        @PathVariable id: Long,

        @RequestParam(defaultValue = "0")
        pagina: Int,

        @RequestParam(defaultValue = "20")
        tamanio: Int
    ): PaginaRespuesta<LaborDto> {
        return laborService.listarPorLote(
            id, pagina, tamanio
        )
    }

    @PostMapping("/{id}/cerrar-campania")
    fun cerrarCampania(
        @PathVariable id: Long
    ): LoteDto {
        return loteService.cerrarCampania(id)
    }
}
