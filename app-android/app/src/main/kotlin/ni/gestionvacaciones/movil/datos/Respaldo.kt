package ni.gestionvacaciones.movil.datos

import ni.gestionvacaciones.movil.dominio.AccionBitacora
import ni.gestionvacaciones.movil.dominio.EstadoSolicitud
import ni.gestionvacaciones.movil.dominio.ReglaDeNegocioException
import ni.gestionvacaciones.movil.dominio.TipoMovimiento
import ni.gestionvacaciones.movil.dominio.TipoSolicitud
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

/** Todo lo que hay en la base, listo para guardar o recién leído de un respaldo. */
data class ContenidoRespaldo(
    val parametros: List<ParametroEntidad>,
    val funcionarios: List<FuncionarioEntidad>,
    val solicitudes: List<SolicitudEntidad>,
    val movimientos: List<MovimientoEntidad>,
    val bitacora: List<BitacoraEntidad>,
)

/**
 * El archivo de respaldo: un JSON con TODOS los datos de la app.
 *
 * Como los datos viven solo en el celular, este archivo es lo que salva el
 * trabajo si el teléfono se pierde o se rompe. Se guarda en Drive, se manda
 * por WhatsApp o se copia a la computadora.
 *
 * ## Al restaurar, se desconfía de todo
 * Antes de tocar la base, el archivo se lee entero y se revisa que:
 * - sea un respaldo de esta app y de una versión conocida;
 * - cada solicitud y movimiento pertenezca a un funcionario que existe;
 * - el saldo de cada funcionario sea exactamente la suma de sus movimientos.
 * Si algo no cuadra, se rechaza y los datos actuales quedan intactos.
 *
 * El PIN NO va en el respaldo: quien restaura en un celular nuevo crea el suyo.
 */
object Respaldo {

    const val FORMATO = "gestion-vacaciones-respaldo"
    const val VERSION = 1

    fun escribir(contenido: ContenidoRespaldo, creadoEn: Instant): String {
        val raiz = JSONObject()
        raiz.put("formato", FORMATO)
        raiz.put("version", VERSION)
        raiz.put("creado_en", creadoEn.toString())

        raiz.put("parametros", JSONObject().apply { contenido.parametros.forEach { put(it.clave, it.valor) } })
        raiz.put("funcionarios", JSONArray().apply {
            contenido.funcionarios.forEach { f ->
                put(JSONObject().apply {
                    put("id", f.id)
                    put("nombre_completo", f.nombreCompleto)
                    putOpt("cargo", f.cargo)
                    putOpt("area_o_dependencia", f.areaODependencia)
                    putOpt("fecha_ingreso", f.fechaIngreso?.toString())
                    put("saldo_minutos", f.saldoMinutos)
                    put("activo", f.activo)
                    put("creado_en", f.creadoEn.toString())
                    put("actualizado_en", f.actualizadoEn.toString())
                })
            }
        })
        raiz.put("solicitudes", JSONArray().apply {
            contenido.solicitudes.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("funcionario_id", s.funcionarioId)
                    put("tipo", s.tipo.name)
                    put("fecha_inicio", s.fechaInicio.toString())
                    put("fecha_fin", s.fechaFin.toString())
                    put("minutos", s.minutos)
                    putOpt("motivo", s.motivo)
                    put("estado", s.estado.name)
                    put("creado_en", s.creadoEn.toString())
                    putOpt("anulado_en", s.anuladoEn?.toString())
                    putOpt("motivo_anulacion", s.motivoAnulacion)
                })
            }
        })
        raiz.put("movimientos", JSONArray().apply {
            contenido.movimientos.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("funcionario_id", m.funcionarioId)
                    putOpt("solicitud_id", m.solicitudId)
                    put("tipo", m.tipo.name)
                    put("minutos", m.minutos)
                    put("saldo_resultante", m.saldoResultante)
                    putOpt("descripcion", m.descripcion)
                    put("creado_en", m.creadoEn.toString())
                })
            }
        })
        raiz.put("bitacora", JSONArray().apply {
            contenido.bitacora.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("accion", b.accion.name)
                    putOpt("detalle", b.detalle)
                    put("creado_en", b.creadoEn.toString())
                })
            }
        })
        return raiz.toString(1)
    }

    /**
     * Lee y valida un respaldo. No toca la base de datos.
     * @throws ReglaDeNegocioException con un mensaje claro si el archivo no sirve
     */
    fun leer(texto: String): ContenidoRespaldo {
        val contenido = try {
            val raiz = JSONObject(texto)
            if (raiz.optString("formato") != FORMATO) {
                throw ReglaDeNegocioException("Ese archivo no es un respaldo de esta app.")
            }
            val version = raiz.optInt("version", -1)
            if (version != VERSION) {
                throw ReglaDeNegocioException(
                    "Ese respaldo es de otra versión de la app ($version) y esta no sabe leerlo. " +
                        "Actualizá la app y volvé a intentar.",
                )
            }
            ContenidoRespaldo(
                parametros = raiz.getJSONObject("parametros").let { o ->
                    o.keys().asSequence().map { ParametroEntidad(it, o.getString(it)) }.toList()
                },
                funcionarios = lista(raiz, "funcionarios") { o ->
                    FuncionarioEntidad(
                        id = o.getLong("id"),
                        nombreCompleto = o.getString("nombre_completo"),
                        cargo = texto(o, "cargo"),
                        areaODependencia = texto(o, "area_o_dependencia"),
                        fechaIngreso = texto(o, "fecha_ingreso")?.let(LocalDate::parse),
                        saldoMinutos = o.getInt("saldo_minutos"),
                        activo = o.getBoolean("activo"),
                        creadoEn = Instant.parse(o.getString("creado_en")),
                        actualizadoEn = Instant.parse(o.getString("actualizado_en")),
                    )
                },
                solicitudes = lista(raiz, "solicitudes") { o ->
                    SolicitudEntidad(
                        id = o.getLong("id"),
                        funcionarioId = o.getLong("funcionario_id"),
                        tipo = TipoSolicitud.valueOf(o.getString("tipo")),
                        fechaInicio = LocalDate.parse(o.getString("fecha_inicio")),
                        fechaFin = LocalDate.parse(o.getString("fecha_fin")),
                        minutos = o.getInt("minutos"),
                        motivo = texto(o, "motivo"),
                        estado = EstadoSolicitud.valueOf(o.getString("estado")),
                        creadoEn = Instant.parse(o.getString("creado_en")),
                        anuladoEn = texto(o, "anulado_en")?.let(Instant::parse),
                        motivoAnulacion = texto(o, "motivo_anulacion"),
                    )
                },
                movimientos = lista(raiz, "movimientos") { o ->
                    MovimientoEntidad(
                        id = o.getLong("id"),
                        funcionarioId = o.getLong("funcionario_id"),
                        solicitudId = if (o.isNull("solicitud_id")) null else o.getLong("solicitud_id"),
                        tipo = TipoMovimiento.valueOf(o.getString("tipo")),
                        minutos = o.getInt("minutos"),
                        saldoResultante = o.getInt("saldo_resultante"),
                        descripcion = texto(o, "descripcion"),
                        creadoEn = Instant.parse(o.getString("creado_en")),
                    )
                },
                bitacora = lista(raiz, "bitacora") { o ->
                    BitacoraEntidad(
                        id = o.getLong("id"),
                        // Si una versión futura agrega una acción nueva, no se rechaza el respaldo entero.
                        accion = AccionBitacora.entries.firstOrNull { it.name == o.getString("accion") }
                            ?: AccionBitacora.PARAMETRO_CAMBIADO,
                        detalle = texto(o, "detalle"),
                        creadoEn = Instant.parse(o.getString("creado_en")),
                    )
                },
            )
        } catch (e: ReglaDeNegocioException) {
            throw e
        } catch (e: JSONException) {
            throw ReglaDeNegocioException("El archivo está dañado o incompleto: no se puede leer como respaldo.")
        } catch (e: RuntimeException) {
            // Fechas mal escritas, tipos desconocidos...
            throw ReglaDeNegocioException("El archivo tiene datos que no se entienden: no parece un respaldo válido.")
        }
        verificar(contenido)
        return contenido
    }

    /**
     * Las mismas garantías que la base de datos de la web aseguraba con
     * restricciones CHECK y claves foráneas.
     */
    fun verificar(c: ContenidoRespaldo) {
        val idsFuncionarios = c.funcionarios.map { it.id }.toSet()
        val idsSolicitudes = c.solicitudes.map { it.id }.toSet()
        if (idsFuncionarios.size != c.funcionarios.size || idsSolicitudes.size != c.solicitudes.size) {
            throw ReglaDeNegocioException("El respaldo tiene registros repetidos: no se puede restaurar.")
        }
        c.solicitudes.forEach { s ->
            if (s.funcionarioId !in idsFuncionarios) incompleto()
            if (s.minutos <= 0 || s.fechaFin.isBefore(s.fechaInicio)) incompleto()
            val anulada = s.estado == EstadoSolicitud.ANULADA
            if (anulada != (s.anuladoEn != null) || anulada != (s.motivoAnulacion != null)) incompleto()
        }
        c.movimientos.forEach { m ->
            if (m.funcionarioId !in idsFuncionarios) incompleto()
            if (m.solicitudId != null && m.solicitudId !in idsSolicitudes) incompleto()
            if (m.minutos == 0) incompleto()
        }
        // El control más importante: el saldo es la suma de su historial.
        val sumas = c.movimientos.groupBy { it.funcionarioId }.mapValues { (_, l) -> l.sumOf { it.minutos.toLong() } }
        c.funcionarios.forEach { f ->
            if ((sumas[f.id] ?: 0L) != f.saldoMinutos.toLong()) {
                throw ReglaDeNegocioException(
                    "En el respaldo, el saldo de ${f.nombreCompleto} no coincide con su historial de movimientos. " +
                        "El archivo fue modificado o está incompleto: no se puede restaurar.",
                )
            }
        }
    }

    private fun incompleto(): Nothing =
        throw ReglaDeNegocioException("El respaldo está incompleto o fue modificado: no se puede restaurar.")

    private fun <T> lista(raiz: JSONObject, clave: String, leer: (JSONObject) -> T): List<T> {
        val arreglo = raiz.getJSONArray(clave)
        return (0 until arreglo.length()).map { leer(arreglo.getJSONObject(it)) }
    }

    private fun texto(o: JSONObject, clave: String): String? = if (o.isNull(clave)) null else o.getString(clave)
}
