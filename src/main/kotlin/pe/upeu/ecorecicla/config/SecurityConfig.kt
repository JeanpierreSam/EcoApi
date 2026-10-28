package pe.upeu.ecorecicla.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import pe.upeu.ecorecicla.seguridad.Privilegios.PESAJE_ANULAR
import pe.upeu.ecorecicla.seguridad.Privilegios.PESAJE_CREAR
import pe.upeu.ecorecicla.seguridad.Privilegios.PESAJE_EDITAR
import pe.upeu.ecorecicla.seguridad.Privilegios.PESAJE_LEER
import pe.upeu.ecorecicla.seguridad.Privilegios.PUNTO_GESTIONAR
import pe.upeu.ecorecicla.seguridad.Privilegios.PUNTO_LEER
import pe.upeu.ecorecicla.seguridad.Privilegios.RUTA_CERRAR
import pe.upeu.ecorecicla.seguridad.UsuarioDetailsService

/**
 * Unico lugar donde se decide que privilegio exige cada operacion. Los controladores
 * no preguntan por roles: sin el privilegio la peticion no llega a ellos (403).
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(
        http: HttpSecurity,
        jwtService: JwtService,
        usuarios: UsuarioDetailsService,
        errores: EscritorDeErrores
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .logout { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    .requestMatchers(POST, "/api/v1/auth/login").permitAll()
                    .requestMatchers("/error").permitAll()

                    .requestMatchers(GET, "/api/v1/rutas/**").hasAuthority(PUNTO_LEER)
                    .requestMatchers(POST, "/api/v1/rutas/*/cierre").hasAuthority(RUTA_CERRAR)

                    .requestMatchers(GET, "/api/v1/puntos/*/pesajes").hasAuthority(PESAJE_LEER)
                    .requestMatchers(GET, "/api/v1/puntos", "/api/v1/puntos/**").hasAuthority(PUNTO_LEER)
                    .requestMatchers(POST, "/api/v1/puntos").hasAuthority(PUNTO_GESTIONAR)

                    .requestMatchers(GET, "/api/v1/pesajes/**").hasAuthority(PESAJE_LEER)
                    .requestMatchers(POST, "/api/v1/pesajes").hasAuthority(PESAJE_CREAR)
                    .requestMatchers(PUT, "/api/v1/pesajes/*").hasAuthority(PESAJE_EDITAR)
                    .requestMatchers(DELETE, "/api/v1/pesajes/*").hasAuthority(PESAJE_ANULAR)

                    // /auth/me y cualquier otra ruta: basta con estar autenticado
                    .anyRequest().authenticated()
            }
            .exceptionHandling { manejo ->
                manejo.authenticationEntryPoint { request, response, _ ->
                    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                    val motivo = request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR) as String?
                    errores.escribir(request, response, HttpStatus.UNAUTHORIZED,
                        motivo ?: "Debe iniciar sesion para acceder a este recurso")
                }
                manejo.accessDeniedHandler { request, response, _ ->
                    errores.escribir(request, response, HttpStatus.FORBIDDEN,
                        "No tiene el privilegio necesario para esta operacion")
                }
            }
            .addFilterBefore(JwtAuthenticationFilter(jwtService, usuarios), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    /** BCrypt: hash lento y con sal; la contrasena nunca se guarda en claro. */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(configuracion: AuthenticationConfiguration): AuthenticationManager =
        configuracion.authenticationManager
}
