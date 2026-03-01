package es.us.meerkat.backend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador SPA (Single Page Application).
 *
 * <p>
 * Redirige todas las rutas que NO sean: - /api/** (endpoints REST) -
 * /actuator/** (health checks
 * de Azure) - Archivos estáticos (JS, CSS, imágenes, etc.)
 *
 * <p>
 * hacia index.html, para que React Router maneje la navegación.
 */
@Controller
public class SpaController {

    private static final String EXCLUDE = "^(?!api|actuator|ws|static|spec|swagger-ui|v3|index\\.html|favicon\\.ico|manifest\\.json|robots\\.txt).*";

    @GetMapping(value = { "/", "/{path:" + EXCLUDE + "}", "/{path:" + EXCLUDE + "}/**" })
    public String forward() {
        return "forward:/index.html";
    }
}
