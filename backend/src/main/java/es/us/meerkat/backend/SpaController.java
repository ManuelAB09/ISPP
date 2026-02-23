package es.us.meerkat.backend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador SPA (Single Page Application).
 * 
 * Redirige todas las rutas que NO sean:
 *   - /api/**   (endpoints REST)
 *   - /actuator/** (health checks de Azure)
 *   - Archivos estáticos (JS, CSS, imágenes, etc.)
 *
 * hacia index.html, para que React Router maneje la navegación.
 */
@Controller
public class SpaController {

    @GetMapping(value = {
        "/",
        "/{path:^(?!api|actuator|static|index\\.html|favicon\\.ico|manifest\\.json|robots\\.txt).*}",
        "/{path:^(?!api|actuator|static|index\\.html|favicon\\.ico|manifest\\.json|robots\\.txt).*}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
