package es.us.meerkat.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import es.us.meerkat.backend.security.JwtAuthenticationFilter;

/**
 * Configuración de seguridad de la aplicación.
 *
 * <p>Define política JWT sin estado, rutas públicas y el bean de cifrado de contraseñas.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configura la cadena de filtros de seguridad HTTP.
     *
     * <p>El filtro JWT se inyecta como parámetro del método para evitar dependencias circulares en
     * el contexto.
     *
     * @param http Objeto de configuración HTTP.
     * @param jwtAuthFilter Filtro JWT a registrar.
     * @return Cadena de filtros configurada.
     * @throws Exception si ocurre un error en la configuración.
     */
    @Bean
    public SecurityFilterChain filterChain(
            final HttpSecurity http, final JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/api/v1/auth/**",
                                                "/api/ubicaciones",
                                                "/api/ubicaciones/**",
                                                "/api/nominatim/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui**",
                                                "/swagger-ui.html",
                                                "/swagger-ui/index.html",
                                                "/v3/api-docs/**",
                                                "/v3/api-docs",
                                                "/spec/**",
                                                "/",
                                                "/index.html",
                                                "/favicon.ico",
                                                "/manifest.json",
                                                "/robots.txt",
                                                "/asset-manifest.json",
                                                "/logo192.png",
                                                "/logo512.png",
                                                "/static/**",
                                                "/error",
                                                "/webjars/**",
                                                "/h2-console/**",
                                                "/actuator/**")
                                        .permitAll()
                                        // Permitir preflight CORS (OPTIONS) en todas las rutas
                                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        // Permitir todas las rutas SPA (React Router)
                                        // que no empiecen por /api
                                        .requestMatchers(
                                                request ->
                                                        "GET".equals(request.getMethod())
                                                                && !request.getRequestURI()
                                                                        .startsWith("/api"))
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/v1/users/{userId}")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Evita que Spring Boot registre el filtro JWT dos veces (una como servlet filter y otra dentro
     * de Spring Security).
     *
     * @param filter Filtro JWT gestionado por Spring Security.
     * @return Bean de registro con el filtro desactivado.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            final JwtAuthenticationFilter filter) {
        final FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Bean para cifrar y verificar contraseñas con BCrypt.
     *
     * @return Instancia de {@link BCryptPasswordEncoder}.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
