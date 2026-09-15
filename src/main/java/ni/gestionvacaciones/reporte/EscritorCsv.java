package ni.gestionvacaciones.reporte;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Escribe una tabla como CSV que Excel abre bien con tildes y eñes.
 *
 * <ul>
 *   <li>UTF-8 con BOM al principio: sin esa marca, Excel muestra "VÃ¡squez".</li>
 *   <li>Separado por comas y con comillas donde hace falta (norma RFC 4180).</li>
 *   <li><b>Protección contra inyección de fórmulas:</b> si un texto empieza con
 *       =, +, -, @, tabulador o retorno, se le antepone un apóstrofo. Si no,
 *       un motivo escrito como "=HIPERVINCULO(...)" se ejecutaría como fórmula
 *       al abrir el archivo en Excel.</li>
 * </ul>
 */
public final class EscritorCsv {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String MARCA_UTF8 = "﻿";

    private EscritorCsv() {
    }

    public static byte[] escribir(Tabla tabla) {
        StringBuilder texto = new StringBuilder(MARCA_UTF8);
        linea(texto, tabla.encabezados());
        for (List<Object> fila : tabla.filas()) {
            linea(texto, fila);
        }
        return texto.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void linea(StringBuilder texto, List<?> celdas) {
        for (int i = 0; i < celdas.size(); i++) {
            if (i > 0) {
                texto.append(',');
            }
            texto.append(celda(celdas.get(i)));
        }
        texto.append("\r\n");
    }

    static String celda(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Number numero) {
            return numero.toString();
        }
        if (valor instanceof LocalDate fecha) {
            return fecha.format(FECHA);
        }
        String texto = neutralizarFormula(valor.toString());
        boolean necesitaComillas = texto.contains(",") || texto.contains("\"") || texto.contains("\n")
                || texto.contains("\r") || texto.startsWith(" ") || texto.endsWith(" ");
        return necesitaComillas ? "\"" + texto.replace("\"", "\"\"") + "\"" : texto;
    }

    private static String neutralizarFormula(String texto) {
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            return "'" + texto;
        }
        return texto;
    }
}
