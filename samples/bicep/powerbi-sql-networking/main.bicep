// --------------------------------------------------------------------------
// Power BI → Azure SQL Private Link Demo
// Deploys a VNet, Azure SQL (private-only), Private Endpoint, and Private DNS Zone
// to demonstrate secure connectivity from Power BI via Private Link.
// --------------------------------------------------------------------------

metadata description = 'Power BI to Azure SQL Private Link demo infrastructure'

@description('Naming prefix for all resources.')
param prefix string = 'pbi-pl-demo'

@description('Azure region for deployment.')
param location string = resourceGroup().location

@description('SQL administrator login name.')
param sqlAdminLogin string

@secure()
@description('SQL administrator password.')
param sqlAdminPassword string

@description('Enable Microsoft Entra (Azure AD) authentication.')
param enableEntraAuth bool = true

@description('Object ID of the Entra admin user or group.')
param entraAdminObjectId string = ''

@description('Display name of the Entra admin user or group.')
param entraAdminDisplayName string = ''

@description('Address space for the VNet.')
param vnetAddressPrefix string = '10.0.0.0/16'

@description('Address prefix for the default subnet (Private Endpoints).')
param defaultSubnetPrefix string = '10.0.1.0/24'

@description('Address prefix for the gateway subnet (VNet Data Gateway).')
param gatewaySubnetPrefix string = '10.0.2.0/24'

// --- VNet ---
module vnet 'modules/vnet.bicep' = {
  name: 'deploy-vnet'
  params: {
    prefix: prefix
    location: location
    vnetAddressPrefix: vnetAddressPrefix
    defaultSubnetPrefix: defaultSubnetPrefix
    gatewaySubnetPrefix: gatewaySubnetPrefix
  }
}

// --- Azure SQL ---
module sql 'modules/sql.bicep' = {
  name: 'deploy-sql'
  params: {
    prefix: prefix
    location: location
    sqlAdminLogin: sqlAdminLogin
    sqlAdminPassword: sqlAdminPassword
    enableEntraAuth: enableEntraAuth
    entraAdminObjectId: entraAdminObjectId
    entraAdminDisplayName: entraAdminDisplayName
  }
}

// --- Private Endpoint ---
module privateEndpoint 'modules/privateEndpoint.bicep' = {
  name: 'deploy-private-endpoint'
  params: {
    prefix: prefix
    location: location
    sqlServerId: sql.outputs.sqlServerId
    subnetId: vnet.outputs.defaultSubnetId
  }
}

// --- Private DNS Zone ---
module privateDns 'modules/privateDns.bicep' = {
  name: 'deploy-private-dns'
  params: {
    prefix: prefix
    vnetId: vnet.outputs.vnetId
    privateEndpointId: privateEndpoint.outputs.privateEndpointId
  }
}

// --- Outputs ---
@description('FQDN of the Azure SQL Server.')
output sqlServerFqdn string = sql.outputs.sqlServerFqdn

@description('Name of the SQL Database.')
output sqlDatabaseName string = sql.outputs.databaseName

@description('Name of the VNet.')
output vnetName string = vnet.outputs.vnetName

@description('Name of the Private Endpoint.')
output privateEndpointName string = '${prefix}-pe-sql'

@description('Name of the Private DNS Zone.')
output privateDnsZoneName string = 'privatelink.database.windows.net'

@description('Name of the resource group.')
output resourceGroupName string = resourceGroup().name
