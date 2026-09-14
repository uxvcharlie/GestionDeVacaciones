package ni.gestionvacaciones.solicitud;

import ni.gestionvacaciones.comun.ConversorTiempo;

/**
 * Los tres tipos de solicitud.
 *
 * <p>El tipo NO cambia el cálculo: los tres descuentan igual del mismo saldo.
 * Solo cambia cómo se escribe y se muestra la cantidad (las vacaciones en
 * días, las citas y los permisos en horas) y sirve para los reportes.</p>
 */
public enum TipoSolicitud {

    VACACIONES("Vacaciones", true),
    CITA_MEDICA("Cita médica", false),
    PERMISO("Permiso", false);

    private final String etiqueta;
    private final boolean seCuentaEnDias;

    TipoSolicitud(String etiqueta, boolean seCuentaEnDias) {
        this.etiqueta = etiqueta;
        this.seCuentaEnDias = seCuentaEnDias;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** true: se escribe en días y horas. false: en horas y minutos. */
    public boolean isSeCuentaEnDias() {
        return seCuentaEnDias;
    }

    /** "5 días" para vacaciones; "2 horas y 30 minutos" para citas y permisos. */
    public String formatear(int minutos, int horasPorJornada) {
        return seCuentaEnDias
                ? ConversorTiempo.formatear(minutos, horasPorJornada)
                : ConversorTiempo.formatearHoras(minutos);
    }
}
