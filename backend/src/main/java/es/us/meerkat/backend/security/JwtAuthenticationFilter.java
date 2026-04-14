package es.us.meerkat.backend.security;

import java.io.IOException;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Filtro JWT que intercepta cada petición HTTP, valida el token e inyecta la autenticación en el
 * contexto de seguridad de Spring.
 *
 * <p>Si no hay token o es inválido, deja pasar la petición sin autenticación para que Spring
 * Security decida según las reglas de autorización configuradas.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Prefijo esperado en la cabecera Authorization. */
    private static final String BEARER_PREFIX = "Bearer ";

    /** Nombre de la cabecera HTTP de autorización. */
    private static final String AUTH_HEADER = "Authorization";

    /** Servicio para operaciones con JWT. */
    private final JwtService jwtService;

    /** Repositorio para cargar el usuario desde la base de datos. */
    private final UsuarioRepository usuarioRepository;

    @SuppressWarnings("deprecation")
    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain)
            throws ServletException, IOException {

        try {
            final String authHeader = request.getHeader(AUTH_HEADER);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }

            final String token = authHeader.substring(BEARER_PREFIX.length());
            final String email;

            try {
                email = jwtService.extractEmail(token);
            } catch (Exception e) {
                // Token malformado: dejar pasar sin autenticar
                filterChain.doFilter(request, response);
                return;
            }

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                final Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

                if (usuario != null && jwtService.isTokenValid(token, email)) {

                    // Rechazar tokens emitidos antes del último cambio de contraseña
                    if (usuario.getPasswordChangedAt() != null) {
                        final java.util.Date tokenIssuedAt = jwtService.extractIssuedAt(token);
                        final java.time.Instant passwordChangedInstant =
                                usuario.getPasswordChangedAt()
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toInstant();
                        if (tokenIssuedAt.toInstant().isBefore(passwordChangedInstant)) {
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }

                    final UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    usuario,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Cualquier otro error: limpiar contexto y dejar pasar
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
