use xmate;
INSERT INTO permissions (id, permission_key) VALUES
(1, 'USER_VIEW'),
(2, 'USER_STATUS_UPDATE'),
(3, 'USER_EDIT'),
(4, 'USER_DELETE'),
(5, 'USER_CREATE'),
(6, 'SUPPLIER_VIEW'),
(7, 'SUPPLIER_EDIT'),
(8, 'SUPPLIER_DELETE'),
(9, 'SUPPLIER_CREATE'),
(10, 'ROLE_MGMT_VIEW'),
(11, 'ROLE_MGMT_EDIT'),
(12, 'ROLE_MGMT_DELETE'),
(13, 'ROLE_MGMT_CREATE'),
(14, 'PRODUCT_VIEW_DETAIL'),
(15, 'PRODUCT_VIEW'),
(16, 'PRODUCT_VARIANT_VIEW'),
(17, 'PRODUCT_VARIANT_MANAGE'),
(18, 'PRODUCT_VARIANT_EDIT'),
(19, 'PRODUCT_VARIANT_DELETE'),
(20, 'PRODUCT_VARIANT_CREATE'),
(21, 'PRODUCT_MEDIA_VIEW'),
(22, 'PRODUCT_MEDIA_PRIMARY'),
(23, 'PRODUCT_MEDIA_MANAGE'),
(24, 'PRODUCT_MEDIA_EDIT'),
(25, 'PRODUCT_MEDIA_DELETE'),
(26, 'PRODUCT_MEDIA_CREATE'),
(27, 'PRODUCT_EDIT'),
(28, 'PRODUCT_DELETE'),
(29, 'PRODUCT_CREATE'),
(30, 'PO_VIEW'),
(31, 'PO_STATUS_UPDATE'),
(32, 'PO_EDIT'),
(33, 'PO_DELETE'),
(34, 'PO_CREATE'),
(35, 'PERM_MGMT_VIEW'),
(36, 'PERM_MGMT_EDIT'),
(37, 'PERM_MGMT_DELETE'),
(38, 'PERM_MGMT_CREATE'),
(39, 'ORDER_VIEW'),
(40, 'ORDER_EDIT'),
(41, 'ORDER_DELETE'),
(42, 'ORDER_CREATE'),
(43, 'MEMBERSHIP_TIER_VIEW'),
(44, 'MEMBERSHIP_TIER_EDIT'),
(45, 'MEMBERSHIP_TIER_DELETE'),
(46, 'MEMBERSHIP_TIER_CREATE'),
(47, 'INVENTORY_VIEW'),
(48, 'INVENTORY_ADJUST'),
(49, 'DISCOUNT_VIEW'),
(50, 'DISCOUNT_EDIT'),
(51, 'DISCOUNT_DELETE'),
(52, 'DISCOUNT_CREATE'),
(53, 'CUSTOMER_VIEW'),
(54, 'CUSTOMER_EDIT'),
(55, 'CUSTOMER_DELETE'),
(56, 'CUSTOMER_CREATE'),
(57, 'CATEGORY_VIEW'),
(58, 'CATEGORY_EDIT'),
(59, 'CATEGORY_DELETE'),
(60, 'CATEGORY_CREATE');

INSERT INTO roles (description, name) VALUES
('Quản trị hệ thống', 'Admin'),
('Quản lý', 'Manager'),
('Chăm sóc khách hàng', 'CS'),
('Quản lý kho', 'Kho'),
('Quản lý nội dung', 'Content');

-- Gán quyền cho Admin
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions;

-- Tạo user test
INSERT INTO users (username, password, full_name, email, sdt, luong, is_active, created_at)
VALUES ('admin', '$2a$10$HdXRr1fSD8xTN4XOiavXyeZoMdIXcJhFSfraj27K/m//2v4C8meaG', 'Nguyen Van A', 'admin@example.com', '0909123456', 15000000, 1, now());
INSERT INTO users (username, password, full_name, email, sdt, luong, is_active, created_at)
VALUES ('admin1', '$2a$10$HdXRr1fSD8xTN4XOiavXyeZoMdIXcJhFSfraj27K/m//2v4C8meaG', 'Nguyen Van A', 'admin@gmail.com', '0909123446', 15000000, 1, now());

-- Gán role Admin cho user
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);