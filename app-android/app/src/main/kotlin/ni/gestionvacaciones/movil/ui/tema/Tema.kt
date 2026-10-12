package ni.gestionvacaciones.movil.ui.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * La paleta obligatoria del proyecto, la misma de la web.
 *
 * Contrastes verificados (WCAG) sobre el fondo #05060E:
 *   texto 18.6:1 · suave 9.2:1 · acento 4.9:1 · éxito 11.1:1 · alerta 10.8:1 · error 8.4:1
 * Botón primario: blanco sobre #5B2A86 = 9.9:1.
 * Sobre #B14AED el texto va OSCURO (#05060E), nunca blanco.
 */
object Paleta {
    val Fondo = Color(0xFF05060E)       // fondo base
    val Superficie = Color(0xFF12163A)  // tarjetas, encabezados
    val Elevada = Color(0xFF2E1A63)     // bordes, separadores, filas alternas
    val Primario = Color(0xFF5B2A86)    // botones primarios y estados activos
    val Acento = Color(0xFFB14AED)      // foco, íconos, barras, insignias

    val Texto = Color(0xFFF5F5F7)
    val Suave = Color(0xFFA9AEC4)

    /** Mismo matiz que el acento, más claro: texto de enlaces sobre tarjetas (≥ 4.7:1). */
    val AcentoTexto = Color(0xFFC16FF1)

    val Exito = Color(0xFF6FD3A0)
    val Alerta = Color(0xFFE8B657)
    val Error = Color(0xFFF08A84)
}

private val esquema = darkColorScheme(
    primary = Paleta.Primario,
    onPrimary = Color.White,
    primaryContainer = Paleta.Primario,
    onPrimaryContainer = Color.White,
    secondary = Paleta.Acento,
    onSecondary = Paleta.Fondo,
    secondaryContainer = Paleta.Elevada,
    onSecondaryContainer = Paleta.Texto,
    tertiary = Paleta.AcentoTexto,
    onTertiary = Paleta.Fondo,
    background = Paleta.Fondo,
    onBackground = Paleta.Texto,
    surface = Paleta.Fondo,
    onSurface = Paleta.Texto,
    surfaceVariant = Paleta.Superficie,
    onSurfaceVariant = Paleta.Suave,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Paleta.Fondo,
    surfaceContainerLow = Paleta.Superficie,
    surfaceContainer = Paleta.Superficie,
    surfaceContainerHigh = Paleta.Superficie,
    surfaceContainerHighest = Paleta.Elevada,
    surfaceBright = Paleta.Elevada,
    surfaceDim = Paleta.Fondo,
    inverseSurface = Paleta.Texto,
    inverseOnSurface = Paleta.Fondo,
    outline = Paleta.Elevada,
    outlineVariant = Paleta.Elevada,
    error = Paleta.Error,
    onError = Paleta.Fondo,
    errorContainer = Paleta.Superficie,
    onErrorContainer = Paleta.Error,
)

// Letra un poco más grande que la de fábrica: la app la usa alguien que no
// quiere entrecerrar los ojos. Respeta igual el tamaño de letra del celular.
private val letras = Typography().run {
    copy(
        bodyLarge = bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
        bodyMedium = bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    )
}

/** El saldo grande de la ficha. */
val EstiloSaldo = TextStyle(fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold)

@Composable
fun TemaVacaciones(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, typography = letras, content = contenido)
}
