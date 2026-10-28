
package pe.upeu.agrosierra.web

import org.springframework.http.HttpStatus

class ApiException(
    val estado: HttpStatus,
    val detalle: String,
    val errores: Map<String, String> = emptyMap()
) : RuntimeException(detalle)
