package ni.gestionvacaciones.comun;

/**
 * Se intentó hacer algo que las reglas del sistema no permiten: registrar más
 * tiempo del que hay en el saldo, anular dos veces, olvidar un motivo...
 *
 * <p>El mensaje está escrito para Brenda, en español humano, y el controlador
 * lo muestra tal cual en pantalla. Nunca lleva detalles técnicos.</p>
 */
public class ReglaDeNegocioException extends RuntimeException {

    private final boolean requiereAutorizacion;

    public ReglaDeNegocioException(String mensaje) {
        this(mensaje, false);
    }

    /**
     * @param requiereAutorizacion {@code true} cuando la operación SÍ se puede
     *        hacer si un ADMIN la autoriza expresamente (el saldo negativo).
     */
    public ReglaDeNegocioException(String mensaje, boolean requiereAutorizacion) {
        super(mensaje);
        this.requiereAutorizacion = requiereAutorizacion;
    }

    public boolean requiereAutorizacion() {
        return requiereAutorizacion;
    }
}
