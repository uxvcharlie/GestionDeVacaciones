package ni.gestionvacaciones.movil.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ni.gestionvacaciones.movil.ui.tema.Paleta

/*
 * Piezas que se repiten en todas las pantallas. Están en un solo lugar para
 * que el tamaño mínimo de los botones, los colores y la accesibilidad sean
 * siempre los mismos, sin depender de que uno se acuerde en cada pantalla.
 */

/** Altura mínima de todo lo que se toca. La guía de Android pide 48 dp. */
val ALTO_MINIMO = 52.dp

@Composable
fun BotonPrimario(texto: String, alTocar: () -> Unit, modifier: Modifier = Modifier, icono: String? = null, habilitado: Boolean = true) {
    Button(
        onClick = alTocar,
        enabled = habilitado,
        modifier = modifier.heightIn(min = ALTO_MINIMO),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Paleta.Primario,
            contentColor = Color.White,
            disabledContainerColor = Paleta.Elevada,
            disabledContentColor = Paleta.Suave,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) { ContenidoBoton(texto, icono) }
}

@Composable
fun BotonSecundario(texto: String, alTocar: () -> Unit, modifier: Modifier = Modifier, icono: String? = null, habilitado: Boolean = true) {
    OutlinedButton(
        onClick = alTocar,
        enabled = habilitado,
        modifier = modifier.heightIn(min = ALTO_MINIMO),
        shape = RoundedCornerShape(12.dp),
        // Borde en el azul grisáceo "suave": 5.4:1, se distingue del fondo.
        border = BorderStroke(1.dp, if (habilitado) Paleta.Suave else Paleta.Elevada),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Paleta.Texto),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) { ContenidoBoton(texto, icono) }
}

/** Para acciones que no se pueden deshacer fácil: dar de baja, anular, restaurar. */
@Composable
fun BotonPeligro(texto: String, alTocar: () -> Unit, modifier: Modifier = Modifier, icono: String? = null) {
    OutlinedButton(
        onClick = alTocar,
        modifier = modifier.heightIn(min = ALTO_MINIMO),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Paleta.Error.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Paleta.Error),
    ) { ContenidoBoton(texto, icono) }
}

/** Botón para la barra superior marino: texto claro y borde claro, como en la web. */
@Composable
fun BotonBarra(texto: String, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = alTocar,
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Paleta.BarraBorde),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Paleta.BarraTexto),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) { Text(texto, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun BotonTexto(texto: String, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = alTocar, modifier = modifier.heightIn(min = ALTO_MINIMO)) {
        Text(texto, color = Paleta.AcentoTexto, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun RowScope.ContenidoBoton(texto: String, icono: String?) {
    if (icono != null) {
        // El ícono es decorativo: el lector de pantalla solo lee el texto.
        Text(icono, modifier = Modifier.clearAndSetSemantics { }, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
    }
    Text(texto, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}

/** Una tarjeta con la superficie de la paleta y un borde sutil. */
@Composable
fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Paleta.Superficie, contentColor = Paleta.Texto),
        border = BorderStroke(1.dp, Paleta.Elevada),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = contenido)
    }
}

@Composable
fun Titulo(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineSmall,
        color = Paleta.Texto,
    )
}

@Composable
fun Subtitulo(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = Paleta.Texto,
    )
}

@Composable
fun TextoSuave(texto: String, modifier: Modifier = Modifier) {
    Text(texto, modifier = modifier, style = MaterialTheme.typography.bodyMedium, color = Paleta.Suave)
}

/** Tipos de aviso. Cada uno lleva color + ícono + texto: nunca solo color. */
enum class TipoAviso(val color: Color, val icono: String, val descripcion: String) {
    ERROR(Paleta.Error, "⚠", "Error"),
    EXITO(Paleta.Exito, "✓", "Listo"),
    ADVERTENCIA(Paleta.Alerta, "⚠", "Atención"),
    INFORMACION(Paleta.AcentoTexto, "ℹ", "Nota"),
}

/**
 * Un aviso. "liveRegion" hace que el lector de pantalla lo lea en cuanto
 * aparece (el equivalente a aria-live de la web).
 */
@Composable
fun Aviso(texto: String, tipo: TipoAviso, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tipo.color.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .border(1.dp, tipo.color.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(14.dp)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "${tipo.descripcion}: $texto"
            },
        verticalAlignment = Alignment.Top,
    ) {
        Text(tipo.icono, color = tipo.color, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Text(texto, color = tipo.color, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Insignia de estado: ícono + texto, con el color como refuerzo. */
@Composable
fun Insignia(texto: String, icono: String, color: Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text("$icono $texto", color = color, style = MaterialTheme.typography.labelMedium)
    }
}

/** Un estado vacío útil: en vez de una pantalla en blanco, qué hacer. */
@Composable
fun EstadoVacio(titulo: String, explicacion: String, accion: String? = null, alTocar: (() -> Unit)? = null) {
    Tarjeta {
        Text("◇", modifier = Modifier.clearAndSetSemantics { }, color = Paleta.AcentoTexto, fontSize = 28.sp)
        Subtitulo(titulo)
        TextoSuave(explicacion)
        if (accion != null && alTocar != null) {
            BotonPrimario(accion, alTocar, Modifier.fillMaxWidth(), icono = "＋")
        }
    }
}

/** Campo de texto con etiqueta, ayuda y error debajo. */
@Composable
fun Campo(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    error: String? = null,
    lineas: Int = 1,
    largoMaximo: Int = 150,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    mayusculas: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { if (it.length <= largoMaximo) alCambiar(it) },
        label = { Text(etiqueta) },
        modifier = modifier.fillMaxWidth(),
        singleLine = lineas == 1,
        minLines = lineas,
        isError = error != null,
        supportingText = when {
            error != null -> ({ Text(error, color = Paleta.Error) })
            ayuda != null -> ({ Text(ayuda, color = Paleta.Suave) })
            else -> null
        },
        keyboardOptions = KeyboardOptions(
            capitalization = mayusculas,
            keyboardType = tipoTeclado,
            imeAction = if (lineas == 1) ImeAction.Next else ImeAction.Default,
        ),
        colors = coloresCampo(),
        shape = RoundedCornerShape(12.dp),
    )
}

/**
 * Campo para números enteros chicos (días, horas, minutos). Solo acepta
 * dígitos: ni signos, ni comas, ni puntos. Así no se puede escribir "1.5 días":
 * para medio día se usan las horas.
 */
@Composable
fun CampoNumero(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    digitos: Int = 4,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo -> if (nuevo.length <= digitos && nuevo.all(Char::isDigit)) alCambiar(nuevo) },
        label = { Text(etiqueta) },
        modifier = modifier,
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it, color = Paleta.Error) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        colors = coloresCampo(),
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
fun coloresCampo() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Paleta.Foco,
    unfocusedBorderColor = Paleta.Suave,
    focusedLabelColor = Paleta.Foco,
    unfocusedLabelColor = Paleta.Suave,
    cursorColor = Paleta.Foco,
    focusedContainerColor = Paleta.Superficie,
    unfocusedContainerColor = Paleta.Superficie,
    focusedTextColor = Paleta.Texto,
    unfocusedTextColor = Paleta.Texto,
    errorBorderColor = Paleta.Error,
    errorLabelColor = Paleta.Error,
)

/** Un renglón "Dato: valor" para fichas y resúmenes. */
@Composable
fun Dato(etiqueta: String, valor: String) {
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }) {
        TextoSuave(etiqueta)
        Text(valor.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, color = Paleta.Texto)
    }
}

/** Separador horizontal con el color de la paleta. */
@Composable
fun Separador() {
    HorizontalDivider(Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Paleta.Elevada)
}
