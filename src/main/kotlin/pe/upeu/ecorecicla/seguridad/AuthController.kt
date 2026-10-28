package pe.upeu.ecorecicla.seguridad

import jakarta.validation.Valid
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pe.upeu.ecorecicla.config.JwtService
import pe.upeu.ecorecicla.web.dto.LoginRequest
import pe.upeu.ecorecicla.web.dto.LoginResponse
import pe.upeu.ecorecicla.web.dto.UsuarioActualResponse

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService
) {

    /** RF-B01. Credenciales incorrectas: BadCredentialsException -> 401 en ManejadorDeErrores. */
    @PostMapping("/login")
    fun login(@Valid @RequestBody peticion: LoginRequest): LoginResponse {
        val autenticacion = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(peticion.usuario, peticion.contrasena)
        )
        val usuario = autenticacion.principal as UsuarioAutenticado
        return LoginResponse(
            token = jwtService.generarToken(usuario),
            tipo = "Bearer",
            expiraEnSegundos = jwtService.expiracionSegundos,
            usuario = usuario.username,
            nombre = usuario.nombre,
            rol = usuario.rol,
            privilegios = usuario.privilegios.toList()
        )
    }

    /** RF-B07: la app decide que acciones ofrece; el servicio igual las verifica. */
    @GetMapping("/me")
    fun yo(@AuthenticationPrincipal usuario: UsuarioAutenticado) = UsuarioActualResponse(
        id = usuario.id,
        usuario = usuario.username,
        nombre = usuario.nombre,
        rol = usuario.rol,
        privilegios = usuario.privilegios.toList()
    )
}
