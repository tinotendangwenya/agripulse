-- Equipment hire: listings, owner-blocked dates, and bookings. Dates are inclusive.
INSERT INTO roles (name) VALUES ('EQUIPMENT_OWNER');

CREATE TABLE equipment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id BIGINT NOT NULL,
  category VARCHAR(20) NOT NULL,
  name VARCHAR(150) NOT NULL,
  description VARCHAR(500),
  province VARCHAR(60) NOT NULL,
  district VARCHAR(80),
  daily_rate DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  operator_included BOOLEAN NOT NULL DEFAULT FALSE,
  fuel_included BOOLEAN NOT NULL DEFAULT FALSE,
  deposit DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_eq_owner FOREIGN KEY (owner_id) REFERENCES users(id),
  CONSTRAINT chk_eq_currency CHECK (currency IN ('USD','ZIG','ZAR')),
  INDEX idx_eq_search (status, category, province),
  INDEX idx_eq_owner (owner_id, id)
);

CREATE TABLE equipment_blocks (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  equipment_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  reason VARCHAR(200),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_eb_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id),
  INDEX idx_eb_range (equipment_id, start_date, end_date)
);

CREATE TABLE equipment_bookings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  equipment_id BIGINT NOT NULL,
  hirer_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  days INT NOT NULL,
  daily_rate DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  total_price DECIMAL(14,2) NOT NULL,
  deposit DECIMAL(12,2) NOT NULL DEFAULT 0,
  note VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ebk_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id),
  CONSTRAINT fk_ebk_hirer FOREIGN KEY (hirer_id) REFERENCES users(id),
  INDEX idx_ebk_equipment (equipment_id, status, start_date, end_date),
  INDEX idx_ebk_hirer (hirer_id, id)
);
