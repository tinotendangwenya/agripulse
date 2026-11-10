-- Expert questions (photos are files on disk, not in the database) and farm records.
CREATE TABLE questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  farmer_id BIGINT NOT NULL,
  farm_id BIGINT NULL,
  subject VARCHAR(20) NOT NULL,
  topic VARCHAR(80),
  title VARCHAR(150) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_q_farmer FOREIGN KEY (farmer_id) REFERENCES users(id),
  CONSTRAINT fk_q_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE SET NULL,
  INDEX idx_q_status (status, updated_at),
  INDEX idx_q_farmer (farmer_id, updated_at)
);

CREATE TABLE question_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  author_role VARCHAR(20) NOT NULL,
  body TEXT,
  image_file VARCHAR(80) NULL,
  image_type VARCHAR(30) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_qm_question FOREIGN KEY (question_id) REFERENCES questions(id),
  CONSTRAINT fk_qm_author FOREIGN KEY (author_id) REFERENCES users(id),
  INDEX idx_qm_question (question_id)
);

-- Farm records. Deleting a farm deletes its records. Money is kept per currency, never converted.
CREATE TABLE farm_transactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  farm_id BIGINT NOT NULL,
  crop_record_id BIGINT NULL,
  entry_type VARCHAR(10) NOT NULL,
  category VARCHAR(20) NOT NULL,
  amount DECIMAL(14,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  tx_date DATE NOT NULL,
  description VARCHAR(300),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tx_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE,
  CONSTRAINT fk_tx_crop FOREIGN KEY (crop_record_id) REFERENCES crop_records(id) ON DELETE SET NULL,
  CONSTRAINT chk_tx_type CHECK (entry_type IN ('INCOME','EXPENSE')),
  CONSTRAINT chk_tx_currency CHECK (currency IN ('USD','ZIG','ZAR')),
  INDEX idx_tx_farm_date (farm_id, tx_date)
);

CREATE TABLE harvests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  farm_id BIGINT NOT NULL,
  crop_record_id BIGINT NOT NULL,
  harvest_date DATE NOT NULL,
  quantity_kg DECIMAL(14,2) NOT NULL,
  note VARCHAR(300),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_h_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE,
  CONSTRAINT fk_h_crop FOREIGN KEY (crop_record_id) REFERENCES crop_records(id) ON DELETE CASCADE,
  INDEX idx_h_farm_date (farm_id, harvest_date)
);

CREATE TABLE livestock_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  farm_id BIGINT NOT NULL,
  animal_type VARCHAR(20) NOT NULL,
  event_type VARCHAR(20) NOT NULL,
  event_date DATE NOT NULL,
  head_count INT NOT NULL,
  note VARCHAR(300),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_le_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE,
  INDEX idx_le_farm (farm_id, event_date)
);
