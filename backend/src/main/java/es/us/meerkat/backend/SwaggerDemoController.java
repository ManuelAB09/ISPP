package es.us.meerkat.backend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Demo", description = "Demo endpoints para Swagger UI")
public class SwaggerDemoController {

    @GetMapping("/api/demo")
    @Operation(summary = "Demo endpoint", description = "Devuelve un mensaje de prueba para Swagger UI.")
    public String demo() {
        return "Swagger UI está funcionando correctamente.";
    }
}
