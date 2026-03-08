package es.us.meerkat.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import es.us.meerkat.backend.util.EnvLoader;

@SpringBootApplication
public final class BackendApplication {

    private BackendApplication() {
        // utility class
    }

    public static void main(final String[] args) {
        EnvLoader.loadIfExists();
        SpringApplication.run(BackendApplication.class, args);
    }
}
