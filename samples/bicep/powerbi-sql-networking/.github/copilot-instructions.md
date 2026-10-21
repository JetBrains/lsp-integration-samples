# Copilot Instructions — powerbi-sql-networking

## Purpose

This repo is a **demo-first** project proving that Power BI Service can access an Azure SQL Database privately via Private Link — no public endpoint exposure. It targets live demos (10–15 min) with both an **automation path** (Bicep/ARM + Azure CLI) and a **portal path** (step-by-step portal instructions).

## Architecture

```mermaid
flowchart TB
    PBI["Power BI Service"] --> GW["VNet Data Gateway\n(gateway subnet)"]
    GW --> PE["Private Endpoint\n(default subnet)"]
    PE --> SQL["Azure SQL DB\n(public access disabled)"]
    GW -.->|"DNS query"| DNS["Private DNS Zone\n(privatelink.database.windows.net)"]
    DNS -.->|"Resolves to 10.0.1.x"| PE
```

Key points:
- Azure SQL has **public network access disabled**; all traffic flows through a Private Endpoint.
- DNS resolution goes through an Azure Private DNS Zone (`privatelink.database.windows.net`) linked to the VNet.
- The VNet Data Gateway runs inside the same VNet and bridges Power BI Service to the private SQL endpoint.
- One VNet, one SQL DB, one Private Endpoint, one gateway — minimal but complete.

## Repository Structure Convention

Follow the numbered-script pattern established in sibling repos (`pbi-fabric-mpe`, `powerbi-network-security`):

```
main.bicep                          # Orchestrator — calls all modules
modules/
  vnet.bicep                        # VNet + subnet definitions
  sql.bicep                         # Azure SQL Server + Database
  privateEndpoint.bicep             # Private Endpoint for SQL
  privateDns.bicep                  # Private DNS zone + VNet link + A record
parameters/
  demo.bicepparam                   # Default parameter file (placeholders)
scripts/
  azure/
    01-deploy-infrastructure.ps1    # Deploys Bicep via az deployment
    02-configure-sql-network.ps1    # Post-deploy SQL network lockdown
    03-validate-deployment.ps1      # DNS resolution + connectivity checks
    04-seed-sql-data.ps1            # Seed SQL data (temp opens public access)
    99-cleanup.ps1                  # Delete RG + verify removal
  sql/
    01-create-schema.sql            # Demo tables
    02-insert-sample-data.sql       # Seed data for the report
    03-verify-data.sql              # Validation queries
docs/
  architecture.md                   # Detailed architecture + ASCII diagrams
  demo-runbook.md                   # Timed demo script with show/tell cues
  portal-walkthrough.md             # Azure Portal click-paths (mirrors automation)
  troubleshooting.md                # Top failure modes: symptom → cause → fix
```

## Pre-Deployment Checklist

Before any Azure deployment, **always** verify the active subscription context:

```powershell
# 1. Confirm you are logged in and targeting the correct subscription
az account show --query "{subscriptionId:id, name:name, tenantId:tenantId}" --output table

# 2. If needed, set the correct subscription
az account set --subscription "<SUBSCRIPTION_ID>"
```

Never assume the current CLI context is correct — always check first.

## Deployment Commands

```powershell
# Deploy all infrastructure
az deployment group create \
  --resource-group $RESOURCE_GROUP \
  --template-file main.bicep \
  --parameters parameters/demo.bicepparam

# Validate DNS resolution
nslookup <server>.privatelink.database.windows.net

# Validate SQL connectivity from a VNet-joined VM
Test-NetConnection -ComputerName <server>.database.windows.net -Port 1433

# Cleanup
az group delete --name $RESOURCE_GROUP --yes --no-wait
```

## Bicep Conventions

- **Naming prefix**: All resources use a configurable `prefix` parameter (default: `pbi-pl-demo`). Resource names follow the pattern `${prefix}-<resource-type>` (e.g., `pbi-pl-demo-vnet`, `pbi-pl-demo-sql`).
- **Parameterize everything deployable**: subscription ID, region, naming prefix, SQL auth model, admin credentials — all go in the parameter file with placeholder values.
- **No secrets in code**: Use `@secure()` decorator for passwords. Recommend Managed Identity / Entra ID auth where possible.
- **Module pattern**: `main.bicep` orchestrates; each `modules/*.bicep` file is a self-contained resource deployment with clear `param` / `output` boundaries.

## PowerShell Script Conventions

Follow the header block pattern from sibling repos:

```powershell
# =============================================================
# FILE: <filename>.ps1
# PURPOSE: <one-line description>
#
# PREREQUISITES:
#   - Azure CLI installed
#   - Run: az login
#   - Contributor access on the target subscription
#
# HOW TO RUN:
#   .\scripts\azure\<filename>.ps1
# =============================================================
```

- Configuration variables go at the top of each script under a `# CONFIGURATION` section with clear `CHANGE THESE VALUES` comments.
- Scripts are numbered (`01-`, `02-`, etc.) to indicate execution order.
- Use `az` CLI commands (not Az PowerShell module) for Azure operations — keeps dependencies minimal.

## SQL Sample Data

- Use a simple, demo-friendly dataset (e.g., Customers/Products/Orders or equivalent retail scenario).
- Keep seed data small (tens of rows) — enough for meaningful visuals, fast to deploy.
- SQL scripts are numbered and idempotent where possible.

## What Cannot Be Automated with Bicep

These steps require Power BI REST API, PowerShell cmdlets, or manual portal configuration:

1. **VNet Data Gateway** — Create and register the gateway in the Power BI Service admin portal or via Power BI PowerShell cmdlets.
2. **Power BI data source configuration** — Bind the SQL connection to the gateway.
3. **Dataset publish + refresh** — Publish from Power BI Desktop or via REST API, then configure scheduled refresh.
4. **Power BI workspace creation** — If not pre-existing.

Document these as explicit manual/scripted steps in `docs/demo-runbook.md` with portal click-paths in `docs/portal-walkthrough.md`.

## Validation Checklist (Post-Deploy)

After deployment, the following must be true:

1. Azure SQL Server shows **Public network access: Disabled**
2. Private Endpoint is in **Approved** state
3. `nslookup <server>.database.windows.net` resolves to a **10.x.x.x** private IP (not a public IP)
4. VNet Data Gateway shows **Online** in Power BI Service
5. Dataset refresh completes successfully over private connectivity

## Troubleshooting Context

When debugging connectivity issues, check in this order:

1. **DNS resolution** — Must resolve to private IP, not public. Check Private DNS Zone link to VNet.
2. **Private Endpoint state** — Must be Approved, not Pending.
3. **SQL public access** — Must be Disabled (verify no firewall exceptions remain).
4. **VNet Data Gateway** — Must be running in a subnet that can reach the Private Endpoint.
5. **Authentication** — Entra ID vs SQL auth mismatch is a common demo-day failure.
6. **Power BI credentials** — Gateway data source credentials must match the SQL auth model.
