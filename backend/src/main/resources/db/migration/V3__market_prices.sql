-- Verified commodity prices entered by admins. Prices keep their original currency (no conversion).
CREATE TABLE market_prices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  commodity VARCHAR(80) NOT NULL,
  market VARCHAR(100) NOT NULL,
  province VARCHAR(60) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  price_date DATE NOT NULL,
  source VARCHAR(200) NOT NULL,
  entered_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_prices_user FOREIGN KEY (entered_by) REFERENCES users(id),
  CONSTRAINT chk_prices_currency CHECK (currency IN ('USD','ZIG','ZAR')),
  INDEX idx_prices_lookup (commodity, price_date)
);
