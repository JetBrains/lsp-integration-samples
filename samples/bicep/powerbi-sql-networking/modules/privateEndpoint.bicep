// --------------------------------------------------------------------------
// Module: Private Endpoint for Azure SQL Server
// --------------------------------------------------------------------------

@description('Naming prefix for all resources.')
param prefix string

@description('Azure region for deployment.')
param location string = resourceGroup().location

@description('Resource ID of the Azure SQL Server.')
param sqlServerId string

@description('Resource ID of the subnet for the Private Endpoint.')
param subnetId string

resource privateEndpoint 'Microsoft.Network/privateEndpoints@2024-01-01' = {
  name: '${prefix}-pe-sql'
  location: location
  properties: {
    subnet: {
      id: subnetId
    }
    privateLinkServiceConnections: [
      {
        name: '${prefix}-pe-sql-conn'
        properties: {
          privateLinkServiceId: sqlServerId
          groupIds: [
            'sqlServer'
          ]
        }
      }
    ]
  }
}

@description('Resource ID of the Private Endpoint.')
output privateEndpointId string = privateEndpoint.id
