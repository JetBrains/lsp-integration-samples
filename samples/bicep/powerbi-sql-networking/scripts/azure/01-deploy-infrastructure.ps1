# =============================================================
# FILE: 01-deploy-infrastructure.ps1
# PURPOSE: Deploy Power BI + Azure SQL Private Link demo infrastructure via Bicep
#
# PREREQUISITES:
#   - Azure CLI installed
#   - Run: az login
#   - Contributor access on the target subscription
#
# HOW TO RUN:
#   .\scripts\azure\01-deploy-infrastructure.ps1
# =============================================================

# CONFIGURATION
$SUBSCRIPTION_ID       = ""                          # Required: your Azure subscription ID
$RESOURCE_GROUP        = "rg-pbi-pl-demo"
$LOCATION              = "eastus"
$PREFIX                = "pbi-pl-demo"
$SQL_ADMIN_LOGIN       = "sqladmin"
$SQL_ADMIN_PASSWORD    = "CHANGE_ME_P@ssw0rd!"       # Change this before running
$ENABLE_ENTRA_AUTH     = $true
$ENTRA_ADMIN_OBJECT_ID   = ""                        # Microsoft Entra admin object ID
$ENTRA_ADMIN_DISPLAY_NAME = ""                       # Microsoft Entra admin display name

# ---------------------------------------------------------------------------

$ErrorActionPreference = "Stop"

function Write-Step  { param($msg) Write-Host "`n>> $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "   [OK] $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "   [WARN] $msg" -ForegroundColor Yellow }
function Write-Err   { param($msg) Write-Host "   [ERROR] $msg" -ForegroundColor Red }

# ── Pre-flight checks ──────────────────────────────────────────────────────
if (-not $SUBSCRIPTION_ID) {
    Write-Err "SUBSCRIPTION_ID is not set. Edit the CONFIGURATION section before running."
    exit 1
}

if ($SQL_ADMIN_PASSWORD -eq "CHANGE_ME_P@ssw0rd!") {
    Write-Warn "SQL_ADMIN_PASSWORD is still the placeholder value. Change it before deploying to production."
}

# ── Step 1: Set subscription ───────────────────────────────────────────────
Write-Step "Setting active subscription to $SUBSCRIPTION_ID"
try {
    az account set --subscription $SUBSCRIPTION_ID 2>&1 | Out-Null
    Write-Ok "Subscription set."
} catch {
    Write-Err "Failed to set subscription: $_"
    exit 1
}

# ── Step 2: Create resource group ──────────────────────────────────────────
Write-Step "Creating resource group '$RESOURCE_GROUP' in '$LOCATION'"
try {
    $rgResult = az group create --name $RESOURCE_GROUP --location $LOCATION | ConvertFrom-Json
    Write-Ok "Resource group '$($rgResult.name)' ready (provisioning state: $($rgResult.properties.provisioningState))."
} catch {
    Write-Err "Failed to create resource group: $_"
    exit 1
}

# ── Step 3: Deploy Bicep template ─────────────────────────────────────────
Write-Step "Deploying Bicep template (this may take several minutes)..."

$repoRoot = Split-Path -Path $PSScriptRoot -Parent | Split-Path -Parent
$templateFile = Join-Path $repoRoot "main.bicep"
$paramFile    = Join-Path $repoRoot "parameters" "demo.bicepparam"

if (-not (Test-Path $templateFile)) {
    Write-Err "Bicep template not found at '$templateFile'."
    exit 1
}

$deployArgs = @(
    "deployment", "group", "create",
    "--resource-group", $RESOURCE_GROUP,
    "--template-file", $templateFile,
    "--name", "deploy-$PREFIX-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
)

if (Test-Path $paramFile) {
    $deployArgs += @("--parameters", $paramFile)
    Write-Ok "Using parameter file: $paramFile"
} else {
    Write-Warn "Parameter file not found at '$paramFile'. Deploying with inline overrides only."
}

# Inline overrides from configuration variables
$deployArgs += @(
    "--parameters",
    "prefix=$PREFIX",
    "sqlAdminLogin=$SQL_ADMIN_LOGIN",
    "sqlAdminPassword=$SQL_ADMIN_PASSWORD",
    "location=$LOCATION"
)

if ($ENABLE_ENTRA_AUTH -and $ENTRA_ADMIN_OBJECT_ID) {
    $deployArgs += @(
        "enableEntraAuth=true",
        "entraAdminObjectId=$ENTRA_ADMIN_OBJECT_ID",
        "entraAdminDisplayName=$ENTRA_ADMIN_DISPLAY_NAME"
    )
}

try {
    $deployOutput = (& az @deployArgs 2>&1) -join "`n"

    if ($LASTEXITCODE -ne 0) {
        Write-Err "Deployment failed:`n$deployOutput"
        exit 1
    }

    $deployment = $deployOutput | ConvertFrom-Json
    Write-Ok "Deployment '$($deployment.name)' succeeded (state: $($deployment.properties.provisioningState))."
} catch {
    Write-Err "Deployment error: $_"
    exit 1
}

# ── Step 4: Capture and display outputs ────────────────────────────────────
Write-Step "Deployment outputs:"

if ($deployment.properties.outputs) {
    $outputs = $deployment.properties.outputs
    $outputs.PSObject.Properties | ForEach-Object {
        Write-Host "   $($_.Name) = $($_.Value.value)" -ForegroundColor White
    }
} else {
    Write-Warn "No outputs returned from the deployment."
}

# ── Step 5: Next steps ────────────────────────────────────────────────────
Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " Deployment complete - Next steps:" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " 1. Run 02-configure-sql-network.ps1 to verify and lock down SQL networking" -ForegroundColor White
Write-Host " 2. Run 03-validate-deployment.ps1 to confirm everything is working" -ForegroundColor White
Write-Host " 3. Configure the Power BI On-Premises Data Gateway" -ForegroundColor White
Write-Host " 4. Create a data source in Power BI pointing to the SQL private endpoint" -ForegroundColor White
Write-Host "=============================================" -ForegroundColor Cyan
