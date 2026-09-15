package ni.gestionvacaciones.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un usuario. La contraseña NO se escribe: la genera el
 * sistema, así nadie inventa una débil ni la reutiliza de otro lado.
 */
public class FormularioUsuario {

    @NotBlank(message = "Escribí el nombre completo.")
    @Size(max = 150, message = "El nombre puede tener hasta 150 caracteres.")
    private String nombreCompleto;

    @NotBlank(message = "Escribí el nombre de usuario.")
    @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
            message = "El nombre de usuario lleva de 3 a 50 caracteres: letras sin tildes, números, punto, guion o guion bajo. Sin espacios.")
    private String username;

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
