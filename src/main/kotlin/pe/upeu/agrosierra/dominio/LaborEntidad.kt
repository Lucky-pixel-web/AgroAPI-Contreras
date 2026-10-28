
package pe.upeu.agrosierra.dominio

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "LABOR")
class LaborEntidad(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOTE_ID", nullable = false)
    var lote: LoteEntidad? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var tipo: TipoLabor = TipoLabor.RIEGO,

    @Column(nullable = false)
    var fecha: LocalDate = LocalDate.now(),

    @Column(nullable = false, length = 80)
    var responsable: String = "",

    @Column(nullable = false)
    var cantidad: Double = 0.0,

    @Column(name = "CANTIDAD_JORNALES", nullable = false)
    var cantidadJornales: Int = 1,

    @Column(nullable = false, length = 500)
    var observacion: String = "",

    @Column(name = "REGISTRADA_EN", nullable = false)
    var registradaEn: LocalDateTime = LocalDateTime.now()
)
