package ni.gestionvacaciones.movil.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ni.gestionvacaciones.movil.ui.tema.Paleta

/**
 * La barra de arriba de cada pantalla: botón de volver y título.
 * El botón de volver lleva descripción para el lector de pantalla.
 */
@Composable
fun Encabezado(titulo: String, alVolver: (() -> Unit)?, acciones: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Paleta.Superficie)
            .heightIn(min = 60.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (alVolver != null) {
            IconButton(
                onClick = alVolver,
                modifier = Modifier.size(52.dp).semantics { contentDescription = "Volver" },
            ) {
                Text("←", fontSize = 24.sp, color = Paleta.Texto, modifier = Modifier.clearAndSetSemantics { })
            }
        } else {
            Spacer(Modifier.size(16.dp))
        }
        Text(
            titulo,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = Paleta.Texto,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        acciones()
    }
}

/**
 * Esqueleto de una pantalla con desplazamiento vertical. En un celular las
 * pantallas son largas: todo se puede bajar con el dedo, nada se corta.
 * El ancho máximo de 640 dp evita renglones larguísimos en tablets.
 */
@Composable
fun PantallaConDesplazamiento(
    titulo: String,
    alVolver: (() -> Unit)?,
    acciones: @Composable () -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Encabezado(titulo, alVolver, acciones)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = contenido,
            )
            Spacer(Modifier.size(24.dp))
        }
    }
}

/**
 * Confirmación antes de una acción importante. El texto siempre nombra a la
 * persona: "¿Dar de baja a María González? Su historial se conserva."
 */
@Composable
fun DialogoConfirmacion(
    titulo: String,
    texto: String,
    confirmar: String,
    alConfirmar: () -> Unit,
    alCancelar: () -> Unit,
    peligroso: Boolean = false,
    contenidoExtra: @Composable ColumnScope.() -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = alCancelar,
        containerColor = Paleta.Superficie,
        titleContentColor = Paleta.Texto,
        textContentColor = Paleta.Texto,
        title = { Text(titulo, modifier = Modifier.semantics { heading() }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(texto, style = MaterialTheme.typography.bodyLarge)
                contenidoExtra()
            }
        },
        confirmButton = {
            if (peligroso) BotonPeligro(confirmar, alConfirmar) else BotonPrimario(confirmar, alConfirmar)
        },
        dismissButton = { BotonSecundario("Cancelar", alCancelar) },
    )
}
