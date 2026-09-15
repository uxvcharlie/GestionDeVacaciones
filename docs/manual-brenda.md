# Manual del Registro de Vacaciones

Esta guía explica cómo usar el sistema, pantalla por pantalla. No hace falta
instalar nada: se usa desde el navegador.

---

## Entrar

1. Abrí la dirección del sistema en el navegador. Te conviene guardarla en
   favoritos.
2. Escribí tu **usuario** y tu **contraseña** y tocá **Ingresar**.

**La primera vez** el sistema te pide cambiar la contraseña que te dieron. Elegí
una de **12 caracteres o más**. Lo más fácil de recordar y lo más seguro es una
frase, por ejemplo: *el perro azul de mi abuela*.

**Si te equivocás 5 veces** seguidas, tu usuario se bloquea 15 minutos. Esperá,
o pedile a quien administra el sistema que lo desbloquee.

**Si abrís el sistema y tarda cerca de un minuto**, es normal: estaba en
reposo y se está despertando. Si ves "El sistema se está despertando", esperá:
la página vuelve a intentar sola.

---

## La pantalla de inicio

Arriba te saluda según la hora del día. Ahí tenés:

- **Registrar solicitud:** el botón grande. Es lo que más vas a usar.
- **Buscar funcionario:** escribí parte del nombre y aparecen los resultados.
  No importan las tildes: "angela" encuentra a "Ángela".
- **Resumen del mes:** cuántos funcionarios hay activos y cuántas vacaciones,
  citas médicas y permisos empiezan este mes.
- **Últimas solicitudes:** las cinco más recientes. Tocá una para ir a la ficha
  de esa persona.

El menú de arriba te lleva a **Inicio**, **Funcionarios**, **Reportes** y
**Administración**.

---

## Agregar un funcionario

1. **Funcionarios → Agregar funcionario.**
2. Escribí el **nombre completo**. El cargo, el área y la fecha de ingreso son
   opcionales.
3. En **Saldo con el que empieza**, escribí los días y las horas que tiene hoy.
   Por ejemplo: 10 días y 0 horas.
4. **Guardar funcionario.**

Después de esto, el saldo solo cambia al registrar solicitudes o con un ajuste.

---

## Registrar una solicitud

Son dos pantallas y un clic.

**1. Completá los datos** (desde el inicio, o desde la ficha del funcionario con
**Registrar solicitud**):

- **Funcionario.** Si hay dos personas con el mismo nombre, fijate en el cargo
  que aparece al lado.
- **Tipo:** Vacaciones, Cita médica o Permiso.
- **Desde y Hasta.** Si es un solo día, poné la misma fecha en los dos.
- **Cantidad:**
  - En **vacaciones**, en días. Para medio día, poné 0 días y 4 horas (con
    jornada de 8 horas).
  - En **citas médicas y permisos**, en horas y minutos. Se descuenta
    exactamente ese tiempo, no un día completo.
- **Motivo** (opcional). En citas médicas **no escribas diagnósticos ni datos de
  salud**: dejalo vacío o poné algo general.

Tocá **Revisar antes de guardar**.

**2. Revisá el resumen.** Por ejemplo: *María González — Vacaciones — del
10/11/2026 al 14/11/2026 — 5 días. Saldo: 12 días → 7 días.* Todavía no se
guardó nada. Si algo está mal, tocá **Corregir datos**.

**3. Tocá Guardar solicitud.** Listo: el saldo ya quedó descontado.

Si aparece un aviso amarillo (por ejemplo, que esas fechas se cruzan con otra
solicitud), leelo. Es un aviso: podés guardar igual si está bien.

### Si el saldo no alcanza

El sistema no te deja guardar y te dice cuánto saldo tiene la persona. Si esa
excepción **está autorizada**, marcá **Autorizo registrar esta solicitud aunque
el saldo quede en negativo**, escribí el motivo (por ejemplo, el número del
memorándum) y guardá. Queda anotado con tu nombre.

---

## La ficha de un funcionario

Tocá el nombre de cualquier persona para ver:

- El **saldo disponible**, bien grande, y lo que usó este año en cada tipo.
- Sus datos.
- Los botones **Registrar solicitud**, **Ajustar saldo**, **Editar datos** y
  **Dar de baja**.
- **Descargar reporte** en Excel o CSV.
- Todas sus **solicitudes** y el **historial del saldo**: cada suma y cada
  resta, con fecha y quién la hizo.

### Anular una solicitud

Si una solicitud se registró por error: en la ficha, junto a esa solicitud,
tocá **Anular**, escribí el motivo y confirmá. El tiempo vuelve al saldo. La
solicitud no se borra: queda marcada como anulada.

### Ajustar el saldo a mano

Para corregir un saldo: **Ajustar saldo**, elegí **Sumar** o **Restar**,
escribí cuánto y el **motivo** (es obligatorio), y guardá.

### Dar de baja y reactivar

**Dar de baja** saca a la persona de la lista para registrar solicitudes, pero
**no borra nada**: su historial queda guardado. Si fue un error, en su ficha
aparece el botón **Reactivar**.

---

## Reportes

En **Reportes** elegí el mes y el año y tocá **Descargar Excel** o
**Descargar CSV**. Sale la lista de todas las solicitudes de ese mes, con un
resumen. El reporte de una sola persona se descarga desde su ficha.

---

## Administración

### Usuarios

- **Crear usuario:** escribí el nombre completo y un nombre de usuario (sin
  espacios, por ejemplo `maria.gonzalez`). El sistema genera una **contraseña
  temporal** y te la muestra **una sola vez**: anotala y entregásela a la
  persona en privado. Al entrar, el sistema le va a pedir que la cambie.
- **Bloquear:** la persona ya no puede entrar. Si tenía el sistema abierto, se
  le cierra en su siguiente clic.
- **Desbloquear:** vuelve a dejarla entrar, también si se bloqueó por
  equivocarse 5 veces.
- **Restablecer contraseña:** si alguien la olvidó. Se genera otra temporal.

Para cambiar **tu propia** contraseña, usá **Mi contraseña**, arriba a la
derecha.

### Bitácora

Muestra todo lo importante que pasó: quién entró, quién registró, anuló o
ajustó algo, y cuándo. Podés filtrar por tipo de acción y por fechas. No se
puede modificar ni borrar.

### Parámetros

- **Horas por jornada:** cuántas horas tiene un día de trabajo. Si la cambiás,
  los saldos no cambian, pero sí cómo se ven en días. Antes de guardar, el
  sistema te muestra un ejemplo.
- **Zona horaria:** para Nicaragua, `America/Managua`. No hace falta tocarla.

---

## Cosas que conviene saber

- **La sesión se cierra sola** después de 30 minutos sin usar el sistema. Un
  minuto antes aparece un aviso: tocá **Seguir trabajando** para continuar sin
  perder lo que estabas escribiendo.
- **Cerrá la sesión** con el botón de arriba cuando dejes la computadora.
- **Nada se borra.** Las solicitudes se anulan y los funcionarios se dan de
  baja, pero la historia queda.
- **Si un formulario dice "ya no era válido"**, es porque estuvo abierto mucho
  tiempo. No se guardó nada: volvé a hacerlo.
