package es.us.meerkat.backend.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

public class EnvLoader {

    public static void loadIfExists() {
        File f = resolveEnvFile();
        if (f == null) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
            br.lines()
                    .map(String::trim)
                    .filter(EnvLoader::isValidLine)
                    .forEach(EnvLoader::processLine);

            System.out.println("[EnvLoader] .env cargado: " + f.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[EnvLoader] Error cargando .env: " + e.getMessage());
        }
    }

    private static File resolveEnvFile() {
        File[] candidates = {
            new File(".env"), new File("../.env"), new File("src/main/resources/.env")
        };

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean isValidLine(String line) {
        return !line.startsWith("#") && !line.startsWith("//") && line.contains("=");
    }

    private static void processLine(String line) {
        int idx = line.indexOf('=');
        if (idx <= 0) {
            return;
        }

        String key = line.substring(0, idx).trim();
        String value = removeQuotes(line.substring(idx + 1).trim());

        if (shouldSetProperty(key)) {
            System.setProperty(key, value);
        }
    }

    private static String removeQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean shouldSetProperty(String key) {
        return System.getenv(key) == null && System.getProperty(key) == null;
    }
}
