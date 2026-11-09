-- Every leaf check is recorded so results can later be compared with what extension officers confirm.
-- Photos are NOT stored. farm_id is set to NULL if the farm is deleted.
CREATE TABLE ai_predictions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  farm_id BIGINT NULL,
  crop VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL,
  top_label VARCHAR(150),
  top_confidence DECIMAL(5,3),
  model VARCHAR(150) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_ai_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE SET NULL,
  INDEX idx_ai_user (user_id, created_at)
);
