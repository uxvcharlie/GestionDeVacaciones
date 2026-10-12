package ni.gestionvacaciones.movil

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import ni.gestionvacaciones.movil.ui.LocalContenedor
import ni.gestionvacaciones.movil.ui.RaizApp
import ni.gestionvacaciones.movil.ui.tema.TemaVacaciones
import java.util.Locale

/** La única pantalla de Android de la app. Todo lo demás lo dibuja Compose adentro. */
class ActividadPrincipal : ComponentActivity() {

    private val contenedor get() = (application as AppVacaciones).contenedor

    /**
     * La app habla SIEMPRE en español, aunque el celular esté configurado en
     * otro idioma: así el calendario dice "octubre" y no "October", y los
     * botones del sistema dentro de la app salen en español.
     *
     * Configuration() vacía = "solo cambiá lo que yo te diga": se reemplaza el
     * idioma y se respeta todo lo demás, como el tamaño de letra del celular.
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        applyOverrideConfiguration(Configuration().apply { setLocale(ESPANOL) })
    }

    private companion object {
        val ESPANOL: Locale = Locale.forLanguageTag("es-NI")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Arriba, íconos claros sobre la franja marino; abajo, íconos oscuros
        // sobre el fondo gris claro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalContenedor provides contenedor) {
                TemaVacaciones { RaizApp() }
            }
        }
    }

    /** Cada toque en la pantalla cuenta como actividad: la sesión no se cierra mientras se usa. */
    override fun dispatchTouchEvent(evento: MotionEvent): Boolean {
        contenedor.sesion.registrarActividad()
        return super.dispatchTouchEvent(evento)
    }
}
