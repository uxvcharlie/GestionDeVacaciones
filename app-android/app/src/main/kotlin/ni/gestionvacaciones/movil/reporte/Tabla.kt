package ni.gestionvacaciones.movil.reporte

/**
 * Una tabla lista para escribir como CSV o como hoja de Excel.
 *
 * Las celdas pueden ser texto, números (Int o Long), fechas (LocalDate) o
 * null (celda vacía).
 */
data class Tabla(val nombre: String, val encabezados: List<String>, val filas: List<List<Any?>>)
