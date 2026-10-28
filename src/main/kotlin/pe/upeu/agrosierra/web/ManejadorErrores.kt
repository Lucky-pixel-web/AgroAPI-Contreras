
package pe.upeu.agrosierra.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*
import pe.upeu.agrosierra.web.dto.RespuestaError

@RestControllerAdvice
class ManejadorErrores {

    @ExceptionHandler(ApiException::class)
    fun manejarApi(
        ex: ApiException,
        request: HttpServletRequest
    ): ResponseEntity<RespuestaError> {

        return ResponseEntity.status(ex.estado).body(
            RespuestaError(
                status = ex.estado.value(),
                error = ex.estado.reasonPhrase,
                message = ex.detalle,
                path = request.requestURI,
                validationErrors = ex.errores
            )
        )
    }

    @ExceptionHandler(
        HttpMessageNotReadableException::class
    )
    fun manejarJson(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<RespuestaError> {

        return ResponseEntity.badRequest().body(
            RespuestaError(
                status = 400,
                error = "Bad Request",
                message = "JSON inválido o tipo de dato incorrecto",
                path = request.requestURI
            )
        )
    }
}
