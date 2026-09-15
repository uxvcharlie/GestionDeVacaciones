package ni.gestionvacaciones.reporte;

import jakarta.servlet.http.HttpServletRequest;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.DateTimeException;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Pantalla de reportes y descargas en Excel o CSV. */
@Controller
public class ControladorReportes {

    private static final Locale ESPANOL = Locale.forLanguageTag("es-NI");

    private final ServicioReportes servicioReportes;
    private final ServicioParametros parametros;
    private final ServicioUsuario servicioUsuario;

    public ControladorReportes(ServicioReportes servicioReportes,
                               ServicioParametros parametros,
                               ServicioUsuario servicioUsuario) {
        this.servicioReportes = servicioReportes;
        this.parametros = parametros;
        this.servicioUsuario = servicioUsuario;
    }

    /** Mes y año con listas desplegables: funcionan igual en todos los navegadores. */
    @GetMapping("/reportes")
    public String pantalla(Model modelo) {
        YearMonth actual = YearMonth.now(parametros.zonaHoraria());
        modelo.addAttribute("mesActual", actual.getMonthValue());
        modelo.addAttribute("anioActual", actual.getYear());
        modelo.addAttribute("meses", Stream.of(Month.values())
                .map(m -> new OpcionMes(m.getValue(), capitalizar(m.getDisplayName(TextStyle.FULL_STANDALONE, ESPANOL))))
                .toList());
        modelo.addAttribute("anios", List.of(actual.getYear() - 2, actual.getYear() - 1, actual.getYear(), actual.getYear() + 1));
        return "reportes";
    }

    @GetMapping("/reportes/mes")
    public ResponseEntity<byte[]> mes(@RequestParam int anio,
                                      @RequestParam int mes,
                                      @RequestParam(defaultValue = "excel") String formato,
                                      Principal principal,
                                      HttpServletRequest peticion) {
        YearMonth periodo;
        try {
            periodo = YearMonth.of(anio, mes);
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return descargar(servicioReportes.mes(periodo, formato(formato), idUsuario(principal), peticion.getRemoteAddr()));
    }

    @GetMapping("/reportes/funcionario/{id}")
    public ResponseEntity<byte[]> funcionario(@PathVariable Long id,
                                              @RequestParam(defaultValue = "excel") String formato,
                                              Principal principal,
                                              HttpServletRequest peticion) {
        return descargar(servicioReportes.funcionario(id, formato(formato), idUsuario(principal), peticion.getRemoteAddr()));
    }

    /** Una opción de la lista de meses: 9 → "Septiembre". */
    public record OpcionMes(int numero, String nombre) {
    }

    private static ResponseEntity<byte[]> descargar(ServicioReportes.Archivo archivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(archivo.contenido());
    }

    private static ServicioReportes.Formato formato(String texto) {
        try {
            return ServicioReportes.Formato.desde(texto);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private Long idUsuario(Principal principal) {
        return servicioUsuario.buscarPorUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario de la sesión."))
                .getId();
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase(ESPANOL) + texto.substring(1);
    }
}
