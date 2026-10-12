package ni.gestionvacaciones.movil.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ni.gestionvacaciones.movil.ui.componentes.BotonPrimario
import ni.gestionvacaciones.movil.ui.pantallas.PantallaAjuste
import ni.gestionvacaciones.movil.ui.pantallas.PantallaAjustes
import ni.gestionvacaciones.movil.ui.pantallas.PantallaBitacora
import ni.gestionvacaciones.movil.ui.pantallas.PantallaCambiarPin
import ni.gestionvacaciones.movil.ui.pantallas.PantallaDesbloqueo
import ni.gestionvacaciones.movil.ui.pantallas.PantallaFicha
import ni.gestionvacaciones.movil.ui.pantallas.PantallaFormularioFuncionario
import ni.gestionvacaciones.movil.ui.pantallas.PantallaFuncionarios
import ni.gestionvacaciones.movil.ui.pantallas.PantallaPrimerUso
import ni.gestionvacaciones.movil.ui.pantallas.PantallaReportes
import ni.gestionvacaciones.movil.ui.pantallas.PantallaRespaldo
import ni.gestionvacaciones.movil.ui.pantallas.PantallaSolicitud
import ni.gestionvacaciones.movil.ui.pantallas.PantallaTablero
import ni.gestionvacaciones.movil.ui.tema.Paleta

/**
 * La raíz de la app. Decide qué se ve:
 *   1. La primera vez: bienvenida, nombre y creación del PIN.
 *   2. Si está bloqueada: el teclado del PIN.
 *   3. Si no: las pantallas de trabajo.
 */
@Composable
fun RaizApp() {
    val contenedor = LocalContenedor.current
    val desbloqueada by contenedor.sesion.desbloqueada.collectAsStateWithLifecycle()
    val parametros by contenedor.repositorio.parametros.collectAsStateWithLifecycle(null)
    var tienePin by remember { mutableStateOf(contenedor.acceso.hayPin()) }

    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val avisar: (String) -> Unit = remember {
        { mensaje ->
            alcance.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(mensaje, withDismissAction = true, duration = SnackbarDuration.Long)
            }
        }
    }

    // Revisa la inactividad cada 10 segundos y al volver a la app.
    var avisoInactividad by remember { mutableStateOf(false) }
    LaunchedEffect(desbloqueada) {
        while (desbloqueada) {
            contenedor.sesion.revisarInactividad()
            avisoInactividad = contenedor.sesion.debeAvisar()
            delay(10_000)
        }
        avisoInactividad = false
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { contenedor.sesion.revisarInactividad() }

    CompositionLocalProvider(LocalAvisar provides avisar) {
        Scaffold(
            containerColor = Paleta.Fondo,
            // Arriba no se deja margen: lo ocupa la franja marino de abajo.
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            snackbarHost = {
                SnackbarHost(snackbar) {
                    Snackbar(
                        it,
                        containerColor = Paleta.Barra,
                        contentColor = Paleta.BarraTexto,
                        dismissActionContentColor = Paleta.BarraTexto,
                    )
                }
            },
        ) { margenes ->
            Column(Modifier.padding(margenes).fillMaxSize()) {
                // La barra de estado del celular (hora, batería) se dibuja sobre
                // marino, así se une con la barra superior de cada pantalla.
                Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.safeDrawing).background(Paleta.Barra))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    val p = parametros
                    when {
                        p == null -> Unit // un instante mientras se abre la base de datos
                        !tienePin -> PantallaPrimerUso(alTerminar = { tienePin = true })
                        !desbloqueada -> PantallaDesbloqueo(p.nombreUsuaria)
                        else -> NavegacionPrincipal()
                    }
                }
            }
        }

        if (avisoInactividad && desbloqueada) {
            AlertDialog(
                onDismissRequest = { contenedor.sesion.registrarActividad(); avisoInactividad = false },
                containerColor = Paleta.Superficie,
                titleContentColor = Paleta.Texto,
                textContentColor = Paleta.Texto,
                title = { Text("¿Seguís ahí?") },
                text = { Text("Por seguridad, la app se va a bloquear en un minuto por falta de uso. Para seguir, tocá el botón.") },
                confirmButton = {
                    BotonPrimario("Seguir usando la app", {
                        contenedor.sesion.registrarActividad()
                        avisoInactividad = false
                    })
                },
            )
        }
    }
}

/** Las pantallas de trabajo y cómo se llega a cada una. */
@Composable
private fun NavegacionPrincipal() {
    val nav = rememberNavController()
    val rutas = remember(nav) { Rutas(nav) }
    val id = listOf(navArgument("id") { type = NavType.LongType })

    NavHost(
        navController = nav,
        startDestination = Rutas.TABLERO,
        enterTransition = { fadeIn(tween(150)) },
        exitTransition = { fadeOut(tween(150)) },
    ) {
        composable(Rutas.TABLERO) { PantallaTablero(rutas) }
        composable(Rutas.FUNCIONARIOS) { PantallaFuncionarios(rutas) }
        composable(Rutas.NUEVO_FUNCIONARIO) { PantallaFormularioFuncionario(rutas, null) }
        composable(Rutas.FICHA, arguments = id) { PantallaFicha(rutas, it.arguments!!.getLong("id")) }
        composable(Rutas.EDITAR, arguments = id) { PantallaFormularioFuncionario(rutas, it.arguments!!.getLong("id")) }
        composable(Rutas.AJUSTE, arguments = id) { PantallaAjuste(rutas, it.arguments!!.getLong("id")) }
        composable(
            Rutas.SOLICITUD,
            arguments = listOf(navArgument("funcionario") { type = NavType.LongType; defaultValue = -1L }),
        ) { PantallaSolicitud(rutas, it.arguments!!.getLong("funcionario").takeIf { f -> f > 0 }) }
        composable(Rutas.REPORTES) { PantallaReportes(rutas) }
        composable(Rutas.RESPALDO) { PantallaRespaldo(rutas) }
        composable(Rutas.BITACORA) { PantallaBitacora(rutas) }
        composable(Rutas.AJUSTES) { PantallaAjustes(rutas) }
        composable(Rutas.CAMBIAR_PIN) { PantallaCambiarPin(rutas) }
    }
}
