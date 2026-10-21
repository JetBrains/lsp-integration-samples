/*
    03-verify-data.sql
    Power BI + SQL Networking Demo – Contoso Retail Dataset

    Verification queries to confirm data was loaded correctly
    and is suitable for Power BI reporting.
*/

-- ============================================================
-- 1. Row counts per table (expect: 15, 10, 25, 50)
-- ============================================================
SELECT 
    'Customers'  AS TableName, COUNT(*) AS RowCount FROM dbo.Customers  UNION ALL
SELECT 
    'Products',               COUNT(*)              FROM dbo.Products   UNION ALL
SELECT 
    'Orders',                 COUNT(*)              FROM dbo.Orders     UNION ALL
SELECT 
    'OrderItems',             COUNT(*)              FROM dbo.OrderItems;
GO

-- ============================================================
-- 2. Total revenue from order items
-- ============================================================
SELECT 
    SUM(LineTotal) AS TotalRevenue
FROM dbo.OrderItems;
GO

-- ============================================================
-- 3. Revenue by product category (for Power BI bar/pie chart)
-- ============================================================
SELECT 
    p.Category,
    COUNT(DISTINCT oi.OrderID)  AS OrderCount,
    SUM(oi.Quantity)            AS UnitsSold,
    SUM(oi.LineTotal)           AS Revenue
FROM dbo.OrderItems oi
JOIN dbo.Products p ON p.ProductID = oi.ProductID
GROUP BY p.Category
ORDER BY Revenue DESC;
GO

-- ============================================================
-- 4. Top 5 customers by total spend
-- ============================================================
SELECT TOP 5
    c.FirstName + N' ' + c.LastName AS CustomerName,
    c.City,
    c.State,
    COUNT(DISTINCT o.OrderID)       AS OrderCount,
    SUM(oi.LineTotal)               AS TotalSpend
FROM dbo.Customers c
JOIN dbo.Orders o     ON o.CustomerID = c.CustomerID
JOIN dbo.OrderItems oi ON oi.OrderID  = o.OrderID
GROUP BY c.CustomerID, c.FirstName, c.LastName, c.City, c.State
ORDER BY TotalSpend DESC;
GO

-- ============================================================
-- 5. Orders by month (for Power BI time-series visual)
-- ============================================================
SELECT 
    FORMAT(o.OrderDate, 'yyyy-MM')  AS OrderMonth,
    COUNT(DISTINCT o.OrderID)       AS OrderCount,
    SUM(oi.LineTotal)               AS MonthlyRevenue
FROM dbo.Orders o
JOIN dbo.OrderItems oi ON oi.OrderID = o.OrderID
GROUP BY FORMAT(o.OrderDate, 'yyyy-MM')
ORDER BY OrderMonth;
GO

-- ============================================================
-- 6. Data quality checks
-- ============================================================

-- 6a. Orphan orders – orders whose CustomerID has no matching customer
SELECT 'Orphan Orders' AS CheckName, COUNT(*) AS Issues
FROM dbo.Orders o
WHERE NOT EXISTS (SELECT 1 FROM dbo.Customers c WHERE c.CustomerID = o.CustomerID);

-- 6b. Orphan order items – items whose OrderID has no matching order
SELECT 'Orphan OrderItems (Order)' AS CheckName, COUNT(*) AS Issues
FROM dbo.OrderItems oi
WHERE NOT EXISTS (SELECT 1 FROM dbo.Orders o WHERE o.OrderID = oi.OrderID);

-- 6c. Orphan order items – items whose ProductID has no matching product
SELECT 'Orphan OrderItems (Product)' AS CheckName, COUNT(*) AS Issues
FROM dbo.OrderItems oi
WHERE NOT EXISTS (SELECT 1 FROM dbo.Products p WHERE p.ProductID = oi.ProductID);

-- 6d. Null checks on key columns
SELECT 'Null Customer Names' AS CheckName, COUNT(*) AS Issues
FROM dbo.Customers WHERE FirstName IS NULL OR LastName IS NULL;

SELECT 'Null Product Names' AS CheckName, COUNT(*) AS Issues
FROM dbo.Products WHERE ProductName IS NULL;

SELECT 'Null Order Dates' AS CheckName, COUNT(*) AS Issues
FROM dbo.Orders WHERE OrderDate IS NULL;
GO

PRINT '>> Verification complete. All checks with Issues = 0 indicate clean data.';
GO
