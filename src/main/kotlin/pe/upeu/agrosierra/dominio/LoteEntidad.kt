
package pe.upeu.agrosierra.dominio

import jakarta.persistence.*

@Entity
@Table(name = "LOTE")
class LoteEntidad(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 30)
    var codigo: String = "",

    @Column(nullable = false, length = 150)
    var nombre: String = "",

    @Column(nullable = false)
    var hectareas: Double = 0.0,

    @Column(nullable = false, length = 100)
    var cultivo: String = "",

    @Column(nullable = false, length = 100)
    var valle: String = "",

    @Column(name = "CAMPANIA_CERRADA", nullable = false)
    var campaniaCerrada: Int = 0,

    @Column(name = "DOSIS_MAXIMA_POR_HECTAREA", nullable = false)
    var dosisMaximaPorHectarea: Double = 0.0
)
