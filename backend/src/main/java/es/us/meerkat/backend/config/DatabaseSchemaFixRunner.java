package es.us.meerkat.backend.config;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSchemaFixRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    // Este runner se ejecutará al iniciar la aplicación y aplicará las correcciones
    // necesarias
    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgres()) {
            log.info("Schema fix skipped: not running on PostgreSQL");
            return;
        }
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
        jdbcTemplate.execute(
                """
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'transaccion_pago'
                    ) THEN
                        ALTER TABLE transaccion_pago
                        DROP CONSTRAINT IF EXISTS transaccion_pago_estado_check;

                        ALTER TABLE transaccion_pago
                        ADD CONSTRAINT transaccion_pago_estado_check
                        CHECK (estado IN ('PENDIENTE', 'COMPLETADA', 'FALLIDA', 'REEMBOLSADA'));
                    END IF;
                END
                $$;
                """);
        log.info("Schema check applied: columna email_verificado verificada en tabla usuario");
        log.info(
                "Schema check applied: constraint de rol de miembros normalizado a"
                        + " ADMIN/PROFESOR/ALUMNO");
        log.info("Schema check applied: constraint de estado de transaccion_pago actualizado");
    }

    private boolean isPostgres() {
        try {
            String url = dataSource.getConnection().getMetaData().getURL();
            return url != null && url.startsWith("jdbc:postgresql");
        } catch (SQLException e) {
            log.warn("Could not determine database type, skipping schema fix", e);
            return false;
        }
    }
}
