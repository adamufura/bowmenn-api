CREATE TABLE IF NOT EXISTS shipment_status_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by UUID REFERENCES users(id),
    note TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_status_logs_shipment ON shipment_status_logs(shipment_id);
