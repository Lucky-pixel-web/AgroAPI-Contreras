
package pe.upeu.agrosierra.seguridad

import jakarta.persistence.*

@Entity
@Table(name = "USUARIO")
class UsuarioEntidad(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 80)
    var username: String = "",

    @Column(name = "CLAVE_HASH", nullable = false, length = 100)
    var claveHash: String = "",

    @Column(nullable = false, length = 150)
    var nombre: String = "",

    @Column(nullable = false)
    var activo: Int = 1,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ROL_ID", nullable = false)
    var rol: RolEntidad? = null
)
