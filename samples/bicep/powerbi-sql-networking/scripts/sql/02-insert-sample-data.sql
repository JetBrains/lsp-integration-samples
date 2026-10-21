/*
    02-insert-sample-data.sql
    Power BI + SQL Networking Demo – Contoso Retail Dataset

    Inserts sample data: 15 customers, 10 products, 25 orders, 50 order items.
    Idempotent: truncates all tables then reinserts with explicit IDs.
    
    Data spans the last ~6 months with varied regions and categories
    to produce meaningful Power BI visuals.
*/

-- Clear data in dependency order
TRUNCATE TABLE dbo.OrderItems;
DELETE FROM dbo.Orders;   -- Cannot TRUNCATE with FK references
DBCC CHECKIDENT ('dbo.Orders', RESEED, 0);
DELETE FROM dbo.Products;
DBCC CHECKIDENT ('dbo.Products', RESEED, 0);
DELETE FROM dbo.Customers;
DBCC CHECKIDENT ('dbo.Customers', RESEED, 0);
GO

-- ============================================================
-- Customers (15 rows – spread across US regions)
-- ============================================================
SET IDENTITY_INSERT dbo.Customers ON;

INSERT INTO dbo.Customers (CustomerID, FirstName, LastName, Email, City, State, Country, CreatedDate) VALUES
( 1, N'Alice',    N'Johnson',   N'alice.johnson@contoso.com',    N'Seattle',       N'WA', N'United States', '2024-01-15'),
( 2, N'Bob',      N'Smith',     N'bob.smith@contoso.com',        N'Portland',      N'OR', N'United States', '2024-02-10'),
( 3, N'Carol',    N'Williams',  N'carol.williams@contoso.com',   N'San Francisco', N'CA', N'United States', '2024-03-05'),
( 4, N'David',    N'Brown',     N'david.brown@contoso.com',      N'Los Angeles',   N'CA', N'United States', '2024-01-22'),
( 5, N'Eva',      N'Davis',     N'eva.davis@contoso.com',        N'Denver',        N'CO', N'United States', '2024-04-01'),
( 6, N'Frank',    N'Garcia',    N'frank.garcia@contoso.com',     N'Austin',        N'TX', N'United States', '2024-02-18'),
( 7, N'Grace',    N'Martinez',  N'grace.martinez@contoso.com',   N'Dallas',        N'TX', N'United States', '2024-03-12'),
( 8, N'Henry',    N'Lopez',     N'henry.lopez@contoso.com',      N'Chicago',       N'IL', N'United States', '2024-05-08'),
( 9, N'Irene',    N'Wilson',    N'irene.wilson@contoso.com',     N'Miami',         N'FL', N'United States', '2024-01-30'),
(10, N'Jack',     N'Anderson',  N'jack.anderson@contoso.com',    N'Atlanta',       N'GA', N'United States', '2024-04-14'),
(11, N'Karen',    N'Thomas',    N'karen.thomas@contoso.com',     N'New York',      N'NY', N'United States', '2024-06-01'),
(12, N'Leo',      N'Taylor',    N'leo.taylor@contoso.com',       N'Boston',        N'MA', N'United States', '2024-05-20'),
(13, N'Maria',    N'Moore',     N'maria.moore@contoso.com',      N'Phoenix',       N'AZ', N'United States', '2024-03-28'),
(14, N'Nathan',   N'Jackson',   N'nathan.jackson@contoso.com',   N'Minneapolis',   N'MN', N'United States', '2024-06-10'),
(15, N'Olivia',   N'White',     N'olivia.white@contoso.com',     N'Raleigh',       N'NC', N'United States', '2024-02-25');

SET IDENTITY_INSERT dbo.Customers OFF;
GO

-- ============================================================
-- Products (10 rows – 4 categories)
-- ============================================================
SET IDENTITY_INSERT dbo.Products ON;

INSERT INTO dbo.Products (ProductID, ProductName, Category, UnitPrice, InStock) VALUES
( 1, N'Wireless Mouse',         N'Electronics',     29.99,  1),
( 2, N'USB-C Hub',              N'Electronics',     49.99,  1),
( 3, N'Bluetooth Headphones',   N'Electronics',     89.99,  1),
( 4, N'Running Shoes',          N'Sports',          74.99,  1),
( 5, N'Yoga Mat',               N'Sports',          24.99,  1),
( 6, N'Cotton T-Shirt',         N'Clothing',        19.99,  1),
( 7, N'Denim Jacket',           N'Clothing',        59.99,  1),
( 8, N'Garden Tool Set',        N'Home & Garden',   34.99,  1),
( 9, N'LED Desk Lamp',          N'Home & Garden',   44.99,  1),
(10, N'Water Bottle',           N'Sports',          14.99,  0);

SET IDENTITY_INSERT dbo.Products OFF;
GO

-- ============================================================
-- Orders (25 rows – spread over ~6 months, various statuses)
-- ============================================================
SET IDENTITY_INSERT dbo.Orders ON;

INSERT INTO dbo.Orders (OrderID, CustomerID, OrderDate, TotalAmount, Status) VALUES
( 1,  1, '2024-07-05',  129.97, N'Completed'),
( 2,  2, '2024-07-12',   49.99, N'Completed'),
( 3,  3, '2024-07-20',  179.97, N'Completed'),
( 4,  4, '2024-08-02',   74.99, N'Completed'),
( 5,  5, '2024-08-10',   59.97, N'Completed'),
( 6,  6, '2024-08-18',  139.98, N'Completed'),
( 7,  7, '2024-08-25',   89.99, N'Completed'),
( 8,  8, '2024-09-03',  154.97, N'Completed'),
( 9,  9, '2024-09-11',   44.99, N'Completed'),
(10, 10, '2024-09-19',  199.96, N'Completed'),
(11, 11, '2024-09-28',   69.98, N'Completed'),
(12, 12, '2024-10-05',  109.98, N'Completed'),
(13, 13, '2024-10-14',   34.99, N'Completed'),
(14, 14, '2024-10-22',  164.97, N'Completed'),
(15, 15, '2024-10-30',   49.98, N'Completed'),
(16,  1, '2024-11-05',   89.99, N'Completed'),
(17,  3, '2024-11-12',  119.98, N'Completed'),
(18,  5, '2024-11-20',   74.99, N'Completed'),
(19,  7, '2024-11-28',   54.98, N'Shipped'),
(20,  9, '2024-12-03',  149.98, N'Shipped'),
(21, 11, '2024-12-08',   29.99, N'Shipped'),
(22,  2, '2024-12-12',  104.98, N'Processing'),
(23,  4, '2024-12-15',   44.99, N'Processing'),
(24,  6, '2024-12-18',  174.97, N'Pending'),
(25,  8, '2024-12-20',   59.98, N'Pending');

SET IDENTITY_INSERT dbo.Orders OFF;
GO

-- ============================================================
-- OrderItems (50 rows – 2 items per order on average)
-- ============================================================
SET IDENTITY_INSERT dbo.OrderItems ON;

INSERT INTO dbo.OrderItems (OrderItemID, OrderID, ProductID, Quantity, UnitPrice) VALUES
-- Order 1: Alice – Electronics
( 1,  1,  1, 1, 29.99),
( 2,  1,  2, 2, 49.99),
-- Order 2: Bob – Electronics
( 3,  2,  2, 1, 49.99),
-- Order 3: Carol – Electronics + Clothing
( 4,  3,  3, 1, 89.99),
( 5,  3,  7, 1, 59.99),
( 6,  3,  1, 1, 29.99),
-- Order 4: David – Sports
( 7,  4,  4, 1, 74.99),
-- Order 5: Eva – Sports + Yoga
( 8,  5,  5, 1, 24.99),
( 9,  5,  8, 1, 34.99),
-- Order 6: Frank – Clothing + Home
(10,  6,  7, 1, 59.99),
(11,  6,  6, 4, 19.99),
-- Order 7: Grace – Electronics
(12,  7,  3, 1, 89.99),
-- Order 8: Henry – Mixed
(13,  8,  9, 2, 44.99),
(14,  8,  5, 1, 24.99),
(15,  8,  6, 2, 19.99),
-- Order 9: Irene – Home
(16,  9,  9, 1, 44.99),
-- Order 10: Jack – Electronics + Sports
(17, 10,  3, 1, 89.99),
(18, 10,  4, 1, 74.99),
(19, 10,  8, 1, 34.99),
-- Order 11: Karen – Clothing
(20, 11,  6, 2, 19.99),
(21, 11,  1, 1, 29.99),
-- Order 12: Leo – Electronics
(22, 12,  2, 1, 49.99),
(23, 12,  7, 1, 59.99),
-- Order 13: Maria – Home
(24, 13,  8, 1, 34.99),
-- Order 14: Nathan – Mixed
(25, 14,  3, 1, 89.99),
(26, 14,  4, 1, 74.99),
-- Order 15: Olivia – Sports
(27, 15,  5, 2, 24.99),
-- Order 16: Alice – Electronics (repeat customer)
(28, 16,  3, 1, 89.99),
-- Order 17: Carol – Clothing + Home
(29, 17,  7, 1, 59.99),
(30, 17,  9, 1, 44.99),
(31, 17,  6, 1, 19.99),
-- Order 18: Eva – Sports
(32, 18,  4, 1, 74.99),
-- Order 19: Grace – Clothing
(33, 19,  6, 1, 19.99),
(34, 19,  8, 1, 34.99),
-- Order 20: Irene – Electronics + Home
(35, 20,  2, 1, 49.99),
(36, 20,  3, 1, 89.99),
(37, 20, 10, 1, 14.99),
-- Order 21: Karen – Electronics
(38, 21,  1, 1, 29.99),
-- Order 22: Bob – Mixed (repeat)
(39, 22,  4, 1, 74.99),
(40, 22,  1, 1, 29.99),
-- Order 23: David – Home
(41, 23,  9, 1, 44.99),
-- Order 24: Frank – Mixed
(42, 24,  3, 1, 89.99),
(43, 24,  6, 3, 19.99),
(44, 24,  5, 1, 24.99),
-- Order 25: Henry – Clothing + Sports
(45, 25,  7, 1, 59.99),
(46, 25,  10, 2, 14.99),
-- Extra items to reach 50
(47,  1,  6, 1, 19.99),
(48,  3,  5, 1, 24.99),
(49, 10,  6, 1, 19.99),
(50, 14,  9, 1, 44.99);

SET IDENTITY_INSERT dbo.OrderItems OFF;
GO

-- ============================================================
-- Recalculate Orders.TotalAmount from actual OrderItems
-- ============================================================
UPDATE o
SET o.TotalAmount = sub.Total
FROM dbo.Orders o
INNER JOIN (
    SELECT OrderID, SUM(Quantity * UnitPrice) AS Total
    FROM dbo.OrderItems
    GROUP BY OrderID
) sub ON sub.OrderID = o.OrderID;
GO

PRINT '>> Sample data inserted successfully.';
GO
