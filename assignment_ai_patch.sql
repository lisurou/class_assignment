ALTER TABLE asssignment
  ADD COLUMN ai_enabled TINYINT(1) DEFAULT 0,
  ADD COLUMN ai_score INT NULL,
  ADD COLUMN ai_comment TEXT NULL;

ALTER TABLE asssignment
  ADD INDEX idx_assignment_course (id, assignment_id),
  ADD INDEX idx_assignment_account (account_id, id, assignment_id);
