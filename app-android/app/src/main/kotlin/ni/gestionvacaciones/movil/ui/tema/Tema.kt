package ni.gestionvacaciones.movil.ui.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * La paleta del proyecto: la misma de la web desde su cambio a tema claro.
 *
 * Cuatro colores base exactos:
 *   #112B3C azul marino (barra superior y texto) · #205375 azul (botones)
 *   #F66B0E naranja (acento) · #EFEFEF gris claro (fondo)
 *
 * Contrastes verificados (WCAG AA: texto 4.5:1, bordes y foco 3:1):
 *   texto 12.7:1 sobre el fondo y 14.6:1 sobre blanco · suave 5.4 y 6.2
 *   botón primario: blanco sobre azul 8.2:1
 *   El naranja puro sobre fondo claro da apenas 2.6:1: no sirve para texto
 *   ni para bordes. Para eso va #AC4906 (mismo matiz, 4.9 y 5.7:1). Sobre la
 *   barra marino sí se usa el naranja puro (4.9:1).
 */
object Paleta {
    // Superficies
    val Fondo = Color(0xFFEFEFEF)       // fondo de la app
    val Superficie = Color(0xFFFFFFFF)  // tarjetas, campos, diálogos
    val Elevada = Color(0xFFDCE3E9)     // separadores, opción elegida

    // Barra superior
    val Barra = Color(0xFF112B3C)
    val BarraTexto = Color(0xFFEFEFEF)  // 12.7:1 sobre la barra
    val BarraSuave = Color(0xFFA9BCCB)  //  7.5:1 sobre la barra
    val BarraBorde = Color(0xFF7C97AB)  //  4.8:1 sobre la barra

    // Acciones
    val Primario = Color(0xFF205375)    // botones primarios, casillas marcadas
    val Acento = Color(0xFFF66B0E)      // naranja: SOLO decoración o sobre la barra
    val AcentoTexto = Color(0xFFAC4906) // naranja oscuro: enlaces y montos destacados
    val Foco = Color(0xFFAC4906)        // borde del campo activo y de la opción elegida

    // Texto
    val Texto = Color(0xFF112B3C)
    val Suave = Color(0xFF42657C)       // también borde de campos y botones secundarios

    // Estados: siempre con ícono y texto, nunca solo color
    val Exito = Color(0xFF17643A)
    val Alerta = Color(0xFF7F5300)
    val Error = Color(0xFFB42318)
}

private val esquema = lightColorScheme(
    primary = Paleta.Primario,
    onPrimary = Color.White,
    primaryContainer = Paleta.Elevada,
    onPrimaryContainer = Paleta.Texto,
    secondary = Paleta.Foco,
    onSecondary = Color.White,
    secondaryContainer = Paleta.Elevada,
    onSecondaryContainer = Paleta.Texto,
    tertiary = Paleta.AcentoTexto,
    onTertiary = Color.White,
    background = Paleta.Fondo,
    onBackground = Paleta.Texto,
    surface = Paleta.Fondo,
    onSurface = Paleta.Texto,
    surfaceVariant = Paleta.Elevada,
    onSurfaceVariant = Paleta.Suave,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Paleta.Superficie,
    surfaceContainerLow = Paleta.Superficie,
    surfaceContainer = Paleta.Superficie,
    surfaceContainerHigh = Paleta.Superficie,
    surfaceContainerHighest = Paleta.Elevada,
    surfaceBright = Paleta.Superficie,
    surfaceDim = Paleta.Elevada,
    inverseSurface = Paleta.Barra,
    inverseOnSurface = Paleta.BarraTexto,
    inversePrimary = Paleta.BarraSuave,
    outline = Paleta.Suave,
    outlineVariant = Paleta.Elevada,
    error = Paleta.Error,
    onError = Color.White,
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
