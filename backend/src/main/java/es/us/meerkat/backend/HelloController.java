package es.us.meerkat.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class HelloController {
    /**
     * Endpoint de prueba usado en la documentación Swagger.
     *
     * @return cadena fija "Hello world"
     */
    @GetMapping("/api/hello")
    public String hello() {
        return "Hello world";
    }
}
