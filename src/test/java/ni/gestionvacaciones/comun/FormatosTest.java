package ni.gestionvacaciones.comun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/** Formatos de fecha y hora de Nicaragua, sin base de datos. */
class FormatosTest {

    private static final ZoneId MANAGUA = ZoneId.of("America/Managua");

    private final Formatos formatos = new Formatos();

    @Test
    @DisplayName("Las fechas se muestran dd/MM/yyyy")
    void fecha() {
        assertThat(formatos.fecha(LocalDate.of(2026, 9, 21))).isEqualTo("21/09/2026");
        assertThat(formatos.fecha(null)).isEmpty();
    }

    @Test
    @DisplayName("La hora es de 12 horas con am/pm, en la hora de Managua (UTC−6)")
    void fechaHoraEnManagua() {
        assertThat(formatos.fechaHora(Instant.parse("2026-09-21T15:00:00Z"), MANAGUA))
                .isEqualTo("21/09/2026 9:00 am");
        assertThat(formatos.fechaHora(Instant.parse("2026-09-21T18:05:00Z"), MANAGUA))
                .isEqualTo("21/09/2026 12:05 pm");
        // Pasada la medianoche en UTC todavía es el día anterior en Managua.
        assertThat(formatos.fechaHora(Instant.parse("2026-09-22T05:30:00Z"), MANAGUA))
                .isEqualTo("21/09/2026 11:30 pm");
        assertThat(formatos.fechaHora(Instant.parse("2026-09-22T06:30:00Z"), MANAGUA))
                .isEqualTo("22/09/2026 12:30 am");
    }

    @Test
    @DisplayName("Los movimientos que suman llevan +, los que restan llevan −")
    void tiempoConSigno() {
        assertThat(formatos.tiempoConSigno(4800, 8)).isEqualTo("+10 días");
        assertThat(formatos.tiempoConSigno(-480, 8)).isEqualTo("−1 día");
    }
}
