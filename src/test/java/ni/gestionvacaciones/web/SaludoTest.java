package ni.gestionvacaciones.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/** El saludo en cada corte de horario: un minuto antes y justo en el cambio. */
class SaludoTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "00:00, Buenas noches",
            "04:59, Buenas noches",
            "05:00, Buenos días",
            "11:59, Buenos días",
            "12:00, Buenas tardes",
            "18:59, Buenas tardes",
            "19:00, Buenas noches",
            "23:59, Buenas noches"
    })
    @DisplayName("Saludo según la hora")
    void saludoSegunLaHora(String hora, String esperado) {
        assertThat(Saludo.para(LocalTime.parse(hora))).isEqualTo(esperado);
    }
}
