package es.us.meerkat.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application for Meerkat Backend.
 */
@SpringBootApplication
public final class BackendApplication {
    // ...existing code...

    /**
     * Main entry point for the Spring Boot application.
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
