# Repaso de seguridad

Qué protege cada medida del sistema, en palabras simples, y qué falta para producción.

---

## 1. Contraseñas

| Medida | Qué protege |
|---|---|
| **BCrypt con costo 12** | Si alguien se roba la base de datos, no ve contraseñas: ve hashes. Cada verificación tarda unos 0,25 s a propósito, así que probar millones de contraseñas contra esos hashes lleva años. |
| **Sal aleatoria por contraseña** (incluida en BCrypt) | Dos personas con la misma contraseña tienen hashes distintos. No se puede usar una tabla precalculada de contraseñas comunes. |
| **Mínimo 12 caracteres y lista de contraseñas obvias** | Evita las contraseñas que se adivinan primero. No exige mayúsculas ni símbolos, porque eso lleva a contraseñas cortas anotadas en papel. |
| **Máximo 72 bytes** | BCrypt ignora lo que pasa de 72 bytes. Así nadie cree que su contraseña larga es más segura de lo que es. |
| **Contraseña temporal generada por el sistema** (`k7mp-x2qd-9tha`) | Nadie inventa una débil para otra persona. Se genera con `SecureRandom`, que no es predecible. Se muestra una sola vez y nunca se guarda en texto plano. |
| **Cambio obligatorio en el primer ingreso** | Quien entregó la contraseña temporal deja de conocer la contraseña real. |
| **Nunca en los registros** | Probado: la contraseña no aparece en el registro de la aplicación. |

## 2. Ingreso

| Medida | Qué protege |
|---|---|
| **Bloqueo de 15 minutos al quinto intento fallido** | Frena el ataque de probar contraseñas. Con 5 intentos cada 15 minutos, adivinar una de 12 caracteres es inviable. |
| **Los intentos no alargan un bloqueo en curso** | Nadie puede dejar a Brenda afuera para siempre escribiendo mal su contraseña cada 15 minutos. |
| **Mensaje igual para "usuario no existe" y "contraseña equivocada"** | No confirma qué nombres de usuario existen. |
| **Bloqueo inmediato desde el panel** | Si alguien deja la oficina, se bloquea y su sesión abierta se cierra en el siguiente clic, sin esperar a que venza. |
| **No se puede bloquear al único administrador activo** | Evita quedarse afuera del propio sistema. |
| **Sin registro público** | Solo un administrador crea usuarios. |

**Compromiso aceptado.** Los mensajes "tu usuario quedó bloqueado" y "tu usuario está bloqueado" revelan que ese nombre de usuario existe. Lo pide la especificación ("mensaje claro") y el costo es bajo: saber que existe "brenda" no ayuda a adivinar su contraseña con 5 intentos cada 15 minutos.

## 3. Sesión y cookie

| Medida | Qué protege |
|---|---|
| **Sesión del lado del servidor** | En el navegador solo viaja un identificador al azar, no datos del usuario. |
| **Cookie `HttpOnly`** | Un JavaScript malicioso no puede leer la cookie de sesión. |
| **Cookie `Secure`** (en producción) | La cookie nunca viaja por HTTP sin cifrar. |
| **Cookie `SameSite=Strict`** | Otro sitio no puede hacer que el navegador de Brenda envíe peticiones con su sesión. |
| **Sesión nueva al ingresar** (contra fijación de sesión) | Nadie puede darle a Brenda un identificador de sesión conocido de antemano y usarlo después de que ella entre. |
| **Vence a los 30 minutos sin actividad, con aviso un minuto antes** | Una computadora olvidada abierta no queda con la sesión viva. |
| **Cerrar sesión solo por POST con token** | Un enlace malicioso no le puede cerrar la sesión a nadie. |

## 4. Formularios

| Medida | Qué protege |
|---|---|
| **Token CSRF en cada formulario** | Otro sitio no puede enviar formularios en nombre de Brenda (por ejemplo, anular una solicitud). Un formulario con token vencido no se procesa y se avisa en el inicio. |
| **Toda validación en el servidor** | La validación del navegador se puede saltar. Las reglas del saldo, las fechas y los motivos obligatorios se revisan de nuevo en el servidor, y otra vez al guardar. |
| **Patrón POST → redirección → GET** | Recargar la página nunca vuelve a enviar un formulario. |
| **Botón deshabilitado tras el primer clic** | Un doble clic no registra dos veces. |

## 5. Cabeceras que se envían en cada respuesta

| Cabecera | Qué protege |
|---|---|
| **`Content-Security-Policy`** sin `unsafe-inline` | Si alguien lograra inyectar un `<script>` en una página, el navegador no lo ejecuta. Solo corre JavaScript de archivos propios. |
| **`X-Frame-Options: DENY`** y `frame-ancestors 'none'` | Nadie puede meter la aplicación dentro de otra página invisible para engañar a Brenda y que haga clic donde no quiere (*clickjacking*). |
| **`X-Content-Type-Options: nosniff`** | El navegador no "adivina" el tipo de un archivo, así un archivo de texto no se ejecuta como script. |
| **`Strict-Transport-Security`** (HSTS, un año) | Una vez que el navegador visitó el sitio por HTTPS, se niega a volver por HTTP. |
| **`Referrer-Policy: same-origin`** | Las direcciones internas (con ids de funcionarios) no se filtran a otros sitios. |
| **`Cache-Control: no-store`** | Datos personales y contraseñas temporales no quedan guardados en la caché del navegador. |

## 6. Datos

| Medida | Qué protege |
|---|---|
| **Consultas con parámetros (JPA)** | Contra inyección SQL: lo que escribe el usuario nunca se pega dentro de una consulta. |
| **Thymeleaf escapa todo lo que muestra** | Contra XSS: un nombre como `<script>` se muestra como texto, no se ejecuta. |
| **Datos mínimos de salud** | Solo se guarda que fue una "cita médica". Nunca diagnósticos ni archivos, y la pantalla recuerda no escribirlos en el motivo. |
| **Borrado lógico** | Funcionarios y solicitudes no se borran nunca: la historia no se puede perder ni esconder. |
| **Restricciones en la base** (`CHECK`, claves foráneas) | Aunque el código tuviera un error, la base no acepta tipos inválidos, anulaciones sin motivo ni movimientos de cero minutos. |
| **Filas bloqueadas al mover el saldo** | Dos registros simultáneos no gastan dos veces el mismo saldo, y dos anulaciones no devuelven el tiempo dos veces. |
| **Libro contable del saldo** | Cada cambio de saldo tiene su renglón. La suma de los movimientos siempre es igual al saldo, y hay una prueba que lo verifica. |

## 7. Bitácora

Queda registrado, con usuario, fecha, hora e IP:

- ingresos exitosos y fallidos, y bloqueos por intentos;
- usuarios creados, bloqueados y desbloqueados, y contraseñas restablecidas o cambiadas;
- funcionarios registrados, dados de baja y reactivados;
- solicitudes registradas y anuladas, ajustes manuales y saldos negativos autorizados;
- cambios de parámetros, con el valor anterior.

La bitácora se escribe dentro de la misma transacción que la acción: nunca registra algo que no pasó. No hay pantalla ni código para editarla o borrarla.

## 8. Secretos

| Medida | Qué protege |
|---|---|
| **Todo por variables de entorno** | Ninguna credencial está en el repositorio. Que alguien lea el código no le da acceso a nada. |
| **`.env` en `.gitignore`** | El archivo con valores reales no se sube por accidente. |
| **Sin contraseña por omisión** | Si faltan las variables del administrador inicial, la aplicación no arranca, en lugar de arrancar con una clave conocida. |

---

## Producción

| Medida | Qué protege | Cómo quedó |
|---|---|---|
| **HTTPS de punta a punta** | Nadie en la red de la oficina puede leer contraseñas ni datos. | Render redirige todo HTTP a HTTPS en su borde. La redirección propia de la aplicación queda como segunda barrera opcional (`EXIGIR_HTTPS=true`). Está apagada por defecto porque, si Tomcat no reconociera al balanceador, entraría en bucle. |
| **Cabeceras del balanceador leídas solo si vienen de la red interna** (`forward-headers-strategy: native`) | La bitácora guarda la IP real, y HSTS se envía. | **Probado en el contenedor:** con una petición desde la red interna se anotó la IP real (`203.0.113.9`). Con un `X-Forwarded-For: 1.2.3.4, 198.51.100.7` falsificado se ignoró la IP inventada. Desde afuera, las cabeceras se ignoran. |
| **Cookie `Secure` forzada** por el perfil `prod` | La sesión nunca viaja sin cifrar. | No depende de ninguna variable. Probado en el contenedor. |
| **Conexión a Neon con `sslmode=require&channelBinding=require`** | Cifra el tráfico con la base e impide que un intermediario se haga pasar por Neon. | En la cadena JDBC de producción. |
| **Imagen sin root y sin herramientas de compilación** | Si alguien lograra ejecutar algo dentro del contenedor, no tendría permisos de administrador ni un compilador. | Usuario `aplicacion` (uid 100), solo el JRE. |
| **Solo `/actuator/health` expuesto, sin detalles** | No se filtran versiones, variables ni el estado interno. | `liveness` no consulta la base. |
| **Despliegue solo con pruebas en verde** | Un cambio que rompe una regla del saldo no llega a producción. | `autoDeployTrigger: checksPass` en `render.yaml`. |
| **Respaldos cifrados en tránsito y fuera de Neon** | Si Neon pierde la base o desaparece el plan gratuito, los datos siguen existiendo. | `scripts/respaldar.sh`, con archivos solo legibles por su dueño. **Probado:** respaldo y restauración idénticos tabla por tabla. |

**Tareas que quedan para siempre:**

- **Dependencias al día.** Una vez por mes, revisar si hay versiones nuevas de Spring Boot con arreglos de seguridad.
- **Respaldos.** Revisar que el temporizador semanal siga corriendo y, cada tanto, probar una restauración en una base aparte.
- **Usuarios.** Bloquear de inmediato a quien deje la oficina.

## Decisiones conscientes

- **Sin 2FA.** Se decidió no implementarlo: agrega el riesgo de que Brenda quede afuera si pierde el teléfono, y el sistema no guarda datos médicos ni financieros.
- **Bloqueo por usuario, no por IP.** Es lo que pide la especificación. Un límite por IP ayudaría contra quien prueba muchos usuarios distintos, pero con pocos usuarios y sin registro público el beneficio es chico.
