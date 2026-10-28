package pe.upeu.ecorecicla.web.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Envoltorio de paginacion de todos los listados (seccion 4.2). */
data class Pagina<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
) {
    companion object {
        fun <E : Any, T> de(page: Page<E>, convertir: (E) -> T) = Pagina(
            page.content.map(convertir), page.number, page.size, page.totalElements, page.totalPages, page.isLast
        )
    }
}

/** Formato de error de todo el servicio (seccion 4.2). */
data class ErrorRespuesta(
    val timestamp: String,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val validationErrors: Map<String, String>?
) {
    companion object {
        fun de(estado: HttpStatus, mensaje: String, ruta: String, campos: Map<String, String>? = null) = ErrorRespuesta(
            LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
            estado.value(), estado.reasonPhrase, mensaje, ruta, campos
        )
    }
}

data class LoginRequest(
    @field:NotBlank(message = "El usuario es obligatorio") val usuario: String?,
    @field:NotBlank(message = "La contrasena es obligatoria") val contrasena: String?
)

data class LoginResponse(
    val token: String,
    val tipo: String,
    val expiraEnSegundos: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: List<String>
)

data class UsuarioActualResponse(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: List<String>
)

data class PuntoResponse(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String
)

data class PuntoRequest(
    @field:NotBlank(message = "La razon social es obligatoria") @field:Size(max = 120) val razonSocial: String?,
    @field:NotBlank(message = "La direccion es obligatoria") @field:Size(max = 200) val direccion: String?,
    @field:NotBlank(message = "La zona es obligatoria") val zona: String?,
    @field:NotBlank(message = "El material habitual es obligatorio") val materialHabitual: String?
)

data class PuntoDeRutaResponse(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String,
    val secuencia: Int,
    val atendido: Boolean,
    val motivoOmision: String?
)

data class RutaResponse(
    val id: Long,
    val fecha: String,
    val estado: String,
    val operario: String,
    val puntos: List<PuntoDeRutaResponse>
)

/** El cliente nunca envia el neto (RN-03): lo calcula el servicio. */
data class PesajeRequest(
    @field:NotNull(message = "La ruta es obligatoria") val rutaId: Long?,
    @field:NotNull(message = "El punto es obligatorio") val puntoId: Long?,
    @field:NotBlank(message = "El material es obligatorio") @field:Size(max = 40) val material: String?,
    @field:NotNull(message = "El peso bruto es obligatorio")
    @field:DecimalMin(value = "0.01", message = "El peso bruto debe ser mayor que cero") val pesoBruto: Double?,
    @field:NotNull(message = "La tara es obligatoria")
    @field:DecimalMin(value = "0.0", message = "La tara no puede ser negativa") val tara: Double?,
    @field:Size(max = 250, message = "La observacion admite 250 caracteres") val observacion: String?
)

data class CorreccionPesajeRequest(
    @field:NotBlank(message = "El material es obligatorio") @field:Size(max = 40) val material: String?,
    @field:NotNull(message = "El peso bruto es obligatorio")
    @field:DecimalMin(value = "0.01", message = "El peso bruto debe ser mayor que cero") val pesoBruto: Double?,
    @field:NotNull(message = "La tara es obligatoria")
    @field:DecimalMin(value = "0.0", message = "La tara no puede ser negativa") val tara: Double?,
    @field:Size(max = 250, message = "La observacion admite 250 caracteres") val observacion: String?
)

data class PesajeResponse(
    val id: Long,
    val rutaId: Long,
    val puntoId: Long,
    val fechaHora: String,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,
    val operario: String,
    val observacion: String?,
    val anulado: Boolean,
    val motivoAnulacion: String?
)
