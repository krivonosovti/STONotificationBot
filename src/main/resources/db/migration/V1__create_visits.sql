CREATE TABLE visits (
    id UUID PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    customer_id VARCHAR(100) NOT NULL,
    vehicle_id VARCHAR(100) NOT NULL,
    visited_on DATE NOT NULL,
    odometer_km INTEGER CHECK (odometer_km BETWEEN 0 AND 10000000),
    status VARCHAR(20) NOT NULL CHECK (status IN ('COMPLETED', 'CANCELLED')),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_visits_vehicle_date ON visits(vehicle_id, visited_on);
CREATE INDEX idx_visits_customer ON visits(customer_id);
CREATE TABLE visit_services (
    visit_id UUID NOT NULL REFERENCES visits(id) ON DELETE CASCADE,
    service_type VARCHAR(40) NOT NULL CHECK (service_type IN ('MAINTENANCE', 'OIL_CHANGE', 'BRAKE_PADS', 'OTHER')),
    PRIMARY KEY (visit_id, service_type)
);
