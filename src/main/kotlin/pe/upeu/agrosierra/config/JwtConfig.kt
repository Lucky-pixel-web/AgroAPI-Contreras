
package pe.upeu.agrosierra.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.*
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtConfig(
    @Value("\${agro.jwt.secret-base64}")
    private val claveBase64: String,

    @Value("\${agro.jwt.issuer}")
    private val emisor: String
) {

    @Bean
    fun claveJwt(): SecretKey {
        val bytes = Base64.getDecoder().decode(claveBase64)

        require(bytes.size >= 32) {
            "La clave JWT debe tener al menos 32 bytes"
        }

        return SecretKeySpec(bytes, "HmacSHA256")
    }

    @Bean
    fun jwtEncoder(clave: SecretKey): JwtEncoder {
        return NimbusJwtEncoder.withSecretKey(clave)
            .algorithm(MacAlgorithm.HS256)
            .build()
    }

    @Bean
    fun jwtDecoder(clave: SecretKey): JwtDecoder {
        return NimbusJwtDecoder.withSecretKey(clave)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
            .apply {
                setJwtValidator(
                    JwtValidators.createDefaultWithIssuer(emisor)
                )
            }
    }
}
