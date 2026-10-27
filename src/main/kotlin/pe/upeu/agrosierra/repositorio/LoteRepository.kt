
package pe.upeu.agrosierra.repositorio

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pe.upeu.agrosierra.dominio.LoteEntidad

interface LoteRepository : JpaRepository<LoteEntidad, Long> {
    fun findByValleIgnoreCase(
        valle: String,
        pageable: Pageable
    ): Page<LoteEntidad>
}
