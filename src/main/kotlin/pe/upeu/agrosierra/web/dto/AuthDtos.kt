
package pe.upeu.agrosierra.web.dto

import java.time.LocalDateTime

data class LoginSolicitud(
    val usuario: String = "",
    val contrasena: String = ""
)

data class LoginRespuesta(
    val token: String,
    val nombre: String,
    val usuario: String,
    val rol: String,
    val privilegios: Set<String>
)

data class PerfilRespuesta(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>
)

data class RespuestaError(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: String = LocalDateTime.now().toString(),
    val validationErrors: Map<String, String> = emptyMap()
)
