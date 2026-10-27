
package pe.upeu.agrosierra.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import pe.upeu.agrosierra.service.LaborService
import pe.upeu.agrosierra.web.dto.*

@RestController
@RequestMapping("/api/v1/labores")
class LaborController(
    private val laborService: LaborService
) {

    @PostMapping
    fun crear(
        @RequestBody solicitud: LaborSolicitud,
        auth: JwtAuthenticationToken
    ): ResponseEntity<LaborDto> {

        val resultado = laborService.crear(
            solicitud, auth
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(resultado)
    }

    @PutMapping("/{id}")
    fun actualizar(
        @PathVariable id: Long,
        @RequestBody solicitud: LaborSolicitud,
        auth: JwtAuthenticationToken
    ): LaborDto {

        return laborService.actualizar(
            id, solicitud, auth
        )
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminar(
        @PathVariable id: Long
    ) {
        laborService.eliminar(id)
    }
}
