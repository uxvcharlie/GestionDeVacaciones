# Registro de Vacaciones, Citas Médicas y Permisos

Aplicación web para llevar el control de vacaciones, citas médicas y permisos
de los funcionarios de una oficina administrativa.

Es un **registro**, no un trámite: el permiso ya fue autorizado fuera del
sistema y acá solo se anota, se descuenta del saldo y queda el historial. No
hay estados "pendiente", "aprobado" ni "rechazado".

- **Manual para quien usa el sistema:** [docs/manual-brenda.md](docs/manual-brenda.md)
- **Repaso de seguridad:** [docs/seguridad.md](docs/seguridad.md)

---

## La idea central: todo se guarda en minutos

En la base de datos no existen los días. Existe un número entero de **minutos**.
Los días son solo una forma de mostrarlos, usando el parámetro
`horas_por_jornada` (por omisión, 8):

```
minutos_por_dia = horas_por_jornada * 60        ->  8 * 60 = 480
dias            = minutos / minutos_por_dia     (división entera)
horas_sueltas   = (minutos % minutos_por_dia) / 60
```

| Momento                                   | En la base de datos | En pantalla      |
|-------------------------------------------|--------------------:|------------------|
| Saldo inicial de 10 días                  |                4800 | 10 días          |
| Se registra 1 día de vacaciones (−480)    |                4320 | 9 días           |
| Se registra una cita médica de 2 h (−120) |                4200 | 8 días y 6 horas |

Con enteros, sumar y restar siempre da exacto. Con decimales, `8.75` termina
convertido en `8.749999999998` después de varias operaciones, y en un registro
laboral eso es un reclamo.

---

## Qué hace

- **Funcionarios:** alta con saldo inicial, buscador sin tildes, ficha con saldo
  e historial, edición, baja lógica y reactivación. Nada se borra.
- **Solicitudes:** vacaciones, citas médicas y permisos descuentan del mismo
  saldo en el momento. Resumen antes de guardar. El saldo negativo solo se
  permite con autorización expresa y motivo. Anular devuelve el tiempo.
- **Ajuste manual** del saldo, siempre con motivo.
- **Tablero:** saludo según la hora de Managua, buscador, resumen del mes y
  últimas solicitudes.
- **Reportes** en Excel y CSV, por funcionario y por mes.
- **Administración:** usuarios (crear, bloquear, desbloquear, restablecer
  contraseña), bitácora y parámetros.
- **Seguridad:** bloqueo por intentos fallidos, sesión que vence con aviso,
  CSRF, CSP estricta, HTTPS obligatorio en producción. Ver
  [docs/seguridad.md](docs/seguridad.md).

## Con qué está hecho

Java 21 · Spring Boot 4.1 (Web, Security, Data JPA, Validation, Thymeleaf,
Actuator) · Flyway · PostgreSQL 17 · HTMX · Tailwind CSS 4 · fastexcel ·
JUnit 5 y Testcontainers · Podman · GitHub Actions · Render · Neon.

Todo se sirve desde la propia aplicación: sin CDN ni fuentes remotas.

---

## Correr en tu computadora

### Qué necesitás

| Herramienta | Para qué             | En Fedora                                   |
|-------------|----------------------|---------------------------------------------|
| JDK 21+     | Compilar y correr    | `sudo dnf install java-25-openjdk-devel`    |
| Podman      | PostgreSQL local     | `sudo dnf install podman`                   |
| Node.js     | Solo si cambiás estilos | `sudo dnf install nodejs npm`            |

Maven **no** hace falta instalarlo: el proyecto trae `./mvnw`.

**Ojo con el JDK en Fedora.** `java-25-openjdk-headless` solo *ejecuta* Java:
no trae compilador. Si `./mvnw` falla con `release version 21 not supported`,
te falta el paquete `-devel`. Compilar con el JDK 25 genera código compatible
con Java 21.

### 1. PostgreSQL con Podman

```bash
podman run -d \
  --name vacaciones-db \
  -e POSTGRES_DB=vacaciones \
  -e POSTGRES_USER=vacaciones \
  -e POSTGRES_PASSWORD=poneUnaClaveLargaAca \
  -p 5432:5432 \
  -v vacaciones-datos:/var/lib/postgresql/data \
  docker.io/library/postgres:17-alpine
```

**Si ya tenés PostgreSQL instalado en el sistema**, el puerto 5432 está ocupado.
Usá `-p 5433:5432` y en `.env` poné
`DB_URL=jdbc:postgresql://localhost:5433/vacaciones`. Para saberlo:
`ss -ltn | grep 5432`.

Apagar y encender (los datos quedan en el volumen):

```bash
podman stop vacaciones-db
podman start vacaciones-db
```

### 2. El archivo `.env`

```bash
cp .env.example .env
```

`DB_PASSWORD` tiene que ser la misma que usaste en Podman, y
`ADMIN_PASSWORD_INICIAL` debe tener al menos 12 caracteres. Los valores con
espacios van **entre comillas**: `ADMIN_NOMBRE="Brenda Vásquez"`.

### 3. Arrancar

```bash
./ejecutar-local.sh
```

Abrí **http://localhost:8080**. La primera vez se crea el administrador inicial
y el sistema le exige cambiar la contraseña.

### Probar la imagen de producción en tu computadora

```bash
podman build --network=host -t gestion-vacaciones .
podman run --rm --memory=512m -p 8080:8080 --env-file .env \
  -e DB_URL=jdbc:postgresql://host.containers.internal:5433/vacaciones \
  gestion-vacaciones
```

`--network=host` en la construcción: en algunas instalaciones de Fedora el
contenedor que compila no resuelve nombres de internet (`bad address
repo.maven.apache.org`) y Maven no puede descargar dependencias.
`--memory=512m` simula el límite del plan gratuito de Render: la aplicación
usa alrededor de 280 MB.

---

## Pruebas

Levantan un PostgreSQL de verdad con Testcontainers. Con Podman, una sola vez:

```bash
systemctl --user enable --now podman.socket
```

Y cada vez:

```bash
export DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
./mvnw test
```

En GitHub corren solas en cada push (`.github/workflows/pruebas.yml`). Esa
tarea también verifica que la hoja de estilos esté regenerada.

---

## Modificar la interfaz

```bash
npm install          # una sola vez
npm run css          # regenera src/main/resources/static/css/estilos.css
npm run css:vigilar  # lo regenera solo al guardar
npm run htmx         # copia HTMX desde node_modules a static/js
```

El CSS generado **se versiona**: así la imagen de producción no necesita Node.
Si cambiás clases en un `.html`, regenerá antes del commit; si te olvidás, la
tarea de GitHub avisa.

---

## Variables de entorno

| Variable                 | Local          | Producción (Render)                          |
|--------------------------|----------------|----------------------------------------------|
| `DB_URL`                 | `jdbc:postgresql://localhost:5433/vacaciones` | JDBC de Neon (ver abajo) |
| `DB_USUARIO`             | `vacaciones`   | usuario de Neon                              |
| `DB_PASSWORD`            | la de Podman   | contraseña de Neon                           |
| `ADMIN_USERNAME`         | `brenda`       | el usuario de Brenda                         |
| `ADMIN_PASSWORD_INICIAL` | 12+ caracteres | temporal, 12+ caracteres                     |
| `ADMIN_NOMBRE`           | `"Brenda Vásquez"` | `Brenda Vásquez`                         |
| `COOKIE_SEGURA`          | `false`        | no hace falta: el perfil `prod` la fuerza    |
| `SPRING_PROFILES_ACTIVE` | (vacío)        | `prod`                                       |
| `EXIGIR_HTTPS`           | no hace falta  | opcional: `true` (ver "HTTPS" más abajo)     |
| `PORT`                   | `8080`         | lo define Render                             |

El perfil `prod` (`application-prod.yml`) fuerza la cookie `Secure` y hace
que Tomcat lea las cabeceras del balanceador de Render: así la bitácora guarda
la IP real y se envía HSTS.

---

## Despliegue

**Resumen:** la base en **Neon**, la aplicación en **Render**, ambas gratis y
en Ohio. Render construye la imagen desde este repositorio con el `Dockerfile`
y la describe `render.yaml`.

### 1. Base de datos en Neon

1. Entrá a **https://neon.com** y creá una cuenta (podés usar la de GitHub).
2. **Create project**:
   - *Name:* `gestion-vacaciones`
   - *Postgres version:* **17** (la misma que en local)
   - *Region:* **AWS US East 2 (Ohio)**
3. En el proyecto, tocá **Connect**:
   - **Desmarcá "Connection pooling".** Flyway usa bloqueos de sesión para
     aplicar migraciones, y el pooler de Neon trabaja por transacción. Con la
     conexión directa no hay sorpresas; esta aplicación usa pocas conexiones.
   - Elegí mostrar la contraseña y copiá los datos. Vas a ver algo como
     `postgresql://neondb_owner:ABC123@ep-algo-123456.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require`.
4. De ahí salen las tres variables de Render:

   | Variable      | Valor (ejemplo)                                                                  |
   |---------------|----------------------------------------------------------------------------------|
   | `DB_URL`      | `jdbc:postgresql://ep-algo-123456.us-east-2.aws.neon.tech/neondb?sslmode=require&channelBinding=require` |
   | `DB_USUARIO`  | `neondb_owner`                                                                   |
   | `DB_PASSWORD` | `ABC123`                                                                         |

   Fijate que la JDBC empieza con `jdbc:`, **no** lleva usuario ni contraseña
   adentro, y el parámetro se escribe `channelBinding` (sin guion bajo).

No hace falta crear tablas: Flyway crea todo en el primer arranque.

**Qué incluye el plan gratuito de Neon:** 0,5 GB de almacenamiento, 100
CU-horas de cómputo al mes y restauración a cualquier punto de las últimas 6
horas. La base se apaga sola tras 5 minutos sin uso y despierta en segundos.

### 2. Aplicación en Render

1. Pasá lo que esté en `desarrollo` a `main` (Render despliega `main`):
   ```bash
   git checkout main && git merge desarrollo && git push origin main
   ```
2. Entrá a **https://render.com**, creá una cuenta con GitHub y dale acceso a
   este repositorio.
3. **New → Blueprint** → elegí el repositorio. Render lee `render.yaml`.
4. Te pide las variables secretas: `DB_URL`, `DB_USUARIO`, `DB_PASSWORD`
   (las de Neon), `ADMIN_USERNAME` y `ADMIN_PASSWORD_INICIAL`.
5. **Apply.** La primera construcción tarda unos minutos. Cuando diga *Live*,
   abrí la dirección `https://gestion-vacaciones-XXXX.onrender.com`.
6. Entrá con el usuario inicial: te obliga a cambiar la contraseña. Después
   podés borrar `ADMIN_PASSWORD_INICIAL` en Render: ya no se usa.

Cada push a `main` despliega solo, **solo si las pruebas pasaron**
(`autoDeployTrigger: checksPass`).

**HTTPS.** Render redirige todo HTTP a HTTPS por su cuenta, y la cookie de
sesión viaja solo por HTTPS. Para confirmar que la aplicación reconoce al
balanceador de Render, después del primer despliegue:

1. Entrá a **Administración → Bitácora** y mirá la IP de tu propio ingreso.
2. **Si es tu IP pública** (la ves en https://ifconfig.me), todo está bien. Como
   segunda barrera, podés agregar en Render la variable `EXIGIR_HTTPS=true`:
   la aplicación también rechazará cualquier petición que no llegue por HTTPS.
3. **Si empieza con `10.`**, Tomcat no reconoce al balanceador: **no** actives
   `EXIGIR_HTTPS` (redirigiría en bucle) y ajustá
   `server.tomcat.remoteip.internal-proxies` con la red de Render.

### 3. Que Brenda no espere: la aplicación dormida

El plan gratuito de Render **duerme el servicio tras 15 minutos sin tráfico**,
y despertarlo tarda **alrededor de un minuto**. Si Brenda abre el sistema a
media mañana después de un rato sin usarlo, esperaría ese minuto.

**La mitigación que está armada:** `.github/workflows/mantener-despierta.yml`
toca la aplicación **cada 10 minutos, de lunes a viernes, de 7:00 am a 5:59 pm
hora de Managua**. Toca `/actuator/health/liveness`, que **no consulta la base**:
Render queda despierto y Neon puede seguir durmiendo cuando nadie trabaja.

Para activarla: en GitHub, **Settings → Secrets and variables → Actions →
Variables → New repository variable**: `URL_APLICACION` =
`https://gestion-vacaciones-XXXX.onrender.com`. Probala desde la pestaña
**Actions → Mantener despierta la aplicación → Run workflow**.

**Costo y beneficio:**

| Opción | Costo | Pros | Contras |
|---|---|---|---|
| **Ping con GitHub Actions** (lo armado) | Gratis: el repositorio es público | En el repositorio, sin otra cuenta. 60 pings diarios × 22 días ≈ 440 h de Render al mes, dentro de las 750 gratuitas | GitHub puede atrasar las tareas programadas en horas pico. Si el repositorio pasa **60 días sin actividad**, GitHub desactiva la tarea (se reactiva con un clic) |
| **cron-job.org** o UptimeRobot | Gratis | Más puntual que GitHub | Otra cuenta que mantener, fuera del repositorio |
| **Render Starter** (siempre encendido) | Pago mensual (desde unos 7 USD, verificá el precio actual) | Nunca duerme, más memoria y CPU | Cuesta todos los meses. Depender de un pago es justo lo que se quería evitar |
| **No hacer nada** | Gratis | Nada que mantener | La primera apertura tras 15 minutos tarda alrededor de un minuto |

**Recomendación:** empezar con el ping de GitHub. Si en la práctica Brenda
igual encuentra la aplicación dormida seguido, pasar a cron-job.org con la
misma dirección. El plan pago solo tiene sentido si la oficina lo va a
sostener.

### 4. La base dormida (Neon)

Neon apaga el cómputo tras 5 minutos sin uso. La aplicación lo tolera:

- El pool de conexiones no deja conexiones abiertas en reposo (así la base
  puede dormir) y espera hasta **30 segundos** a que despierte.
- Flyway reintenta **10 veces** al arrancar.
- Si aun así la base no responde, Brenda ve **"El sistema se está
  despertando"**. Si estaba consultando, la página se recarga sola. Si estaba
  guardando, se le avisa que no se guardó nada y nunca se reenvía el
  formulario.

---

## Respaldos

El trabajo de la oficina no puede depender del plan gratuito de nadie. Los
respaldos se guardan **en tu computadora**.

### Hacer un respaldo

```bash
cp .env.respaldo.example .env.respaldo   # una sola vez: poné la URL de Neon
./scripts/respaldar.sh
```

- Usa `pg_dump` dentro de Podman: no hay que instalar PostgreSQL.
- Guarda en `~/Respaldos/GestionDeVacaciones/vacaciones_AAAA-MM-DD_HHMM.dump`.
- Verifica que el archivo se pueda leer y conserva los últimos 30.
- La carpeta y los archivos quedan con permisos solo para vos: tienen datos
  personales. **No los subas a ningún lado público.**

En `.env.respaldo` va la URL `postgresql://` de Neon (no la `jdbc:`), también
con *Connection pooling* desmarcado.

### Cada cuánto

**Una vez por semana**, como mínimo. Para no depender de acordarte, un
temporizador de systemd en tu Fedora (todos los viernes a las 5:30 pm, y si la
compu estaba apagada, apenas se encienda):

```bash
mkdir -p ~/.config/systemd/user

cat > ~/.config/systemd/user/respaldo-vacaciones.service <<'UNIDAD'
[Unit]
Description=Respaldo de la base de Gestión de Vacaciones

[Service]
Type=oneshot
ExecStart=%h/Repos/GestionDeVacaciones/scripts/respaldar.sh
UNIDAD

cat > ~/.config/systemd/user/respaldo-vacaciones.timer <<'UNIDAD'
[Unit]
Description=Respaldo semanal de Gestión de Vacaciones

[Timer]
OnCalendar=Fri 17:30
Persistent=true

[Install]
WantedBy=timers.target
UNIDAD

systemctl --user daemon-reload
systemctl --user enable --now respaldo-vacaciones.timer
systemctl --user list-timers | grep respaldo
```

Ver cómo salió el último: `journalctl --user -u respaldo-vacaciones -n 20`.

Y de vez en cuando, **copiá la carpeta de respaldos a un disco externo**.

### Restaurar un respaldo

**Primero probalo en una base aparte**, nunca directo sobre producción:

```bash
podman run -d --name restauracion -e POSTGRES_PASSWORD=clave-de-prueba \
  -p 5434:5432 docker.io/library/postgres:17-alpine

 URL_BASE_DESTINO='postgresql://postgres:clave-de-prueba@host.containers.internal:5434/postgres' \
  ./scripts/restaurar.sh ~/Respaldos/GestionDeVacaciones/vacaciones_2026-09-14_1730.dump
```

(El espacio al principio de la segunda línea evita que la contraseña quede en
el historial de la terminal.)

El script pide escribir `RESTAURAR` para continuar, y restaura todo en una
sola transacción: si algo falla, no queda nada a medias.

**Si hay que restaurar producción** (por ejemplo, Neon perdió la base):

1. Creá un proyecto nuevo en Neon (Postgres 17, Ohio) y copiá su URL
   `postgresql://`.
2. Restaurá el último respaldo ahí, con el comando de arriba.
3. En Render, cambiá `DB_URL`, `DB_USUARIO` y `DB_PASSWORD` por los del
   proyecto nuevo. Render reinicia la aplicación sola.
4. Entrá y verificá el tablero y un par de fichas. Las contraseñas y el
   historial vienen en el respaldo: todo sigue igual.

Si el problema fue un error de hace pocas horas, antes de todo esto probá la
**restauración a un punto en el tiempo de Neon** (últimas 6 horas, en el panel
de Neon → Branches → Restore).

---

## Cómo está organizado

```
src/main/java/ni/gestionvacaciones/
├── admin/        panel de usuarios, bitácora y parámetros
├── auditoria/    bitácora: quién hizo qué, cuándo y desde dónde
├── comun/        conversor de minutos a días y formatos de fecha
├── config/       seguridad, MVC, reloj y administrador inicial
├── empleado/     alta, búsqueda, ficha, edición, baja y reactivación
├── parametro/    lectura y cambio de parámetros
├── reporte/      reportes en Excel y CSV
├── saldo/        libro contable del saldo: el único que lo modifica
├── seguridad/    usuarios, contraseñas, ingreso y bloqueo por intentos
├── solicitud/    registrar y anular vacaciones, citas médicas y permisos
└── web/          tablero, sesión, estado del usuario y errores

src/main/resources/
├── db/migration/          migraciones de Flyway (el esquema se maneja SOLO desde acá)
├── templates/             plantillas HTML (Thymeleaf)
├── static/                CSS generado, HTMX y JavaScript propio
├── application.yml        configuración común
├── application-dev.yml    perfil de desarrollo
└── application-prod.yml   perfil de producción

src/main/tailwind/   archivo fuente de los estilos
scripts/             respaldar y restaurar la base
docs/                manual para Brenda y repaso de seguridad
Dockerfile           imagen de dos etapas para Render
render.yaml          descripción del servicio en Render
.github/workflows/   pruebas en cada push y ping en horario de oficina
```

---

## Decisiones técnicas

- **Spring Boot 4.1:** la última versión estable. La línea 3.5 terminó su soporte
  gratuito el 30 de junio de 2026; la 4.1 lo mantiene hasta julio de 2027.
- **Flyway, nunca `ddl-auto: update`:** el esquema se versiona en archivos SQL.
  Hibernate arranca con `validate`: si una entidad no coincide con su tabla,
  la aplicación no arranca y avisa.
- **PostgreSQL en los tres lados:** local, pruebas y producción. Nada de bases
  en memoria que se comportan distinto.
- **Thymeleaf y HTMX, no una aplicación de página única:** un repositorio, un
  despliegue, menos piezas.
- **fastexcel en vez de Apache POI:** solo se generan archivos, y POI pesa
  mucho más para una aplicación con 512 MB de RAM.
- **Sin CDN ni fuentes remotas:** la oficina puede tener internet limitado, y la
  política de seguridad del navegador solo permite archivos propios.

---

## Estado del proyecto

- [x] **Fase 1** — Esqueleto: base de datos, Flyway, ingreso seguro, cambio obligatorio de contraseña.
- [x] **Fase 2** — Funcionarios: alta con saldo inicial, buscador, ficha, edición y baja lógica.
- [x] **Fase 3** — Solicitudes y saldos: descuento automático, anulación, ajuste manual, saldo negativo autorizado.
- [x] **Fase 4** — Interfaz: tablero, aviso de sesión, errores amables, reactivación.
- [x] **Fase 5** — Administración y seguridad: usuarios, bitácora, parámetros, bloqueo por intentos.
- [x] **Fase 6** — Despliegue: Docker, Neon, Render, GitHub Actions, reportes, respaldos y manual.
