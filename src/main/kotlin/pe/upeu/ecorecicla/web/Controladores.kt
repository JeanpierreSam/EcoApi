package pe.upeu.ecorecicla.web

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado
import pe.upeu.ecorecicla.servicio.PesajeService
import pe.upeu.ecorecicla.servicio.PuntoService
import pe.upeu.ecorecicla.servicio.RutaService
import pe.upeu.ecorecicla.web.dto.CorreccionPesajeRequest
import pe.upeu.ecorecicla.web.dto.PesajeRequest
import pe.upeu.ecorecicla.web.dto.PuntoRequest
import java.time.LocalDate

// Los privilegios de cada ruta se exigen en SecurityConfig; aqui solo se delega al servicio.

@RestController
@RequestMapping("/api/v1/rutas")
class RutaController(private val servicio: RutaService) {

    /** RF-B02: GET /api/v1/rutas/del-dia?fecha=2026-10-27 (por defecto, hoy). */
    @GetMapping("/del-dia")
    fun delDia(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fecha: LocalDate?
    ) = servicio.delDia(usuario, fecha ?: LocalDate.now())

    /** RF-B06: POST /api/v1/rutas/{id}/cierre */
    @PostMapping("/{id}/cierre")
    fun cerrar(@PathVariable id: Long) = servicio.cerrar(id)
}

@RestController
@RequestMapping("/api/v1/puntos")
class PuntoController(
    private val puntos: PuntoService,
    private val pesajes: PesajeService
) {
    /** RF-B03: GET /api/v1/puntos?zona=NORTE&pagina=0&tamanio=20 */
    @GetMapping
    fun listar(
        @RequestParam(required = false) zona: String?,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int
    ) = puntos.listar(zona, pagina, tamanio)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun crear(@Valid @RequestBody peticion: PuntoRequest) = puntos.crear(peticion)

    /** Pesajes de un punto, del mas reciente al mas antiguo. */
    @GetMapping("/{id}/pesajes")
    fun pesajes(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int
    ) = pesajes.listarDePunto(id, pagina, tamanio)
}

@RestController
@RequestMapping("/api/v1/pesajes")
class PesajeController(private val servicio: PesajeService) {

    @GetMapping("/{id}")
    fun obtener(@PathVariable id: Long) = servicio.obtener(id)

    /** RF-B04: 201 con el id definitivo y el neto calculado. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registrar(@Valid @RequestBody peticion: PesajeRequest, @AuthenticationPrincipal usuario: UsuarioAutenticado) =
        servicio.registrar(peticion, usuario)

    @PutMapping("/{id}")
    fun corregir(
        @PathVariable id: Long,
        @Valid @RequestBody peticion: CorreccionPesajeRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ) = servicio.corregir(id, peticion, usuario)

    /** Anulacion logica: 204 sin cuerpo; el registro queda marcado en la base. */
    @DeleteMapping("/{id}")
    fun anular(@PathVariable id: Long, @RequestParam(required = false) motivo: String?): ResponseEntity<Void> {
        servicio.anular(id, motivo)
        return ResponseEntity.noContent().build()
    }
}
