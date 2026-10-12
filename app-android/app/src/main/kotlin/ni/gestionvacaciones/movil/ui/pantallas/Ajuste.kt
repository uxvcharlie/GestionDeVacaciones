package ni.gestionvacaciones.movil.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ni.gestionvacaciones.movil.datos.Parametros
import ni.gestionvacaciones.movil.dominio.BorradorAjuste
import ni.gestionvacaciones.movil.dominio.ConversorTiempo
import ni.gestionvacaciones.movil.dominio.OperacionAjuste
import ni.gestionvacaciones.movil.ui.LocalAvisar
import ni.gestionvacaciones.movil.ui.LocalContenedor
import ni.gestionvacaciones.movil.ui.Resultado
import ni.gestionvacaciones.movil.ui.Rutas
import ni.gestionvacaciones.movil.ui.componentes.ALTO_MINIMO
import ni.gestionvacaciones.movil.ui.componentes.Aviso
import ni.gestionvacaciones.movil.ui.componentes.BotonPrimario
import ni.gestionvacaciones.movil.ui.componentes.Campo
import ni.gestionvacaciones.movil.ui.componentes.CampoNumero
import ni.gestionvacaciones.movil.ui.componentes.PantallaConDesplazamiento
import ni.gestionvacaciones.movil.ui.componentes.Subtitulo
import ni.gestionvacaciones.movil.ui.componentes.Tarjeta
import ni.gestionvacaciones.movil.ui.componentes.TextoSuave
import ni.gestionvacaciones.movil.ui.componentes.TipoAviso
import ni.gestionvacaciones.movil.ui.intentar
import ni.gestionvacaciones.movil.ui.tema.Paleta

/** Sumar o restar tiempo a mano, siempre con motivo. Queda en el historial y en la bitácora. */
@Composable
fun PantallaAjuste(rutas: Rutas, id: Long) {
    val repo = LocalContenedor.current.repositorio
    val avisar = LocalAvisar.current
    val alcance = rememberCoroutineScope()
    val p by repo.parametros.collectAsStateWithLifecycle(Parametros())
    val funcionario by remember(id) { repo.funcionario(id) }.collectAsStateWithLifecycle(null)

    var operacion by rememberSaveable { mutableStateOf<OperacionAjuste?>(null) }
    var dias by rememberSaveable { mutableStateOf("") }
    var horas by rememberSaveable { mutableStateOf("") }
    var minutos by rememberSaveable { mutableStateOf("") }
    var motivo by rememberSaveable { mutableStateOf("") }
    var autorizar by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pideAutorizacion by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    PantallaConDesplazamiento("Ajustar saldo", alVolver = rutas::volver) {
        val f = funcionario ?: return@PantallaConDesplazamiento
        val j = p.horasPorJornada
        Tarjeta {
            Text(f.nombreCompleto, style = MaterialTheme.typography.titleMedium, color = Paleta.Texto)
            Text("Saldo actual: " + ConversorTiempo.formatear(f.saldoMinutos, j), color = Paleta.AcentoTexto)
        }
        TextoSuave("Usalo para corregir un saldo o cargar días que se ganaron. El motivo es obligatorio y queda guardado.")

        Subtitulo("¿Sumar o restar?")
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OperacionAjuste.entries.forEach { op ->
                Row(
                    Modifier
                        .weight(1f)
                        .heightIn(min = ALTO_MINIMO)
                        .selectable(selected = operacion == op, role = Role.RadioButton, onClick = { operacion = op; error = null }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(operacion == op, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Paleta.Primario, unselectedColor = Paleta.Suave))
                    Text(if (op == OperacionAjuste.SUMAR) "＋ Sumar" else "− Restar", Modifier.padding(start = 8.dp), color = Paleta.Texto)
                }
            }
        }

        Subtitulo("¿Cuánto?")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoNumero("Días", dias, { dias = it }, Modifier.weight(1f))
            CampoNumero("Horas", horas, { horas = it }, Modifier.weight(1f), digitos = 2)
            CampoNumero("Minutos", minutos, { minutos = it }, Modifier.weight(1f), digitos = 2)
        }
        Campo("Motivo del ajuste", motivo, { motivo = it; error = null }, lineas = 2, largoMaximo = 300)

        error?.let { Aviso(it, TipoAviso.ERROR) }
        if (pideAutorizacion) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = ALTO_MINIMO)
                    .toggleable(value = autorizar, role = Role.Checkbox, onValueChange = { autorizar = it }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(autorizar, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Paleta.Primario, uncheckedColor = Paleta.Suave))
                Text("Autorizo que el saldo quede en negativo.", Modifier.weight(1f).padding(start = 8.dp), color = Paleta.Texto)
            }
        }

        BotonPrimario("Guardar ajuste", {
            if (guardando) return@BotonPrimario
            guardando = true
            val borrador = BorradorAjuste(
                operacion, dias.toIntOrNull() ?: 0, horas.toIntOrNull() ?: 0, minutos.toIntOrNull() ?: 0, motivo, autorizar,
            )
            alcance.launch {
                val r = intentar { repo.ajustarSaldo(id, borrador) }
                guardando = false
                when (r) {
                    is Resultado.Bien -> {
                        avisar(
                            "Listo: saldo de ${f.nombreCompleto}: ${ConversorTiempo.formatear(r.valor.saldoAntes, j)} → " +
                                ConversorTiempo.formatear(r.valor.saldoDespues, j) + ".",
                        )
                        rutas.volver()
                    }
                    is Resultado.Mal -> {
                        error = r.mensaje
                        if (r.requiereAutorizacion) pideAutorizacion = true
                    }
                }
            }
        }, Modifier.fillMaxWidth(), icono = "✓", habilitado = !guardando)
    }
}
