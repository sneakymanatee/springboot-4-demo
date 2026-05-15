-- V2__seed_initial_products.sql
-- Insert initial products with default prices

-- Insert laptops category
INSERT INTO products (sku, name, description, base_price)
VALUES
    ('LAPTOP-001', 'Premium Pro Laptop', 'High-performance laptop for professionals', 1299.99),
    ('LAPTOP-002', 'Student Budget Laptop', 'Affordable laptop for everyday use', 499.99),
    ('LAPTOP-003', 'Gaming Beast Laptop', 'Ultimate gaming laptop with RTX', 1799.99)
ON CONFLICT (sku) DO NOTHING;

-- Insert phones category
INSERT INTO products (sku, name, description, base_price)
VALUES
    ('PHONE-001', 'Flagship Smartphone', 'Latest flagship with all features', 999.99),
    ('PHONE-002', 'Mid-Range Phone', 'Great balance of price and performance', 599.99),
    ('PHONE-003', 'Budget Phone', 'Essential smartphone', 299.99)
ON CONFLICT (sku) DO NOTHING;

-- Insert monitors category
INSERT INTO products (sku, name, description, base_price)
VALUES
    ('MONITOR-001', '4K Ultra Monitor', '4K resolution for professionals', 499.99),
    ('MONITOR-002', '27 inch FHD Monitor', 'Standard Full HD display', 249.99),
    ('MONITOR-003', 'Gaming 144Hz Monitor', 'High refresh rate for gaming', 349.99)
ON CONFLICT (sku) DO NOTHING;

-- Insert initial prices for each product (base prices)
INSERT INTO prices (product_id, amount, currency, source, valid_from)
SELECT id, base_price, 'USD', 'SEED', CURRENT_TIMESTAMP
FROM products p
WHERE NOT EXISTS (
    SELECT 1
    FROM prices pr
    WHERE pr.product_id = p.id
      AND pr.source = 'SEED'
);

