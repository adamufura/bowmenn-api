CREATE TABLE IF NOT EXISTS shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tracking_number VARCHAR(20) UNIQUE NOT NULL,
    customer_id UUID NOT NULL REFERENCES users(id),
    driver_id UUID REFERENCES users(id),
    pickup_address TEXT NOT NULL,
    pickup_lat DECIMAL(10,8),
    pickup_lng DECIMAL(11,8),
    delivery_address TEXT NOT NULL,
    delivery_lat DECIMAL(10,8),
    delivery_lng DECIMAL(11,8),
    cargo_description TEXT NOT NULL,
    cargo_weight DECIMAL(10,2) NOT NULL,
    truck_type VARCHAR(20) NOT NULL CHECK (truck_type IN ('MINI', 'MEDIUM', 'LARGE')),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','ASSIGNED','ACCEPTED','PICKED_UP','IN_TRANSIT','DELIVERED','CANCELLED','REJECTED')),
    estimated_distance_km DECIMAL(10,2),
    estimated_price DECIMAL(12,2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_shipments_customer ON shipments(customer_id);
CREATE INDEX idx_shipments_driver ON shipments(driver_id);
CREATE INDEX idx_shipments_status ON shipments(status);
CREATE INDEX idx_shipments_tracking ON shipments(tracking_number);
