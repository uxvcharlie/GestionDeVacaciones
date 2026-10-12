package ni.gestionvacaciones.movil.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ni.gestionvacaciones.movil.datos.FuncionarioEntidad
import ni.gestionvacaciones.movil.datos.MovimientoEntidad
import ni.gestionvacaciones.movil.datos.Parametros
import ni.gestionvacaciones.movil.datos.SolicitudEntidad
import ni.gestionvacaciones.movil.dominio.AccionBitacora
import ni.gestionvacaciones.movil.dominio.ConversorTiempo
import ni.gestionvacaciones.movil.dominio.EstadoSolicitud
import ni.gestionvacaciones.movil.dominio.Fechas
import ni.gestionvacaciones.movil.dominio.TipoSolicitud
import ni.gestionvacaciones.movil.reporte.FormatoReporte
import ni.gestionvacaciones.movil.reporte.Reportes
import ni.gestionvacaciones.movil.ui.LocalAvisar
import ni.gestionvacaciones.movil.ui.LocalContenedor
import ni.gestionvacaciones.movil.ui.Resultado
import ni.gestionvacaciones.movil.ui.Rutas
import ni.gestionvacaciones.movil.ui.componentes.Aviso
import ni.gestionvacaciones.movil.ui.componentes.BotonPeligro
import ni.gestionvacaciones.movil.ui.componentes.BotonPrimario
import ni.gestionvacaciones.movil.ui.componentes.BotonSecundario
import ni.gestionvacaciones.movil.ui.componentes.Campo
import ni.gestionvacaciones.movil.ui.componentes.Dato
import ni.gestionvacaciones.movil.ui.componentes.DialogoConfirmacion
import ni.gestionvacaciones.movil.ui.componentes.Insignia
import ni.gestionvacaciones.movil.ui.componentes.PantallaConDesplazamiento
import ni.gestionvacaciones.movil.ui.componentes.Separador
import ni.gestionvacaciones.movil.ui.componentes.Subtitulo
import ni.gestionvacaciones.movil.ui.componentes.Tarjeta
import ni.gestionvacaciones.movil.ui.componentes.TextoSuave
import ni.gestionvacaciones.movil.ui.componentes.TipoAviso
import ni.gestionvacaciones.movil.ui.componentes.rememberExportador
import ni.gestionvacaciones.movil.ui.intentar
import ni.gestionvacaciones.movil.ui.tema.EstiloSaldo
import ni.gestionvacaciones.movil.ui.tema.Paleta

/** La ficha: datos, saldo bien grande, historial completo y acciones. */
@Composable
fun PantallaFicha(rutas: Rutas, id: Long) {
    val repo = LocalContenedor.current.repositorio
    val avisar = LocalAvisar.current
    val alcance = rememberCoroutineScope()
    val p by repo.parametros.collectAsStateWithLifecycle(Parametros())
    val funcionario by remember(id) { repo.funcionario(id) }.collectAsStateWithLifecycle(null)
    val solicitudes by remember(id) { repo.solicitudesDe(id) }.collectAsStateWithLifecycle(emptyList())
    val movimientos by remember(id) { repo.movimientosDe(id) }.collectAsStateWithLifecycle(emptyList())

    var confirmarBaja by rememberSaveable { mutableStateOf(false) }
    var aAnular by remember { mutableStateOf<SolicitudEntidad?>(null) }
    val exportador = rememberExportador(
        "El reporte está listo",
        "Podés mandarlo por WhatsApp o correo, o guardarlo en el teléfono para imprimirlo después.",
    )

    val f = funcionario
    PantallaConDesplazamiento("Funcionario", alVolver = rutas::volver) {
        if (f == null) {
            TextoSuave("Cargando…")
            return@PantallaConDesplazamiento
        }
        val j = p.horasPorJornada

        Text(f.nombreCompleto, style = MaterialTheme.typography.headlineMedium, color = Paleta.Texto)
        listOfNotNull(f.cargo, f.areaODependencia).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { TextoSuave(it) }
        if (!f.activo) Insignia("De baja. Su historial se conserva.", "⊘", Paleta.Alerta)

        // --- El saldo, bien grande ---
        Tarjeta {
            TextoSuave("Saldo disponible")
            Text(
                ConversorTiempo.formatear(f.saldoMinutos, j),
                style = EstiloSaldo,
                color = if (f.saldoMinutos < 0) Paleta.Error else Paleta.Texto,
                modifier = Modifier.semantics { contentDescription = "Saldo disponible: ${ConversorTiempo.formatear(f.saldoMinutos, j)}" },
            )
            TextoSuave("Jornada de $j horas")
            if (f.saldoMinutos < 0) Aviso("El saldo está en negativo por una autorización expresa.", TipoAviso.ADVERTENCIA)
            Separador()
            val anio = repo.hoy(p.zona).year
            val desglose = repo.desgloseDelAnio(solicitudes, anio)
            Text("Usado en $anio", style = MaterialTheme.typography.titleSmall, color = Paleta.Texto)
            if (desglose.values.all { it == 0 }) {
                TextoSuave("Todavía no usó tiempo este año.")
            } else {
                TipoSolicitud.entries.forEach { tipo ->
                    TextoSuave("${tipo.etiqueta}: ${tipo.formatear(desglose[tipo] ?: 0, j)}")
                }
            }
        }

        // --- Acciones ---
        if (f.activo) {
            BotonPrimario("Registrar solicitud", { rutas.solicitud(f.id) }, Modifier.fillMaxWidth(), icono = "＋")
            BotonSecundario("Ajustar saldo", { rutas.ajuste(f.id) }, Modifier.fillMaxWidth(), icono = "±")
        }
        BotonSecundario("Editar datos", { rutas.editar(f.id) }, Modifier.fillMaxWidth(), icono = "✎")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormatoReporte.entries.forEach { formato ->
                BotonSecundario("Reporte ${formato.etiqueta}", {
                    alcance.launch {
                        val r = intentar {
                            val (fun_, sol, mov) = repo.datosParaReporteDeFuncionario(f.id)
                            Reportes.deFuncionario(fun_, sol, mov, j, p.zona, repo.ahora(), formato).also {
                                repo.anotar(AccionBitacora.REPORTE_GENERADO, "Reporte de ${f.nombreCompleto} (${formato.etiqueta})")
                            }
                        }
                        when (r) {
                            is Resultado.Bien -> exportador.ofrecer(r.valor)
                            is Resultado.Mal -> avisar(r.mensaje)
                        }
                    }
                }, Modifier.weight(1f), icono = "⬇")
            }
        }
        if (f.activo) {
            BotonPeligro("Dar de baja", { confirmarBaja = true }, Modifier.fillMaxWidth(), icono = "⊘")
        } else {
            TextoSuave("Mientras esté de baja no aparece al registrar solicitudes. Si se dio de baja por error, podés reactivar su registro: el saldo y el historial siguen iguales.")
            BotonPrimario("Reactivar", {
                alcance.launch {
                    when (val r = intentar { repo.reactivar(f.id) }) {
                        is Resultado.Bien -> avisar("Listo: ${f.nombreCompleto} está activo otra vez.")
                        is Resultado.Mal -> avisar(r.mensaje)
                    }
                }
            }, Modifier.fillMaxWidth(), icono = "↺")
        }

        // --- Datos ---
        Subtitulo("Datos")
        Tarjeta {
            Dato("Cargo", f.cargo.orEmpty())
            Dato("Área o dependencia", f.areaODependencia.orEmpty())
            Dato("Fecha de ingreso", Fechas.fecha(f.fechaIngreso))
            Dato("Fecha de registro", Fechas.fechaHora(f.creadoEn, p.zona))
            Dato("Estado", if (f.activo) "✓ Activo" else "⊘ De baja")
        }

        // --- Solicitudes ---
        Subtitulo("Solicitudes")
        if (solicitudes.isEmpty()) {
            TextoSuave("Todavía no tiene solicitudes registradas.")
        } else {
            solicitudes.forEach { s -> TarjetaSolicitud(s, j, p) { aAnular = s } }
        }

        // --- Movimientos del saldo ---
        Subtitulo("Movimientos del saldo")
        TextoSuave("Cada cambio del saldo, del más nuevo al más viejo. El saldo es siempre la suma de estos movimientos.")
        if (movimientos.isEmpty()) {
            TextoSuave("Empezó con saldo cero y todavía no tuvo movimientos.")
        } else {
            Tarjeta { movimientos.forEachIndexed { i, m -> if (i > 0) Separador(); FilaMovimiento(m, j, p) } }
        }
    }

    if (confirmarBaja && f != null) {
        DialogoConfirmacion(
            titulo = "¿Dar de baja a ${f.nombreCompleto}?",
            texto = "Su historial se conserva. Mientras esté de baja no va a aparecer al registrar solicitudes. Si fue un error, se puede reactivar.",
            confirmar = "Dar de baja",
            peligroso = true,
            alCancelar = { confirmarBaja = false },
            alConfirmar = {
                confirmarBaja = false
                alcance.launch {
                    when (val r = intentar { repo.darDeBaja(f.id) }) {
                        is Resultado.Bien -> avisar("${f.nombreCompleto} quedó de baja. Su historial se conserva.")
                        is Resultado.Mal -> avisar(r.mensaje)
                    }
                }
            },
        )
    }

    aAnular?.let { s ->
        if (f != null) DialogoAnular(f, s, p.horasPorJornada, alCerrar = { aAnular = null })
    }
}

@Composable
private fun TarjetaSolicitud(s: SolicitudEntidad, jornada: Int, p: Parametros, alAnular: () -> Unit) {
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.tipo.icono, modifier = Modifier.padding(end = 12.dp).clearAndSetSemantics { }, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { }) {
                Text(s.tipo.etiqueta, style = MaterialTheme.typography.titleMedium, color = Paleta.Texto)
                TextoSuave(Fechas.rango(s.fechaInicio, s.fechaFin))
            }
            if (s.estado == EstadoSolicitud.REGISTRADA) Insignia("Registrada", "✓", Paleta.Exito)
            else Insignia("Anulada", "✕", Paleta.Error)
        }
        Text(s.tipo.formatear(s.minutos, jornada), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Paleta.Texto)
        s.motivo?.let { TextoSuave("Motivo: $it") }
        TextoSuave("Registrada el ${Fechas.fechaHora(s.creadoEn, p.zona)}")
        if (s.estado == EstadoSolicitud.ANULADA) {
            Text("Anulada el ${Fechas.fechaHora(s.anuladoEn, p.zona)} · Motivo: ${s.motivoAnulacion}", color = Paleta.Error)
        } else {
            BotonPeligro("Anular", alAnular, icono = "✕")
        }
    }
}

@Composable
private fun FilaMovimiento(m: MovimientoEntidad, jornada: Int, p: Parametros) {
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }) {
        Row {
            Text(m.tipo.etiqueta, Modifier.weight(1f), color = Paleta.Texto, fontWeight = FontWeight.SemiBold)
            Text(
                ConversorTiempo.formatearConSigno(m.minutos, jornada),
                color = if (m.minutos > 0) Paleta.Exito else Paleta.Error,
                fontWeight = FontWeight.SemiBold,
            )
        }
        TextoSuave("${Fechas.fechaHora(m.creadoEn, p.zona)} · Saldo: ${ConversorTiempo.formatear(m.saldoResultante, jornada)}")
        m.descripcion?.let { TextoSuave(it) }
    }
}

/** Confirmación de anulación, con el nombre y lo que se devuelve. El motivo es obligatorio. */
@Composable
private fun DialogoAnular(f: FuncionarioEntidad, s: SolicitudEntidad, jornada: Int, alCerrar: () -> Unit) {
    val repo = LocalContenedor.current.repositorio
    val avisar = LocalAvisar.current
    val alcance = rememberCoroutineScope()
    var motivo by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    DialogoConfirmacion(
        titulo = "¿Anular esta solicitud?",
        texto = "${s.tipo.etiqueta} de ${f.nombreCompleto} ${Fechas.rango(s.fechaInicio, s.fechaFin)}. " +
            "Se devuelven ${s.tipo.formatear(s.minutos, jornada)} a su saldo. " +
            "La solicitud no se borra: queda en el historial como anulada.",
        confirmar = "Anular solicitud",
        peligroso = true,
        alCancelar = alCerrar,
        alConfirmar = {
            alcance.launch {
                when (val r = intentar { repo.anularSolicitud(s.id, motivo) }) {
                    is Resultado.Bien -> {
                        avisar("Solicitud anulada. Saldo de ${f.nombreCompleto}: ${ConversorTiempo.formatear(r.valor.saldoAntes, jornada)} → ${ConversorTiempo.formatear(r.valor.saldoDespues, jornada)}.")
                        alCerrar()
                    }
                    is Resultado.Mal -> error = r.mensaje
                }
            }
        },
    ) {
        Campo("Motivo de la anulación", motivo, { motivo = it; error = null }, error = error, lineas = 2, largoMaximo = 300)
    }
}
