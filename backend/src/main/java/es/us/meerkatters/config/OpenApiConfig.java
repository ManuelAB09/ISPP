package es.us.meerkatters.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI/Swagger para la documentación de la API.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MeerKatters API")
                        .version("1.0.0")
                        .description("""
                                API REST para la plataforma MeerKatters.
                                
                                ## Autenticación
                                La mayoría de endpoints requieren autenticación JWT.
                                Usa el endpoint `/api/v1/auth/login` para obtener un token.
                                
                                ## Modelo de Autorización
                                Los permisos se evalúan por contexto (usuario + comunidad),
                                no por roles globales. Un mismo usuario puede ser estudiante
                                en una comunidad y tutor en otra.
                                """)
                        .contact(new Contact()
                                .name("Equipo MeerKatters")
                                .email("meerkatters@alum.us.es"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor de desarrollo"),
                        new Server()
                                .url("https://meerkatters.azurewebsites.net")
                                .description("Servidor de producción")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido del endpoint /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
