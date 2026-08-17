ALTER TABLE t_order ADD CONSTRAINT fk_order_member FOREIGN KEY(member_id) REFERENCES t_member(id);
ALTER TABLE t_order_item ADD CONSTRAINT fk_order_item_order FOREIGN KEY(order_id) REFERENCES t_order(id), ADD CONSTRAINT fk_order_item_product FOREIGN KEY(product_id) REFERENCES t_card_product(id);
ALTER TABLE t_membership_card ADD CONSTRAINT fk_card_member FOREIGN KEY(member_id) REFERENCES t_member(id), ADD CONSTRAINT fk_card_product FOREIGN KEY(product_id) REFERENCES t_card_product(id), ADD CONSTRAINT fk_card_order_item FOREIGN KEY(order_item_id) REFERENCES t_order_item(id);
ALTER TABLE t_verification ADD CONSTRAINT fk_verification_card FOREIGN KEY(card_id) REFERENCES t_membership_card(id), ADD CONSTRAINT fk_verification_member FOREIGN KEY(member_id) REFERENCES t_member(id), ADD CONSTRAINT fk_verification_operator FOREIGN KEY(operator_member_id) REFERENCES t_member(id);
ALTER TABLE t_notification ADD CONSTRAINT fk_notification_member FOREIGN KEY(member_id) REFERENCES t_member(id);
ALTER TABLE t_consumed_member_code ADD CONSTRAINT fk_consumed_member FOREIGN KEY(member_id) REFERENCES t_member(id);
ALTER TABLE t_order ADD KEY idx_order_paid_report(pay_status,paid_at,pay_method);
ALTER TABLE t_verification ADD KEY idx_verification_report(created_at,benefit_id);
