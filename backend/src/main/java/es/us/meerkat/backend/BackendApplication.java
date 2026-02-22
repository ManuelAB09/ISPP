
/**
 * Main Spring Boot application for Meerkat Backend.
 */
package es.us.meerkat.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication

/**
 * Utility class for starting the Spring Boot application.
 */
public final class BackendApplication {


	/**
	 * Main entry point for the Spring Boot application.
	 * @param args the command line arguments
	 */
	public static void main(final String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
