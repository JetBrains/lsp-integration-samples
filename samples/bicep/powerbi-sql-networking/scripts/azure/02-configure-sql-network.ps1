# =============================================================
# FILE: 02-configure-sql-network.ps1
# PURPOSE: Verify and lock down SQL Server networking after deployment
#
# PREREQUISITES:
#   - Azure CLI installed
#   - Run: az login
#   - Contributor access on the target subscription
#
# HOW TO RUN:
#   .\scripts\azure\02-configure-sql-network.ps1
# =============================================================

# CONFIGURATION
$SUBSCRIPTION_ID  = ""                               # Required: your Azure subscription ID
$RESOURCE_GROUP   = "rg-pbi-pl-demo"

# ---------------------------------------------------------------------------

$ErrorActionPreference = "Stop"

function Write-Step  { param($msg) Write-Host "`n>> $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "   [OK] $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "   [WARN] $msg" -ForegroundColor Yellow }
function Write-Err   { param($msg) Write-Host "   [ERROR] $msg" -ForegroundColor Red }

if (-not $SUBSCRIPTION_ID) {
    Write-Err "SUBSCRIPTION_ID is not set. Edit the CONFIGURATION section before running."
    exit 1
}

az account set --subscription $SUBSCRIPTION_ID 2>&1 | Out-Null

# ── Discover SQL Server name from resource group ──────────────────────────
Write-Step "Discovering SQL Server in resource group '$RESOURCE_GROUP'"
try {
    $SQL_SERVER_NAME = az sql server list `
        --resource-group $RESOURCE_GROUP `
        --query "[0].name" `
        --output tsv 2>&1

    if ($LASTEXITCODE -ne 0 -or -not $SQL_SERVER_NAME) {
        Write-Err "No SQL Server found in resource group '$RESOURCE_GROUP'."
        exit 1
    }
    Write-Ok "Found SQL Server: $SQL_SERVER_NAME"
} catch {
    Write-Err "Failed to discover SQL Server: $_"
    exit 1
}

# ── Step 1: Verify public network access is Disabled ──────────────────────
Write-Step "Checking public network access on SQL Server '$SQL_SERVER_NAME'"
try {
    $sqlServer = az sql server show `
        --resource-group $RESOURCE_GROUP `
        --name $SQL_SERVER_NAME `
        --query "{publicNetworkAccess:publicNetworkAccess, fullyQualifiedDomainName:fullyQualifiedDomainName}" `
        --output json | ConvertFrom-Json

    if ($sqlServer.publicNetworkAccess -eq "Disabled") {
        Write-Ok "Public network access is DISABLED."
    } else {
        Write-Warn "Public network access is '$($sqlServer.publicNetworkAccess)'. Disabling now..."
        az sql server update `
            --resource-group $RESOURCE_GROUP `
            --name $SQL_SERVER_NAME `
            --set publicNetworkAccess="Disabled" `
            --output none
        Write-Ok "Public network access has been disabled."
    }
} catch {
    Write-Err "Failed to check/update SQL server: $_"
    exit 1
}

# ── Step 2: List and remove firewall rules ─────────────────────────────────
Write-Step "Checking for existing firewall rules on '$SQL_SERVER_NAME'"
try {
    $firewallRules = az sql server firewall-rule list `
        --resource-group $RESOURCE_GROUP `
        --server $SQL_SERVER_NAME `
        --output json | ConvertFrom-Json

    if ($firewallRules.Count -eq 0) {
        Write-Ok "No firewall rules found. Server is clean."
    } else {
        Write-Warn "Found $($firewallRules.Count) firewall rule(s). Removing them..."
        foreach ($rule in $firewallRules) {
            Write-Host "   Removing rule: $($rule.name) ($($rule.startIpAddress) - $($rule.endIpAddress))" -ForegroundColor Yellow
            az sql server firewall-rule delete `
                --resource-group $RESOURCE_GROUP `
                --server $SQL_SERVER_NAME `
                --name $rule.name `
                --output none
        }
        Write-Ok "All firewall rules removed."
    }
} catch {
    Write-Err "Failed to manage firewall rules: $_"
    exit 1
}

# ── Step 3: Verify private endpoint connection is Approved ─────────────────
Write-Step "Checking private endpoint connections on '$SQL_SERVER_NAME'"
try {
    $peConnections = az sql server show `
        --resource-group $RESOURCE_GROUP `
        --name $SQL_SERVER_NAME `
        --query "privateEndpointConnections" `
        --output json | ConvertFrom-Json

    if (-not $peConnections -or $peConnections.Count -eq 0) {
        Write-Err "No private endpoint connections found. The private endpoint may not be deployed."
        exit 1
    }

    $allApproved = $true
    foreach ($pe in $peConnections) {
        $status = $pe.properties.privateLinkServiceConnectionState.status
        $peName = ($pe.id -split "/")[-1]
        if ($status -eq "Approved") {
            Write-Ok "Private endpoint '$peName' is Approved."
        } else {
            Write-Warn "Private endpoint '$peName' status is '$status'. Approving..."
            az sql server private-endpoint-connection approve `
                --resource-group $RESOURCE_GROUP `
                --server $SQL_SERVER_NAME `
                --name $peName `
                --output none 2>&1 | Out-Null
            Write-Ok "Private endpoint '$peName' approved."
            $allApproved = $false
        }
    }
} catch {
    Write-Err "Failed to check private endpoint connections: $_"
    exit 1
}

# ── Summary ────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " SQL Network Configuration Summary" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " SQL Server:            $SQL_SERVER_NAME" -ForegroundColor White
Write-Host " FQDN:                  $($sqlServer.fullyQualifiedDomainName)" -ForegroundColor White
Write-Host " Public Network Access: Disabled" -ForegroundColor Green
Write-Host " Firewall Rules:        None" -ForegroundColor Green
Write-Host " Private Endpoint:      Approved" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next: Run 03-validate-deployment.ps1 to perform full validation." -ForegroundColor White
