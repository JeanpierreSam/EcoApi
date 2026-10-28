package pe.upeu.ecorecicla.servicio

import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.ecorecicla.dominio.EstadoRuta
import pe.upeu.ecorecicla.dominio.Pesaje
import pe.upeu.ecorecicla.dominio.PuntoAcopio
import pe.upeu.ecorecicla.dominio.RecursoNoEncontradoException
import pe.upeu.ecorecicla.dominio.ReglaNegocioException
import pe.upeu.ecorecicla.dominio.Ruta
import pe.upeu.ecorecicla.dominio.ValidacionException
import pe.upeu.ecorecicla.repositorio.PesajeRepository
import pe.upeu.ecorecicla.repositorio.PuntoAcopioRepository
import pe.upeu.ecorecicla.repositorio.RutaPuntoRepository
import pe.upeu.ecorecicla.repositorio.RutaRepository
import pe.upeu.ecorecicla.seguridad.Privilegios
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado
import pe.upeu.ecorecicla.seguridad.UsuarioRepository
import pe.upeu.ecorecicla.web.dto.CorreccionPesajeRequest
import pe.upeu.ecorecicla.web.dto.Pagina
import pe.upeu.ecorecicla.web.dto.PesajeRequest
import pe.upeu.ecorecicla.web.dto.PesajeResponse
import pe.upeu.ecorecicla.web.dto.PuntoDeRutaResponse
import pe.upeu.ecorecicla.web.dto.PuntoRequest
import pe.upeu.ecorecicla.web.dto.PuntoResponse
import pe.upeu.ecorecicla.web.dto.RutaResponse
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class RutaService(
    private val rutas: RutaRepository,
    private val pesajes: PesajeRepository
) {
    @Transactional(readOnly = true)
    fun delDia(usuario: UsuarioAutenticado, fecha: LocalDate): RutaResponse {
        val ruta = rutas.delUsuario(fecha, usuario.id).firstOrNull()
            ?: throw RecursoNoEncontradoException("No tiene una ruta asignada para el $fecha")
        return aRespuesta(ruta)
    }

    /** RF-B06 + RN-04: no se cierra mientras quede un punto sin pesaje ni motivo de omision. */
    @Transactional
    fun cerrar(rutaId: Long): RutaResponse {
        val ruta = rutas.findByIdOrNull(rutaId) ?: throw RecursoNoEncontradoException("La ruta $rutaId no existe")
        if (ruta.cerrada) throw ReglaNegocioException("La ruta ya esta cerrada")
        val pesados = pesajes.puntosPesados(rutaId)
        val faltan = ruta.puntos.count { it.punto.id !in pesados && it.motivoOmision == null }
        if (faltan > 0) {
            throw ReglaNegocioException("No se puede cerrar la ruta: faltan $faltan puntos sin pesaje ni motivo de omision")
        }
        ruta.estado = EstadoRuta.CERRADA
        return aRespuesta(ruta)
    }

    private fun aRespuesta(ruta: Ruta): RutaResponse {
        val pesados = pesajes.puntosPesados(ruta.id!!)
        return RutaResponse(
            id = ruta.id!!,
            fecha = ruta.fecha.toString(),
            estado = ruta.estado.name,
            operario = ruta.operario.nombre,
            puntos = ruta.puntos.map {
                PuntoDeRutaResponse(
                    it.punto.id!!, it.punto.razonSocial, it.punto.direccion, it.punto.zona,
                    it.punto.materialHabitual, it.secuencia,
                    atendido = it.punto.id in pesados || it.motivoOmision != null,
                    motivoOmision = it.motivoOmision
                )
            }
        )
    }
}

@Service
class PuntoService(private val puntos: PuntoAcopioRepository) {

    @Transactional(readOnly = true)
    fun listar(zona: String?, pagina: Int, tamanio: Int): Pagina<PuntoResponse> {
        val pedido = paginaValida(pagina, tamanio)
        val page = if (zona.isNullOrBlank()) puntos.findAll(pedido) else puntos.findByZonaIgnoreCase(zona, pedido)
        return Pagina.de(page, ::aRespuesta)
    }

    @Transactional
    fun crear(p: PuntoRequest): PuntoResponse = aRespuesta(
        puntos.save(PuntoAcopio(null, p.razonSocial!!.trim(), p.direccion!!.trim(), p.zona!!.trim().uppercase(), p.materialHabitual!!.trim().uppercase()))
    )

    private fun aRespuesta(p: PuntoAcopio) = PuntoResponse(p.id!!, p.razonSocial, p.direccion, p.zona, p.materialHabitual)
}

/** Reglas RN-01 a RN-03 sobre los pesajes; los controladores solo delegan. */
@Service
class PesajeService(
    private val pesajes: PesajeRepository,
    private val rutaPuntos: RutaPuntoRepository,
    private val puntos: PuntoAcopioRepository,
    private val usuarios: UsuarioRepository
) {
    @Transactional(readOnly = true)
    fun listarDePunto(puntoId: Long, pagina: Int, tamanio: Int): Pagina<PesajeResponse> {
        if (!puntos.existsById(puntoId)) throw RecursoNoEncontradoException("El punto de acopio $puntoId no existe")
        return Pagina.de(pesajes.findByPuntoIdOrderByFechaHoraDescIdDesc(puntoId, paginaValida(pagina, tamanio)), ::aRespuesta)
    }

    @Transactional(readOnly = true)
    fun obtener(id: Long): PesajeResponse = aRespuesta(buscar(id))

    @Transactional
    fun registrar(p: PesajeRequest, usuario: UsuarioAutenticado): PesajeResponse {
        val rutaPunto = rutaPuntos.findByRutaIdAndPuntoId(p.rutaId!!, p.puntoId!!)
            ?: throw RecursoNoEncontradoException("El punto ${p.puntoId} no pertenece a la ruta ${p.rutaId}")
        exigirRutaAbierta(rutaPunto.ruta)                                     // RN-01
        val (bruto, tara, neto) = pesos(p.pesoBruto!!, p.tara!!)              // RN-03
        val nuevo = Pesaje(
            ruta = rutaPunto.ruta, punto = rutaPunto.punto,
            usuario = usuarios.getReferenceById(usuario.id),
            fechaHora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            material = p.material!!.trim().uppercase(),
            pesoBruto = bruto, tara = tara, pesoNeto = neto,
            observacion = p.observacion?.trim()?.ifBlank { null }
        )
        return aRespuesta(pesajes.save(nuevo))
    }

    @Transactional
    fun corregir(id: Long, p: CorreccionPesajeRequest, usuario: UsuarioAutenticado): PesajeResponse {
        val pesaje = buscar(id)
        exigirRutaAbierta(pesaje.ruta)                                        // RN-01
        if (pesaje.anulado) throw ReglaNegocioException("El pesaje esta anulado y no puede corregirse")
        // RN-02: sin PESAJE_EDITAR_TODOS solo se corrigen los propios y del mismo dia
        if (!usuario.tiene(Privilegios.PESAJE_EDITAR_TODOS)) {
            if (pesaje.usuario.id != usuario.id) throw AccessDeniedException("Solo puede corregir los pesajes que usted registro")
            if (pesaje.fechaHora.toLocalDate() != LocalDate.now()) throw AccessDeniedException("Solo puede corregir pesajes registrados hoy")
        }
        val (bruto, tara, neto) = pesos(p.pesoBruto!!, p.tara!!)              // RN-03
        pesaje.material = p.material!!.trim().uppercase()
        pesaje.pesoBruto = bruto
        pesaje.tara = tara
        pesaje.pesoNeto = neto
        pesaje.observacion = p.observacion?.trim()?.ifBlank { null }
        return aRespuesta(pesaje)
    }

    /** Anulacion logica: el registro se conserva marcado. */
    @Transactional
    fun anular(id: Long, motivo: String?) {
        if (motivo.isNullOrBlank()) throw ValidacionException("motivo", "El motivo de anulacion es obligatorio")
        val pesaje = buscar(id)
        exigirRutaAbierta(pesaje.ruta)
        if (pesaje.anulado) throw ReglaNegocioException("El pesaje ya estaba anulado")
        pesaje.anulado = true
        pesaje.motivoAnulacion = motivo.trim()
    }

    private fun buscar(id: Long) = pesajes.findByIdOrNull(id) ?: throw RecursoNoEncontradoException("El pesaje $id no existe")

    private fun exigirRutaAbierta(ruta: Ruta) {
        if (ruta.cerrada) throw ReglaNegocioException("La ruta del ${ruta.fecha} esta cerrada: no admite pesajes ni correcciones")
    }

    /** RN-03: el neto lo calcula el servicio y debe ser mayor que cero. */
    private fun pesos(bruto: Double, tara: Double): Triple<BigDecimal, BigDecimal, BigDecimal> {
        val b = BigDecimal.valueOf(bruto).setScale(2, RoundingMode.HALF_UP)
        val t = BigDecimal.valueOf(tara).setScale(2, RoundingMode.HALF_UP)
        val neto = b - t
        if (neto.signum() <= 0) throw ValidacionException("tara", "La tara debe ser menor que el peso bruto (neto = $neto)")
        return Triple(b, t, neto)
    }

    private fun aRespuesta(p: Pesaje) = PesajeResponse(
        p.id!!, p.ruta.id!!, p.punto.id!!, p.fechaHora.toString(), p.material,
        p.pesoBruto.toDouble(), p.tara.toDouble(), p.pesoNeto.toDouble(),
        p.usuario.username, p.observacion, p.anulado, p.motivoAnulacion
    )
}

private fun paginaValida(pagina: Int, tamanio: Int): PageRequest {
    if (pagina < 0) throw ValidacionException("pagina", "La pagina no puede ser negativa")
    if (tamanio !in 1..100) throw ValidacionException("tamanio", "El tamanio debe estar entre 1 y 100")
    return PageRequest.of(pagina, tamanio)
}
