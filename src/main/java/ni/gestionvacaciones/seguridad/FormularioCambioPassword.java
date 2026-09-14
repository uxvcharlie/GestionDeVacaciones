package ni.gestionvacaciones.seguridad;

import jakarta.validation.constraints.NotBlank;

/**
 * Los tres campos del formulario de cambio de contraseña.
 *
 * <p>Acá solo validamos que no vengan vacíos. Las reglas de fondo (largo
 * mínimo, que no sea obvia, que coincida la confirmación) las aplica
 * {@link ServicioUsuario}, del lado del servidor, que es donde importa.</p>
 */
public class FormularioCambioPassword {

    @NotBlank(message = "Escribí tu contraseña actual.")
    private String actual;

    @NotBlank(message = "Escribí la contraseña nueva.")
    private String nueva;

    @NotBlank(message = "Repetí la contraseña nueva.")
    private String confirmacion;

    public String getActual() {
        return actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    public String getNueva() {
        return nueva;
    }

    public void setNueva(String nueva) {
        this.nueva = nueva;
    }

    public String getConfirmacion() {
        return confirmacion;
    }

    public void setConfirmacion(String confirmacion) {
        this.confirmacion = confirmacion;
    }
}
