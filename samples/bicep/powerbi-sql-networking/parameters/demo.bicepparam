using '../main.bicep'

// Naming prefix applied to all resources
param prefix = 'pbi-pl-demo'

// Azure region (e.g., eastus, westeurope)
param location = 'eastus'

// SQL administrator login name
param sqlAdminLogin = 'sqladmin'

// Replace with a strong password (min 12 chars, mixed case, numbers, symbols)
param sqlAdminPassword = 'REPLACE-WITH-STRONG-PASSWORD'

// Set to true to enable Microsoft Entra (Azure AD) authentication
param enableEntraAuth = true

// Object ID of your Entra user or security group for SQL admin
param entraAdminObjectId = 'YOUR-ENTRA-USER-OR-GROUP-OBJECT-ID'

// Display name of the Entra admin (e.g., 'SQL Admins' or 'user@contoso.com')
param entraAdminDisplayName = 'YOUR-DISPLAY-NAME'
