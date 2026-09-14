package ni.gestionvacaciones.seguridad;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Las reglas que debe cumplir una contraseña, en un solo lugar.
 *
 * <p>La política es a propósito simple: <b>largo mínimo de 12 caracteres</b> y
 * que no sea una contraseña obvia. No exigimos "una mayúscula, un número y un
 * símbolo" porque ese tipo de regla empeora la seguridad real: la gente termina
 * escribiendo {@code Managua2024!} y anotándola en un papel pegado al monitor.
 * Una frase larga y fácil de recordar es más segura y más cómoda.</p>
 */
@Component
public class PoliticaPassword {

    /** Largo mínimo exigido. */
    public static final int LARGO_MINIMO = 12;

    /**
     * BCrypt solo toma en cuenta los primeros 72 bytes de la contraseña, y la
     * librería falla si le mandamos más. Por eso cortamos ahí.
     */
    public static final int BYTES_MAXIMOS = 72;

    /**
     * Lista corta de contraseñas obvias. No pretende ser exhaustiva: es una
     * red para atajar los casos más comunes.
     */
    private static final Set<String> COMUNES = Set.of(
            "123456789012", "contrasena123", "contraseña123", "password1234",
            "qwertyuiop12", "administrador", "admin1234567", "123456789abc",
            "abcd12345678", "iloveyou1234", "bienvenido12", "nicaragua123",
            "managua12345", "vacaciones12", "brenda1234567", "usuario12345",
            "passwordpassword", "111111111111", "000000000000", "123123123123",
            "aaaaaaaaaaaa", "qwertyqwerty", "contrasenia1", "clave1234567",
            "sistema12345", "secretaria12", "corte12345678", "justicia1234"
    );

    /**
     * Revisa una contraseña.
     *
     * @param password     la contraseña propuesta
     * @param username     el nombre de usuario (no debe estar contenido en ella)
     * @return vacío si la contraseña sirve; si no, el mensaje en español que hay
     *         que mostrarle a la persona
     */
    public Optional<String> revisar(String password, String username) {
        if (password == null || password.isBlank()) {
            return Optional.of("Escribí una contraseña.");
        }
        if (password.length() < LARGO_MINIMO) {
            return Optional.of("La contraseña debe tener al menos " + LARGO_MINIMO
                    + " caracteres. La que escribiste tiene " + password.length() + ".");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS) {
            return Optional.of("La contraseña es demasiado larga. Usá 72 caracteres o menos.");
        }

        String enMinusculas = password.toLowerCase(Locale.ROOT);
        if (COMUNES.contains(enMinusculas)) {
            return Optional.of("Esa contraseña es muy conocida y cualquiera la adivinaría. "
                    + "Probá con una frase que solo vos recordés.");
        }
        if (username != null && !username.isBlank()
                && enMinusculas.contains(username.toLowerCase(Locale.ROOT))) {
            return Optional.of("La contraseña no puede contener tu nombre de usuario.");
        }
        return Optional.empty();
    }

    /** Texto que se le muestra a la persona en pantalla, sin tecnicismos. */
    public String explicacion() {
        return "Al menos " + LARGO_MINIMO + " caracteres. Lo más seguro es una frase "
                + "que solo vos recordés, por ejemplo: el perro azul de mi abuela.";
    }
}
