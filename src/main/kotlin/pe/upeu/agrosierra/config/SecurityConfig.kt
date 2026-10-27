
package pe.upeu.agrosierra.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import pe.upeu.agrosierra.web.dto.RespuestaError
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jsonMapper: JsonMapper
) {

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val permisos = JwtGrantedAuthoritiesConverter()

        permisos.setAuthoritiesClaimName("privilegios")
        permisos.setAuthorityPrefix("")

        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(permisos)
        }
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        converter: JwtAuthenticationConverter
    ): SecurityFilterChain {

        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            .authorizeHttpRequests { reglas ->

                reglas.requestMatchers(
                    "/api/health",
                    "/api/v1/auth/login"
                ).permitAll()

                reglas.requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/lotes/*/labores"
                ).hasAuthority("LABOR_LEER")

                reglas.requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/lotes/**"
                ).hasAuthority("LOTE_LEER")

                reglas.requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/labores"
                ).hasAuthority("LABOR_CREAR")

                reglas.requestMatchers(
                    HttpMethod.PUT,
                    "/api/v1/labores/*"
                ).hasAuthority("LABOR_EDITAR")

                reglas.requestMatchers(
                    HttpMethod.DELETE,
                    "/api/v1/labores/*"
                ).hasAuthority("LABOR_ELIMINAR")

                reglas.requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/lotes/*/cerrar-campania"
                ).hasAuthority("CAMPANIA_CERRAR")

                reglas.requestMatchers(
                    "/api/v1/usuarios/me"
                ).authenticated()

                reglas.anyRequest().authenticated()
            }
            .exceptionHandling { errores ->

                errores.authenticationEntryPoint {
                        request, response, _ ->
                    escribirError(
                        request, response,
                        HttpStatus.UNAUTHORIZED,
                        "Token ausente, inválido o vencido"
                    )
                }

                errores.accessDeniedHandler {
                        request, response, _ ->
                    escribirError(
                        request, response,
                        HttpStatus.FORBIDDEN,
                        "No tiene privilegios para esta operación"
                    )
                }
            }
            .oauth2ResourceServer { oauth ->

                oauth.jwt {
                    it.jwtAuthenticationConverter(converter)
                }

                oauth.authenticationEntryPoint {
                        request, response, _ ->
                    escribirError(
                        request, response,
                        HttpStatus.UNAUTHORIZED,
                        "Token ausente, inválido o vencido"
                    )
                }

                oauth.accessDeniedHandler {
                        request, response, _ ->
                    escribirError(
                        request, response,
                        HttpStatus.FORBIDDEN,
                        "No tiene privilegios para esta operación"
                    )
                }
            }

        return http.build()
    }

    private fun escribirError(
        request: HttpServletRequest,
        response: HttpServletResponse,
        estado: HttpStatus,
        mensaje: String
    ) {
        response.status = estado.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val error = RespuestaError(
            status = estado.value(),
            error = estado.reasonPhrase,
            message = mensaje,
            path = request.requestURI
        )

        response.writer.write(
            jsonMapper.writeValueAsString(error)
        )
    }
}
