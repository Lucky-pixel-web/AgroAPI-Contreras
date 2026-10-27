
package pe.upeu.agrosierra.seguridad

import org.springframework.data.jpa.repository.JpaRepository

interface RolRepository : JpaRepository<RolEntidad, Long> {
    fun findByCodigo(codigo: String): RolEntidad?
}
