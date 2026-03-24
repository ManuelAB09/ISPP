-- Activar notificaciones push para todos los usuarios existentes (opt-in por defecto)
UPDATE usuario SET notificaciones_push = true WHERE notificaciones_push = false;
