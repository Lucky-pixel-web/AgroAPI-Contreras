
package pe.upeu.agrosierra.repositorio

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pe.upeu.agrosierra.dominio.LaborEntidad

interface LaborRepository : JpaRepository<LaborEntidad, Long> {
    fun findByLote_IdOrderByFechaDesc(
        loteId: Long,
        pageable: Pageable
    ): Page<LaborEntidad>

    fun existsByLote_IdAndTipo(
        loteId: Long,
        tipo: pe.upeu.agrosierra.dominio.TipoLabor
    ): Boolean
}
