package ni.gestionvacaciones.comun;

import java.util.ArrayList;
import java.util.List;

/**
 * El corazón del sistema: convierte entre minutos y "días y horas".
 *
 * <h2>Por qué minutos</h2>
 * <p>En la base de datos todo el tiempo es un número entero de minutos. Los
 * días no se guardan nunca: son solo una forma de mostrar esos minutos. Con
 * enteros, sumar y restar da siempre exacto; con decimales, 8.75 días termina
 * siendo 8.749999999998 y eso en un registro laboral es un reclamo.</p>
 *
 * <h2>La conversión</h2>
 * <pre>
 *   minutos_por_dia = horas_por_jornada × 60        (jornada de 8 h → 480)
 *   días            = minutos ÷ minutos_por_dia     (división entera)
 *   resto           = minutos mód minutos_por_dia
 *   horas           = resto ÷ 60                    (división entera)
 *   minutos sueltos = resto mód 60
 * </pre>
 *
 * <h2>El ejemplo de Brenda, con jornada de 8 horas</h2>
 * <pre>
 *   10 días            = 10 × 480          = 4800 minutos  → "10 días"
 *   − 1 día vacaciones = 4800 − 480        = 4320 minutos  → "9 días"
 *   − cita de 2 horas  = 4320 − 120        = 4200 minutos
 *                        4200 ÷ 480 = 8 días, sobran 360 = 6 horas
 *                                                          → "8 días y 6 horas"
 * </pre>
 *
 * <p>La clase no toca la base de datos ni Spring: recibe la jornada como
 * argumento. Por eso se puede probar con pruebas unitarias simples.</p>
 */
public final class ConversorTiempo {

    public static final int MINUTOS_POR_HORA = 60;

    private ConversorTiempo() {
        // Solo métodos estáticos.
    }

    /** Minutos que tiene una jornada completa. */
    public static int minutosPorDia(int horasPorJornada) {
        if (horasPorJornada < 1 || horasPorJornada > 24) {
            throw new IllegalArgumentException("La jornada debe tener entre 1 y 24 horas.");
        }
        return horasPorJornada * MINUTOS_POR_HORA;
    }

    /**
     * Convierte días, horas y minutos a minutos totales.
     *
     * <p>Usa aritmética "exacta": si el resultado no cupiera en un entero, lanza
     * un error en vez de dar un número equivocado en silencio.</p>
     */
    public static int aMinutos(int dias, int horas, int minutos, int horasPorJornada) {
        if (dias < 0 || horas < 0 || minutos < 0) {
            throw new IllegalArgumentException("Los días, las horas y los minutos no pueden ser negativos.");
        }
        int deDias = Math.multiplyExact(dias, minutosPorDia(horasPorJornada));
        int deHoras = Math.multiplyExact(horas, MINUTOS_POR_HORA);
        return Math.addExact(deDias, Math.addExact(deHoras, minutos));
    }

    /**
     * Muestra un saldo en lenguaje natural. Nunca en decimales.
     *
     * <ul>
     *   <li>4800 → "10 días"</li>
     *   <li>4200 → "8 días y 6 horas"</li>
     *   <li>90 → "1 hora y 30 minutos"</li>
     *   <li>0 → "0 días"</li>
     * </ul>
     *
     * <p>Un saldo negativo (solo posible con autorización de un ADMIN) se muestra
     * con el signo menos adelante: "−2 días y 4 horas".</p>
     */
    public static String formatear(int minutos, int horasPorJornada) {
        int minutosDia = minutosPorDia(horasPorJornada);

        // long: Math.abs(Integer.MIN_VALUE) no cabe en un int.
        long total = Math.abs((long) minutos);
        long dias = total / minutosDia;
        long resto = total % minutosDia;
        long horas = resto / MINUTOS_POR_HORA;
        long sueltos = resto % MINUTOS_POR_HORA;

        List<String> partes = new ArrayList<>();
        if (dias > 0) {
            partes.add(dias + (dias == 1 ? " día" : " días"));
        }
        if (horas > 0) {
            partes.add(horas + (horas == 1 ? " hora" : " horas"));
        }
        if (sueltos > 0) {
            partes.add(sueltos + (sueltos == 1 ? " minuto" : " minutos"));
        }
        if (partes.isEmpty()) {
            return "0 días";
        }
        return (minutos < 0 ? "−" : "") + unir(partes);
    }

    /** "a" · "a y b" · "a, b y c" */
    private static String unir(List<String> partes) {
        if (partes.size() == 1) {
            return partes.get(0);
        }
        String inicio = String.join(", ", partes.subList(0, partes.size() - 1));
        return inicio + " y " + partes.get(partes.size() - 1);
    }
}
