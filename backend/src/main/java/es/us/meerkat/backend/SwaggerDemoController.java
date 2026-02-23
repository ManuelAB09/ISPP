package es.us.meerkat.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Demo", description = "Demo endpoints para Swagger UI")
public final class SwaggerDemoController {

    /**
     * Punto de entrada de demostración que aparece en la UI de Swagger.
     *
     * @return texto de confirmación
     */
    @GetMapping("/api/demo")
    @Operation(
            summary = "Demo endpoint",
            description = "Devuelve un mensaje de prueba para Swagger UI."
    )
    public String demo() {
        return "Swagger UI está funcionando correctamente.";
    }
}
