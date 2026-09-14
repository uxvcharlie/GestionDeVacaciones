package ni.gestionvacaciones.web;

import java.time.LocalTime;

/**
 * El saludo del tablero según la hora.
 *
 * <ul>
 *   <li>5:00 a 11:59 → "Buenos días"</li>
 *   <li>12:00 a 18:59 → "Buenas tardes"</li>
 *   <li>19:00 a 4:59 → "Buenas noches"</li>
 * </ul>
 *
 * <p>Recibe la hora ya convertida a la zona de Managua. Se calcula en el
 * servidor, nunca con la hora del navegador, que puede estar mal configurada.</p>
 */
public final class Saludo {

    private Saludo() {
    }

    public static String para(LocalTime hora) {
        int h = hora.getHour();
        if (h >= 5 && h < 12) {
            return "Buenos días";
        }
        if (h >= 12 && h < 19) {
            return "Buenas tardes";
        }
        return "Buenas noches";
    }
}
