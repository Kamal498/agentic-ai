-- Seed Inventory
INSERT INTO inventory (product_id, product_name, category, quantity_available, reorder_threshold, unit_price, description, supplier, last_updated) VALUES
('PROD-001', 'MacBook Pro 14"', 'Laptops', 25, 5, 2499.00, 'Apple MacBook Pro 14-inch M3 Pro chip, 18GB RAM, 512GB SSD - professional grade laptop for developers', 'Apple Inc.', CURRENT_TIMESTAMP),
('PROD-002', 'Dell XPS 15', 'Laptops', 18, 5, 1899.00, 'Dell XPS 15 OLED display, Intel Core i7, 16GB RAM, 512GB SSD - premium laptop for creative professionals', 'Dell Technologies', CURRENT_TIMESTAMP),
('PROD-003', 'Logitech MX Master 3S', 'Peripherals', 120, 20, 99.00, 'Ergonomic wireless mouse with MagSpeed scrolling, Bluetooth and USB receiver, programmable buttons', 'Logitech', CURRENT_TIMESTAMP),
('PROD-004', 'Samsung 4K Monitor 32"', 'Monitors', 40, 8, 699.00, '32-inch 4K UHD IPS monitor with USB-C connectivity, HDR support, excellent color accuracy for design work', 'Samsung Electronics', CURRENT_TIMESTAMP),
('PROD-005', 'Mechanical Keyboard', 'Peripherals', 3, 10, 149.00, 'Compact 75% mechanical keyboard with Cherry MX switches, RGB backlight, programmable macros', 'Keychron', CURRENT_TIMESTAMP),
('PROD-006', 'USB-C Hub 10-in-1', 'Accessories', 200, 30, 59.00, '10-in-1 USB-C hub with HDMI, Ethernet, SD card reader, 4 USB ports, 100W power delivery', 'Anker', CURRENT_TIMESTAMP),
('PROD-007', 'Webcam 4K', 'Peripherals', 55, 10, 199.00, '4K ultra HD webcam with autofocus, built-in noise-canceling microphone, works with Zoom and Teams', 'Logitech', CURRENT_TIMESTAMP),
('PROD-008', 'Ergonomic Office Chair', 'Furniture', 12, 3, 549.00, 'Mesh back ergonomic chair with adjustable lumbar support, armrests, and seat height - ideal for home office', 'Herman Miller', CURRENT_TIMESTAMP),
('PROD-009', 'NVMe SSD 1TB', 'Storage', 90, 15, 119.00, 'PCIe Gen 4 NVMe M.2 SSD, 7000MB/s read speed, 5-year warranty - performance storage upgrade', 'Samsung', CURRENT_TIMESTAMP),
('PROD-010', 'Noise-Cancelling Headphones', 'Audio', 65, 10, 349.00, 'Over-ear wireless headphones with active noise cancellation, 30-hour battery life, premium sound quality', 'Sony', CURRENT_TIMESTAMP);

-- Seed Orders (orders table)
INSERT INTO orders (order_number, customer_id, customer_name, customer_email, status, order_date, total_amount, description, shipping_address) VALUES
('ORD-A1B2C3D4', 'CUST-001', 'Alice Johnson', 'alice@techcorp.com', 'DELIVERED', '2024-11-15 10:30:00', 5047.00, 'Office equipment upgrade for development team - laptops and peripherals for new joiners', '123 Main St, San Francisco, CA 94102'),
('ORD-E5F6G7H8', 'CUST-002', 'Bob Smith', 'bob@startupxyz.com', 'SHIPPED', '2024-11-20 14:15:00', 2648.00, 'Home office setup - monitor and ergonomic accessories for remote work', '456 Oak Ave, Austin, TX 78701'),
('ORD-I9J0K1L2', 'CUST-001', 'Alice Johnson', 'alice@techcorp.com', 'DELIVERED', '2024-10-05 09:00:00', 1199.00, 'Storage upgrades for development workstations - NVMe SSDs bulk purchase', '123 Main St, San Francisco, CA 94102'),
('ORD-M3N4O5P6', 'CUST-003', 'Carol White', 'carol@designstudio.com', 'PROCESSING', '2024-11-28 11:45:00', 3846.00, 'Creative workstation setup - MacBook and 4K monitor for graphic design work', '789 Pine Rd, New York, NY 10001'),
('ORD-Q7R8S9T0', 'CUST-004', 'David Lee', 'david@enterprise.com', 'CONFIRMED', '2024-11-30 16:00:00', 996.00, 'Conference room audio-visual equipment - webcams and headphones for video meetings', '321 Elm St, Chicago, IL 60601'),
('ORD-U1V2W3X4', 'CUST-002', 'Bob Smith', 'bob@startupxyz.com', 'PENDING', '2024-12-01 08:30:00', 1498.00, 'Laptop replacement for damaged unit - urgent order for developer workstation', '456 Oak Ave, Austin, TX 78701'),
('ORD-Y5Z6A7B8', 'CUST-005', 'Eve Martinez', 'eve@consulting.com', 'DELIVERED', '2024-10-20 13:00:00', 748.00, 'Accessories bundle for consulting team - USB hubs and keyboards for field work', '654 Maple Ln, Seattle, WA 98101'),
('ORD-C9D0E1F2', 'CUST-003', 'Carol White', 'carol@designstudio.com', 'CANCELLED', '2024-11-10 10:00:00', 2498.00, 'Duplicate order cancellation - monitor purchase cancelled in favor of a different model', '789 Pine Rd, New York, NY 10001'),
('ORD-G3H4I5J6', 'CUST-006', 'Frank Zhang', 'frank@datacenter.com', 'DELIVERED', '2024-09-15 09:30:00', 4748.00, 'Data center peripheral refresh - storage and networking equipment for server room', '987 Corporate Blvd, Seattle, WA 98102'),
('ORD-K7L8M9N0', 'CUST-004', 'David Lee', 'david@enterprise.com', 'SHIPPED', '2024-11-25 15:30:00', 2199.00, 'Enterprise laptop deployment for sales team - standardized hardware rollout', '321 Elm St, Chicago, IL 60601');

-- Seed Order Items
INSERT INTO order_items (product_id, product_name, quantity, unit_price, total_price, order_id) VALUES
('PROD-001', 'MacBook Pro 14"', 2, 2499.00, 4998.00, 1),
('PROD-003', 'Logitech MX Master 3S', 2, 99.00, 198.00, 1),
('PROD-004', 'Samsung 4K Monitor 32"', 1, 699.00, 699.00, 2),
('PROD-008', 'Ergonomic Office Chair', 1, 549.00, 549.00, 2),
('PROD-003', 'Logitech MX Master 3S', 2, 99.00, 198.00, 2),
('PROD-006', 'USB-C Hub 10-in-1', 2, 59.00, 118.00, 2),
('PROD-009', 'NVMe SSD 1TB', 8, 119.00, 952.00, 3),
('PROD-006', 'USB-C Hub 10-in-1', 4, 59.00, 236.00, 3),
('PROD-001', 'MacBook Pro 14"', 1, 2499.00, 2499.00, 4),
('PROD-004', 'Samsung 4K Monitor 32"', 1, 699.00, 699.00, 4),
('PROD-005', 'Mechanical Keyboard', 1, 149.00, 149.00, 4),
('PROD-007', 'Webcam 4K', 3, 199.00, 597.00, 5),
('PROD-010', 'Noise-Cancelling Headphones', 2, 349.00, 698.00, 5),
('PROD-006', 'USB-C Hub 10-in-1', 1, 59.00, 59.00, 5),
('PROD-003', 'Logitech MX Master 3S', 1, 99.00, 99.00, 5),
('PROD-002', 'Dell XPS 15', 1, 1899.00, 1899.00, 6),
('PROD-006', 'USB-C Hub 10-in-1', 2, 59.00, 118.00, 6),
('PROD-006', 'USB-C Hub 10-in-1', 8, 59.00, 472.00, 7),
('PROD-005', 'Mechanical Keyboard', 2, 149.00, 298.00, 7),
('PROD-002', 'Dell XPS 15', 1, 1899.00, 1899.00, 8),
('PROD-003', 'Logitech MX Master 3S', 2, 99.00, 198.00, 8),
('PROD-009', 'NVMe SSD 1TB', 20, 119.00, 2380.00, 9),
('PROD-007', 'Webcam 4K', 8, 199.00, 1592.00, 9),
('PROD-003', 'Logitech MX Master 3S', 8, 99.00, 792.00, 9),
('PROD-002', 'Dell XPS 15', 1, 1899.00, 1899.00, 10),
('PROD-003', 'Logitech MX Master 3S', 1, 99.00, 99.00, 10),
('PROD-007', 'Webcam 4K', 1, 199.00, 199.00, 10);

-- Seed Payments
INSERT INTO payments (payment_id, order_id, customer_id, status, amount, payment_method, payment_date, transaction_id, notes) VALUES
('PAY-AA001', 1, 'CUST-001', 'COMPLETED', 5047.00, 'CREDIT_CARD', '2024-11-15 10:35:00', 'TXN-001AABBCC1122', 'Visa card ending 4242'),
('PAY-BB002', 2, 'CUST-002', 'COMPLETED', 2648.00, 'BANK_TRANSFER', '2024-11-20 14:20:00', 'TXN-002BBCCDD2233', 'Wire transfer from Chase'),
('PAY-CC003', 3, 'CUST-001', 'COMPLETED', 1199.00, 'CREDIT_CARD', '2024-10-05 09:05:00', 'TXN-003CCDDE E3344', 'Amex corporate card'),
('PAY-DD004', 4, 'CUST-003', 'PENDING', 3846.00, 'INVOICE', '2024-11-28 11:50:00', 'TXN-004DDEEFF4455', 'Net-30 invoice payment'),
('PAY-EE005', 5, 'CUST-004', 'PENDING', 996.00, 'PURCHASE_ORDER', '2024-11-30 16:05:00', 'TXN-005EEFFGG5566', 'PO #ENT-2024-1130'),
('PAY-FF006', 7, 'CUST-005', 'COMPLETED', 748.00, 'PAYPAL', '2024-10-20 13:05:00', 'TXN-006FFGGHH6677', 'PayPal business account'),
('PAY-GG007', 9, 'CUST-006', 'COMPLETED', 4748.00, 'BANK_TRANSFER', '2024-09-15 09:35:00', 'TXN-007GGHHI I7788', 'ACH transfer approved'),
('PAY-HH008', 8, 'CUST-003', 'REFUNDED', 2498.00, 'CREDIT_CARD', '2024-11-10 10:05:00', 'TXN-008HHIIJJ8899', 'Full refund processed for cancelled order');
