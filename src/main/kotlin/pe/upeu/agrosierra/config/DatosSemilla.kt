
package pe.upeu.agrosierra.config

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.dominio.LaborEntidad
import pe.upeu.agrosierra.dominio.LoteEntidad
import pe.upeu.agrosierra.dominio.TipoLabor
import pe.upeu.agrosierra.repositorio.LaborRepository
import pe.upeu.agrosierra.repositorio.LoteRepository
import pe.upeu.agrosierra.seguridad.*
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class DatosSemilla(
    private val lotes: LoteRepository,
    private val labores: LaborRepository,
    private val usuarios: UsuarioRepository,
    private val roles: RolRepository,
    private val privilegios: PrivilegioRepository,
    private val cifrado: PasswordEncoder,
    @Value("\${AGRO_SEED_PASSWORD:Agro2026*}")
    private val clavePrueba: String
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) {
        cargarSeguridad()
        cargarLotesYLabores()
    }

    private fun cargarSeguridad() {
        val codigos = listOf(
            "LOTE_LEER",
            "LABOR_LEER",
            "LABOR_CREAR",
            "LABOR_EDITAR",
            "LABOR_ELIMINAR",
            "CAMPANIA_CERRAR",
            "USUARIO_GESTIONAR"
        )

        val permisos = codigos.associateWith { codigo ->
            privilegios.findByCodigo(codigo)
                ?: privilegios.save(
                    PrivilegioEntidad(codigo = codigo)
                )
        }

        fun guardarRol(
            codigo: String,
            vararg nombres: String
        ): RolEntidad {
            val rol = roles.findByCodigo(codigo)
                ?: RolEntidad(codigo = codigo)

            rol.privilegios = nombres.map {
                permisos.getValue(it)
            }.toMutableSet()

            return roles.save(rol)
        }

        val tecnico = guardarRol(
            "TECNICO",
            "LOTE_LEER",
            "LABOR_LEER",
            "LABOR_CREAR",
            "LABOR_EDITAR"
        )

        val jefe = guardarRol(
            "JEFE_CAMPO",
            "LOTE_LEER",
            "LABOR_LEER",
            "LABOR_CREAR",
            "LABOR_EDITAR",
            "LABOR_ELIMINAR",
            "CAMPANIA_CERRAR"
        )

        val supervisor = guardarRol(
            "SUPERVISOR_CALIDAD",
            "LOTE_LEER",
            "LABOR_LEER",
            "LABOR_EDITAR"
        )

        val administrador = guardarRol(
            "ADMINISTRADOR",
            "LOTE_LEER",
            "LABOR_LEER",
            "USUARIO_GESTIONAR"
        )

        fun guardarUsuario(
            username: String,
            nombre: String,
            rol: RolEntidad
        ) {
            if (usuarios.findByUsername(username) == null) {
                usuarios.save(
                    UsuarioEntidad(
                        username = username,
                        nombre = nombre,
                        claveHash = cifrado.encode(clavePrueba)
                            ?: error("No se pudo cifrar la contraseña"),
                        activo = 1,
                        rol = rol
                    )
                )
            }
        }

        guardarUsuario(
            "tecnico1", "Tecnico de campo", tecnico
        )
        guardarUsuario(
            "jefe1", "Jefe de campo", jefe
        )
        guardarUsuario(
            "admin", "Administrador", administrador
        )
        guardarUsuario(
            "supervisor1",
            "Supervisor de calidad",
            supervisor
        )
    }

    private fun cargarLotesYLabores() {
        val lotesIniciales = listOf(
            LoteEntidad(
                codigo = "L001",
                nombre = "Maiz Norte",
                hectareas = 2.5,
                cultivo = "Maiz",
                valle = "Chillon",
                campaniaCerrada = 0,
                dosisMaximaPorHectarea = 3.0
            ),
            LoteEntidad(
                codigo = "L002",
                nombre = "Papa Alta",
                hectareas = 3.0,
                cultivo = "Papa",
                valle = "Chillon",
                campaniaCerrada = 0,
                dosisMaximaPorHectarea = 2.0
            ),
            LoteEntidad(
                codigo = "L003",
                nombre = "Palta Este",
                hectareas = 4.0,
                cultivo = "Palta",
                valle = "Chillon",
                campaniaCerrada = 1,
                dosisMaximaPorHectarea = 2.5
            ),
            LoteEntidad(
                codigo = "L004",
                nombre = "Arroz Bajo",
                hectareas = 5.0,
                cultivo = "Arroz",
                valle = "Mantaro",
                campaniaCerrada = 0,
                dosisMaximaPorHectarea = 1.5
            ),
            LoteEntidad(
                codigo = "L005",
                nombre = "Quinua Sur",
                hectareas = 2.0,
                cultivo = "Quinua",
                valle = "Mantaro",
                campaniaCerrada = 0,
                dosisMaximaPorHectarea = 1.0
            ),
            LoteEntidad(
                codigo = "L006",
                nombre = "Hortalizas Centro",
                hectareas = 1.8,
                cultivo = "Hortalizas",
                valle = "Mantaro",
                campaniaCerrada = 1,
                dosisMaximaPorHectarea = 0.8
            ),
            LoteEntidad(
                codigo = "L007",
                nombre = "Uva Ladera",
                hectareas = 3.2,
                cultivo = "Uva",
                valle = "Santa",
                campaniaCerrada = 0,
                dosisMaximaPorHectarea = 2.2
            ),
            LoteEntidad(
                codigo = "L008",
                nombre = "Maiz Mecanizado",
                hectareas = 4.5,
                cultivo = "Maiz",
                valle = "Santa",
                campaniaCerrada = 0,
                dosisMaximaPorHectarea = 3.0
            )
        )

        val lotePorCodigo = lotes.findAll()
            .associateBy { it.codigo }
            .toMutableMap()

        lotesIniciales.forEach { lote ->
            if (lote.codigo !in lotePorCodigo) {
                lotePorCodigo[lote.codigo] = lotes.save(lote)
            }
        }

        // Las labores iniciales se cargan una sola vez.
        if (labores.count() > 0L) return

        val hoy = LocalDate.now()
        val ahora = LocalDateTime.now()

        val datos = listOf(
            Trabajo("L001", TipoLabor.RIEGO, 2, "tecnico1", 3.0, "Riego inicial"),
            Trabajo("L001", TipoLabor.FUMIGACION, 1, "tecnico1", 4.0, "Control preventivo"),
            Trabajo("L001", TipoLabor.COSECHA, 0, "jefe1", 180.0, "Primera cosecha"),

            Trabajo("L002", TipoLabor.RIEGO, 3, "tecnico1", 2.0, "Riego de papa"),
            Trabajo("L002", TipoLabor.FUMIGACION, 2, "tecnico1", 5.0, "Control de plagas"),
            Trabajo("L002", TipoLabor.RIEGO, 1, "jefe1", 2.0, "Segundo riego"),

            Trabajo("L003", TipoLabor.RIEGO, 20, "tecnico1", 4.0, "Riego de palta"),
            Trabajo("L003", TipoLabor.COSECHA, 18, "jefe1", 200.0, "Cosecha terminada"),

            Trabajo("L004", TipoLabor.RIEGO, 1, "tecnico1", 6.0, "Riego de arroz"),
            Trabajo("L004", TipoLabor.FUMIGACION, 1, "tecnico1", 4.0, "Fumigacion"),
            Trabajo("L004", TipoLabor.COSECHA, 0, "jefe1", 100.0, "Cosecha parcial"),

            Trabajo("L005", TipoLabor.RIEGO, 3, "tecnico1", 2.0, "Riego de quinua"),
            Trabajo("L005", TipoLabor.COSECHA, 0, "tecnico1", 120.0, "Cosecha de quinua"),

            Trabajo("L006", TipoLabor.RIEGO, 30, "tecnico1", 2.0, "Riego inicial"),
            Trabajo("L006", TipoLabor.FUMIGACION, 29, "jefe1", 1.0, "Fumigacion final"),

            Trabajo("L007", TipoLabor.RIEGO, 3, "tecnico1", 3.0, "Riego de uva"),
            Trabajo("L007", TipoLabor.COSECHA, 1, "jefe1", 140.0, "Cosecha de uva"),
            Trabajo("L007", TipoLabor.FUMIGACION, 0, "tecnico1", 3.0, "Prevencion"),

            Trabajo("L008", TipoLabor.RIEGO, 2, "tecnico1", 4.0, "Riego de maiz"),
            Trabajo("L008", TipoLabor.FUMIGACION, 1, "tecnico1", 7.0, "Control de plagas")
        )

        datos.forEachIndexed { indice, trabajo ->
            labores.save(
                LaborEntidad(
                    lote = lotePorCodigo.getValue(trabajo.loteCodigo),
                    tipo = trabajo.tipo,
                    fecha = hoy.minusDays(trabajo.dias),
                    responsable = trabajo.responsable,
                    cantidad = trabajo.cantidad,
                    observacion = trabajo.observacion,
                    registradaEn = ahora.minusHours(indice.toLong() + 1)
                )
            )
        }
    }

    private data class Trabajo(
        val loteCodigo: String,
        val tipo: TipoLabor,
        val dias: Long,
        val responsable: String,
        val cantidad: Double,
        val observacion: String
    )
}
