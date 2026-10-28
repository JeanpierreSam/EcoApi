package pe.upeu.ecorecicla.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}") secretoBase64: String,
    @Value("\${app.jwt.expiracion-minutos:60}") private val expiracionMinutos: Long
) {
    // HS256 exige al menos 256 bits: con una clave mas corta falla al arrancar.
    private val clave: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64))

    val expiracionSegundos: Long get() = expiracionMinutos * 60

    /** sub = usuario, uid = id, rol y privilegios; vence en una hora. Sin renovacion. */
    fun generarToken(usuario: UsuarioAutenticado): String {
        val ahora = Instant.now()
        return Jwts.builder()
            .subject(usuario.username)
            .claim("uid", usuario.id)
            .claim("nombre", usuario.nombre)
            .claim("rol", usuario.rol)
            .claim("privilegios", usuario.privilegios.toList())
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
            .signWith(clave)
            .compact()
    }

    /** Verifica firma y vencimiento: lanza ExpiredJwtException o JwtException si no son validos. */
    fun extraerUsuario(token: String): String =
        Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).payload.subject
}
