CREATE TABLE t_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, openid VARCHAR(64) NOT NULL, phone VARCHAR(20), nickname VARCHAR(64), avatar VARCHAR(255),
  staff_role VARCHAR(20) NOT NULL DEFAULT 'customer', level INT NOT NULL DEFAULT 0, points INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 0, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_member_openid(openid), UNIQUE KEY uk_member_phone(phone), KEY idx_member_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_card_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL, category VARCHAR(20) NOT NULL, cover VARCHAR(255), benefits JSON NOT NULL,
  price DECIMAL(10,2) NOT NULL, sale_price DECIMAL(10,2) NOT NULL, activity_price DECIMAL(10,2), activity_start_at DATETIME, activity_end_at DATETIME,
  valid_days INT NOT NULL, rules VARCHAR(500), status TINYINT NOT NULL DEFAULT 0, sort INT NOT NULL DEFAULT 0, deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_product_status_sort(status, deleted, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_activity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(100) NOT NULL, image VARCHAR(255) NOT NULL, target_type VARCHAR(20) NOT NULL DEFAULT 'none',
  target_id BIGINT, start_at DATETIME NOT NULL, end_at DATETIME NOT NULL, sort INT NOT NULL DEFAULT 0, status TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_activity_active(status,start_at,end_at,sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, order_no VARCHAR(32) NOT NULL, member_id BIGINT NOT NULL, payable_amount DECIMAL(10,2) NOT NULL,
  paid_amount DECIMAL(10,2), pay_method VARCHAR(10) NOT NULL, pay_status TINYINT NOT NULL DEFAULT 0, operator_member_id BIGINT,
  transaction_id VARCHAR(64), paid_at DATETIME, expire_at DATETIME, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order_no(order_no), KEY idx_order_member(member_id,created_at), KEY idx_order_status(pay_status,expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, product_id BIGINT NOT NULL, quantity INT NOT NULL DEFAULT 1,
  unit_price DECIMAL(10,2) NOT NULL, product_snapshot JSON NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order_item_order(order_id), KEY idx_order_item_product(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_membership_card (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, member_id BIGINT NOT NULL, product_id BIGINT NOT NULL, order_item_id BIGINT NOT NULL,
  benefits_remaining JSON NOT NULL, valid_from DATETIME NOT NULL, valid_until DATETIME NOT NULL, status VARCHAR(20) NOT NULL,
  version INT NOT NULL DEFAULT 0, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_card_order_item(order_item_id), KEY idx_card_member_status(member_id,status), KEY idx_card_valid_until(valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_verification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, request_no VARCHAR(64) NOT NULL, card_id BIGINT NOT NULL, member_id BIGINT NOT NULL,
  benefit_id VARCHAR(64) NOT NULL, item_snapshot VARCHAR(100) NOT NULL, quantity DECIMAL(10,2) NOT NULL,
  operator_name VARCHAR(50) NOT NULL, operator_member_id BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_verification_request(request_no), KEY idx_verification_card(card_id), KEY idx_verification_member(member_id), KEY idx_verification_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, member_id BIGINT NOT NULL, type VARCHAR(20) NOT NULL, title VARCHAR(100) NOT NULL,
  content VARCHAR(500) NOT NULL, ref_id BIGINT, is_read TINYINT NOT NULL DEFAULT 0, push_status VARCHAR(20) NOT NULL DEFAULT 'pending',
  push_attempts INT NOT NULL DEFAULT 0, push_last_error VARCHAR(255), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_notification_member_read(member_id,is_read,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_admin_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50) NOT NULL, password VARCHAR(100) NOT NULL, name VARCHAR(50) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admin_username(username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_idempotency_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, actor_type VARCHAR(20) NOT NULL, actor_id BIGINT NOT NULL, api_scope VARCHAR(50) NOT NULL,
  request_no VARCHAR(64) NOT NULL, request_hash VARCHAR(64) NOT NULL, status VARCHAR(20) NOT NULL, response_snapshot JSON, expire_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_idempotency_actor(actor_type,actor_id,api_scope,request_no), KEY idx_idempotency_expire(expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_consumed_member_code (
  nonce VARCHAR(64) PRIMARY KEY, member_id BIGINT NOT NULL, action VARCHAR(20) NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expire_at DATETIME NOT NULL, KEY idx_consumed_expire(expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, operator_type VARCHAR(20) NOT NULL, operator_id BIGINT, action VARCHAR(50) NOT NULL,
  target_type VARCHAR(30), target_id BIGINT, detail JSON, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operation_operator(operator_type,operator_id), KEY idx_operation_target(target_type,target_id), KEY idx_operation_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
