-- MySQL schema (utf8mb4)
-- You can also let SQLAlchemy auto-create tables, but keeping this file helps deployment on cloud DB.

CREATE TABLE IF NOT EXISTS closet_items (
  id INT AUTO_INCREMENT PRIMARY KEY,
  owner VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  category VARCHAR(64) NOT NULL,
  color VARCHAR(64) NOT NULL DEFAULT '',
  season VARCHAR(64) NOT NULL DEFAULT '',
  style VARCHAR(64) NOT NULL DEFAULT '',
  scene VARCHAR(64) NOT NULL DEFAULT '',
  is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
  image_name VARCHAR(128) NOT NULL,
  image_url TEXT NOT NULL,
  tags_json TEXT NOT NULL,
  tag_model VARCHAR(64) NOT NULL DEFAULT '',
  tag_updated_at BIGINT NOT NULL DEFAULT 0,
  created_at BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL DEFAULT 0,
  INDEX idx_closet_owner_created (owner, created_at)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

