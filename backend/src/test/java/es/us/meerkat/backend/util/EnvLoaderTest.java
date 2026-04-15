package es.us.meerkat.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EnvLoaderTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("ENV_LOADER_DIRECT");
        System.clearProperty("ENV_LOADER_KEEP");
    }

    @Test
    void processLineShouldSetPropertyAndStripQuotes() throws Exception {
        Method processLine = EnvLoader.class.getDeclaredMethod("processLine", String.class);
        processLine.setAccessible(true);

        processLine.invoke(null, "ENV_LOADER_DIRECT='quoted-value'");

        assertThat(System.getProperty("ENV_LOADER_DIRECT")).isEqualTo("quoted-value");
    }

    @Test
    void processLineShouldNotOverrideExistingSystemProperty() throws Exception {
        Method processLine = EnvLoader.class.getDeclaredMethod("processLine", String.class);
        processLine.setAccessible(true);

        System.setProperty("ENV_LOADER_KEEP", "original");
        processLine.invoke(null, "ENV_LOADER_KEEP=updated");

        assertThat(System.getProperty("ENV_LOADER_KEEP")).isEqualTo("original");
    }

    @Test
    void loadIfExistsShouldReadEnvFileFromCurrentDirectory() throws Exception {
        String key = "ENV_LOADER_TMP_" + System.nanoTime();
        Path envPath = Path.of(".env");

        String previousContent = Files.exists(envPath) ? Files.readString(envPath) : null;

        try {
            Files.write(
                    envPath,
                    List.of(
                            "# comment",
                            "// comment",
                            "INVALID_LINE",
                            key + "=\"value from env\""),
                    StandardCharsets.UTF_8);

            System.clearProperty(key);
            EnvLoader.loadIfExists();

            assertThat(System.getProperty(key)).isEqualTo("value from env");
        } finally {
            System.clearProperty(key);
            if (previousContent != null) {
                Files.writeString(envPath, previousContent, StandardCharsets.UTF_8);
            } else {
                Files.deleteIfExists(envPath);
            }
        }
    }
}
