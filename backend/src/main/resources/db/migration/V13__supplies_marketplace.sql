CREATE TABLE supplier_profiles (
  user_id BIGINT PRIMARY KEY,
  business_name VARCHAR(150) NOT NULL,
  province VARCHAR(60) NOT NULL,
  district VARCHAR(80),
  address VARCHAR(255),
  phone VARCHAR(20) NOT NULL,
  description TEXT,
  offers_delivery BOOLEAN NOT NULL DEFAULT FALSE,
  verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_supprof_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE supply_products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_id BIGINT NOT NULL,
  name VARCHAR(150) NOT NULL,
  brand VARCHAR(100),
  category VARCHAR(30) NOT NULL,
  description TEXT,
  unit VARCHAR(20) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  stock_qty DECIMAL(12,2) NOT NULL DEFAULT 0,
  registration_no VARCHAR(60),
  expiry_date DATE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_supprod_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_profiles(user_id),
  CONSTRAINT chk_supprod_currency CHECK (currency IN ('USD','ZIG','ZAR')),
  INDEX idx_supprod_search (status, category)
);