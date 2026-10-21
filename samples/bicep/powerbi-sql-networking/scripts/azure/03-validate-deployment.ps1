# =============================================================
# FILE: 03-validate-deployment.ps1
# PURPOSE: Validate the Power BI + SQL Private Link deployment end-to-end
#
# PREREQUISITES:
#   - Azure CLI installed
#   - Run: az login
#   - Contributor access on the target subscription
#
# HOW TO RUN:
#   .\scripts\azure\03-validate-deployment.ps1
# =============================================================

# CONFIGURATION
$SUBSCRIPTION_ID    = ""                             # Required: your Azure subscription ID
$RESOURCE_GROUP     = "rg-pbi-pl-demo"
$PRIVATE_DNS_ZONE   = "privatelink.database.windows.net"

# ---------------------------------------------------------------------------

$ErrorActionPreference = "Continue"

function Write-Step  { param($msg) Write-Host "`n>> $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "   [PASS] $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "   [WARN] $msg" -ForegroundColor Yellow }
function Write-Err   { param($msg) Write-Host "   [FAIL] $msg" -ForegroundColor Red }

if (-not $SUBSCRIPTION_ID) {
    Write-Host "   [ERROR] SUBSCRIPTION_ID is not set. Edit the CONFIGURATION section before running." -ForegroundColor Red
    exit 1
}

az account set --subscription $SUBSCRIPTION_ID 2>&1 | Out-Null

$checks = @{}

# ── Discover SQL Server name from resource group ──────────────────────────
Write-Step "Discovering SQL Server in resource group '$RESOURCE_GROUP'"
$SQL_SERVER_NAME = az sql server list `
    --resource-group $RESOURCE_GROUP `
    --query "[0].name" `
    --output tsv 2>&1

if ($LASTEXITCODE -ne 0 -or -not $SQL_SERVER_NAME) {
    Write-Host "   [WARN] No SQL Server found in resource group. Some checks will be skipped." -ForegroundColor Yellow
    $SQL_SERVER_NAME = $null
} else {
    Write-Host "   [OK] Found SQL Server: $SQL_SERVER_NAME" -ForegroundColor Green
}

# ── Check 1: Resource group exists ─────────────────────────────────────────
Write-Step "Check 1: Resource group '$RESOURCE_GROUP'"
try {
    $rg = az group show --name $RESOURCE_GROUP --output json 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Ok "Resource group exists."
        $checks["Resource Group"] = "PASS"

        Write-Host "`n   Resources in group:" -ForegroundColor White
        $resources = az resource list --resource-group $RESOURCE_GROUP --query "[].{Name:name, Type:type}" --output table 2>&1
        $resources | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }
    } else {
        Write-Err "Resource group '$RESOURCE_GROUP' not found."
        $checks["Resource Group"] = "FAIL"
    }
} catch {
    Write-Err "Error checking resource group: $_"
    $checks["Resource Group"] = "FAIL"
}

# ── Check 2: SQL Server public access disabled ────────────────────────────
Write-Step "Check 2: SQL Server public network access"
try {
    $publicAccess = az sql server show `
        --resource-group $RESOURCE_GROUP `
        --name $SQL_SERVER_NAME `
        --query "publicNetworkAccess" `
        --output tsv 2>&1

    if ($LASTEXITCODE -eq 0 -and $publicAccess -eq "Disabled") {
        Write-Ok "Public network access is Disabled."
        $checks["SQL Public Access Disabled"] = "PASS"
    } elseif ($LASTEXITCODE -eq 0) {
        Write-Err "Public network access is '$publicAccess' (expected 'Disabled')."
        $checks["SQL Public Access Disabled"] = "FAIL"
    } else {
        Write-Err "Could not query SQL server."
        $checks["SQL Public Access Disabled"] = "FAIL"
    }
} catch {
    Write-Err "Error checking SQL server: $_"
    $checks["SQL Public Access Disabled"] = "FAIL"
}

# ── Check 3: Private endpoint connection is Approved ───────────────────────
Write-Step "Check 3: Private endpoint connection state"
try {
    $peConnections = az sql server show `
        --resource-group $RESOURCE_GROUP `
        --name $SQL_SERVER_NAME `
        --query "privateEndpointConnections[].{name:name, status:properties.privateLinkServiceConnectionState.status}" `
        --output json 2>&1

    if ($LASTEXITCODE -eq 0) {
        $peList = $peConnections | ConvertFrom-Json
        if ($peList.Count -gt 0) {
            $allApproved = $true
            foreach ($pe in $peList) {
                if ($pe.status -eq "Approved") {
                    Write-Ok "Endpoint '$($pe.name)' state: Approved"
                } else {
                    Write-Err "Endpoint '$($pe.name)' state: $($pe.status)"
                    $allApproved = $false
                }
            }
            $checks["Private Endpoint Approved"] = if ($allApproved) { "PASS" } else { "FAIL" }
        } else {
            Write-Err "No private endpoint connections found on SQL server."
            $checks["Private Endpoint Approved"] = "FAIL"
        }
    } else {
        Write-Err "Could not query private endpoint connections."
        $checks["Private Endpoint Approved"] = "FAIL"
    }
} catch {
    Write-Err "Error checking private endpoints: $_"
    $checks["Private Endpoint Approved"] = "FAIL"
}

# ── Check 4: DNS resolution ───────────────────────────────────────────────
Write-Step "Check 4: DNS resolution of SQL FQDN"
if (-not $SQL_SERVER_NAME) {
    Write-Warn "Skipping DNS check — SQL Server not discovered."
    $checks["DNS Private Resolution"] = "WARN"
} else {
$sqlFqdn = "$SQL_SERVER_NAME.database.windows.net"
try {
    Write-Host "   Resolving $sqlFqdn ..." -ForegroundColor Gray
    $dnsResult = Resolve-DnsName -Name $sqlFqdn -ErrorAction Stop 2>&1

    $dnsResult | ForEach-Object {
        Write-Host "   $($_.Name) -> $($_.IPAddress)$($_.NameHost)" -ForegroundColor Gray
    }

    $privateIp = $dnsResult | Where-Object { $_.IPAddress -match "^10\." -or $_.IPAddress -match "^172\.(1[6-9]|2[0-9]|3[01])\." -or $_.IPAddress -match "^192\.168\." }
    if ($privateIp) {
        Write-Ok "DNS resolves to private IP: $($privateIp.IPAddress -join ', ')"
        $checks["DNS Private Resolution"] = "PASS"
    } else {
        Write-Warn "DNS does not resolve to a private IP from this machine (expected if running outside the VNet)."
        $checks["DNS Private Resolution"] = "WARN"
    }
} catch {
    Write-Warn "DNS resolution failed from this machine (expected if Private DNS zone is VNet-linked only)."
    $checks["DNS Private Resolution"] = "WARN"
}
} # end SQL_SERVER_NAME check

# ── Check 5: Private DNS zone records ─────────────────────────────────────
Write-Step "Check 5: Private DNS zone records"
try {
    $dnsZones = az network private-dns zone list `
        --resource-group $RESOURCE_GROUP `
        --query "[?name=='$PRIVATE_DNS_ZONE'].name" `
        --output tsv 2>&1

    if ($LASTEXITCODE -eq 0 -and $dnsZones) {
        Write-Ok "Private DNS zone '$PRIVATE_DNS_ZONE' exists."

        $records = az network private-dns record-set a list `
            --resource-group $RESOURCE_GROUP `
            --zone-name $PRIVATE_DNS_ZONE `
            --output json 2>&1

        if ($LASTEXITCODE -eq 0) {
            $recordList = $records | ConvertFrom-Json
            foreach ($rec in $recordList) {
                $ips = ($rec.aRecords | ForEach-Object { $_.ipv4Address }) -join ", "
                Write-Host "   $($rec.fqdn) -> $ips" -ForegroundColor Gray
            }
            $checks["Private DNS Zone Records"] = "PASS"
        } else {
            Write-Warn "Could not list A records in the DNS zone."
            $checks["Private DNS Zone Records"] = "WARN"
        }
    } else {
        Write-Warn "Private DNS zone '$PRIVATE_DNS_ZONE' not found in '$RESOURCE_GROUP'. It may be in a different resource group."
        $checks["Private DNS Zone Records"] = "WARN"
    }
} catch {
    Write-Warn "Error checking Private DNS zone: $_"
    $checks["Private DNS Zone Records"] = "WARN"
}

# ── Summary ────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " Validation Summary" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$passCount = 0; $failCount = 0; $warnCount = 0
foreach ($check in $checks.GetEnumerator()) {
    switch ($check.Value) {
        "PASS" { Write-Host "   [PASS] $($check.Key)" -ForegroundColor Green;  $passCount++ }
        "FAIL" { Write-Host "   [FAIL] $($check.Key)" -ForegroundColor Red;    $failCount++ }
        "WARN" { Write-Host "   [WARN] $($check.Key)" -ForegroundColor Yellow; $warnCount++ }
    }
}

Write-Host "---------------------------------------------" -ForegroundColor Gray
Write-Host "   Total: $passCount passed, $failCount failed, $warnCount warnings" -ForegroundColor White
Write-Host "=============================================" -ForegroundColor Cyan

if ($failCount -gt 0) {
    Write-Host "`n   Some checks FAILED. Review the output above and re-run after fixing." -ForegroundColor Red
    exit 1
} else {
    Write-Host "`n   All critical checks passed. Deployment looks good!" -ForegroundColor Green
}
