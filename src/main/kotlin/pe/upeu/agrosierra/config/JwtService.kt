
package pe.upeu.agrosierra.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Service
import pe.upeu.agrosierra.seguridad.UsuarioEntidad
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class JwtService(
    private val encoder: JwtEncoder,

    @Value("\${agro.jwt.issuer}")
    private val emisor: String
) {

    fun generar(usuario: UsuarioEntidad): String {
        val ahora = Instant.now()

        val permisos = usuario.rol
            ?.privilegios
            ?.map { it.codigo }
            ?.sorted()
            ?: emptyList()

        val claims = JwtClaimsSet.builder()
            .issuer(emisor)
            .subject(
                requireNotNull(usuario.id).toString()
            )
            .issuedAt(ahora)
            .expiresAt(
                ahora.plus(1, ChronoUnit.HOURS)
            )
            .claim("usuario", usuario.username)
            .claim("privilegios", permisos)
            .build()

        val header = JwsHeader.with(MacAlgorithm.HS256)
            .build()

        return encoder.encode(
            JwtEncoderParameters.from(header, claims)
        ).tokenValue
    }
}
