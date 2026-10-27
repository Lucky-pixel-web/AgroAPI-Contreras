
package pe.upeu.agrosierra.repositorio

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import pe.upeu.agrosierra.dominio.LoteEntidad

interface LoteRepository :
    JpaRepository<LoteEntidad, Long>,
    JpaSpecificationExecutor<LoteEntidad>
