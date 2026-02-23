package es.us.meerkat.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom swagger-config response supplying multiple URL options.
 */
@RestController
public class SwaggerConfigController {
    @GetMapping("/v3/api-docs/swagger-config")
    public Map<String,Object> swaggerConfig() {
        Map<String,Object> config = new HashMap<>();
        List<Map<String,String>> urls = new ArrayList<>();
        urls.add(Map.of("name","Dynamic","url","/v3/api-docs"));
        urls.add(Map.of("name","Static","url","/spec/openapi.yaml"));
        config.put("urls", urls);
        config.put("oauth2RedirectUrl", "http://localhost:8080/swagger-ui/oauth2-redirect.html");
        config.put("validatorUrl", "");
        return config;
    }
}
