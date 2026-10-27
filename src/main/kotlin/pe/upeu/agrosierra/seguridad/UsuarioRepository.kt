
package pe.upeu.agrosierra.seguridad

import org.springframework.data.jpa.repository.JpaRepository

interface UsuarioRepository : JpaRepository<UsuarioEntidad, Long> {
    fun findByUsername(username: String): UsuarioEntidad?
}
