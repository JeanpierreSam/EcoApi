package pe.upeu.ecorecicla.seguridad

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(name = "usuario")
class Usuario(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var username: String = "",
    var password: String = "",
    var nombre: String = "",
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id")
    var rol: Rol = Rol(),
    var activo: Boolean = true
)

/**
 * Principal de Spring Security. Las authorities son SOLO los privilegios del rol:
 * por eso las reglas de SecurityConfig usan hasAuthority(privilegio) y nunca hasRole.
 */
class UsuarioAutenticado(
    val id: Long,
    private val nombreUsuario: String,
    private val hash: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>,
    private val activo: Boolean
) : UserDetails {

    fun tiene(privilegio: String) = privilegio in privilegios

    override fun getAuthorities(): Collection<GrantedAuthority> = privilegios.map(::SimpleGrantedAuthority)
    override fun getPassword() = hash
    override fun getUsername() = nombreUsuario
    override fun isEnabled() = activo

    companion object {
        fun de(u: Usuario) = UsuarioAutenticado(
            id = u.id!!,
            nombreUsuario = u.username,
            hash = u.password,
            nombre = u.nombre,
            rol = u.rol.nombre,
            privilegios = u.rol.privilegios.map { it.nombre }.toSortedSet(),
            activo = u.activo
        )
    }
}
