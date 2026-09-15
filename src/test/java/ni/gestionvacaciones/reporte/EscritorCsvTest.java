package ni.gestionvacaciones.reporte;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** CSV: tildes, comillas y protección contra fórmulas. Sin base de datos. */
class EscritorCsvTest {

    @Test
    @DisplayName("Empieza con la marca UTF-8 para que Excel muestre bien las tildes")
    void marcaUtf8() {
        byte[] bytes = EscritorCsv.escribir(new Tabla("T", List.of("Nombre"), List.of(List.of("Brenda Vásquez"))));
        String texto = new String(bytes, StandardCharsets.UTF_8);

        assertThat(texto).startsWith("﻿").contains("Brenda Vásquez");
    }

    @Test
    @DisplayName("Comas, comillas y saltos de línea van entre comillas")
    void comillas() {
        assertThat(EscritorCsv.celda("González, María")).isEqualTo("\"González, María\"");
        assertThat(EscritorCsv.celda("Dijo \"hola\"")).isEqualTo("\"Dijo \"\"hola\"\"\"");
        assertThat(EscritorCsv.celda("dos\nlíneas")).isEqualTo("\"dos\nlíneas\"");
        assertThat(EscritorCsv.celda("simple")).isEqualTo("simple");
    }

    @Test
    @DisplayName("Un texto que parece fórmula se neutraliza; los números negativos no")
    void formulas() {
        assertThat(EscritorCsv.celda("=HIPERVINCULO(\"http://malo\")")).startsWith("\"'=");
        assertThat(EscritorCsv.celda("+1")).isEqualTo("'+1");
        assertThat(EscritorCsv.celda("@SUMA(A1)")).isEqualTo("'@SUMA(A1)");
        assertThat(EscritorCsv.celda(-480)).isEqualTo("-480");
    }

    @Test
    @DisplayName("Fechas en dd/MM/yyyy y celdas vacías sin texto")
    void fechasYVacios() {
        String texto = new String(EscritorCsv.escribir(new Tabla("T", List.of("A", "B", "C"),
                List.of(Arrays.asList(LocalDate.of(2026, 9, 21), null, 4800)))), StandardCharsets.UTF_8);

        assertThat(texto).contains("A,B,C\r\n").contains("21/09/2026,,4800\r\n");
    }
}
