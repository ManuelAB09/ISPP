package es.us.meerkat.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSchemaFixRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(
                """
                        ALTER TABLE usuario
                        ADD COLUMN IF NOT EXISTS email_verificado BOOLEAN NOT NULL DEFAULT FALSE
                        """);
        jdbcTemplate.execute(
                """
                        DO $$
                        BEGIN
                            IF EXISTS (
                                SELECT 1
                                FROM information_schema.tables
                                WHERE table_schema = 'public'
                                  AND table_name = 'miembros_comunidad'
                            ) THEN
                                ALTER TABLE miembros_comunidad
                                DROP CONSTRAINT IF EXISTS miembros_comunidad_rol_check;

                                UPDATE miembros_comunidad
                                SET rol = 'ALUMNO'
                                WHERE UPPER(rol) = 'MIEMBRO';

                                ALTER TABLE miembros_comunidad
                                ADD CONSTRAINT miembros_comunidad_rol_check
                                CHECK (rol IN ('ADMIN', 'PROFESOR', 'ALUMNO'));
                            END IF;
                        END
                        $$;
                        """);
        log.info("Schema check applied: columna email_verificado verificada en tabla usuario");
        log.info(
                "Schema check applied: constraint de rol de miembros normalizado a"
                        + " ADMIN/PROFESOR/ALUMNO");
    }
}
