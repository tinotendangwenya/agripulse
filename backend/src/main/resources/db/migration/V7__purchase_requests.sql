-- Buyers post what they want to buy; farmers answer with offers made from their own listings.
CREATE TABLE purchase_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  buyer_id BIGINT NOT NULL,
  product_name VARCHAR(100) NOT NULL,
  category VARCHAR(30) NOT NULL,
  quantity DECIMAL(12,2) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  max_price DECIMAL(12,2) NULL,
  currency VARCHAR(3) NOT NULL,
  province VARCHAR(60) NOT NULL,
  district VARCHAR(80),
  needed_by DATE NULL,
  note VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pr_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
  CONSTRAINT chk_pr_currency CHECK (currency IN ('USD','ZIG','ZAR')),
  INDEX idx_pr_open (status, product_name),
  INDEX idx_pr_buyer (buyer_id, id)
);

CREATE TABLE purchase_offers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  farmer_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity DECIMAL(12,2) NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  message VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  order_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_po_request FOREIGN KEY (request_id) REFERENCES purchase_requests(id),
  CONSTRAINT fk_po_farmer FOREIGN KEY (farmer_id) REFERENCES users(id),
  CONSTRAINT fk_po_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_po_order FOREIGN KEY (order_id) REFERENCES orders(id),
  INDEX idx_po_request (request_id, status),
  INDEX idx_po_farmer (farmer_id, id)
);
