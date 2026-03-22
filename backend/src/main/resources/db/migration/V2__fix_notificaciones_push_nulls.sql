-- Migración para corregir valores NULL en notificaciones_push
-- Actualiza todos los valores NULL a false (valor por defecto)

UPDATE usuario SET notificaciones_push = false WHERE notificaciones_push IS NULL;

-- Asegura que no haya más NULLs en la columna (añade constraint NOT NULL si no existe)
ALTER TABLE usuario ALTER COLUMN notificaciones_push SET NOT NULL;
