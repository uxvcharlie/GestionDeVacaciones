package ni.gestionvacaciones.movil.datos

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * SQLite no sabe qué es una fecha: solo guarda números y texto. Estas
 * funciones le dicen a Room cómo traducir.
 *
 * - Una fecha (LocalDate) se guarda como número de días desde 1970. Así se
 *   puede comparar y ordenar con < y > en SQL.
 * - Un instante (Instant) se guarda como milisegundos desde 1970 en UTC. La
 *   zona horaria de Managua se aplica recién al mostrarlo.
 *
 * Los enums (tipo, estado...) Room los guarda solo, como texto: 'VACACIONES'.
 */
class Conversores {
    @TypeConverter fun fechaANumero(fecha: LocalDate?): Long? = fecha?.toEpochDay()
    @TypeConverter fun numeroAFecha(dias: Long?): LocalDate? = dias?.let(LocalDate::ofEpochDay)
    @TypeConverter fun instanteANumero(instante: Instant?): Long? = instante?.toEpochMilli()
    @TypeConverter fun numeroAInstante(milis: Long?): Instant? = milis?.let(Instant::ofEpochMilli)
}
