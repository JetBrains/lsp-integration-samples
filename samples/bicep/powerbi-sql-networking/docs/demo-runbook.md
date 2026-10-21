# Demo Runbook: Power BI + Azure SQL Private Link

**Duration:** 10–15 minutes (including Q&A buffer)

---

## Talk Track (60–90 seconds)

> "Today I'm going to show you how Power BI can securely refresh data from an Azure SQL Database that has **zero public internet exposure**. The SQL server has public access completely disabled — there's no public endpoint at all. Instead, we use three things working together: a **Private Endpoint** that gives the SQL server a private IP inside a Virtual Network, a **Private DNS Zone** that makes sure DNS resolves to that private IP, and a **VNet Data Gateway** that lets Power BI Service send its refresh traffic through that same VNet. The result? The data never leaves Azure's private backbone. Let me walk you through exactly how this works."

---

## Prerequisites Checklist

Before starting the demo, verify **all** of the following:

- [ ] Azure resources deployed and healthy (resource group `pbi-pl-demo-rg`)
- [ ] SQL database `ContosoRetail` populated with sample data — run `.\scripts\azure\04-seed-sql-data.ps1` (Customers, Products, Orders, OrderItems)
- [ ] Private Endpoint status is **Approved** (not Pending)
- [ ] Private DNS Zone `privatelink.database.windows.net` has A record pointing to PE IP
- [ ] VNet Data Gateway shows **Online** in Power BI Service → Settings → Manage gateways
- [ ] Power BI report published to a workspace (Premium or PPU capacity)
- [ ] Data source credentials configured on the gateway in Power BI Service
- [ ] Browser tabs pre-opened:
  - Azure Portal → Resource Group `pbi-pl-demo-rg`
  - Azure Portal → SQL Server `pbi-pl-demo-sql` → Networking blade
  - Azure Portal → Private Endpoint `pbi-pl-demo-sql-pe`
  - Azure Portal → Private DNS Zone `privatelink.database.windows.net`
  - Power BI Service → Manage gateways
  - Power BI Service → Workspace with the published report
- [ ] Terminal ready with `nslookup` or `Resolve-DnsName` (optional, for DNS resolution demo from a VM in the VNet)

---

## Demo Sections

---

### [0:00–1:30] Introduction + Talk Track

**SHOW:** Title slide or architecture diagram (see `architecture.md`).

**SAY:** Deliver the talk track above. Emphasize: "Public access is disabled. There is no way to reach this SQL server from the internet."

**Checkpoint:** Audience understands the goal — secure, private-only connectivity from Power BI to SQL.

---

### [1:30–3:00] Show Azure Resources

**SHOW:** Azure Portal → Resource Group `pbi-pl-demo-rg` → Overview blade showing all resources.

**SAY:**
> "Here's our resource group. You can see the Virtual Network, the SQL Server and database, the Private Endpoint, and the Private DNS Zone. Everything is in one resource group for easy management. Let me call out the key pieces."

Point out each resource:
- `pbi-pl-demo-vnet` — the VNet (10.0.0.0/16)
- `pbi-pl-demo-sql` — the SQL logical server
- `ContosoRetail` — the database
- `pbi-pl-demo-sql-pe` — the Private Endpoint
- `privatelink.database.windows.net` — the Private DNS Zone

**Checkpoint:** All five resources visible and healthy in the portal.

---

### [3:00–4:30] Show SQL Public Access Is Disabled

**SHOW:** Azure Portal → SQL Server `pbi-pl-demo-sql` → **Networking** blade.

**SAY:**
> "This is the critical security control. Public network access is set to **Disable**. There are no firewall rules — no IPs, no 'Allow Azure services' checkbox. The only way to reach this server is through a Private Endpoint."

Point out:
- "Public network access" toggle → **Disabled**
- Firewall rules section → empty
- "Exceptions" → none checked

**Checkpoint:** Audience sees that public access is definitively off. No firewall exceptions.

---

### [4:30–6:00] Show Private Endpoint + Private DNS Zone + DNS Resolution

**SHOW:** Azure Portal → Private Endpoint `pbi-pl-demo-sql-pe` → Overview.

**SAY:**
> "Here's the Private Endpoint. It's connected to our SQL Server's `sqlServer` sub-resource, and its connection status is **Approved**. It has a network interface with a private IP in the 10.0.1.0/24 subnet."

Then switch to: Azure Portal → Private DNS Zone `privatelink.database.windows.net` → **Recordsets**.

**SAY:**
> "And here's the DNS magic. This Private DNS Zone has an A record for `pbi-pl-demo-sql` pointing to the Private Endpoint's IP — for example, 10.0.1.4. This zone is linked to our VNet, so any DNS query from inside the VNet resolves the SQL server's FQDN to this private IP instead of a public IP."

**(Optional) Live DNS resolution demo** — if you have a VM or Cloud Shell in the VNet:
```powershell
Resolve-DnsName pbi-pl-demo-sql.database.windows.net
```
or
```bash
nslookup pbi-pl-demo-sql.database.windows.net
```
Expected output: resolves to `10.0.1.x` (private IP), **not** a public IP.

**Checkpoint:** Private Endpoint is Approved. DNS A record points to private IP. (Optional) Live `nslookup` confirms private resolution.

---

### [6:00–8:00] Show VNet Data Gateway Is Connected

**SHOW:** Power BI Service → **Settings** (gear icon) → **Manage connections and gateways** → **Virtual network data gateways** tab.

**SAY:**
> "Now let's look at the Power BI side. Here's our VNet Data Gateway. It's associated with the gateway subnet (10.0.2.0/24) in our VNet. The status shows **Online**, which means Power BI can send traffic through this gateway into the VNet. This is the bridge — it's how Power BI, which is a SaaS service, gets access to the private network where our SQL server lives."

Point out:
- Gateway name: `pbi-pl-demo-vnetgw`
- VNet/Subnet association
- Status: **Online** (green)

Then show: **Data sources** tab → the SQL data source configured on this gateway.

**SAY:**
> "And here's the data source — it's configured to connect to `pbi-pl-demo-sql.database.windows.net` using the gateway. The credentials are stored here in Power BI Service."

**Checkpoint:** Gateway is Online. Data source is configured and shows a valid connection.

---

### [8:00–10:00] Publish Dataset + Trigger Refresh

**SHOW:** Power BI Service → Workspace → Dataset `ContosoRetail` → **Refresh now**.

**SAY:**
> "Let's trigger a refresh right now. This will cause Power BI to send the SQL queries through the VNet Data Gateway, through the Private Endpoint, to our SQL database — all over the private network."

Click **Refresh now**.

While waiting, show the SQL query the dataset uses:

```sql
-- Sample query used by the Power BI report
SELECT
    c.CustomerName,
    p.ProductName,
    o.OrderDate,
    oi.Quantity,
    oi.UnitPrice,
    (oi.Quantity * oi.UnitPrice) AS LineTotal
FROM Orders o
INNER JOIN Customers c ON o.CustomerID = c.CustomerID
INNER JOIN OrderItems oi ON o.OrderID = oi.OrderID
INNER JOIN Products p ON oi.ProductID = p.ProductID
ORDER BY o.OrderDate DESC;
```

**SAY:**
> "While that's running — this is the query the report is based on. It joins Customers, Orders, OrderItems, and Products to build a sales detail view. Nothing special about the query itself; what's special is the network path it takes."

**Checkpoint:** Refresh is triggered (spinning icon visible).

---

### [10:00–12:00] Show Successful Refresh + Power BI Report

**SHOW:** Power BI Service → Dataset → **Refresh history**.

**SAY:**
> "And there it is — refresh completed successfully. You can see the timestamp and duration. The data traveled from Azure SQL through the Private Endpoint, through the VNet, through the VNet Data Gateway, and into Power BI — entirely on Azure's private backbone."

Then open the Power BI report.

**SAY:**
> "And here's the report with live data. We're looking at customer orders, product sales — all sourced from a SQL database that has zero public internet exposure."

Interact with the report briefly — click a filter, show data refreshing in visuals.

**Checkpoint:** Refresh history shows **Completed**. Report displays current data from the ContosoRetail database.

---

### [12:00–13:00] Recap Security Posture

**SHOW:** Architecture diagram (slide or `architecture.md`).

**SAY:**
> "Let me recap the security posture:
>
> 1. **SQL public access is disabled** — no one on the internet can reach it.
> 2. **Private Endpoint** gives the SQL server a private IP inside our VNet.
> 3. **Private DNS Zone** ensures DNS resolves to that private IP, not a public one.
> 4. **VNet Data Gateway** lets Power BI Service — a SaaS product — send traffic through the VNet.
> 5. **The entire data path is private** — no data touches the public internet.
>
> This is a pattern you can use in production for any data source that supports Private Link — not just SQL. Synapse, Cosmos DB, Storage Accounts — they all work the same way."

**Checkpoint:** Audience can articulate the four components and why each is necessary.

---

### [13:00–15:00] Q&A Buffer

**SHOW:** Architecture diagram or resource group overview (whichever supports the questions).

**SAY:**
> "That's the demo. Happy to take questions — about the setup, the networking, cost, or how this applies to your scenario."

**Common questions to prepare for:**

| Question | Short Answer |
|----------|-------------|
| "What about on-premises data gateways?" | VNet Data Gateway is cloud-native; no on-prem VM needed. For on-prem sources, you still need the traditional gateway. |
| "Does this work with Synapse/Cosmos/Storage?" | Yes — any Azure service that supports Private Link follows the same pattern. |
| "What's the cost?" | VNet Data Gateway requires Premium or PPU capacity. The Private Endpoint has a small hourly cost (~$0.01/hr) plus data processing. |
| "Can multiple datasets share one gateway?" | Yes — you can configure multiple data sources on a single VNet Data Gateway. |
| "What if I need to query from SSMS too?" | Add a VM or Azure Bastion in the VNet, or use a point-to-site VPN. The Private Endpoint serves any client inside the VNet. |

**Checkpoint:** Questions answered. Demo complete.
