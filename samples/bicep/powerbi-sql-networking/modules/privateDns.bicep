// --------------------------------------------------------------------------
// Module: Private DNS Zone for Azure SQL with VNet link and auto-registration
// --------------------------------------------------------------------------

@description('Naming prefix for all resources.')
param prefix string

@description('Resource ID of the VNet to link.')
param vnetId string

@description('Resource ID of the Private Endpoint.')
param privateEndpointId string

resource privateDnsZone 'Microsoft.Network/privateDnsZones@2024-06-01' = {
  name: 'privatelink.database.windows.net'
  location: 'global'
}

resource vnetLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2024-06-01' = {
  parent: privateDnsZone
  name: '${prefix}-dns-link'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: vnetId
    }
    registrationEnabled: false
  }
}

resource dnsZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2024-01-01' = {
  name: '${last(split(privateEndpointId, '/'))}/default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'privatelink-database-windows-net'
        properties: {
          privateDnsZoneId: privateDnsZone.id
        }
      }
    ]
  }
}

@description('Resource ID of the Private DNS Zone.')
output privateDnsZoneId string = privateDnsZone.id
