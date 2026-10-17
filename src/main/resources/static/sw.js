/*
 * Service worker: el pequeño programa que convierte el sistema en una
 * aplicación instalable en el celular.
 *
 * Qué hace y qué NO hace, a propósito:
 *
 * - Las PANTALLAS (saldos, solicitudes, funcionarios) siempre se piden al
 *   servidor y NUNCA se guardan en el celular. Son datos personales de
 *   trabajadores: si el teléfono se pierde, no queda nada guardado en él, y
 *   Brenda nunca ve un saldo viejo creyendo que es el actual.
 * - Si no hay internet, en vez del error del navegador se muestra una
 *   pantalla propia, en español, con un botón para reintentar.
 * - Los archivos fijos (estilos, JavaScript, íconos) se piden primero al
 *   servidor; la copia guardada solo se usa sin conexión, para que la pantalla
 *   de "sin conexión" se vea con los colores del sistema.
 * - Formularios (POST) y búsquedas de HTMX pasan directo, sin tocarlos.
 */

// Cambiar este número obliga a renovar la copia guardada en los celulares.
const VERSION = 'v1';
const CACHE = 'vacaciones-' + VERSION;

const PANTALLA_SIN_CONEXION = '/sin-conexion.html';

const ARCHIVOS_FIJOS = [
    PANTALLA_SIN_CONEXION,
    '/css/estilos.css',
    '/js/app.js',
    '/iconos/icono-192.png'
];

self.addEventListener('install', function (evento) {
    evento.waitUntil(
        caches.open(CACHE)
            .then(function (cache) { return cache.addAll(ARCHIVOS_FIJOS); })
            .then(function () { return self.skipWaiting(); })
    );
});

self.addEventListener('activate', function (evento) {
    // Borra las copias de versiones anteriores.
    evento.waitUntil(
        caches.keys()
            .then(function (nombres) {
                return Promise.all(nombres
                    .filter(function (nombre) { return nombre.startsWith('vacaciones-') && nombre !== CACHE; })
                    .map(function (nombre) { return caches.delete(nombre); }));
            })
            .then(function () { return self.clients.claim(); })
    );
});

function esArchivoFijo(url) {
    return url.pathname.startsWith('/css/')
        || url.pathname.startsWith('/js/')
        || url.pathname.startsWith('/iconos/');
}

self.addEventListener('fetch', function (evento) {
    const peticion = evento.request;
    if (peticion.method !== 'GET') {
        return; // formularios: directo al servidor
    }
    const url = new URL(peticion.url);
    if (url.origin !== self.location.origin) {
        return;
    }

    // Una pantalla completa: siempre del servidor; sin red, la pantalla propia.
    if (peticion.mode === 'navigate') {
        evento.respondWith(
            fetch(peticion).catch(function () {
                return caches.match(PANTALLA_SIN_CONEXION);
            })
        );
        return;
    }

    // Estilos, JavaScript e íconos: primero el servidor, la copia solo sin red.
    if (esArchivoFijo(url)) {
        evento.respondWith(
            fetch(peticion)
                .then(function (respuesta) {
                    if (respuesta.ok) {
                        const copia = respuesta.clone();
                        caches.open(CACHE).then(function (cache) { cache.put(peticion, copia); });
                    }
                    return respuesta;
                })
                .catch(function () { return caches.match(peticion); })
        );
    }
    // Todo lo demás (búsquedas de HTMX, etc.) pasa sin tocarse.
});
