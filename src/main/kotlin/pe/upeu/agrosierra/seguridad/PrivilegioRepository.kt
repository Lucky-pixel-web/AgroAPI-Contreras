
package pe.upeu.agrosierra.seguridad

import org.springframework.data.jpa.repository.JpaRepository

interface PrivilegioRepository : JpaRepository<PrivilegioEntidad, Long> {
    fun findByCodigo(codigo: String): PrivilegioEntidad?
}
