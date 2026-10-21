# Architecture: Power BI + Azure SQL Private Link

## Overview

This architecture demonstrates how Power BI Service can securely connect to an Azure SQL Database that has **all public network access disabled**, using a combination of a VNet Data Gateway, Private Endpoint, and Private DNS Zone. The result is a fully private data path — no data traverses the public internet, the SQL server has no public IP exposure, and DNS resolution is handled entirely within the Azure private network. This pattern is suitable for production workloads where regulatory or security requirements demand network-level isolation of data sources.

## Architecture Diagram

```mermaid
flowchart TB
    subgraph Microsoft Cloud
        subgraph PBI["Power BI Service"]
            DS["Dataset\n(ContosoRetail)"]
            RPT["Power BI Report"]
        end

        subgraph VNET["Azure VNet: pbi-pl-demo-vnet\n10.0.0.0/16"]
            subgraph GWSub["Gateway Subnet: 10.0.2.0/24"]
                GW["VNet Data Gateway\n(pbi-pl-demo-vnetgw)"]
            end
            subgraph DefSub["Default Subnet: 10.0.1.0/24"]
                PE["Private Endpoint\n(pbi-pl-demo-sql-pe)\nNIC IP: 10.0.1.x"]
            end
        end

        subgraph SQL["Azure SQL Server\npbi-pl-demo-sql.database.windows.net\nPublic network access: DISABLED"]
            DB["Database: ContosoRetail\nTables: Customers, Products,\nOrders, OrderItems"]
        end

        DNS["Private DNS Zone\nprivatelink.database.windows.net\nA Record: pbi-pl-demo-sql → 10.0.1.x\nVNet Link: pbi-pl-demo-vnet"]
    end

    DS -->|"① Refresh request"| GW
    GW -->|"② VNet traffic"| PE
    PE -->|"③ Private Link"| SQL
    GW -.->|"DNS query"| DNS
    DNS -.->|"Resolves to 10.0.1.x"| PE
```

## Component Descriptions

| Resource | Name | Role |
|----------|------|------|
| **Resource Group** | `pbi-pl-demo-rg` | Container for all demo resources; single unit for deployment and teardown. |
| **Virtual Network** | `pbi-pl-demo-vnet` (10.0.0.0/16) | Provides the private network boundary. All private traffic stays within this VNet. |
| **Default Subnet** | `default` (10.0.1.0/24) | Hosts the Private Endpoint NIC. This is where the SQL Server's private IP address lives. |
| **Gateway Subnet** | `gateway` (10.0.2.0/24) | Dedicated subnet for the VNet Data Gateway. Separating subnets allows independent NSG and routing rules. |
| **Azure SQL Server** | `pbi-pl-demo-sql` | Logical SQL server with **public network access disabled**. Only reachable via Private Endpoint. |
| **SQL Database** | `ContosoRetail` | Sample database with four tables (Customers, Products, Orders, OrderItems) used by the Power BI report. |
| **Private Endpoint** | `pbi-pl-demo-sql-pe` | Creates a private NIC in the default subnet mapped to the SQL Server's `sqlServer` sub-resource. Traffic to this NIC is tunneled over Private Link to the SQL Server. |
| **Private DNS Zone** | `privatelink.database.windows.net` | Hosts the A record that maps `pbi-pl-demo-sql.database.windows.net` to the Private Endpoint's private IP. |
| **VNet DNS Link** | `pbi-pl-demo-vnet-link` | Links the Private DNS Zone to the VNet so that any DNS query originating inside the VNet resolves the private A record. |
| **VNet Data Gateway** | `pbi-pl-demo-vnetgw` | Power BI-managed gateway injected into the gateway subnet. Acts as the bridge between Power BI Service and the VNet. |

## Network Flow: What Happens When Power BI Refreshes a Dataset

When a user or schedule triggers a dataset refresh in Power BI Service, the following sequence occurs:

### Step 1 — Power BI initiates refresh
Power BI Service identifies that the dataset `ContosoRetail` is bound to a VNet Data Gateway. It sends the refresh request to the gateway control plane.

### Step 2 — VNet Data Gateway receives the request
The VNet Data Gateway instance running inside `10.0.2.0/24` receives the instruction to execute the dataset's SQL queries. It constructs a TDS (Tabular Data Stream) connection to `pbi-pl-demo-sql.database.windows.net` on port 1433.

### Step 3 — DNS resolution (private path)
The gateway resolves `pbi-pl-demo-sql.database.windows.net`. Because the Private DNS Zone `privatelink.database.windows.net` is linked to the VNet:
1. The DNS query for `pbi-pl-demo-sql.database.windows.net` returns a CNAME to `pbi-pl-demo-sql.privatelink.database.windows.net`.
2. The Private DNS Zone resolves this to the Private Endpoint's IP (e.g., `10.0.1.4`).

### Step 4 — TCP connection over Private Link
The gateway opens a TCP connection to `10.0.1.4:1433`. This traffic stays entirely within the Azure backbone — it never touches the public internet. The Private Endpoint NIC forwards the traffic over the Private Link tunnel to the Azure SQL Server.

### Step 5 — Authentication and query execution
The SQL Server authenticates the connection (SQL auth or Entra ID, depending on configuration). The gateway executes the dataset's SQL queries against the `ContosoRetail` database.

### Step 6 — Data returned to Power BI
Query results flow back through the same private path: SQL Server → Private Link → Private Endpoint NIC → VNet → VNet Data Gateway → Power BI Service. The dataset is refreshed and the report reflects the latest data.

## DNS Resolution Flow

```mermaid
sequenceDiagram
    participant GW as VNet Data Gateway
    participant ADNS as Azure DNS
    participant PDNS as Private DNS Zone<br/>(privatelink.database.windows.net)
    participant PE as Private Endpoint<br/>(10.0.1.4)

    GW->>ADNS: Query: pbi-pl-demo-sql.database.windows.net
    ADNS->>ADNS: Check VNet-linked Private DNS Zones
    ADNS->>PDNS: CNAME → pbi-pl-demo-sql.privatelink.database.windows.net
    PDNS-->>ADNS: A Record: 10.0.1.4 (PE NIC IP)
    ADNS-->>GW: Resolved: 10.0.1.4
    GW->>PE: Connect to 10.0.1.4:1433 (private, in-VNet)
```

**Key point:** If the Private DNS Zone is *not* linked to the VNet, or the A record is missing, the DNS query falls through to public DNS and resolves to the SQL Server's public IP — which will be **rejected** because public access is disabled. This is the #1 cause of connectivity failures in Private Link setups.

## Security Posture Summary

| Control | Status | Effect |
|---------|--------|--------|
| SQL Server public network access | **Disabled** | No client on the public internet can reach the SQL server, even with valid credentials. |
| Private Endpoint | **Enabled** | SQL server is reachable only from within the VNet via a private IP address. |
| Private DNS Zone | **Linked to VNet** | DNS resolution inside the VNet returns the private IP, not the public IP. |
| VNet Data Gateway | **Injected into VNet** | Power BI's refresh traffic originates inside the VNet, so it uses the private path. |
| No public IP on SQL Server | **Enforced** | The SQL server has no public endpoint; `DENY` is the default for all public traffic. |
| Network segmentation | **Subnets separated** | Private Endpoint and VNet Data Gateway are in different subnets, allowing independent NSG policies. |

This architecture ensures **zero public internet exposure** for the SQL database while still allowing Power BI Service (a SaaS product) to refresh data on schedule.
