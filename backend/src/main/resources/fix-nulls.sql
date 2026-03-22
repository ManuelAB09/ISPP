-- Fix NULL values in usuario table before Hibernate applies schema updates
UPDATE usuario SET autenticacion_dos_factores = false WHERE autenticacion_dos_factores IS NULL;
UPDATE usuario SET notificaciones_email = true WHERE notificaciones_email IS NULL;
UPDATE usuario SET visible_en_listados = true WHERE visible_en_listados IS NULL;
UPDATE usuario SET es_tutor = false WHERE es_tutor IS NULL;
UPDATE usuario SET notificaciones_push = false WHERE notificaciones_push IS NULL;
