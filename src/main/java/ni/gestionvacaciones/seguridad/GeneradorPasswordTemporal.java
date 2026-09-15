package ni.gestionvacaciones.seguridad;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Genera la contraseña temporal que Brenda le entrega a una persona nueva.
 *
 * <p>Formato: tres grupos de cuatro, como {@code k7mp-x2qd-9tha}. Es fácil de
 * dictar y de copiar a mano: no usa letras que se confunden (i, l, o) ni los
 * números 0 y 1. Son 31 símbolos en 12 posiciones: unas 10<sup>18</sup>
 * combinaciones, imposibles de adivinar con 5 intentos cada 15 minutos.</p>
 *
 * <p>{@link SecureRandom} y no {@code Random}: el azar común es predecible, y
 * con una contraseña eso no se puede permitir.</p>
 */
@Component
public class GeneradorPasswordTemporal {

    private static final char[] SIMBOLOS = "abcdefghjkmnpqrstuvwxyz23456789".toCharArray();

    private final SecureRandom azar = new SecureRandom();
    private final PoliticaPassword politica;

    public GeneradorPasswordTemporal(PoliticaPassword politica) {
        this.politica = politica;
    }

    /** Siempre devuelve una contraseña que cumple la política para ese usuario. */
    public String generar(String username) {
        while (true) {
            String candidata = grupo() + "-" + grupo() + "-" + grupo();
            if (politica.revisar(candidata, username).isEmpty()) {
                return candidata;
            }
        }
    }

    private String grupo() {
        StringBuilder grupo = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            grupo.append(SIMBOLOS[azar.nextInt(SIMBOLOS.length)]);
        }
        return grupo.toString();
    }
}
