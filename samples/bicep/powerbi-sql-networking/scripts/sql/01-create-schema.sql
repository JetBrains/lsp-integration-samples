/*
    01-create-schema.sql
    Power BI + SQL Networking Demo – Contoso Retail Dataset
    
    Creates the core schema: Customers, Products, Orders, OrderItems.
    Idempotent: drops and recreates tables in dependency order.
*/

-- Drop tables in reverse dependency order
IF OBJECT_ID('dbo.OrderItems', 'U') IS NOT NULL DROP TABLE dbo.OrderItems;
IF OBJECT_ID('dbo.Orders',     'U') IS NOT NULL DROP TABLE dbo.Orders;
IF OBJECT_ID('dbo.Products',   'U') IS NOT NULL DROP TABLE dbo.Products;
IF OBJECT_ID('dbo.Customers',  'U') IS NOT NULL DROP TABLE dbo.Customers;
GO

-- Customers
CREATE TABLE dbo.Customers (
    CustomerID  INT            IDENTITY(1,1) PRIMARY KEY,
    FirstName   NVARCHAR(50)   NOT NULL,
    LastName    NVARCHAR(50)   NOT NULL,
    Email       NVARCHAR(100)  NOT NULL,
    City        NVARCHAR(50)   NOT NULL,
    State       NVARCHAR(50)   NOT NULL,
    Country     NVARCHAR(50)   NOT NULL DEFAULT N'United States',
    CreatedDate DATE           NOT NULL DEFAULT GETDATE()
);
GO

-- Products
CREATE TABLE dbo.Products (
    ProductID   INT            IDENTITY(1,1) PRIMARY KEY,
    ProductName NVARCHAR(100)  NOT NULL,
    Category    NVARCHAR(50)   NOT NULL,
    UnitPrice   DECIMAL(10,2)  NOT NULL,
    InStock     BIT            NOT NULL DEFAULT 1
);
GO

-- Orders
CREATE TABLE dbo.Orders (
    OrderID     INT            IDENTITY(1,1) PRIMARY KEY,
    CustomerID  INT            NOT NULL
        CONSTRAINT FK_Orders_Customers FOREIGN KEY REFERENCES dbo.Customers(CustomerID),
    OrderDate   DATE           NOT NULL,
    TotalAmount DECIMAL(10,2)  NOT NULL DEFAULT 0,
    Status      NVARCHAR(20)   NOT NULL DEFAULT N'Completed'
);
GO

-- Order line items (LineTotal is a computed column)
CREATE TABLE dbo.OrderItems (
    OrderItemID INT            IDENTITY(1,1) PRIMARY KEY,
    OrderID     INT            NOT NULL
        CONSTRAINT FK_OrderItems_Orders FOREIGN KEY REFERENCES dbo.Orders(OrderID),
    ProductID   INT            NOT NULL
        CONSTRAINT FK_OrderItems_Products FOREIGN KEY REFERENCES dbo.Products(ProductID),
    Quantity    INT            NOT NULL,
    UnitPrice   DECIMAL(10,2)  NOT NULL,
    LineTotal   AS (Quantity * UnitPrice)
);
GO

PRINT '>> Schema created successfully.';
GO
