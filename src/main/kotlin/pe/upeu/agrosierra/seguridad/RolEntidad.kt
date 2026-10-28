
package pe.upeu.agrosierra.seguridad

import jakarta.persistence.*

@Entity
@Table(name = "ROL")
class RolEntidad(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    var codigo: String = "",

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "ROL_PRIVILEGIO",
        joinColumns = [JoinColumn(name = "ROL_ID")],
        inverseJoinColumns = [JoinColumn(name = "PRIVILEGIO_ID")]
    )
    var privilegios: MutableSet<PrivilegioEntidad> = mutableSetOf()
)
