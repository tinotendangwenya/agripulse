-- Transporters, their vehicles, transport jobs and quotes.
INSERT INTO roles (name) VALUES ('TRANSPORTER');

CREATE TABLE vehicles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id BIGINT NOT NULL,
  vehicle_type VARCHAR(30) NOT NULL,
  description VARCHAR(150) NOT NULL,
  capacity_kg DECIMAL(12,2) NOT NULL,
  cargo_types VARCHAR(60) NOT NULL,
  base_province VARCHAR(60) NOT NULL,
  base_district VARCHAR(80),
  rate_per_km DECIMAL(10,2) NOT NULL,
  min_charge DECIMAL(10,2) NOT NULL DEFAULT 0,
  currency VARCHAR(3) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_vehicle_owner FOREIGN KEY (owner_id) REFERENCES users(id),
  CONSTRAINT chk_vehicle_currency CHECK (currency IN ('USD','ZIG','ZAR')),
  INDEX idx_vehicle_search (status, capacity_kg),
  INDEX idx_vehicle_owner (owner_id, id)
);

CREATE TABLE transport_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  requester_id BIGINT NOT NULL,
  cargo_type VARCHAR(20) NOT NULL,
  description VARCHAR(200) NOT NULL,
  weight_kg DECIMAL(12,2) NOT NULL,
  pickup_province VARCHAR(60) NOT NULL,
  pickup_district VARCHAR(80),
  delivery_province VARCHAR(60) NOT NULL,
  delivery_district VARCHAR(80),
  needed_by DATE NULL,
  note VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  accepted_quote_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tr_requester FOREIGN KEY (requester_id) REFERENCES users(id),
  INDEX idx_tr_open (status, id),
  INDEX idx_tr_requester (requester_id, id)
);

CREATE TABLE transport_quotes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  transporter_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  message VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tq_request FOREIGN KEY (request_id) REFERENCES transport_requests(id),
  CONSTRAINT fk_tq_transporter FOREIGN KEY (transporter_id) REFERENCES users(id),
  CONSTRAINT fk_tq_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  INDEX idx_tq_request (request_id, status),
  INDEX idx_tq_transporter (transporter_id, id)
);
