ALTER TABLE t_verification
  ADD COLUMN member_name_snapshot VARCHAR(64) NULL AFTER quantity,
  ADD COLUMN member_phone_snapshot VARCHAR(20) NULL AFTER member_name_snapshot,
  ADD COLUMN product_name_snapshot VARCHAR(100) NULL AFTER member_phone_snapshot,
  ADD COLUMN operator_phone_snapshot VARCHAR(20) NULL AFTER operator_name;
