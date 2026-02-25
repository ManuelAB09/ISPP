package es.us.meerkat.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Main Spring Boot application for Meerkat Backend. */
@SpringBootApplication
public final class BackendApplication {
    /** Private constructor for Checkstyle compliance. Do not use. */
    private BackendApplication() {
        // NO-OP: Required by Checkstyle.
        // Spring Boot needs a default constructor.
    }

    // ...existing code...

    /**
     * Main entry point for the Spring Boot application.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
