package pe.upeu.ecorecicla.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import pe.upeu.ecorecicla.dominio.RecursoNoEncontradoException
import pe.upeu.ecorecicla.dominio.ReglaNegocioException
import pe.upeu.ecorecicla.dominio.ValidacionException
import pe.upeu.ecorecicla.web.dto.ErrorRespuesta
import tools.jackson.databind.ObjectMapper

/** Traduce cada excepcion al formato de error comun de la seccion 4.2. */
@RestControllerAdvice
class ManejadorDeErrores {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(ex: MethodArgumentNotValidException, req: HttpServletRequest) =
        responder(HttpStatus.BAD_REQUEST, "Datos invalidos", req,
            ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor invalido") })

    @ExceptionHandler(ValidacionException::class)
    fun validacionNegocio(ex: ValidacionException, req: HttpServletRequest) =
        responder(HttpStatus.BAD_REQUEST, "Datos invalidos", req, mapOf(ex.campo to ex.message!!))

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun tipoInvalido(ex: MethodArgumentTypeMismatchException, req: HttpServletRequest) =
        responder(HttpStatus.BAD_REQUEST, "Datos invalidos", req, mapOf(ex.name to "Valor invalido: ${ex.value}"))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun cuerpoIlegible(ex: HttpMessageNotReadableException, req: HttpServletRequest) =
        responder(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es valido", req)

    /** Login con credenciales incorrectas. */
    @ExceptionHandler(AuthenticationException::class)
    fun credenciales(ex: AuthenticationException, req: HttpServletRequest) =
        responder(HttpStatus.UNAUTHORIZED, "Usuario o contrasena incorrectos", req)

    /** RN-02: el servicio rechaza corregir pesajes ajenos o de otro dia. */
    @ExceptionHandler(AccessDeniedException::class)
    fun sinPrivilegio(ex: AccessDeniedException, req: HttpServletRequest) =
        responder(HttpStatus.FORBIDDEN, ex.message ?: "No tiene el privilegio necesario para esta operacion", req)

    @ExceptionHandler(RecursoNoEncontradoException::class)
    fun noEncontrado(ex: RecursoNoEncontradoException, req: HttpServletRequest) =
        responder(HttpStatus.NOT_FOUND, ex.message!!, req)

    @ExceptionHandler(ReglaNegocioException::class)
    fun conflicto(ex: ReglaNegocioException, req: HttpServletRequest) =
        responder(HttpStatus.CONFLICT, ex.message!!, req)

    /** Las de Spring MVC (404 de ruta, 405...) conservan su codigo; el resto es 500 sin detalles. */
    @ExceptionHandler(Exception::class)
    fun inesperado(ex: Exception, req: HttpServletRequest): ResponseEntity<ErrorRespuesta> {
        if (ex is ErrorResponse) {
            val estado = HttpStatus.valueOf(ex.statusCode.value())
            return responder(estado, ex.body.detail ?: estado.reasonPhrase, req)
        }
        log.error("Error no controlado en {}", req.requestURI, ex)
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado. Intente nuevamente", req)
    }

    private fun responder(
        estado: HttpStatus, mensaje: String, req: HttpServletRequest, campos: Map<String, String>? = null
    ) = ResponseEntity.status(estado).body(ErrorRespuesta.de(estado, mensaje, req.requestURI, campos))
}

/** Los 401 y 403 del filtro de seguridad ocurren antes del controlador: se escriben aqui con el mismo formato. */
@Component
class EscritorDeErrores(private val json: ObjectMapper) {
    fun escribir(req: HttpServletRequest, res: HttpServletResponse, estado: HttpStatus, mensaje: String) {
        res.status = estado.value()
        res.contentType = MediaType.APPLICATION_JSON_VALUE
        res.characterEncoding = "UTF-8"
        json.writeValue(res.outputStream, ErrorRespuesta.de(estado, mensaje, req.requestURI))
    }
}
