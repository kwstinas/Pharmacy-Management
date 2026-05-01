USE pharmacy_db;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role ENUM('ADMIN','PHARMACIST') NOT NULL DEFAULT 'PHARMACIST',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS med_categories (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  description TEXT,
  user_id BIGINT UNSIGNED NOT NULL DEFAULT 1,
  INDEX idx_categories_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS medicines (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  price DECIMAL(10,2) UNSIGNED NOT NULL,
  stock_qty INT UNSIGNED NOT NULL DEFAULT 0,
  category_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL DEFAULT 1,
  CONSTRAINT fk_medicines_category
    FOREIGN KEY (category_id) REFERENCES med_categories(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  INDEX idx_medicines_category (category_id),
  INDEX idx_medicines_name (name),
  INDEX idx_medicines_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_movements (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  medicine_id BIGINT UNSIGNED NOT NULL,
  type ENUM('IN','OUT') NOT NULL,
  quantity INT UNSIGNED NOT NULL,
  occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note TEXT,
  user_id BIGINT UNSIGNED NOT NULL DEFAULT 1,
  CONSTRAINT fk_movements_medicine
    FOREIGN KEY (medicine_id) REFERENCES medicines(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  INDEX idx_movements_medicine_date (medicine_id, occurred_at),
  INDEX idx_movements_type (type),
  INDEX idx_movements_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS medicine_ingredients (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  medicine_id BIGINT UNSIGNED NOT NULL,
  ingredient_name VARCHAR(255) NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL DEFAULT 1,
  CONSTRAINT fk_ingredients_medicine
    FOREIGN KEY (medicine_id) REFERENCES medicines(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  INDEX idx_ingredient_name (ingredient_name),
  INDEX idx_ingredient_medicine (medicine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activity_log (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  action VARCHAR(50) NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id BIGINT UNSIGNED,
  description TEXT,
  occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  user_id BIGINT UNSIGNED NOT NULL DEFAULT 1,
  INDEX idx_log_date (occurred_at),
  INDEX idx_log_action (action),
  INDEX idx_log_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;