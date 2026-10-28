package pe.upeu.ecorecicla.seguridad

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "privilegio")
class Privilegio(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var nombre: String = ""
)

/**
 * Nombres de los privilegios tal como estan en la tabla privilegio. Solo los usa
 * SecurityConfig para proteger rutas: que rol tiene cada uno vive en rol_privilegio.
 */
object Privilegios {
    const val PUNTO_LEER = "PUNTO_LEER"
    const val PESAJE_LEER = "PESAJE_LEER"
    const val PESAJE_CREAR = "PESAJE_CREAR"
    const val PESAJE_EDITAR = "PESAJE_EDITAR"
    /** Corregir pesajes de otros y de dias anteriores (columna «Todos» de la matriz). */
    const val PESAJE_EDITAR_TODOS = "PESAJE_EDITAR_TODOS"
    const val PESAJE_ANULAR = "PESAJE_ANULAR"
    const val RUTA_CERRAR = "RUTA_CERRAR"
    const val PUNTO_GESTIONAR = "PUNTO_GESTIONAR"
    /** Ver la ruta del dia aunque no se este asignado a ella (AUDITOR, ADMINISTRADOR). */
    const val RUTA_LEER_TODAS = "RUTA_LEER_TODAS"
}
