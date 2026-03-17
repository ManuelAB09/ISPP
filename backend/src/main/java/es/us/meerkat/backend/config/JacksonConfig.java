package es.us.meerkat.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuracion de Jackson para serializacion JSON.
 */
@Configuration
public class JacksonConfig {

    /**
     * Provee un ObjectMapper con los modulos descubiertos en classpath.
     *
     * @return mapper configurado para tipos Java Time, entre otros.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
