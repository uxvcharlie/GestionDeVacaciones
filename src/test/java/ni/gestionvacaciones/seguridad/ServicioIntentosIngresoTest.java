package ni.gestionvacaciones.seguridad;

import ni.gestionvacaciones.PruebaConPostgres;
import ni.gestionvacaciones.auditoria.Auditoria;
import ni.gestionvacaciones.auditoria.AuditoriaRepositorio;
import ni.gestionvacaciones.auditoria.ServicioAuditoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Conteo de intentos fallidos y bloqueo de 15 minutos, contra PostgreSQL real. */
class ServicioIntentosIngresoTest extends PruebaConPostgres {

    @Autowired
    private ServicioIntentosIngreso intentos;

    @Autowired
    private ServicioUsuario servicioUsuario;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private AuditoriaRepositorio auditoriaRepositorio;

    @Test
    @DisplayName("Cuatro intentos fallidos no bloquean")
    void cuatroNoBloquean() {
        Usuario usuario = crear("intentos-cuatro");

        for (int i = 0; i < 4; i++) {
            assertThat(intentos.registrarFallo("intentos-cuatro", "10.0.0.1"))
                    .isEqualTo(ServicioIntentosIngreso.Resultado.CREDENCIALES_INCORRECTAS);
        }

        Usuario guardado = recargar(usuario);
        assertThat(guardado.getIntentosFallidos()).isEqualTo(4);
        assertThat(guardado.getBloqueadoHasta()).isNull();
    }

    @Test
    @DisplayName("El quinto intento fallido bloquea 15 minutos y queda en la bitácora")
    void quintoBloquea() {
        Usuario usuario = crear("intentos-cinco");

        for (int i = 0; i < 4; i++) {
            intentos.registrarFallo("intentos-cinco", "10.0.0.2");
        }
        Instant antes = Instant.now();
        assertThat(intentos.registrarFallo("INTENTOS-CINCO", "10.0.0.2"))
                .as("el nombre de usuario se compara sin importar mayúsculas")
                .isEqualTo(ServicioIntentosIngreso.Resultado.BLOQUEADO);

        Usuario guardado = recargar(usuario);
        assertThat(guardado.getBloqueadoHasta())
                .isBetween(antes.plus(Duration.ofMinutes(14)), antes.plus(Duration.ofMinutes(16)));
        assertThat(guardado.getIntentosFallidos()).isZero();

        List<String> acciones = bitacora(usuario);
        assertThat(acciones).filteredOn("INGRESO_FALLIDO"::equals).hasSize(5);
        assertThat(acciones).contains("USUARIO_BLOQUEADO_POR_INTENTOS");
    }

    @Test
    @DisplayName("Mientras está bloqueado, los intentos no alargan el bloqueo")
    void bloqueadoNoSeAlarga() {
        Usuario usuario = crear("intentos-no-alarga");
        for (int i = 0; i < 5; i++) {
            intentos.registrarFallo("intentos-no-alarga", null);
        }
        Instant hasta = recargar(usuario).getBloqueadoHasta();

        assertThat(intentos.registrarFallo("intentos-no-alarga", null))
                .isEqualTo(ServicioIntentosIngreso.Resultado.BLOQUEADO);
        assertThat(recargar(usuario).getBloqueadoHasta()).isEqualTo(hasta);
    }

    @Test
    @DisplayName("Un bloqueo vencido deja volver a intentar desde cero")
    void bloqueoVencido() {
        Usuario usuario = crear("intentos-vencido");
        usuario.setBloqueadoHasta(Instant.now().minus(Duration.ofMinutes(1)));
        usuarioRepositorio.save(usuario);

        assertThat(intentos.registrarFallo("intentos-vencido", null))
                .isEqualTo(ServicioIntentosIngreso.Resultado.CREDENCIALES_INCORRECTAS);
        assertThat(recargar(usuario).getIntentosFallidos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Un ingreso correcto reinicia el contador, anota el último ingreso y queda en la bitácora")
    void exitoReinicia() {
        Usuario usuario = crear("intentos-exito");
        intentos.registrarFallo("intentos-exito", null);
        intentos.registrarFallo("intentos-exito", null);

        intentos.registrarExito("intentos-exito", "10.0.0.3");

        Usuario guardado = recargar(usuario);
        assertThat(guardado.getIntentosFallidos()).isZero();
        assertThat(guardado.getUltimoLogin()).isNotNull();
        assertThat(bitacora(usuario)).contains("INGRESO_EXITOSO");
    }

    @Test
    @DisplayName("Un usuario bloqueado por administración recibe su propio resultado, sin contar intentos")
    void desactivado() {
        Usuario usuario = crear("intentos-desactivado");
        usuario.setActivo(false);
        usuarioRepositorio.save(usuario);

        assertThat(intentos.registrarFallo("intentos-desactivado", null))
                .isEqualTo(ServicioIntentosIngreso.Resultado.DESACTIVADO);
        assertThat(recargar(usuario).getIntentosFallidos()).isZero();
    }

    @Test
    @DisplayName("Un usuario que no existe da el mismo resultado que una contraseña equivocada")
    void usuarioInexistente() {
        assertThat(intentos.registrarFallo("nadie-se-llama-asi", "10.0.0.4"))
                .isEqualTo(ServicioIntentosIngreso.Resultado.CREDENCIALES_INCORRECTAS);
    }

    private Usuario crear(String username) {
        Usuario usuario = servicioUsuario.crear(username, "frase-larga-de-intentos", "Persona " + username, null);
        usuario.setDebeCambiarPassword(false);
        return usuarioRepositorio.save(usuario);
    }

    private Usuario recargar(Usuario usuario) {
        return usuarioRepositorio.findById(usuario.getId()).orElseThrow();
    }

    private List<String> bitacora(Usuario usuario) {
        return auditoriaRepositorio.findByEntidadAndEntidadIdOrderByCreadoEnDescIdDesc(
                        ServicioAuditoria.ENTIDAD_USUARIO, usuario.getId())
                .stream().map(Auditoria::getAccion).toList();
    }
}
