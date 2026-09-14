package ni.gestionvacaciones.seguridad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Una persona con acceso al sistema.
 *
 * <p>Cada campo de esta clase corresponde a una columna de la tabla
 * {@code usuario} creada por Flyway en V1__esquema_inicial.sql. Como la
 * aplicación arranca con {@code ddl-auto: validate}, si alguna vez no
 * coinciden, la aplicación no arranca y te avisa: es a propósito.</p>
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Siempre en minúsculas, para que "Brenda" y "brenda" sean el mismo usuario. */
    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    /** Hash BCrypt. La contraseña en texto plano no existe en ningún lado. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol = Rol.ADMIN;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = true;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    /** Si tiene fecha y todavía no pasó, el usuario está bloqueado. */
    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "ultimo_login")
    private Instant ultimoLogin;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    /** Id del usuario que lo creó. NULL en el administrador inicial. */
    @Column(name = "creado_por")
    private Long creadoPor;

    // ---------------------------------------------------------------------
    // Métodos de negocio
    // ---------------------------------------------------------------------

    /** ¿Está bloqueado en este momento por intentos fallidos? */
    public boolean estaBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(Instant.now());
    }

    // ---------------------------------------------------------------------
    // Getters y setters
    // ---------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public void setDebeCambiarPassword(boolean debeCambiarPassword) {
        this.debeCambiarPassword = debeCambiarPassword;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(Instant bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public Instant getUltimoLogin() {
        return ultimoLogin;
    }

    public void setUltimoLogin(Instant ultimoLogin) {
        this.ultimoLogin = ultimoLogin;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Long getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(Long creadoPor) {
        this.creadoPor = creadoPor;
    }
}
