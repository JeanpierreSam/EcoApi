package pe.upeu.ecorecicla.repositorio

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import pe.upeu.ecorecicla.dominio.Pesaje
import pe.upeu.ecorecicla.dominio.PuntoAcopio
import pe.upeu.ecorecicla.dominio.Ruta
import pe.upeu.ecorecicla.dominio.RutaPunto
import java.time.LocalDate

interface PuntoAcopioRepository : JpaRepository<PuntoAcopio, Long> {
    fun findByZonaIgnoreCase(zona: String, pageable: Pageable): Page<PuntoAcopio>
}

interface RutaRepository : JpaRepository<Ruta, Long> {
    /** La ruta del dia es la que el usuario recorre como operario o supervisa. */
    @Query("select r from Ruta r where r.fecha = :fecha and (r.operario.id = :usuarioId or r.supervisor.id = :usuarioId)")
    fun delUsuario(fecha: LocalDate, usuarioId: Long): List<Ruta>
}

interface RutaPuntoRepository : JpaRepository<RutaPunto, Long> {
    fun findByRutaIdAndPuntoId(rutaId: Long, puntoId: Long): RutaPunto?
}

interface PesajeRepository : JpaRepository<Pesaje, Long> {
    fun findByPuntoIdOrderByFechaHoraDescIdDesc(puntoId: Long, pageable: Pageable): Page<Pesaje>

    /** Puntos de la ruta con al menos un pesaje vigente (los anulados no cuentan). */
    @Query("select distinct p.punto.id from Pesaje p where p.ruta.id = :rutaId and p.anulado = false")
    fun puntosPesados(rutaId: Long): Set<Long>
}
