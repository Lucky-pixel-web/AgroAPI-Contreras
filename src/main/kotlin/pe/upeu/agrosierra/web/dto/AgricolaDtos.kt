
package pe.upeu.agrosierra.web.dto

import org.springframework.data.domain.Page
import pe.upeu.agrosierra.dominio.*

data class PaginaRespuesta<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
)

fun <T : Any> Page<T>.aRespuesta(): PaginaRespuesta<T> =
    PaginaRespuesta(
        contenido = content,
        pagina = number,
        tamanio = size,
        totalElementos = totalElements,
        totalPaginas = totalPages,
        ultima = isLast
    )

data class LoteDto(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val hectareas: Double,
    val cultivo: String,
    val valle: String,
    val campaniaCerrada: Boolean,
    val dosisMaximaPorHectarea: Double
)

fun LoteEntidad.aDto() = LoteDto(
    id = requireNotNull(id),
    codigo = codigo,
    nombre = nombre,
    hectareas = hectareas,
    cultivo = cultivo,
    valle = valle,
    campaniaCerrada = campaniaCerrada == 1,
    dosisMaximaPorHectarea = dosisMaximaPorHectarea
)


data class LaborSolicitud(
    val loteId: Long? = null,
    val tipo: TipoLabor? = null,
    val fecha: String? = null,
    val cantidad: Double? = null,
    val observacion: String? = null,
    val cantidadJornales: Int? = null
)

data class LaborDto(
    val id: Long,
    val loteId: Long,
    val tipo: TipoLabor,
    val fecha: String,
    val responsable: String,
    val cantidad: Double,
    val observacion: String,
    val pendiente: Boolean = false,
    val cantidadJornales: Int
)

fun LaborEntidad.aDto() = LaborDto(
    id = requireNotNull(id),
    loteId = requireNotNull(lote?.id),
    tipo = tipo,
    fecha = fecha.toString(),
    responsable = responsable,
    cantidad = cantidad,
    observacion = observacion,
    pendiente = false,
    cantidadJornales = cantidadJornales
)

