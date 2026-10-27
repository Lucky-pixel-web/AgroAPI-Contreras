
package pe.upeu.agrosierra.seguridad

import jakarta.persistence.*

@Entity
@Table(name = "PRIVILEGIO")
class PrivilegioEntidad(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    var codigo: String = ""
)
