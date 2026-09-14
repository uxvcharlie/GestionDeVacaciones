package ni.gestionvacaciones.parametro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Un valor de configuración del sistema, editable desde la interfaz (Fase 5).
 *
 * <p>Se guarda como clave y valor de texto. Quien lo lee es responsable de
 * convertirlo al tipo que corresponda: eso lo hace {@link ServicioParametros}.</p>
 */
@Entity
@Table(name = "parametro")
public class Parametro {

    @Id
    @Column(name = "clave", length = 50)
    private String clave;

    @Column(name = "valor", nullable = false, length = 200)
    private String valor;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }

    public Long getActualizadoPor() {
        return actualizadoPor;
    }

    public void setActualizadoPor(Long actualizadoPor) {
        this.actualizadoPor = actualizadoPor;
    }
}
