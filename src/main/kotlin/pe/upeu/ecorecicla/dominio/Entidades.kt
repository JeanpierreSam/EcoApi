package pe.upeu.ecorecicla.dominio

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import pe.upeu.ecorecicla.seguridad.Usuario
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "punto_acopio")
class PuntoAcopio(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var razonSocial: String = "",
    var direccion: String = "",
    var zona: String = "",
    var materialHabitual: String = ""
)

enum class EstadoRuta { ABIERTA, CERRADA }

@Entity
@Table(name = "ruta")
class Ruta(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var fecha: LocalDate = LocalDate.now(),
    @ManyToOne @JoinColumn(name = "operario_id")
    var operario: Usuario = Usuario(),
    @ManyToOne @JoinColumn(name = "supervisor_id")
    var supervisor: Usuario = Usuario(),
    @Enumerated(EnumType.STRING)
    var estado: EstadoRuta = EstadoRuta.ABIERTA,
    @OneToMany(mappedBy = "ruta")
    @OrderBy("secuencia ASC")
    var puntos: MutableList<RutaPunto> = mutableListOf()
) {
    val cerrada get() = estado == EstadoRuta.CERRADA
}

/** Punto planificado dentro de una ruta, con su orden de visita. */
@Entity
@Table(name = "ruta_punto")
class RutaPunto(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne @JoinColumn(name = "ruta_id")
    var ruta: Ruta = Ruta(),
    @ManyToOne @JoinColumn(name = "punto_id")
    var punto: PuntoAcopio = PuntoAcopio(),
    var secuencia: Int = 0,
    var motivoOmision: String? = null
)

@Entity
@Table(name = "pesaje")
class Pesaje(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne @JoinColumn(name = "ruta_id")
    var ruta: Ruta = Ruta(),
    @ManyToOne @JoinColumn(name = "punto_id")
    var punto: PuntoAcopio = PuntoAcopio(),
    @ManyToOne @JoinColumn(name = "usuario_id")
    var usuario: Usuario = Usuario(),
    var fechaHora: LocalDateTime = LocalDateTime.now(),
    var material: String = "",
    var pesoBruto: BigDecimal = BigDecimal.ZERO,
    var tara: BigDecimal = BigDecimal.ZERO,
    var pesoNeto: BigDecimal = BigDecimal.ZERO,
    var observacion: String? = null,
    var anulado: Boolean = false,
    var motivoAnulacion: String? = null
)

/** 404 */
class RecursoNoEncontradoException(mensaje: String) : RuntimeException(mensaje)

/** 409: regla de negocio incumplida (RN-01, RN-04). */
class ReglaNegocioException(mensaje: String) : RuntimeException(mensaje)

/** 400 con el detalle en [campo] (RN-03). */
class ValidacionException(val campo: String, mensaje: String) : RuntimeException(mensaje)
