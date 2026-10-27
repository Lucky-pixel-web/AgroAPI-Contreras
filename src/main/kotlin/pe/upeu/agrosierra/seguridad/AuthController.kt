
package pe.upeu.agrosierra.seguridad

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import pe.upeu.agrosierra.config.JwtService
import pe.upeu.agrosierra.web.dto.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val usuarios: UsuarioRepository,
    private val cifrado: PasswordEncoder,
    private val jwtService: JwtService
) {

    @PostMapping("/login")
    fun login(
        @RequestBody solicitud: LoginSolicitud,
        request: HttpServletRequest
    ): ResponseEntity<Any> {

        val usuario = usuarios.findByUsername(
            solicitud.usuario
        )

        val credencialesCorrectas =
            usuario != null &&
                    usuario.activo == 1 &&
                    cifrado.matches(
                        solicitud.contrasena,
                        usuario.claveHash
                    ) == true

        if (!credencialesCorrectas || usuario == null) {
            return ResponseEntity.status(
                HttpStatus.UNAUTHORIZED
            ).body(
                RespuestaError(
                    status = 401,
                    error = "Unauthorized",
                    message = "Usuario o contraseña incorrectos",
                    path = request.requestURI
                )
            )
        }

        val permisos = usuario.rol
            ?.privilegios
            ?.map { it.codigo }
            ?.toSet()
            ?: emptySet()

        return ResponseEntity.ok(
            LoginRespuesta(
                token = jwtService.generar(usuario),
                nombre = usuario.nombre,
                usuario = usuario.username,
                rol = usuario.rol?.codigo ?: "",
                privilegios = permisos
            )
        )
    }
}
