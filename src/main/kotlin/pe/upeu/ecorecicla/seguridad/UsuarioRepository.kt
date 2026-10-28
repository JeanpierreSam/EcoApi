package pe.upeu.ecorecicla.seguridad

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun findByUsername(username: String): Usuario?
}

/** Lo usan el login (AuthenticationManager) y el filtro JWT en cada peticion. */
@Service
class UsuarioDetailsService(private val usuarios: UsuarioRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String): UsuarioAutenticado =
        usuarios.findByUsername(username)?.let(UsuarioAutenticado::de)
            ?: throw UsernameNotFoundException("Usuario no encontrado: $username")
}
