# Portal Walkthrough: Power BI + Azure SQL Private Link

Step-by-step Azure Portal and Power BI Service instructions to manually build the entire demo environment.

---

## 1. Create Resource Group

**Portal path:** Azure Portal → **Create a resource** → search "Resource group" → **Create**

| Setting | Value |
|---------|-------|
| Subscription | *(your subscription)* |
| Resource group | `pbi-pl-demo-rg` |
| Region | `East US` (or your preferred region) |

Click **Review + create** → **Create**.

> 📸 **Screenshot hint:** Resource group overview page showing the empty group with name and region.

---

## 2. Create Virtual Network + Subnets

**Portal path:** Azure Portal → **Create a resource** → search "Virtual Network" → **Create**

### Basics tab

| Setting | Value |
|---------|-------|
| Resource group | `pbi-pl-demo-rg` |
| Name | `pbi-pl-demo-vnet` |
| Region | Same as resource group |

### IP Addresses tab

| Setting | Value |
|---------|-------|
| Address space | `10.0.0.0/16` |

Delete the auto-created `default` subnet, then add two subnets:

| Subnet name | Address range | Purpose |
|-------------|--------------|---------|
| `default` | `10.0.1.0/24` | Private Endpoint |
| `gateway` | `10.0.2.0/24` | VNet Data Gateway |

Click **Review + create** → **Create**.

> 📸 **Screenshot hint:** VNet overview showing address space and both subnets.

---

## 3. Create Azure SQL Server + Database (Public Access Disabled)

### 3a. Create SQL Server

**Portal path:** Azure Portal → **Create a resource** → search "SQL server" → **Create**

| Setting | Value |
|---------|-------|
| Resource group | `pbi-pl-demo-rg` |
| Server name | `pbi-pl-demo-sql` |
| Region | Same as VNet |
| Authentication | SQL authentication (or Entra ID — match your Power BI data source config) |
| Server admin login | `sqladmin` |
| Password | *(strong password)* |

### 3b. Create SQL Database

**Portal path:** Continue from SQL Server creation, or Azure Portal → **Create a resource** → search "SQL database" → **Create**

| Setting | Value |
|---------|-------|
| Resource group | `pbi-pl-demo-rg` |
| Database name | `ContosoRetail` |
| Server | `pbi-pl-demo-sql` |
| Compute + storage | Basic or S0 (sufficient for demo) |
| Backup storage redundancy | Locally-redundant (cost savings for demo) |

### 3c. Disable Public Network Access

**Portal path:** Azure Portal → SQL Server `pbi-pl-demo-sql` → **Networking** (under Security)

| Setting | Value |
|---------|-------|
| Public network access | **Disable** |
| Firewall rules | Remove all (should be empty) |
| Exceptions | Uncheck all |

Click **Save**.

> 📸 **Screenshot hint:** Networking blade showing "Disable" selected, empty firewall rules, no exceptions.

### 3d. Populate the Database

Connect from a VM inside the VNet (or temporarily enable public access) and run:

```sql
CREATE TABLE Customers (
    CustomerID INT PRIMARY KEY IDENTITY(1,1),
    CustomerName NVARCHAR(100),
    Email NVARCHAR(100),
    City NVARCHAR(50)
);

CREATE TABLE Products (
    ProductID INT PRIMARY KEY IDENTITY(1,1),
    ProductName NVARCHAR(100),
    Category NVARCHAR(50),
    UnitPrice DECIMAL(10,2)
);

CREATE TABLE Orders (
    OrderID INT PRIMARY KEY IDENTITY(1,1),
    CustomerID INT FOREIGN KEY REFERENCES Customers(CustomerID),
    OrderDate DATE,
    Status NVARCHAR(20)
);

CREATE TABLE OrderItems (
    OrderItemID INT PRIMARY KEY IDENTITY(1,1),
    OrderID INT FOREIGN KEY REFERENCES Orders(OrderID),
    ProductID INT FOREIGN KEY REFERENCES Products(ProductID),
    Quantity INT,
    UnitPrice DECIMAL(10,2)
);

-- Sample data
INSERT INTO Customers (CustomerName, Email, City) VALUES
    ('Contoso Ltd', 'orders@contoso.com', 'Seattle'),
    ('Fabrikam Inc', 'sales@fabrikam.com', 'Portland'),
    ('Adventure Works', 'info@adventureworks.com', 'San Francisco');

INSERT INTO Products (ProductName, Category, UnitPrice) VALUES
    ('Widget A', 'Hardware', 29.99),
    ('Widget B', 'Hardware', 49.99),
    ('Service Plan', 'Services', 199.99);

INSERT INTO Orders (CustomerID, OrderDate, Status) VALUES
    (1, '2024-01-15', 'Completed'),
    (2, '2024-01-20', 'Completed'),
    (3, '2024-02-01', 'Processing'),
    (1, '2024-02-10', 'Completed');

INSERT INTO OrderItems (OrderID, ProductID, Quantity, UnitPrice) VALUES
    (1, 1, 10, 29.99),
    (1, 3, 1, 199.99),
    (2, 2, 5, 49.99),
    (3, 1, 20, 29.99),
    (4, 2, 3, 49.99),
    (4, 3, 2, 199.99);
```

> 📸 **Screenshot hint:** Query editor (or SSMS) showing tables created and sample data inserted.

---

## 4. Create Private Endpoint for SQL

**Portal path:** Azure Portal → **Create a resource** → search "Private endpoint" → **Create**

### Basics tab

| Setting | Value |
|---------|-------|
| Resource group | `pbi-pl-demo-rg` |
| Name | `pbi-pl-demo-sql-pe` |
| Network Interface Name | `pbi-pl-demo-sql-pe-nic` |
| Region | Same as VNet |

### Resource tab

| Setting | Value |
|---------|-------|
| Connection method | "Connect to an Azure resource in my directory" |
| Resource type | `Microsoft.Sql/servers` |
| Resource | `pbi-pl-demo-sql` |
| Target sub-resource | `sqlServer` |

### Virtual Network tab

| Setting | Value |
|---------|-------|
| Virtual network | `pbi-pl-demo-vnet` |
| Subnet | `default` (10.0.1.0/24) |
| Private IP configuration | Dynamically allocate IP address |

### DNS tab

| Setting | Value |
|---------|-------|
| Integrate with private DNS zone | **Yes** |
| Private DNS Zone | `privatelink.database.windows.net` (create new if not exists) |

Click **Review + create** → **Create**.

> 📸 **Screenshot hint:** Private Endpoint overview showing "Approved" connection status and the private IP.

---

## 5. Create Private DNS Zone + Link to VNet

> **Note:** If you selected "Integrate with private DNS zone" in Step 4, the DNS zone and VNet link are created automatically. Verify them here; otherwise, create manually.

### 5a. Create Private DNS Zone (if not auto-created)

**Portal path:** Azure Portal → **Create a resource** → search "Private DNS zone" → **Create**

| Setting | Value |
|---------|-------|
| Resource group | `pbi-pl-demo-rg` |
| Name | `privatelink.database.windows.net` |

### 5b. Add VNet Link

**Portal path:** Private DNS Zone → **Virtual network links** → **+ Add**

| Setting | Value |
|---------|-------|
| Link name | `pbi-pl-demo-vnet-link` |
| Virtual network | `pbi-pl-demo-vnet` |
| Enable auto registration | No (not needed for PE records) |

Click **OK**.

### 5c. Verify A Record

**Portal path:** Private DNS Zone → **Recordsets**

Verify an A record exists:

| Name | Type | Value |
|------|------|-------|
| `pbi-pl-demo-sql` | A | `10.0.1.x` (the PE's private IP) |

> 📸 **Screenshot hint:** DNS zone recordsets showing the A record mapping to the private IP.

---

## 6. Verify DNS Resolution

From a VM inside the VNet, or using Azure Cloud Shell connected to the VNet:

```powershell
Resolve-DnsName pbi-pl-demo-sql.database.windows.net
```

**Expected output:**
```
Name       : pbi-pl-demo-sql.privatelink.database.windows.net
Type       : A
TTL        : 10
IP4Address : 10.0.1.4
```

The key indicator: the resolved IP is in the `10.0.1.0/24` range (your PE subnet), **not** a public IP.

From outside the VNet (for contrast):
```powershell
Resolve-DnsName pbi-pl-demo-sql.database.windows.net
```
Will resolve to a public IP — which confirms that only VNet-linked clients get the private resolution.

> 📸 **Screenshot hint:** Side-by-side terminal output showing private vs public DNS resolution.

---

## 7. Create VNet Data Gateway in Power BI Service

**Portal path:** Power BI Service (app.powerbi.com) → **Settings** (⚙️) → **Manage connections and gateways**

### 7a. Register the VNet Data Gateway

1. Click the **Virtual network data gateways** tab.
2. Click **+ New**.
3. Fill in:

| Setting | Value |
|---------|-------|
| Gateway name | `pbi-pl-demo-vnetgw` |
| Subscription | *(your subscription)* |
| Resource group | `pbi-pl-demo-rg` |
| VNet | `pbi-pl-demo-vnet` |
| Subnet | `gateway` (10.0.2.0/24) |

4. Click **Create**.
5. Wait for status to change to **Online** (may take 1–3 minutes).

> **Prerequisites:** The workspace must be on Premium or PPU capacity. The user must have Contributor or higher on the Azure subscription.

> 📸 **Screenshot hint:** Gateway list showing `pbi-pl-demo-vnetgw` with green "Online" status.

---

## 8. Configure Data Source on Gateway

**Portal path:** Power BI Service → **Settings** → **Manage connections and gateways** → **Connections** tab

1. Click **+ New**.
2. Fill in:

| Setting | Value |
|---------|-------|
| Gateway cluster name | `pbi-pl-demo-vnetgw` |
| Connection name | `ContosoRetail-SQL` |
| Connection type | SQL Server |
| Server | `pbi-pl-demo-sql.database.windows.net` |
| Database | `ContosoRetail` |
| Authentication method | Basic (SQL auth) or OAuth2 (Entra ID) |
| Username | `sqladmin` (if SQL auth) |
| Password | *(your password)* |
| Privacy level | Organizational |

3. Click **Create**.
4. Verify the connection shows a green checkmark (successful test).

> 📸 **Screenshot hint:** Data source configuration with successful connection test.

---

## 9. Publish Power BI Report + Configure Refresh

### 9a. Publish from Power BI Desktop

1. Open Power BI Desktop.
2. **Get Data** → **SQL Server**.
3. Server: `pbi-pl-demo-sql.database.windows.net`, Database: `ContosoRetail`.
4. Import the four tables (Customers, Products, Orders, OrderItems).
5. Create a simple report (e.g., sales by customer, orders over time).
6. **File** → **Publish** → select your Premium/PPU workspace.

### 9b. Bind Dataset to Gateway

**Portal path:** Power BI Service → Workspace → Dataset `ContosoRetail` → **Settings** → **Gateway and cloud connections**

1. Under "Gateway connection", select `pbi-pl-demo-vnetgw`.
2. Map the data source to `ContosoRetail-SQL` (the connection created in Step 8).
3. Click **Apply**.

### 9c. Configure Scheduled Refresh (Optional)

**Portal path:** Dataset Settings → **Scheduled refresh**

| Setting | Value |
|---------|-------|
| Keep your data up to date | On |
| Refresh frequency | Daily |
| Time | *(pick a time)* |

Click **Apply**.

> 📸 **Screenshot hint:** Gateway connection settings showing the dataset mapped to the VNet Data Gateway.

---

## 10. Verify Refresh Succeeds

### 10a. Trigger Manual Refresh

**Portal path:** Power BI Service → Workspace → Dataset `ContosoRetail` → **⟳ Refresh now**

### 10b. Check Refresh History

**Portal path:** Dataset → **Refresh history**

| Field | Expected Value |
|-------|---------------|
| Status | **Completed** |
| Type | On Demand |
| Duration | ~10–30 seconds (for small dataset) |

### 10c. Open the Report

Navigate to the published report. Verify visuals display current data.

> 📸 **Screenshot hint:** Refresh history showing "Completed" status, and the report with live data from ContosoRetail.
