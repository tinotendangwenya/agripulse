-- Price quotations (approved by an admin before they appear) and price alerts.
CREATE INDEX idx_prices_date ON market_prices (price_date);

CREATE TABLE price_quotes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  submitter_id BIGINT NOT NULL,
  commodity VARCHAR(80) NOT NULL,
  market VARCHAR(100) NOT NULL,
  province VARCHAR(60) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  price_date DATE NOT NULL,
  note VARCHAR(300),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reviewed_by BIGINT NULL,
  reviewed_at TIMESTAMP NULL,
  review_note VARCHAR(300),
  market_price_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pq_submitter FOREIGN KEY (submitter_id) REFERENCES users(id),
  CONSTRAINT fk_pq_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id),
  CONSTRAINT fk_pq_price FOREIGN KEY (market_price_id) REFERENCES market_prices(id) ON DELETE SET NULL,
  CONSTRAINT chk_pq_currency CHECK (currency IN ('USD','ZIG','ZAR')),
  INDEX idx_pq_status (status, id),
  INDEX idx_pq_submitter (submitter_id, id)
);

CREATE TABLE price_alerts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  commodity VARCHAR(80) NOT NULL,
  market VARCHAR(100) NULL,
  currency VARCHAR(3) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  direction VARCHAR(5) NOT NULL,
  threshold DECIMAL(12,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  triggered_at TIMESTAMP NULL,
  triggered_price DECIMAL(12,2) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pa_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_pa_direction CHECK (direction IN ('ABOVE','BELOW')),
  CONSTRAINT chk_pa_currency CHECK (currency IN ('USD','ZIG','ZAR')),
  INDEX idx_pa_match (status, commodity, currency, unit),
  INDEX idx_pa_user (user_id, id)
);
