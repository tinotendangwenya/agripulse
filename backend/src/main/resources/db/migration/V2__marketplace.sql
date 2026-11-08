CREATE TABLE products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  seller_id BIGINT NOT NULL,
  name VARCHAR(150) NOT NULL,
  category VARCHAR(30) NOT NULL,
  description TEXT,
  quantity DECIMAL(12,2) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  province VARCHAR(60) NOT NULL,
  district VARCHAR(80),
  available_from DATE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users(id),
  CONSTRAINT chk_products_currency CHECK (currency IN ('USD','ZIG','ZAR')),
  INDEX idx_products_search (status, province, currency)
);

CREATE TABLE orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  quantity DECIMAL(12,2) NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  total_price DECIMAL(14,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  note VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_orders_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users(id)
);
