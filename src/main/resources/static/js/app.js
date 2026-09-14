/*
 * JavaScript propio de la aplicación.
 *
 * Va en un archivo aparte (y no dentro del HTML) porque la política de
 * seguridad del navegador (CSP) no permite JavaScript incrustado.
 *
 * Todo lo de acá es COMODIDAD. Sin JavaScript la aplicación funciona igual, y
 * todas las reglas se revisan en el servidor.
 */

// ---------------------------------------------------------------------------
// Buscador de funcionarios: después de cada búsqueda, anuncia a los lectores de
// pantalla cuántos resultados quedaron ("3 funcionarios"). El anuncio vive fuera
// de la zona que HTMX reemplaza; si viviera adentro, el lector no lo leería.
// ---------------------------------------------------------------------------
document.addEventListener('htmx:afterSwap', function () {
    var conteo = document.querySelector('[data-conteo]');
    var anuncio = document.getElementById('anuncio-busqueda');
    if (conteo && anuncio) {
        anuncio.textContent = conteo.textContent;
    }
});

// ---------------------------------------------------------------------------
// Formulario de solicitud
// ---------------------------------------------------------------------------
(function () {
    var formulario = document.querySelector('[data-formulario-solicitud]');
    if (!formulario) {
        return;
    }

    var radios = formulario.querySelectorAll('[data-tipo-solicitud]');
    var inicio = formulario.querySelector('[data-fecha-inicio]');
    var fin = formulario.querySelector('[data-fecha-fin]');
    var aviso = formulario.querySelector('[data-dias-corridos]');

    function tipoElegido() {
        for (var i = 0; i < radios.length; i++) {
            if (radios[i].checked) {
                return radios[i].value;
            }
        }
        return null;
    }

    // Vacaciones se escriben en días; citas y permisos, en horas y minutos.
    // La nota de "no escribas datos de salud" solo aparece en citas médicas.
    function actualizarTipo() {
        var tipo = tipoElegido();
        formulario.querySelectorAll('[data-cantidad]').forEach(function (bloque) {
            var esDeVacaciones = bloque.getAttribute('data-cantidad') === 'VACACIONES';
            bloque.hidden = tipo !== null && (tipo === 'VACACIONES') !== esDeVacaciones;
        });
        formulario.querySelectorAll('[data-solo-tipo]').forEach(function (elemento) {
            elemento.hidden = tipo !== null && elemento.getAttribute('data-solo-tipo') !== tipo;
        });
    }

    // Referencia de días corridos. Solo informa: la cantidad la escribe Brenda.
    function actualizarDias() {
        if (!aviso || !inicio || !fin) {
            return;
        }
        if (!inicio.value || !fin.value) {
            aviso.textContent = '';
            return;
        }
        var desde = Date.parse(inicio.value + 'T00:00:00Z');
        var hasta = Date.parse(fin.value + 'T00:00:00Z');
        var dias = Math.round((hasta - desde) / 86400000) + 1;
        if (dias < 1) {
            aviso.textContent = 'La fecha final es anterior a la de inicio.';
        } else if (dias === 1) {
            aviso.textContent = 'Es un solo día.';
        } else {
            aviso.textContent = 'Ese rango abarca ' + dias + ' días corridos, contando sábados y domingos. '
                + 'Es solo una referencia: la cantidad la escribís vos.';
        }
    }

    // Si la fecha de inicio pasa a ser posterior a la final, la final la acompaña.
    // Así registrar un solo día es escribir una sola fecha.
    function acompanarFechaFinal() {
        if (inicio.value && (!fin.value || fin.value < inicio.value)) {
            fin.value = inicio.value;
        }
        actualizarDias();
    }

    radios.forEach(function (radio) {
        radio.addEventListener('change', actualizarTipo);
    });
    if (inicio && fin) {
        inicio.addEventListener('change', acompanarFechaFinal);
        fin.addEventListener('change', actualizarDias);
    }

    actualizarTipo();
    actualizarDias();
})();

// ---------------------------------------------------------------------------
// Evitar el doble envío: un doble clic en "Guardar" no registra dos veces.
// El botón se deshabilita un instante DESPUÉS de enviar; si se deshabilitara
// antes, el navegador no lo incluiría en el envío.
// ---------------------------------------------------------------------------
document.addEventListener('submit', function (evento) {
    var boton = evento.submitter;
    if (boton && boton.hasAttribute('data-un-solo-envio')) {
        setTimeout(function () {
            boton.disabled = true;
        }, 0);
    }
});

// Si se vuelve con el botón "atrás" del navegador, los botones vuelven a funcionar.
window.addEventListener('pageshow', function (evento) {
    if (evento.persisted) {
        document.querySelectorAll('[data-un-solo-envio]').forEach(function (boton) {
            boton.disabled = false;
        });
    }
});
