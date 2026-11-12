-- Session invalidation, privacy consent, expert applications and an admin audit log.
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN consent_at TIMESTAMP NULL;

CREATE TABLE expert_applications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  qualification VARCHAR(200) NOT NULL,
  organisation VARCHAR(150),
  registration_number VARCHAR(80),
  province VARCHAR(60) NOT NULL,
  speciality VARCHAR(150) NOT NULL,
  statement VARCHAR(1000),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reviewed_by BIGINT NULL,
  reviewed_at TIMESTAMP NULL,
  review_note VARCHAR(300),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ea_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_ea_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id),
  INDEX idx_ea_status (status, id),
  INDEX idx_ea_user (user_id, id)
);

-- No foreign keys on purpose: audit rows must survive whatever happens to the data they describe.
CREATE TABLE audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor_id BIGINT NULL,
  action VARCHAR(40) NOT NULL,
  target_user_id BIGINT NULL,
  detail VARCHAR(300),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_time (created_at),
  INDEX idx_audit_target (target_user_id)
);
