package pe.upeu.agrosierra.config

import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeHttpRequests { reglas ->
                reglas.requestMatchers("/api/health").permitAll()
                reglas.anyRequest().authenticated()
            }
            .exceptionHandling { errores ->
                errores.authenticationEntryPoint { _, respuesta, _ ->
                    respuesta.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                }
            }
        return http.build()
    }
}
