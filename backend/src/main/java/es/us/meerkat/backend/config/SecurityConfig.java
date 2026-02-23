package es.us.meerkat.backend.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad de la aplicación.
 *
 * Define los beans necesarios para la autenticación y cifrado de contraseñas.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configura la cadena de filtros de seguridad HTTP.
     *
     * @param http Objeto de configuración de seguridad HTTP.
     * @return Cadena de filtros configurada.
     * @throws Exception si ocurre un error en la configuración.
     */
    @Bean
    public SecurityFilterChain filterChain(
        final HttpSecurity http) throws Exception {
        http
        .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
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
