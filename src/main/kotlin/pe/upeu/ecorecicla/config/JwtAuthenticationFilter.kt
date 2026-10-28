package pe.upeu.ecorecicla.config

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.filter.OncePerRequestFilter
import pe.upeu.ecorecicla.seguridad.UsuarioDetailsService

/**
 * Lee "Authorization: Bearer <token>", verifica la firma y deja al usuario con sus
 * privilegios en el contexto. Si el token falta o no sirve, la peticion sigue sin
 * autenticar y SecurityConfig responde 401 con el motivo guardado en ATRIBUTO_ERROR.
 */
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val usuarios: UsuarioDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val cabecera = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            try {
                // Se relee de la base: un usuario desactivado deja de entrar aunque su token siga vigente.
                val usuario = usuarios.loadUserByUsername(jwtService.extraerUsuario(cabecera.removePrefix(PREFIJO).trim()))
                if (usuario.isEnabled) {
                    SecurityContextHolder.getContext().authentication =
                        UsernamePasswordAuthenticationToken(usuario, null, usuario.authorities)
                } else {
                    request.setAttribute(ATRIBUTO_ERROR, "El usuario esta deshabilitado")
                }
            } catch (e: ExpiredJwtException) {
                request.setAttribute(ATRIBUTO_ERROR, "El token ha vencido. Inicie sesion nuevamente")
            } catch (e: UsernameNotFoundException) {
                request.setAttribute(ATRIBUTO_ERROR, "El usuario del token no existe")
            } catch (e: JwtException) {
                request.setAttribute(ATRIBUTO_ERROR, "El token no es valido")
            } catch (e: IllegalArgumentException) {
                request.setAttribute(ATRIBUTO_ERROR, "El token no es valido")
            }
        }
        chain.doFilter(request, response)
    }

    companion object {
        const val ATRIBUTO_ERROR = "jwt.error"
        private const val PREFIJO = "Bearer "
    }
}
