package ni.gestionvacaciones.movil.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ni.gestionvacaciones.movil.datos.Parametros
import ni.gestionvacaciones.movil.dominio.BorradorFuncionario
import ni.gestionvacaciones.movil.dominio.ConversorTiempo
import ni.gestionvacaciones.movil.dominio.ReglasDeFuncionario
import ni.gestionvacaciones.movil.ui.LocalAvisar
import ni.gestionvacaciones.movil.ui.LocalContenedor
import ni.gestionvacaciones.movil.ui.Resultado
import ni.gestionvacaciones.movil.ui.Rutas
import ni.gestionvacaciones.movil.ui.componentes.ALTO_MINIMO
import ni.gestionvacaciones.movil.ui.componentes.Aviso
import ni.gestionvacaciones.movil.ui.componentes.BotonPrimario
import ni.gestionvacaciones.movil.ui.componentes.Campo
import ni.gestionvacaciones.movil.ui.componentes.CampoNumero
import ni.gestionvacaciones.movil.ui.componentes.EstadoVacio
import ni.gestionvacaciones.movil.ui.componentes.PantallaConDesplazamiento
import ni.gestionvacaciones.movil.ui.componentes.SelectorFecha
import ni.gestionvacaciones.movil.ui.componentes.Separador
import ni.gestionvacaciones.movil.ui.componentes.Subtitulo
import ni.gestionvacaciones.movil.ui.componentes.Tarjeta
import ni.gestionvacaciones.movil.ui.componentes.TextoSuave
import ni.gestionvacaciones.movil.ui.componentes.TipoAviso
import ni.gestionvacaciones.movil.ui.intentar
import ni.gestionvacaciones.movil.ui.tema.Paleta
import java.time.LocalDate

/** La lista de funcionarios, con buscador. Los dados de baja se ocultan salvo que se pidan. */
@Composable
fun PantallaFuncionarios(rutas: Rutas) {
    val repo = LocalContenedor.current.repositorio
    val p by repo.parametros.collectAsStateWithLifecycle(Parametros())
    val todos by repo.funcionarios.collectAsStateWithLifecycle(null)
    var texto by rememberSaveable { mutableStateOf("") }
    var verBajas by rememberSaveable { mutableStateOf(false) }

    PantallaConDesplazamiento("Funcionarios", alVolver = rutas::volver) {
        BotonPrimario("Agregar funcionario", { rutas.nuevoFuncionario() }, Modifier.fillMaxWidth(), icono = "＋")
        val lista = todos ?: return@PantallaConDesplazamiento
        if (lista.isEmpty()) {
            EstadoVacio(
                "Todavía no hay funcionarios registrados",
                "Agregá a cada persona con el saldo que tiene hoy. Después vas a poder registrarle solicitudes.",
            )
            return@PantallaConDesplazamiento
        }

        Campo("Buscar por nombre", texto, { texto = it }, ayuda = "No importan las tildes ni las mayúsculas.")
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = ALTO_MINIMO)
                .toggleable(value = verBajas, role = Role.Checkbox, onValueChange = { verBajas = it }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = verBajas, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Paleta.Acento))
            Text("Mostrar también a los dados de baja", Modifier.weight(1f).padding(start = 8.dp), color = Paleta.Texto)
        }

        val visibles = repo.buscar(lista, texto, verBajas)
        val activos = lista.count { it.activo }
        TextoSuave(
            "$activos ${if (activos == 1) "funcionario activo" else "funcionarios activos"}" +
                (lista.size - activos).let { if (it > 0) " · $it de baja" else "" },
        )
        if (visibles.isEmpty()) {
            TextoSuave("No hay nadie que coincida con «${texto.trim()}».")
        } else {
            Tarjeta {
                visibles.forEachIndexed { i, f ->
                    if (i > 0) Separador()
                    FilaFuncionario(f, p.horasPorJornada) { rutas.ficha(f.id) }
                }
            }
        }
    }
}

/**
 * Alta o edición. Al editar, el saldo no se muestra: el saldo solo cambia con
 * solicitudes o con un ajuste manual, y siempre deja su rastro.
 */
@Composable
fun PantallaFormularioFuncionario(rutas: Rutas, id: Long?) {
    val repo = LocalContenedor.current.repositorio
    val avisar = LocalAvisar.current
    val alcance = rememberCoroutineScope()
    val p by repo.parametros.collectAsStateWithLifecycle(Parametros())
    val esAlta = id == null

    var cargado by rememberSaveable { mutableStateOf(esAlta) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var cargo by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var fechaIngreso by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var dias by rememberSaveable { mutableStateOf("0") }
    var horas by rememberSaveable { mutableStateOf("0") }
    var errores by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var errorGeneral by rememberSaveable { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    // Al editar, precarga los datos actuales una sola vez.
    LaunchedEffect(id) {
        if (id != null && !cargado) {
            repo.funcionario(id).first()?.let {
                nombre = it.nombreCompleto
                cargo = it.cargo.orEmpty()
                area = it.areaODependencia.orEmpty()
                fechaIngreso = it.fechaIngreso
            }
            cargado = true
        }
    }

    PantallaConDesplazamiento(if (esAlta) "Agregar funcionario" else "Editar datos", alVolver = rutas::volver) {
        if (!cargado) return@PantallaConDesplazamiento
        errorGeneral?.let { Aviso(it, TipoAviso.ERROR) }

        Campo("Nombre completo", nombre, { nombre = it }, error = errores["nombre"], mayusculas = KeyboardCapitalization.Words)
        Campo("Cargo (opcional)", cargo, { cargo = it }, error = errores["cargo"], largoMaximo = 100)
        Campo("Área o dependencia (opcional)", area, { area = it }, error = errores["area"], largoMaximo = 100)
        SelectorFecha("Fecha de ingreso", fechaIngreso, { fechaIngreso = it }, permitirVacia = true, error = errores["fechaIngreso"])

        if (esAlta) {
            Tarjeta {
                Subtitulo("Saldo con el que empieza")
                TextoSuave(
                    "Escribilo en días y horas enteros, nunca con decimales. Para medio día, usá las horas: " +
                        "con jornada de ${p.horasPorJornada} horas, medio día son ${p.horasPorJornada / 2} horas.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoNumero("Días", dias, { dias = it }, Modifier.weight(1f), error = errores["saldoDias"])
                    CampoNumero("Horas", horas, { horas = it }, Modifier.weight(1f), error = errores["saldoHoras"], digitos = 2)
                }
                val d = dias.toIntOrNull() ?: 0
                val h = horas.toIntOrNull() ?: 0
                if (d in 0..3650 && h in 0..23) {
                    Text(
                        "Va a empezar con " + ConversorTiempo.formatear(ConversorTiempo.aMinutos(d, h, 0, p.horasPorJornada), p.horasPorJornada),
                        color = Paleta.AcentoTexto,
                    )
                }
            }
        }

        BotonPrimario(if (esAlta) "Guardar funcionario" else "Guardar cambios", {
            val borrador = BorradorFuncionario(
                nombre, cargo, area, fechaIngreso,
                saldoDias = dias.toIntOrNull() ?: -1,
                saldoHoras = horas.toIntOrNull() ?: -1,
            )
            errores = ReglasDeFuncionario.revisar(borrador, repo.hoy(p.zona), esAlta)
            if (errores.isNotEmpty() || guardando) return@BotonPrimario
            guardando = true
            alcance.launch {
                val r = intentar {
                    if (id == null) repo.crearFuncionario(borrador) else { repo.editarFuncionario(id, borrador); id }
                }
                guardando = false
                when (r) {
                    is Resultado.Bien -> {
                        val minutos = ConversorTiempo.aMinutos(borrador.saldoDias, borrador.saldoHoras, 0, p.horasPorJornada)
                        if (esAlta) {
                            avisar("Listo: se registró a ${nombre.trim()} con ${ConversorTiempo.formatear(minutos, p.horasPorJornada)} de saldo.")
                            rutas.fichaReemplazando(r.valor)
                        } else {
                            avisar("Listo: se guardaron los cambios.")
                            rutas.volver()
                        }
                    }
                    is Resultado.Mal -> errorGeneral = r.mensaje
                }
            }
        }, Modifier.fillMaxWidth(), icono = "✓", habilitado = !guardando)
    }
}
