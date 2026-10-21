# =============================================================
# FILE: 99-cleanup.ps1
# PURPOSE: Delete all demo resources and remind about Power BI cleanup
#
# PREREQUISITES:
#   - Azure CLI installed
#   - Run: az login
#   - Contributor access on the target subscription
#
# HOW TO RUN:
#   .\scripts\azure\99-cleanup.ps1
#   .\scripts\azure\99-cleanup.ps1 -Force    # skip confirmation
# =============================================================

param(
    [switch]$Force
)

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

# ── Step 1: Confirm resource group ────────────────────────────────────────
Write-Step "Target resource group: '$RESOURCE_GROUP'"

$rgExists = az group exists --name $RESOURCE_GROUP --output tsv 2>&1
if ($rgExists -ne "true") {
    Write-Warn "Resource group '$RESOURCE_GROUP' does not exist. Nothing to clean up."
    exit 0
}

Write-Ok "Resource group found."

# ── Step 2: Show what will be deleted ──────────────────────────────────────
Write-Step "Resources that will be DELETED:"

$resources = az resource list `
    --resource-group $RESOURCE_GROUP `
    --query "[].{Name:name, Type:type, Location:location}" `
    --output table 2>&1

if ($LASTEXITCODE -eq 0) {
    $resources | ForEach-Object { Write-Host "   $_" -ForegroundColor Yellow }
} else {
    Write-Warn "Could not list resources."
}

# ── Step 3: Prompt for confirmation ────────────────────────────────────────
if (-not $Force) {
    Write-Host ""
    Write-Host "   !! WARNING: This will permanently delete ALL resources in '$RESOURCE_GROUP' !!" -ForegroundColor Red
    Write-Host ""
    $confirmation = Read-Host "   Type the resource group name to confirm deletion"

    if ($confirmation -ne $RESOURCE_GROUP) {
        Write-Err "Confirmation did not match. Aborting cleanup."
        exit 1
    }
} else {
    Write-Warn "Force flag set - skipping confirmation."
}

# ── Step 4: Delete resource group ──────────────────────────────────────────
Write-Step "Deleting resource group '$RESOURCE_GROUP' (async)..."
try {
    az group delete `
        --name $RESOURCE_GROUP `
        --yes `
        --no-wait `
        --output none

    Write-Ok "Deletion initiated (running in background with --no-wait)."
} catch {
    Write-Err "Failed to initiate deletion: $_"
    exit 1
}

# ── Step 5: Verify deletion started ────────────────────────────────────────
Write-Step "Verifying deletion is in progress..."
Start-Sleep -Seconds 5

try {
    $rgState = az group show --name $RESOURCE_GROUP --query "properties.provisioningState" --output tsv 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Ok "Resource group state: $rgState (deletion in progress)."
    } else {
        Write-Ok "Resource group is already gone or being removed."
    }
} catch {
    Write-Ok "Resource group is being deleted."
}

# ── Step 6: Power BI cleanup reminder ─────────────────────────────────────
Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " Azure cleanup initiated!" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host " Don't forget to clean up Power BI resources manually:" -ForegroundColor Yellow
Write-Host ""
Write-Host "   1. Remove the On-Premises Data Gateway connection" -ForegroundColor White
Write-Host "      - Power BI Service > Settings > Manage gateways" -ForegroundColor Gray
Write-Host ""
Write-Host "   2. Remove the data source from the gateway" -ForegroundColor White
Write-Host "      - Gateway settings > Data sources > Remove" -ForegroundColor Gray
Write-Host ""
Write-Host "   3. Delete the dataset/report in Power BI workspace" -ForegroundColor White
Write-Host "      - Workspace > ... > Delete" -ForegroundColor Gray
Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
