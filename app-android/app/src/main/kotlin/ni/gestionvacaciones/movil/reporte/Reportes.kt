package ni.gestionvacaciones.movil.reporte

import ni.gestionvacaciones.movil.datos.FuncionarioEntidad
import ni.gestionvacaciones.movil.datos.MovimientoEntidad
import ni.gestionvacaciones.movil.datos.SolicitudConFuncionario
import ni.gestionvacaciones.movil.datos.SolicitudEntidad
import ni.gestionvacaciones.movil.dominio.ConversorTiempo
import ni.gestionvacaciones.movil.dominio.EstadoSolicitud
import ni.gestionvacaciones.movil.dominio.Fechas
import ni.gestionvacaciones.movil.dominio.Texto
import ni.gestionvacaciones.movil.dominio.TipoSolicitud
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

enum class FormatoReporte(val extension: String, val tipoContenido: String, val etiqueta: String) {
    EXCEL("xlsx", EscritorExcel.TIPO_CONTENIDO, "Excel"),
    CSV("csv", "text/csv", "CSV"),
}

/** Un archivo listo para compartir. */
class ArchivoGenerado(val nombre: String, val tipoContenido: String, val contenido: ByteArray)

/**
 * Arma los reportes: las mismas hojas y columnas que la web.
 *
 * - De un funcionario. Excel: hojas Resumen, Solicitudes y Movimientos.
 *   CSV: la tabla de solicitudes.
 * - Del mes: todas las solicitudes que EMPIEZAN en ese mes, también las
 *   anuladas (marcadas). Excel: Resumen y Solicitudes. CSV: solicitudes.
 */
object Reportes {

    private val ENCABEZADOS_SOLICITUD = listOf(
        "Tipo", "Desde", "Hasta", "Tiempo", "Minutos", "Estado", "Motivo",
        "Registrada el", "Anulada el", "Motivo de la anulación",
    )

    fun deFuncionario(
        f: FuncionarioEntidad,
        solicitudes: List<SolicitudEntidad>,
        movimientos: List<MovimientoEntidad>,
        jornada: Int,
        zona: ZoneId,
        ahora: Instant,
        formato: FormatoReporte,
    ): ArchivoGenerado {
        val tablaSolicitudes = Tabla("Solicitudes", ENCABEZADOS_SOLICITUD, solicitudes.map { celdas(it, jornada, zona) })
        val hoy = ahora.atZone(zona).toLocalDate()
        val nombre = "funcionario-${Texto.nombreDeArchivo(f.nombreCompleto)}-$hoy.${formato.extension}"

        if (formato == FormatoReporte.CSV) {
            return ArchivoGenerado(nombre, formato.tipoContenido, EscritorCsv.escribir(tablaSolicitudes))
        }
        val anio = hoy.year
        val resumen = buildList<List<Any?>> {
            add(listOf("Funcionario", f.nombreCompleto))
            add(listOf("Cargo", f.cargo))
            add(listOf("Área o dependencia", f.areaODependencia))
            add(listOf("Fecha de ingreso", f.fechaIngreso))
            add(listOf("Estado", if (f.activo) "Activo" else "De baja"))
            add(listOf("Saldo disponible", ConversorTiempo.formatear(f.saldoMinutos, jornada)))
            add(listOf("Saldo en minutos", f.saldoMinutos))
            add(listOf("Jornada", "$jornada horas"))
            TipoSolicitud.entries.forEach { tipo ->
                val minutos = solicitudes
                    .filter { it.tipo == tipo && it.estado == EstadoSolicitud.REGISTRADA && it.fechaInicio.year == anio }
                    .sumOf { it.minutos }
                add(listOf("${tipo.etiqueta} en $anio", tipo.formatear(minutos, jornada)))
            }
            add(listOf("Reporte generado el", Fechas.fechaHora(ahora, zona)))
        }
        val tablaMovimientos = Tabla(
            "Movimientos",
            listOf("Fecha y hora", "Movimiento", "Tiempo", "Minutos", "Saldo después", "Saldo después (minutos)", "Descripción"),
            movimientos.map { m ->
                listOf(
                    Fechas.fechaHora(m.creadoEn, zona), m.tipo.etiqueta, ConversorTiempo.formatearConSigno(m.minutos, jornada),
                    m.minutos, ConversorTiempo.formatear(m.saldoResultante, jornada), m.saldoResultante, m.descripcion,
                )
            },
        )
        return ArchivoGenerado(
            nombre, formato.tipoContenido,
            EscritorExcel.escribir(listOf(Tabla("Resumen", listOf("Dato", "Valor"), resumen), tablaSolicitudes, tablaMovimientos)),
        )
    }

    fun delMes(
        mes: YearMonth,
        filas: List<SolicitudConFuncionario>,
        jornada: Int,
        zona: ZoneId,
        ahora: Instant,
        formato: FormatoReporte,
    ): ArchivoGenerado {
        val tablaSolicitudes = Tabla(
            "Solicitudes",
            listOf("Funcionario", "Cargo", "Área o dependencia") + ENCABEZADOS_SOLICITUD,
            filas.map { listOf(it.nombreFuncionario, it.cargoFuncionario, it.areaFuncionario) + celdas(it.solicitud, jornada, zona) },
        )
        val nombre = "solicitudes-$mes.${formato.extension}"
        if (formato == FormatoReporte.CSV) {
            return ArchivoGenerado(nombre, formato.tipoContenido, EscritorCsv.escribir(tablaSolicitudes))
        }

        val resumen = mutableListOf<List<Any?>>(listOf("Mes", Fechas.mes(mes)))
        var totalCantidad = 0
        var totalMinutos = 0
        TipoSolicitud.entries.forEach { tipo ->
            val delTipo = filas.filter { it.solicitud.tipo == tipo && it.solicitud.estado == EstadoSolicitud.REGISTRADA }
            val cantidad = delTipo.size
            val minutos = delTipo.sumOf { it.solicitud.minutos }
            totalCantidad += cantidad
            totalMinutos += minutos
            resumen += listOf(tipo.etiqueta, "${solicitudes(cantidad)} · ${tipo.formatear(minutos, jornada)}", minutos)
        }
        resumen += listOf("Total", "${solicitudes(totalCantidad)} · ${ConversorTiempo.formatear(totalMinutos, jornada)}", totalMinutos)
        resumen += listOf("Anuladas (no se suman)", filas.count { it.solicitud.estado == EstadoSolicitud.ANULADA })
        resumen += listOf("Reporte generado el", Fechas.fechaHora(ahora, zona))

        return ArchivoGenerado(
            nombre, formato.tipoContenido,
            EscritorExcel.escribir(listOf(Tabla("Resumen", listOf("Dato", "Valor", "Minutos"), resumen), tablaSolicitudes)),
        )
    }

    private fun solicitudes(n: Int) = if (n == 1) "1 solicitud" else "$n solicitudes"

    private fun celdas(s: SolicitudEntidad, jornada: Int, zona: ZoneId): List<Any?> = listOf<Any?>(
        s.tipo.etiqueta,
        s.fechaInicio,
        s.fechaFin,
        s.tipo.formatear(s.minutos, jornada),
        s.minutos,
        s.estado.etiqueta,
        s.motivo,
        Fechas.fechaHora(s.creadoEn, zona),
        s.anuladoEn?.let { Fechas.fechaHora(it, zona) },
        s.motivoAnulacion,
    )
}
