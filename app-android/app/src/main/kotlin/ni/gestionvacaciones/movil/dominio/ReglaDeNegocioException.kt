package ni.gestionvacaciones.movil.dominio

/**
 * Se intentó hacer algo que las reglas no permiten: registrar más tiempo del
 * que hay en el saldo, anular dos veces, olvidar un motivo...
 *
 * El mensaje está escrito para Brenda, en español humano, y la pantalla lo
 * muestra tal cual. Nunca lleva detalles técnicos.
 *
 * @param requiereAutorizacion true cuando la operación SÍ se puede hacer si se
 *        autoriza expresamente (el saldo negativo).
 */
class ReglaDeNegocioException(
    mensaje: String,
    val requiereAutorizacion: Boolean = false,
) : RuntimeException(mensaje)
