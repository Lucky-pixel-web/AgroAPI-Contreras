
package pe.upeu.agrosierra.web

import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import pe.upeu.agrosierra.seguridad.UsuarioRepository
import pe.upeu.agrosierra.web.dto.PerfilRespuesta

@RestController
@RequestMapping("/api/v1/usuarios")
class UsuarioController(
    private val usuarios: UsuarioRepository
) {

    @GetMapping("/me")
    fun miPerfil(
        autenticacion: JwtAuthenticationToken
    ): PerfilRespuesta {

        val id = autenticacion.token.subject
            ?.toLongOrNull()
            ?: throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Token inválido"
            )

        val usuario = usuarios.findById(id)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no encontrado"
                )
            }

        val rol = usuario.rol
            ?: throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario sin rol"
            )

        return PerfilRespuesta(
            id = id,
            usuario = usuario.username,
            nombre = usuario.nombre,
            rol = rol.codigo,
            privilegios = rol.privilegios
                .map { it.codigo }
                .toSet()
        )
    }
}
