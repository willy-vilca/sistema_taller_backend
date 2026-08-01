-- Ejecutar una sola vez en la base de datos PostgreSQL de Azure ANTES de desplegar la API con esta versión.
-- El script es seguro de volver a ejecutar: no duplica las programaciones migradas desde el historial.

CREATE TABLE IF NOT EXISTS scheduled_services (
    id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    source_service_record_id UUID,
    completed_service_record_id UUID,
    created_by_user_id UUID NOT NULL,
    description VARCHAR(2000),
    scheduled_date DATE NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version BIGINT,
    CONSTRAINT pk_scheduled_services PRIMARY KEY (id),
    CONSTRAINT uk_scheduled_services_source_record UNIQUE (source_service_record_id),
    CONSTRAINT uk_scheduled_services_completed_record UNIQUE (completed_service_record_id),
    CONSTRAINT fk_scheduled_services_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT fk_scheduled_services_source_record
        FOREIGN KEY (source_service_record_id) REFERENCES service_records (id),
    CONSTRAINT fk_scheduled_services_completed_record
        FOREIGN KEY (completed_service_record_id) REFERENCES service_records (id),
    CONSTRAINT fk_scheduled_services_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES system_users (id)
);

CREATE INDEX IF NOT EXISTS idx_scheduled_services_vehicle_date
    ON scheduled_services (vehicle_id, scheduled_date);

CREATE INDEX IF NOT EXISTS idx_scheduled_services_date
    ON scheduled_services (scheduled_date);

-- Conserva las fechas de próximo servicio que ya existían antes de esta funcionalidad.
-- Como antes no se registraba una descripción, estas programaciones quedarán sin descripción.
INSERT INTO scheduled_services (
    id,
    vehicle_id,
    source_service_record_id,
    created_by_user_id,
    description,
    scheduled_date,
    created_at,
    updated_at,
    version
)
SELECT
    md5(random()::text || clock_timestamp()::text || service_record.id::text)::uuid,
    service_record.vehicle_id,
    service_record.id,
    service_record.registered_by_user_id,
    NULL,
    service_record.next_service_date,
    service_record.created_at,
    NOW(),
    0
FROM service_records service_record
WHERE service_record.next_service_date IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM scheduled_services scheduled_service
      WHERE scheduled_service.source_service_record_id = service_record.id
  );
