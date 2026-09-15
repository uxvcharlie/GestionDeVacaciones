package ni.gestionvacaciones.reporte;

import ni.gestionvacaciones.PruebaConPostgres;
import ni.gestionvacaciones.empleado.Empleado;
import ni.gestionvacaciones.empleado.FormularioEmpleado;
import ni.gestionvacaciones.empleado.ServicioEmpleados;
import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import ni.gestionvacaciones.solicitud.FormularioSolicitud;
import ni.gestionvacaciones.solicitud.ServicioSolicitudes;
import ni.gestionvacaciones.solicitud.Solicitud;
import ni.gestionvacaciones.solicitud.TipoSolicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

/** Reportes contra PostgreSQL real: contenido, formato y bitácora. */
class ServicioReportesTest extends PruebaConPostgres {

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Managua"));

    @Autowired
    private ServicioReportes servicioReportes;

    @Autowired
    private ServicioEmpleados servicioEmpleados;

    @Autowired
    private ServicioSolicitudes servicioSolicitudes;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Test
    @DisplayName("Reporte de funcionario en Excel: tres hojas, con saldo, solicitudes y movimientos")
    void funcionarioEnExcel() throws IOException {
        Empleado funcionario = crearFuncionario("Reporte Ángela Núñez", 10);
        registrarVacaciones(funcionario, 5);

        ServicioReportes.Archivo archivo = servicioReportes.funcionario(
                funcionario.getId(), ServicioReportes.Formato.EXCEL, brenda().getId(), null);

        assertThat(archivo.nombre()).startsWith("funcionario-reporte-angela-nunez-").endsWith(".xlsx");
        assertThat(archivo.tipoContenido()).isEqualTo(EscritorExcel.TIPO_CONTENIDO);
        String contenido = textoDelExcel(archivo.contenido());
        assertThat(contenido).contains("Resumen", "Solicitudes", "Movimientos",
                "Reporte Ángela Núñez", "5 días", "Vacaciones", "Saldo inicial", "Descuento por solicitud");
        assertThat(contenido).doesNotContain("5.0 días", "0.625");
    }

    @Test
    @DisplayName("Reporte del mes en CSV: incluye las anuladas marcadas, con encabezados")
    void mesEnCsv() {
        Empleado funcionario = crearFuncionario("Reporte Mes Funcionario", 10);
        Solicitud anulada = registrarVacaciones(funcionario, 2);
        servicioSolicitudes.anular(anulada.getId(), "=CMD() no es fórmula", brenda(), null);
        registrarVacaciones(funcionario, 1);

        ServicioReportes.Archivo archivo = servicioReportes.mes(
                YearMonth.from(HOY), ServicioReportes.Formato.CSV, brenda().getId(), null);

        assertThat(archivo.nombre()).isEqualTo("solicitudes-" + YearMonth.from(HOY) + ".csv");
        String csv = new String(archivo.contenido(), StandardCharsets.UTF_8);
        assertThat(csv).contains("Funcionario,Cargo,Área o dependencia,Tipo,Desde,Hasta,Tiempo,Minutos,Estado");
        assertThat(csv).contains("Reporte Mes Funcionario", "Anulada", "Registrada", "2 días", "960");
        assertThat(csv).contains("'=CMD() no es fórmula").doesNotContain(",=CMD()");
    }

    /**
     * Lee el .xlsx con ZipFile, que usa el índice central del zip, igual que Excel.
     * ZipInputStream no sirve: no entiende entradas que anotan su tamaño al final,
     * que es como las escribe fastexcel.
     */
    private String textoDelExcel(byte[] xlsx) throws IOException {
        Path temporal = Files.createTempFile("reporte-prueba", ".xlsx");
        try {
            Files.write(temporal, xlsx);
            List<String> partes = new ArrayList<>();
            try (ZipFile zip = new ZipFile(temporal.toFile())) {
                assertThat(zip.getEntry("xl/workbook.xml")).as("es un libro de Excel válido").isNotNull();
                Enumeration<? extends ZipEntry> entradas = zip.entries();
                while (entradas.hasMoreElements()) {
                    ZipEntry entrada = entradas.nextElement();
                    if (entrada.getName().endsWith(".xml")) {
                        try (InputStream contenido = zip.getInputStream(entrada)) {
                            partes.add(new String(contenido.readAllBytes(), StandardCharsets.UTF_8));
                        }
                    }
                }
            }
            return decodificarEntidades(String.join("\n", partes));
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    /**
     * fastexcel escribe las letras con tilde como referencias numéricas de XML
     * (por ejemplo "d&#237;as"). Es XML válido y Excel las muestra bien; acá se
     * decodifican para poder comparar el texto.
     */
    private static String decodificarEntidades(String xml) {
        StringBuilder salida = new StringBuilder();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("&#(x?)([0-9a-fA-F]+);").matcher(xml);
        while (m.find()) {
            int codigo = Integer.parseInt(m.group(2), m.group(1).isEmpty() ? 10 : 16);
            m.appendReplacement(salida, java.util.regex.Matcher.quoteReplacement(new String(Character.toChars(codigo))));
        }
        m.appendTail(salida);
        return salida.toString().replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"");
    }

    private Usuario brenda() {
        return usuarioRepositorio.findByUsername("brenda").orElseThrow();
    }

    private Empleado crearFuncionario(String nombre, int dias) {
        FormularioEmpleado formulario = new FormularioEmpleado();
        formulario.setNombreCompleto(nombre);
        formulario.setSaldoDias(dias);
        formulario.setSaldoHoras(0);
        return servicioEmpleados.crear(formulario, brenda().getId());
    }

    private Solicitud registrarVacaciones(Empleado funcionario, int dias) {
        FormularioSolicitud formulario = new FormularioSolicitud();
        formulario.setEmpleadoId(funcionario.getId());
        formulario.setTipo(TipoSolicitud.VACACIONES);
        formulario.setFechaInicio(HOY.withDayOfMonth(1));
        formulario.setFechaFin(HOY.withDayOfMonth(1));
        formulario.setDias(dias);
        return servicioSolicitudes.registrar(formulario, brenda(), null).solicitud();
    }
}
