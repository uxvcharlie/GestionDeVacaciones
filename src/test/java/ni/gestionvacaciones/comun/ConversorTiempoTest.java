package ni.gestionvacaciones.comun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas del conversor de minutos. No necesitan base de datos ni Spring:
 * corren en milisegundos.
 */
class ConversorTiempoTest {

    private static final int JORNADA = 8;

    @Test
    @DisplayName("El ejemplo exacto de Brenda: 10 días → 9 días → 8 días y 6 horas")
    void ejemploDeBrenda() {
        int saldo = ConversorTiempo.aMinutos(10, 0, 0, JORNADA);
        assertThat(saldo).isEqualTo(4800);
        assertThat(ConversorTiempo.formatear(saldo, JORNADA)).isEqualTo("10 días");

        saldo -= ConversorTiempo.aMinutos(1, 0, 0, JORNADA);   // 1 día de vacaciones
        assertThat(saldo).isEqualTo(4320);
        assertThat(ConversorTiempo.formatear(saldo, JORNADA)).isEqualTo("9 días");

        saldo -= ConversorTiempo.aMinutos(0, 2, 0, JORNADA);   // cita médica de 2 horas
        assertThat(saldo).isEqualTo(4200);
        assertThat(ConversorTiempo.formatear(saldo, JORNADA)).isEqualTo("8 días y 6 horas");
    }

    @ParameterizedTest(name = "{0} minutos → \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "0      | 0 días",
            "480    | 1 día",
            "960    | 2 días",
            "60     | 1 hora",
            "240    | 4 horas",
            "1      | 1 minuto",
            "90     | 1 hora y 30 minutos",
            "540    | 1 día y 1 hora",
            "1025   | 2 días, 1 hora y 5 minutos",
            "-1500  | −3 días y 1 hora"
    })
    @DisplayName("Formato en lenguaje natural, sin decimales")
    void formatoNatural(int minutos, String esperado) {
        assertThat(ConversorTiempo.formatear(minutos, JORNADA)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "{0} minutos en horas → \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "0    | 0 horas",
            "45   | 45 minutos",
            "60   | 1 hora",
            "90   | 1 hora y 30 minutos",
            "120  | 2 horas",
            "480  | 8 horas",
            "600  | 10 horas"
    })
    @DisplayName("Citas y permisos se muestran en horas y minutos, nunca en días")
    void formatoEnHoras(int minutos, String esperado) {
        assertThat(ConversorTiempo.formatearHoras(minutos)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Medio día con jornada de 8 horas son 4 horas")
    void medioDia() {
        assertThat(ConversorTiempo.aMinutos(0, 4, 0, JORNADA)).isEqualTo(240);
        assertThat(ConversorTiempo.formatear(240, JORNADA)).isEqualTo("4 horas");
    }

    @Test
    @DisplayName("Cambiar la jornada cambia cómo se MUESTRA, nunca los minutos guardados")
    void cambiarJornadaSoloCambiaLaPresentacion() {
        int guardado = 4800;
        assertThat(ConversorTiempo.formatear(guardado, 8)).isEqualTo("10 días");
        assertThat(ConversorTiempo.formatear(guardado, 6)).isEqualTo("13 días y 2 horas");
    }

    @Test
    @DisplayName("No se aceptan cantidades negativas ni jornadas imposibles")
    void rechazaValoresInvalidos() {
        assertThatThrownBy(() -> ConversorTiempo.aMinutos(-1, 0, 0, JORNADA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConversorTiempo.formatear(100, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConversorTiempo.formatear(100, 25))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Un número que no cabe en un entero da error, no un saldo equivocado")
    void desbordamientoDaError() {
        assertThatThrownBy(() -> ConversorTiempo.aMinutos(Integer.MAX_VALUE, 0, 0, JORNADA))
                .isInstanceOf(ArithmeticException.class);
    }
}
