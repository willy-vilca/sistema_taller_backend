-- Ejecutar una sola vez en PostgreSQL ANTES de desplegar esta versión de la API.
-- El script es seguro de volver a ejecutar y conserva todos los usuarios existentes.

ALTER TABLE system_users
    ADD COLUMN IF NOT EXISTS email VARCHAR(254);

ALTER TABLE system_users
    ADD COLUMN IF NOT EXISTS schedule_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Ningún administrador existente recibirá correos hasta que se registre su dirección de correo
-- y se active la preferencia desde Administración > Usuarios.
UPDATE system_users
SET schedule_notifications_enabled = FALSE
WHERE email IS NULL;

CREATE TABLE IF NOT EXISTS scheduled_service_notification_deliveries (
    id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,
    notification_date DATE NOT NULL,
    scheduled_service_count INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_operation_id VARCHAR(150),
    error_message VARCHAR(1000),
    sent_at TIMESTAMP(6) WITH TIME ZONE,
    attempt_count INTEGER NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_scheduled_service_notification_deliveries PRIMARY KEY (id),
    CONSTRAINT fk_scheduled_service_notification_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES system_users (id),
    CONSTRAINT uk_scheduled_service_notification_recipient_date
        UNIQUE (recipient_user_id, notification_date)
);

CREATE INDEX IF NOT EXISTS idx_scheduled_service_notification_date
    ON scheduled_service_notification_deliveries (notification_date);
