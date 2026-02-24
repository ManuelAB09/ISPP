package es.us.meerkat.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    /**
     * Cadena de filtros que configura la aplicación para permitir todas las peticiones y
     * deshabilitar CSRF. Utilizado en entorno local.
     *
     * @param http objeto de configuración de http
     * @return filtro construido
     * @throws Exception en caso de fallo de configuración
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        // Permit all requests (including swagger paths) to simplify local
        // development
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
