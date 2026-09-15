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

// ---------------------------------------------------------------------------
// Aviso de sesión por vencer.
//
// La sesión se cierra tras N minutos sin actividad (lo decide el servidor).
// Un minuto antes aparece el aviso con "Seguir trabajando", que hace una
// petición mínima al servidor y así reinicia la cuenta, sin recargar la página
// ni perder lo que se estaba escribiendo.
//
// Cada búsqueda con HTMX también es actividad, así que reinicia la cuenta.
// ---------------------------------------------------------------------------
(function () {
    var meta = document.querySelector('meta[name="sesion-minutos"]');
    var dialogo = document.getElementById('aviso-sesion');
    if (!meta || !dialogo || typeof dialogo.showModal !== 'function') {
        return;
    }

    var duracion = parseInt(meta.content, 10) * 60 * 1000;
    var anticipacion = 60 * 1000;
    var segundos = dialogo.querySelector('[data-segundos-sesion]');
    var botonSeguir = dialogo.querySelector('[data-seguir-trabajando]');
    var temporizadorAviso = null;
    var temporizadorCierre = null;
    var cuentaRegresiva = null;

    function programar() {
        clearTimeout(temporizadorAviso);
        clearTimeout(temporizadorCierre);
        clearInterval(cuentaRegresiva);
        temporizadorAviso = setTimeout(mostrarAviso, Math.max(duracion - anticipacion, 0));
        // Unos segundos de margen: para cuando esto se ejecuta, el servidor
        // seguro ya cerró la sesión, y el inicio redirige al ingreso con aviso.
        temporizadorCierre = setTimeout(sesionVencida, duracion + 5000);
    }

    function mostrarAviso() {
        var restantes = Math.round(anticipacion / 1000);
        if (segundos) {
            segundos.textContent = restantes;
        }
        cuentaRegresiva = setInterval(function () {
            restantes = Math.max(restantes - 1, 0);
            if (segundos) {
                segundos.textContent = restantes;
            }
        }, 1000);
        if (!dialogo.open) {
            dialogo.showModal();
        }
    }

    function sesionVencida() {
        clearInterval(cuentaRegresiva);
        window.location.href = '/';
    }

    function seguirTrabajando() {
        fetch('/sesion/mantener', { credentials: 'same-origin', redirect: 'manual' })
            .then(function (respuesta) {
                if (respuesta.status === 204) {
                    clearInterval(cuentaRegresiva);
                    dialogo.close();
                    programar();
                } else {
                    sesionVencida();
                }
            })
            .catch(function () {
                // Sin conexión: se cierra el aviso y se vuelve a intentar al próximo aviso.
                clearInterval(cuentaRegresiva);
                dialogo.close();
                programar();
            });
    }

    botonSeguir.addEventListener('click', seguirTrabajando);

    // Escape cierra el diálogo: lo tomamos como "seguir trabajando".
    dialogo.addEventListener('cancel', function (evento) {
        evento.preventDefault();
        seguirTrabajando();
    });

    document.addEventListener('htmx:afterRequest', programar);
    programar();
})();

// ---------------------------------------------------------------------------
// Página "el sistema se está despertando": vuelve a intentar sola, con cuenta
// regresiva. Solo aparece en consultas, nunca después de enviar un formulario.
// ---------------------------------------------------------------------------
(function () {
    var aviso = document.querySelector('[data-reintentar-en]');
    if (!aviso) {
        return;
    }
    var segundos = parseInt(aviso.getAttribute('data-reintentar-en'), 10) || 8;
    var contador = aviso.querySelector('[data-segundos-reintento]');
    var intervalo = setInterval(function () {
        segundos -= 1;
        if (contador) {
            contador.textContent = Math.max(segundos, 0);
        }
        if (segundos <= 0) {
            clearInterval(intervalo);
            window.location.reload();
        }
    }, 1000);
})();

