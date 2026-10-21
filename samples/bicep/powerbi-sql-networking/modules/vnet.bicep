// --------------------------------------------------------------------------
// Module: Virtual Network with subnets for Private Endpoints and VNet Data Gateway
// --------------------------------------------------------------------------

@description('Naming prefix for all resources.')
param prefix string

@description('Azure region for deployment.')
param location string = resourceGroup().location

@description('Address space for the VNet.')
param vnetAddressPrefix string = '10.0.0.0/16'

@description('Address prefix for the default subnet (Private Endpoints).')
param defaultSubnetPrefix string = '10.0.1.0/24'

@description('Address prefix for the gateway subnet (VNet Data Gateway).')
param gatewaySubnetPrefix string = '10.0.2.0/24'

resource vnet 'Microsoft.Network/virtualNetworks@2024-01-01' = {
  name: '${prefix}-vnet'
  location: location
  properties: {
    addressSpace: {
      addressPrefixes: [
        vnetAddressPrefix
      ]
    }
    subnets: [
      {
        name: 'default'
        properties: {
          addressPrefix: defaultSubnetPrefix
        }
      }
      {
        name: 'gateway'
        properties: {
          addressPrefix: gatewaySubnetPrefix
          delegations: [
            {
              name: 'PowerPlatformVnetAccessLinks'
              properties: {
                serviceName: 'Microsoft.PowerPlatform/vnetaccesslinks'
              }
            }
          ]
        }
      }
    ]
  }
}

@description('Resource ID of the VNet.')
output vnetId string = vnet.id

@description('Name of the VNet.')
output vnetName string = vnet.name

@description('Resource ID of the default subnet.')
output defaultSubnetId string = vnet.properties.subnets[0].id

@description('Resource ID of the gateway subnet.')
output gatewaySubnetId string = vnet.properties.subnets[1].id
