package ni.gestionvacaciones.seguridad;

import ni.gestionvacaciones.auditoria.AccionAuditoria;
import ni.gestionvacaciones.auditoria.ServicioAuditoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Cuenta los intentos de ingreso y bloquea al quinto fallido.
 *
 * <p><b>Qué protege:</b> sin límite, alguien podría probar miles de
 * contraseñas por minuto contra el usuario de Brenda. Con 5 intentos cada 15
 * minutos, adivinar una contraseña de 12 caracteres tomaría más que la edad
 * del universo.</p>
 *
 * <p>El contador es por usuario. Un ingreso correcto lo reinicia. Mientras el
 * usuario está bloqueado, los intentos no alargan el bloqueo: si no, cualquiera
 * podría mantener a Brenda afuera para siempre con solo escribir mal su
 * contraseña cada 15 minutos.</p>
 */
@Service
public class ServicioIntentosIngreso {

    public static final int INTENTOS_MAXIMOS = 5;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);
    private static final int LARGO_MAXIMO_USUARIO_EN_BITACORA = 50;

    /** Qué pasó con el intento, para elegir el mensaje de la pantalla de ingreso. */
    public enum Resultado {
        CREDENCIALES_INCORRECTAS,
        BLOQUEADO,
        DESACTIVADO
    }

    private final UsuarioRepositorio repositorio;
    private final ServicioAuditoria auditoria;
    private final Clock reloj;

    public ServicioIntentosIngreso(UsuarioRepositorio repositorio, ServicioAuditoria auditoria, Clock reloj) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado registrarFallo(String usuarioEscrito, String ip) {
        String username = usuarioEscrito == null ? "" : usuarioEscrito.trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> encontrado = repositorio.findByUsername(username);

        if (encontrado.isEmpty()) {
            String anotado = username.length() > LARGO_MAXIMO_USUARIO_EN_BITACORA
                    ? username.substring(0, LARGO_MAXIMO_USUARIO_EN_BITACORA) + "…"
                    : username;
            auditoria.registrar(null, AccionAuditoria.INGRESO_FALLIDO, ServicioAuditoria.ENTIDAD_USUARIO, null,
                    "Usuario que no existe: «" + anotado + "»", ip);
            return Resultado.CREDENCIALES_INCORRECTAS;
        }

        Usuario usuario = encontrado.get();
        Instant ahora = reloj.instant();

        if (!usuario.isActivo()) {
            registrar(usuario, AccionAuditoria.INGRESO_FALLIDO, "Intento de un usuario bloqueado por administración", ip);
            return Resultado.DESACTIVADO;
        }
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora)) {
            registrar(usuario, AccionAuditoria.INGRESO_FALLIDO, "Intento mientras estaba bloqueado por intentos", ip);
            return Resultado.BLOQUEADO;
        }

        int intentos = usuario.getIntentosFallidos() + 1;
        registrar(usuario, AccionAuditoria.INGRESO_FALLIDO, "Intento " + intentos + " de " + INTENTOS_MAXIMOS, ip);

        if (intentos >= INTENTOS_MAXIMOS) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(ahora.plus(DURACION_BLOQUEO));
            registrar(usuario, AccionAuditoria.USUARIO_BLOQUEADO_POR_INTENTOS,
                    "Bloqueado " + DURACION_BLOQUEO.toMinutes() + " minutos tras " + INTENTOS_MAXIMOS + " intentos fallidos", ip);
            return Resultado.BLOQUEADO;
        }
        usuario.setIntentosFallidos(intentos);
        return Resultado.CREDENCIALES_INCORRECTAS;
    }

    @Transactional
    public void registrarExito(String username, String ip) {
        repositorio.findByUsername(username.toLowerCase(Locale.ROOT)).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuario.setUltimoLogin(reloj.instant());
            registrar(usuario, AccionAuditoria.INGRESO_EXITOSO, null, ip);
        });
    }

    private void registrar(Usuario usuario, AccionAuditoria accion, String detalle, String ip) {
        auditoria.registrar(usuario.getId(), accion, ServicioAuditoria.ENTIDAD_USUARIO, usuario.getId(), detalle, ip);
    }
}
