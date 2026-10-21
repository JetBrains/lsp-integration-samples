# =============================================================
# FILE: 04-seed-sql-data.ps1
# PURPOSE: Seed ContosoRetail database with sample data
#
# PREREQUISITES:
#   - Azure CLI installed + logged in (az login)
#   - sqlcmd installed (winget install sqlcmd)
#   - Contributor access on the target subscription
#   - Infrastructure already deployed (run 01-deploy-infrastructure.ps1 first)
#
# HOW TO RUN:
#   .\scripts\azure\04-seed-sql-data.ps1
# =============================================================

# CONFIGURATION — CHANGE THESE VALUES
$SUBSCRIPTION_ID = ""                    # Required
$RESOURCE_GROUP  = "rg-pbi-pl-demo-2"
$DATABASE_NAME   = "ContosoRetail"

# ---------------------------------------------------------------------------

$ErrorActionPreference = "Continue"

function Write-Step  { param($msg) Write-Host "`n>> $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "   [OK]   $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "   [WARN] $msg" -ForegroundColor Yellow }
function Write-Err   { param($msg) Write-Host "   [FAIL] $msg" -ForegroundColor Red }

# ── Resolve repo root and SQL script paths ─────────────────────────────────
$repoRoot = Split-Path -Path $PSScriptRoot -Parent | Split-Path -Parent
$sqlScripts = @(
    "$repoRoot\scripts\sql\01-create-schema.sql",
    "$repoRoot\scripts\sql\02-insert-sample-data.sql",
    "$repoRoot\scripts\sql\03-verify-data.sql"
)

# ── Pre-flight checks ─────────────────────────────────────────────────────
Write-Step "Pre-flight checks"

if (-not $SUBSCRIPTION_ID) {
    Write-Err "SUBSCRIPTION_ID is not set. Edit the CONFIGURATION section before running."
    exit 1
}

# Check sqlcmd is available
$sqlcmdPath = Get-Command sqlcmd -ErrorAction SilentlyContinue
if (-not $sqlcmdPath) {
    Write-Err "sqlcmd is not installed or not in PATH."
    Write-Host "   Install with:  winget install sqlcmd" -ForegroundColor Yellow
    Write-Host "   Then restart your terminal and try again." -ForegroundColor Yellow
    exit 1
}
Write-Ok "sqlcmd found: $($sqlcmdPath.Source)"

# Verify SQL script files exist
foreach ($script in $sqlScripts) {
    if (-not (Test-Path $script)) {
        Write-Err "SQL script not found: $script"
        exit 1
    }
}
Write-Ok "All SQL scripts found ($($sqlScripts.Count) files)"

az account set --subscription $SUBSCRIPTION_ID 2>&1 | Out-Null

# ── Discover SQL Server ────────────────────────────────────────────────────
Write-Step "Discovering SQL Server in resource group '$RESOURCE_GROUP'"
$SQL_SERVER_NAME = az sql server list `
    --resource-group $RESOURCE_GROUP `
    --query "[0].name" `
    --output tsv 2>&1

if ($LASTEXITCODE -ne 0 -or -not $SQL_SERVER_NAME) {
    Write-Err "No SQL Server found in resource group '$RESOURCE_GROUP'."
    Write-Host "   Have you run 01-deploy-infrastructure.ps1 first?" -ForegroundColor Yellow
    exit 1
}

$SQL_FQDN = "$SQL_SERVER_NAME.database.windows.net"
Write-Ok "Found SQL Server: $SQL_SERVER_NAME ($SQL_FQDN)"

# ── Track cleanup state ───────────────────────────────────────────────────
$firewallRuleName = $null
$publicAccessWasEnabled = $false
$seedSuccess = $false

try {
    # ── Enable public network access ──────────────────────────────────────
    Write-Step "Temporarily enabling public network access on SQL Server"
    az sql server update `
        --resource-group $RESOURCE_GROUP `
        --name $SQL_SERVER_NAME `
        --set publicNetworkAccess="Enabled" `
        --output none 2>&1

    if ($LASTEXITCODE -ne 0) {
        Write-Err "Failed to enable public network access."
        exit 1
    }
    $publicAccessWasEnabled = $true
    Write-Ok "Public network access enabled (temporary)"

    # ── Get public IP and add firewall rule ───────────────────────────────
    Write-Step "Adding temporary firewall rule"
    $myIp = Invoke-RestMethod -Uri "https://api.ipify.org"
    if (-not $myIp) {
        Write-Err "Could not determine public IP address."
        throw "IP lookup failed"
    }
    Write-Ok "Public IP: $myIp"

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $firewallRuleName = "temp-seed-data-$timestamp"

    az sql server firewall-rule create `
        --resource-group $RESOURCE_GROUP `
        --server $SQL_SERVER_NAME `
        --name $firewallRuleName `
        --start-ip-address $myIp `
        --end-ip-address $myIp `
        --output none 2>&1

    if ($LASTEXITCODE -ne 0) {
        Write-Err "Failed to create firewall rule."
        throw "Firewall rule creation failed"
    }
    Write-Ok "Firewall rule '$firewallRuleName' created for $myIp"

    # ── Run SQL scripts ───────────────────────────────────────────────────
    Write-Step "Seeding database '$DATABASE_NAME' with sample data"
    $allScriptsOk = $true

    foreach ($script in $sqlScripts) {
        $scriptName = Split-Path $script -Leaf
        Write-Host "   Running $scriptName ..." -ForegroundColor Gray

        sqlcmd -S $SQL_FQDN -d $DATABASE_NAME --authentication-method=ActiveDirectoryDefault -i $script 2>&1 | ForEach-Object {
            Write-Host "   $_" -ForegroundColor Gray
        }

        if ($LASTEXITCODE -ne 0) {
            Write-Err "Script failed: $scriptName"
            $allScriptsOk = $false
            break
        }
        Write-Ok "$scriptName completed"
    }

    if (-not $allScriptsOk) {
        throw "One or more SQL scripts failed"
    }
    $seedSuccess = $true

} catch {
    Write-Err "Error during seeding: $_"
} finally {
    # ── Cleanup: remove firewall rule ─────────────────────────────────────
    Write-Step "Cleaning up — removing temporary firewall rule"
    if ($firewallRuleName) {
        az sql server firewall-rule delete `
            --resource-group $RESOURCE_GROUP `
            --server $SQL_SERVER_NAME `
            --name $firewallRuleName `
            --output none 2>&1

        if ($LASTEXITCODE -eq 0) {
            Write-Ok "Firewall rule '$firewallRuleName' removed"
        } else {
            Write-Warn "Could not remove firewall rule '$firewallRuleName' — remove manually"
        }
    } else {
        Write-Ok "No firewall rule to remove"
    }

    # ── Cleanup: disable public access ────────────────────────────────────
    Write-Step "Re-disabling public network access"
    if ($publicAccessWasEnabled) {
        az sql server update `
            --resource-group $RESOURCE_GROUP `
            --name $SQL_SERVER_NAME `
            --set publicNetworkAccess="Disabled" `
            --output none 2>&1

        if ($LASTEXITCODE -eq 0) {
            Write-Ok "Public network access disabled"
        } else {
            Write-Warn "Could not disable public access — disable manually in the portal"
        }
    } else {
        Write-Ok "Public access was not enabled by this script — no change needed"
    }
}

# ── Verify lockdown ──────────────────────────────────────────────────────
Write-Step "Verifying security lockdown"

$publicAccess = az sql server show `
    --resource-group $RESOURCE_GROUP `
    --name $SQL_SERVER_NAME `
    --query "publicNetworkAccess" `
    --output tsv 2>&1

if ($publicAccess -eq "Disabled") {
    Write-Ok "Public network access: Disabled"
} else {
    Write-Err "Public network access: $publicAccess (expected Disabled)"
}

$fwRules = az sql server firewall-rule list `
    --resource-group $RESOURCE_GROUP `
    --server $SQL_SERVER_NAME `
    --query "length(@)" `
    --output tsv 2>&1

if ($fwRules -eq "0") {
    Write-Ok "Firewall rules: 0"
} else {
    Write-Err "Firewall rules: $fwRules (expected 0)"
}

# ── Summary ──────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " Seed Data Summary" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "   SQL Server:      $SQL_SERVER_NAME" -ForegroundColor White
Write-Host "   Database:        $DATABASE_NAME" -ForegroundColor White
Write-Host "   Scripts run:     $($sqlScripts.Count)" -ForegroundColor White

if ($seedSuccess) {
    Write-Host "   Result:          SUCCESS" -ForegroundColor Green
    Write-Host ""
    Write-Host "   Tables seeded: Customers, Products, Orders, OrderItems" -ForegroundColor Green
} else {
    Write-Host "   Result:          FAILED" -ForegroundColor Red
    Write-Host ""
    Write-Host "   Review errors above and re-run. The script is safe to run again." -ForegroundColor Yellow
}

Write-Host "   Public access:   $publicAccess" -ForegroundColor White
Write-Host "   Firewall rules:  $fwRules" -ForegroundColor White
Write-Host "=============================================" -ForegroundColor Cyan

if (-not $seedSuccess) {
    exit 1
}
