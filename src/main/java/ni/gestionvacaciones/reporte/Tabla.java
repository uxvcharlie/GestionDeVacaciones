package ni.gestionvacaciones.reporte;

import java.util.List;

/**
 * Una tabla de un reporte: nombre, encabezados y filas.
 *
 * <p>Es el formato intermedio: los reportes arman tablas, y después las mismas
 * tablas se escriben como Excel o como CSV. Así los dos formatos nunca dicen
 * cosas distintas.</p>
 *
 * <p>Cada celda es un {@code String}, un número entero, una {@code LocalDate}
 * o {@code null}. Nunca decimales: el tiempo va en minutos enteros y en texto.</p>
 */
public record Tabla(String nombre, List<String> encabezados, List<List<Object>> filas) {
}
