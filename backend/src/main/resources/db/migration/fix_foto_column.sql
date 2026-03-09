-- Migración para permitir imágenes de perfil en base64
-- Cambia la columna 'foto' de VARCHAR(255) a TEXT

ALTER TABLE usuario ALTER COLUMN foto TYPE TEXT;
