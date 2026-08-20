CREATE TABLE t_member_wechat_identity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  member_id BIGINT NOT NULL,
  provider VARCHAR(24) NOT NULL,
  app_id VARCHAR(32) NOT NULL,
  openid VARCHAR(64) NOT NULL,
  unionid VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_wechat_identity(provider,app_id,openid),
  KEY idx_wechat_identity_union(unionid),
  KEY idx_wechat_identity_member(member_id),
  CONSTRAINT fk_wechat_identity_member FOREIGN KEY(member_id) REFERENCES t_member(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE t_order ADD COLUMN payment_appid VARCHAR(32) NULL AFTER transaction_id;
