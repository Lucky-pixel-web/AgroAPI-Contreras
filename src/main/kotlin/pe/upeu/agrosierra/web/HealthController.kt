package pe.upeu.agrosierra.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HealthController {

    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf(
        "estado" to "OK",
        "servicio" to "AgroAPI-Contreras"
    )
}
