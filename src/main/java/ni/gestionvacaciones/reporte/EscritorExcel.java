package ni.gestionvacaciones.reporte;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Escribe tablas como un archivo Excel (.xlsx), una hoja por tabla.
 *
 * <p>Los textos se guardan como texto, nunca como fórmulas, así que en Excel
 * no hay riesgo de inyección. Las fechas se guardan como fechas reales con
 * formato dd/MM/yyyy, para que se puedan ordenar y filtrar.</p>
 */
public final class EscritorExcel {

    public static final String TIPO_CONTENIDO =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final int ANCHO_MINIMO = 10;
    private static final int ANCHO_MAXIMO = 60;

    private EscritorExcel() {
    }

    public static byte[] escribir(List<Tabla> tablas) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            Workbook libro = new Workbook(salida, "GestionDeVacaciones", "1.0");
            for (Tabla tabla : tablas) {
                escribirHoja(libro.newWorksheet(tabla.nombre()), tabla);
            }
            libro.finish();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo Excel.", e);
        }
        return salida.toByteArray();
    }

    private static void escribirHoja(Worksheet hoja, Tabla tabla) {
        int columnas = tabla.encabezados().size();
        int[] anchos = new int[columnas];

        for (int c = 0; c < columnas; c++) {
            String encabezado = tabla.encabezados().get(c);
            hoja.value(0, c, encabezado);
            hoja.style(0, c).bold().set();
            anchos[c] = encabezado.length();
        }

        for (int f = 0; f < tabla.filas().size(); f++) {
            List<Object> fila = tabla.filas().get(f);
            int renglon = f + 1;
            for (int c = 0; c < columnas && c < fila.size(); c++) {
                Object valor = fila.get(c);
                if (valor instanceof Number numero) {
                    hoja.value(renglon, c, numero);
                    anchos[c] = Math.max(anchos[c], numero.toString().length());
                } else if (valor instanceof LocalDate fecha) {
                    hoja.value(renglon, c, fecha);
                    hoja.style(renglon, c).format("dd/MM/yyyy").set();
                    anchos[c] = Math.max(anchos[c], 10);
                } else if (valor != null) {
                    String texto = valor.toString();
                    hoja.value(renglon, c, texto);
                    anchos[c] = Math.max(anchos[c], texto.length());
                }
            }
        }

        for (int c = 0; c < columnas; c++) {
            hoja.width(c, Math.min(Math.max(anchos[c] + 2, ANCHO_MINIMO), ANCHO_MAXIMO));
        }
    }
}
