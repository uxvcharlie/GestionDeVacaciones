/*
 * JavaScript propio de la aplicación.
 *
 * Va en un archivo aparte (y no dentro del HTML) porque la política de
 * seguridad del navegador (CSP) no permite JavaScript incrustado.
 */

// Buscador de empleados: después de cada búsqueda, anuncia a los lectores de
// pantalla cuántos resultados quedaron ("3 empleados"). El anuncio vive fuera
// de la zona que HTMX reemplaza; si viviera adentro, el lector no lo leería.
document.addEventListener('htmx:afterSwap', function () {
    var conteo = document.querySelector('[data-conteo]');
    var anuncio = document.getElementById('anuncio-busqueda');
    if (conteo && anuncio) {
        anuncio.textContent = conteo.textContent;
    }
});
