// --------------------------------------------------------------------------
// Module: Azure SQL Server and Database with optional Entra-only authentication
// --------------------------------------------------------------------------

@description('Naming prefix for all resources.')
param prefix string

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

var sqlServerName = '${prefix}-sql-${uniqueString(resourceGroup().id)}'

resource sqlServer 'Microsoft.Sql/servers@2023-08-01-preview' = {
  name: sqlServerName
  location: location
  properties: {
    minimalTlsVersion: '1.2'
    publicNetworkAccess: 'Disabled'
    administrators: enableEntraAuth ? {
      administratorType: 'ActiveDirectory'
      login: entraAdminDisplayName
      sid: entraAdminObjectId
      tenantId: subscription().tenantId
      azureADOnlyAuthentication: true
      principalType: 'User'
    } : null
    administratorLogin: enableEntraAuth ? null : sqlAdminLogin
    administratorLoginPassword: enableEntraAuth ? null : sqlAdminPassword
  }
}

resource sqlDatabase 'Microsoft.Sql/servers/databases@2023-08-01-preview' = {
  parent: sqlServer
  name: 'ContosoRetail'
  location: location
  sku: {
    name: 'Basic'
    tier: 'Basic'
  }
}

@description('Resource ID of the SQL Server.')
output sqlServerId string = sqlServer.id

@description('Name of the SQL Server.')
output sqlServerName string = sqlServer.name

@description('FQDN of the SQL Server.')
output sqlServerFqdn string = sqlServer.properties.fullyQualifiedDomainName

@description('Name of the SQL Database.')
output databaseName string = sqlDatabase.name
