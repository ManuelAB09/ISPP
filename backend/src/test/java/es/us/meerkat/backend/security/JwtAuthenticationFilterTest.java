package es.us.meerkat.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueFilterChainWithoutAuthWhenNoHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldContinueFilterChainWithoutAuthWhenHeaderDoesNotStartWithBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldContinueFilterChainWithoutAuthWhenTokenIsMalformed() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer malformed-token");
        when(jwtService.extractEmail("malformed-token")).thenThrow(new RuntimeException("bad"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSetAuthenticationWhenTokenIsValid() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("user@test.es");

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractEmail("valid-token")).thenReturn("user@test.es");
        when(usuarioRepository.findByEmail("user@test.es")).thenReturn(Optional.of(usuario));
        when(jwtService.isTokenValid("valid-token", "user@test.es")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(usuario);
    }

    @Test
    void shouldNotSetAuthenticationWhenTokenIsInvalid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtService.extractEmail("invalid-token")).thenReturn("user@test.es");
        when(usuarioRepository.findByEmail("user@test.es")).thenReturn(Optional.of(new Usuario()));
        when(jwtService.isTokenValid("invalid-token", "user@test.es")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldNotSetAuthenticationWhenUserNotFound() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractEmail("valid-token")).thenReturn("unknown@test.es");
        when(usuarioRepository.findByEmail("unknown@test.es")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldRejectTokenIssuedBeforePasswordChange() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("user@test.es");
        usuario.setPasswordChangedAt(LocalDateTime.now());

        when(request.getHeader("Authorization")).thenReturn("Bearer old-token");
        when(jwtService.extractEmail("old-token")).thenReturn("user@test.es");
        when(usuarioRepository.findByEmail("user@test.es")).thenReturn(Optional.of(usuario));
        when(jwtService.isTokenValid("old-token", "user@test.es")).thenReturn(true);
        // Token was issued 1 hour before password change
        when(jwtService.extractIssuedAt("old-token"))
                .thenReturn(
                        Date.from(
                                usuario.getPasswordChangedAt()
                                        .minusHours(1)
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldAcceptTokenIssuedAfterPasswordChange() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("user@test.es");
        usuario.setPasswordChangedAt(LocalDateTime.now().minusHours(2));

        when(request.getHeader("Authorization")).thenReturn("Bearer new-token");
        when(jwtService.extractEmail("new-token")).thenReturn("user@test.es");
        when(usuarioRepository.findByEmail("user@test.es")).thenReturn(Optional.of(usuario));
        when(jwtService.isTokenValid("new-token", "user@test.es")).thenReturn(true);
        // Token was issued 1 hour after password change
        when(jwtService.extractIssuedAt("new-token"))
                .thenReturn(
                        Date.from(
                                usuario.getPasswordChangedAt()
                                        .plusHours(1)
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
