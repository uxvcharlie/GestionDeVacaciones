package ni.gestionvacaciones.seguridad;

/**
 * Roles que puede tener un usuario del sistema.
 *
 * <p>Hoy existe uno solo: {@link #ADMIN}. Se decidió que únicamente Brenda y
 * las personas que ella autorice entren al sistema, y todas con acceso total.
 * El tipo existe igual para que agregar un rol limitado en el futuro sea
 * cambiar una línea y no rehacer la seguridad completa.</p>
 */
public enum Rol {

    /** Acceso total: registrar, anular, ajustar saldos, administrar usuarios. */
    ADMIN;

    /**
     * Nombre que usa Spring Security internamente. Spring exige que las
     * autorizaciones empiecen con el prefijo {@code ROLE_}.
     */
    public String comoAutoridad() {
        return "ROLE_" + name();
    }
}
