# Registro de Vacaciones, Citas Médicas y Permisos

Aplicación web para llevar el control de vacaciones, citas médicas y permisos
de los trabajadores de una oficina administrativa.

Es un **registro**, no un trámite: el permiso ya fue autorizado fuera del
sistema y acá únicamente se anota, se descuenta del saldo y queda el historial.
No hay estados "pendiente", "aprobado" ni "rechazado".

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

Ejemplo real:

| Momento                                   | En la base de datos | En pantalla          |
|-------------------------------------------|--------------------:|----------------------|
| Saldo inicial de 10 días                  |                4800 | 10 días              |
| Se registra 1 día de vacaciones (−480)    |                4320 | 9 días               |
| Se registra una cita médica de 2 h (−120) |                4200 | 8 días y 6 horas     |

Con enteros, sumar y restar siempre da exacto. Con decimales, `8.75` termina
convertido en `8.749999999998` después de varias operaciones, y eso en un
registro laboral se transforma en un reclamo.

---

## Qué necesitás instalado

| Herramienta | Para qué           | Cómo instalarla en Fedora                     |
|-------------|--------------------|-----------------------------------------------|
| JDK 21+     | Compilar y correr  | `sudo dnf install java-25-openjdk-devel`      |
| Podman      | PostgreSQL local   | `sudo dnf install podman`                     |
| Node.js     | Regenerar el CSS   | `sudo dnf install nodejs npm`                 |

Maven **no** hace falta instalarlo: el proyecto trae su propio `./mvnw`.

**Ojo con el JDK en Fedora.** El paquete `java-25-openjdk-headless` es solo
para *ejecutar* Java: no trae el compilador (`javac`). Si `./mvnw` falla con
`release version 21 not supported`, te falta el `-devel`. Fedora 44 ya no
publica Java 21, y no hace falta: el proyecto genera código compatible con
Java 21 aunque compiles con el JDK 25.

Node solo se necesita si vas a **modificar** la interfaz. La hoja de estilos ya
generada está versionada, así que la aplicación arranca y se ve bien sin Node.

---

## Cómo correrlo en tu computadora

### 1. Levantar PostgreSQL con Podman

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

**Si ya tenés PostgreSQL instalado en el sistema** (el servicio `postgresql`),
el puerto 5432 está ocupado y el contenedor no arranca. Usá otro puerto del
lado de tu computadora, `-p 5433:5432`, y en `.env` poné
`DB_URL=jdbc:postgresql://localhost:5433/vacaciones`. Para saber si es tu caso:

```bash
ss -ltn | grep 5432
```

Comprobá que quedó arriba:

```bash
podman ps
podman logs vacaciones-db | tail -5
```

Para apagarlo y encenderlo después (los datos se conservan en el volumen):

```bash
podman stop vacaciones-db
podman start vacaciones-db
```

### 2. Crear tu archivo `.env`

```bash
cp .env.example .env
```

Abrilo y poné tus valores. `DB_PASSWORD` tiene que ser la misma clave que
usaste en el comando de Podman, y `ADMIN_PASSWORD_INICIAL` tiene que tener al
menos 12 caracteres.

Los valores con espacios van **entre comillas**: `ADMIN_NOMBRE="Brenda Vásquez"`.
Sin comillas, el script lee solo "Brenda" y la variable queda vacía.

`.env` está en `.gitignore`: nunca se sube a GitHub.

### 3. Arrancar la aplicación

```bash
./ejecutar-local.sh
```

Cuando en la consola aparezca `Started GestionVacacionesApplication`, abrí:

**http://localhost:8080**

La primera vez vas a ver en el registro un mensaje como:

```
Administrador inicial creado: brenda (Brenda Vásquez).
En su primer ingreso el sistema le va a exigir cambiar la contraseña.
```

---

## Cómo probar que quedó bien

1. Entrá a `http://localhost:8080`. Te tiene que mandar a la pantalla de ingreso.
2. Escribí una contraseña equivocada: debe decir *"Usuario o contraseña
   incorrectos"*, sin revelar si el usuario existe.
3. Entrá con los datos correctos: te manda directo a **Cambiá tu contraseña**.
4. Probá escribir una dirección a mano, por ejemplo `http://localhost:8080/`:
   te devuelve al cambio de contraseña. No se puede saltar.
5. Probá una contraseña de menos de 12 caracteres: te explica el problema en
   español.
6. Poné una contraseña válida: entrás al tablero y te saluda con tu nombre.
7. Cerrá sesión y volvé a entrar con la contraseña nueva.
8. Abrí `http://localhost:8080/una-direccion-que-no-existe`: tiene que aparecer
   la página de error propia, nunca una pantalla blanca con datos técnicos.

Para ver las tablas que creó Flyway:

```bash
podman exec -it vacaciones-db psql -U vacaciones -d vacaciones -c '\dt'
```

---

## Pruebas automatizadas

Las pruebas levantan un PostgreSQL de verdad con Testcontainers. Para que
funcione con Podman hay que habilitar su socket una sola vez:

```bash
systemctl --user enable --now podman.socket
export DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
```

Después:

```bash
./mvnw test
```

Si no querés configurar eso todavía, podés compilar sin pruebas:

```bash
./mvnw package -DskipTests
```

---

## Modificar la interfaz

Los estilos se escriben en `src/main/tailwind/entrada.css` y de ahí se genera
`src/main/resources/static/css/estilos.css`, que es el archivo que realmente se
sirve al navegador y **sí se versiona**.

```bash
npm install          # una sola vez
npm run css          # regenera el archivo, minificado
npm run css:vigilar  # lo regenera solo, cada vez que guardás
npm run htmx         # copia HTMX desde node_modules a static/js
```

HTMX (lo que hace que el buscador filtre mientras escribís) también se sirve
desde la propia aplicación, sin CDN. Su versión exacta queda fijada en
`package.json`.

Si cambiás clases en un archivo `.html`, acordate de regenerar el CSS antes de
hacer commit.

---

## Variables de entorno

| Variable                 | Para qué                                              |
|--------------------------|-------------------------------------------------------|
| `DB_URL`                 | Dirección JDBC de PostgreSQL                          |
| `DB_USUARIO`             | Usuario de la base de datos                           |
| `DB_PASSWORD`            | Contraseña de la base de datos                        |
| `ADMIN_USERNAME`         | Usuario del administrador inicial                     |
| `ADMIN_PASSWORD_INICIAL` | Su contraseña temporal (12 caracteres o más)          |
| `ADMIN_NOMBRE`           | Su nombre completo                                    |
| `COOKIE_SEGURA`          | `false` en local, `true` en producción (HTTPS)        |
| `PORT`                   | Puerto donde escucha la aplicación                    |

El administrador inicial se crea **una sola vez**, y solo si la tabla de
usuarios está vacía. En arranques posteriores no se toca nada, así que no puede
sobrescribir la contraseña que la persona ya eligió.

---

## Cómo está organizado

```
src/main/java/ni/gestionvacaciones/
├── admin/        panel de usuarios, pantalla de bitácora y de parámetros
├── auditoria/    bitácora: quién hizo qué, cuándo y desde dónde
├── comun/        conversor de minutos a días y formatos de fecha
├── config/       configuración de seguridad, MVC y administrador inicial
├── empleado/     alta, búsqueda, ficha, edición y baja de funcionarios
├── parametro/    lectura de parámetros (horas por jornada, zona horaria)
├── saldo/        libro contable del saldo: el único que lo modifica
├── seguridad/    usuarios, contraseñas, ingreso
├── solicitud/    registrar y anular vacaciones, citas médicas y permisos
└── web/          controladores y pantallas comunes

src/main/resources/
├── db/migration/ migraciones de Flyway (el esquema se maneja SOLO desde acá)
├── templates/    plantillas HTML (Thymeleaf)
└── static/       hoja de estilos generada y JavaScript

src/main/tailwind/  archivo fuente de los estilos
```

---

## Decisiones técnicas

- **Spring Boot 4.1.1**: es la última versión estable. La línea 3.5 terminó su
  soporte gratuito el 30 de junio de 2026 y la 4.0 lo termina en diciembre de
  2026; la 4.1 lo mantiene hasta julio de 2027.
- **Flyway, nunca `ddl-auto: update`**: el esquema se versiona en archivos SQL
  que se pueden leer y revisar. Hibernate arranca con `validate`, así que si
  una entidad y su tabla no coinciden, la aplicación no arranca y avisa.
- **PostgreSQL en los tres lados**: en tu computadora, en las pruebas y en
  producción. Nada de bases en memoria que se comportan distinto.
- **Thymeleaf en el servidor, no una aplicación de página única**: un solo
  repositorio, un solo despliegue, una sola cosa que puede fallar.
- **Sin CDN ni fuentes remotas**: todo se sirve desde la misma aplicación,
  porque la oficina puede tener internet limitado y porque la política de
  seguridad del navegador (CSP) es estricta.

---

## Estado del proyecto

- [x] **Fase 1** — Esqueleto: base de datos, Flyway, ingreso seguro, cambio
      obligatorio de contraseña.
- [x] **Fase 2** — Empleados: alta con saldo inicial, buscador sin tildes,
      ficha con historial, edición y baja lógica.
- [x] **Fase 3** — Solicitudes y saldos: descuento automático, anulación con
      devolución, ajuste manual, saldo negativo solo con autorización y
      bitácora de esas acciones.
- [x] **Fase 4** — Interfaz y experiencia de uso: tablero con saludo según la
      hora, buscador, resumen del mes, últimas solicitudes, aviso antes de que
      venza la sesión, errores amables y reactivación de funcionarios.
- [x] **Fase 5** — Administración y seguridad: panel de usuarios, bitácora,
      parámetros y bloqueo por intentos fallidos. Ver
      [el repaso de seguridad](docs/seguridad.md).
- [ ] **Fase 6** — Despliegue, respaldos y manual.
