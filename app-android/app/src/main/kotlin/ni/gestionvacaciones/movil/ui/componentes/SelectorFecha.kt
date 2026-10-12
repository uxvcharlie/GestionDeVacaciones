package ni.gestionvacaciones.movil.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ni.gestionvacaciones.movil.dominio.Fechas
import ni.gestionvacaciones.movil.ui.tema.Paleta
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Un botón que muestra la fecha elegida y abre el calendario del celular.
 *
 * El calendario de Android trabaja en milisegundos UTC; acá se convierte a una
 * fecha simple (LocalDate) para que nunca se "corra un día" por la zona horaria.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(
    etiqueta: String,
    fecha: LocalDate?,
    alElegir: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    permitirVacia: Boolean = false,
    error: String? = null,
) {
    var abierto by rememberSaveable { mutableStateOf(false) }

    BotonSecundario(
        texto = "$etiqueta: " + (fecha?.let(Fechas::fecha) ?: "sin fecha"),
        alTocar = { abierto = true },
        // Sin emoji de calendario: el de Android trae dibujado "JULY" en inglés.
        icono = "▾",
        modifier = modifier.fillMaxWidth(),
    )
    if (error != null) Text(error, color = Paleta.Error)

    if (abierto) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                BotonPrimario("Elegir", {
                    estado.selectedDateMillis?.let { alElegir(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    abierto = false
                })
            },
            dismissButton = {
                if (permitirVacia) {
                    BotonTexto("Dejar sin fecha", {
                        alElegir(null)
                        abierto = false
                    })
                } else {
                    BotonTexto("Cancelar", { abierto = false })
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Paleta.Superficie),
        ) {
            DatePicker(
                state = estado,
                colors = DatePickerDefaults.colors(
                    containerColor = Paleta.Superficie,
                    selectedDayContainerColor = Paleta.Primario,
                    selectedDayContentColor = androidx.compose.ui.graphics.Color.White,
                    todayDateBorderColor = Paleta.Acento,
                    todayContentColor = Paleta.AcentoTexto,
                ),
            )
        }
    }
}
