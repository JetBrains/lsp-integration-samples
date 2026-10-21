# Troubleshooting: Power BI + Azure SQL Private Link

---

## 1. DNS Resolves to Public IP Instead of Private IP

**Symptom:** `nslookup pbi-pl-demo-sql.database.windows.net` returns a public IP (e.g., `40.x.x.x`) instead of a private IP (`10.0.1.x`), even from inside the VNet.

**Likely Cause:** The Private DNS Zone `privatelink.database.windows.net` is not linked to the VNet, or the A record for the SQL server is missing.

**Fix:**
1. Go to Azure Portal → Private DNS Zone `privatelink.database.windows.net` → **Virtual network links**.
2. Verify a link to `pbi-pl-demo-vnet` exists. If not, add one.
3. Go to **Recordsets** and verify an A record for `pbi-pl-demo-sql` exists pointing to the PE's private IP.
4. If the A record is missing, check the Private Endpoint — it may need to be deleted and recreated with DNS integration enabled.

**Verify:**
```bash
nslookup pbi-pl-demo-sql.database.windows.net
# Expected: 10.0.1.x (private IP)
```

---

## 2. Private Endpoint Stuck in Pending State

**Symptom:** Private Endpoint shows connection status **Pending** instead of **Approved** in the portal.

**Likely Cause:** The Private Endpoint connection was not auto-approved. This happens when the PE is in a different subscription or tenant than the SQL server, or when the user lacks `Microsoft.Sql/servers/privateEndpointConnectionsApproval/action` permission.

**Fix:**
1. Go to Azure Portal → SQL Server `pbi-pl-demo-sql` → **Networking** → **Private access** tab.
2. Find the pending connection and click **Approve**.
3. Alternatively, if you own both resources in the same subscription, delete the PE and recreate it — same-subscription PEs are auto-approved.

**Verify:** Private Endpoint → Overview → Connection status shows **Approved**.

---

## 3. SQL Firewall Still Allowing Public Access

**Symptom:** SQL server is reachable from the public internet (e.g., SSMS connects from a laptop without VPN). This undermines the security posture.

**Likely Cause:** Public network access is still set to "Selected networks" or "All networks" instead of "Disabled". Or the "Allow Azure services and resources to access this server" exception is checked.

**Fix:**
1. Go to Azure Portal → SQL Server `pbi-pl-demo-sql` → **Networking**.
2. Set **Public network access** to **Disable**.
3. Remove all firewall rules (IP ranges).
4. Uncheck **"Allow Azure services and resources to access this server"**.
5. Click **Save**.

**Verify:** Try connecting from SSMS outside the VNet — connection should time out or be refused.

---

## 4. VNet Data Gateway Not Showing as Online

**Symptom:** The VNet Data Gateway in Power BI Service shows status **Offline**, **Error**, or does not appear at all.

**Likely Cause:**
- The gateway subnet has an NSG blocking required outbound traffic.
- The subnet has a UDR (User Defined Route) forcing traffic through a firewall that blocks Power BI control plane traffic.
- The workspace is not on Premium or PPU capacity.
- The user doesn't have the required Azure RBAC role (Contributor) on the VNet/subscription.

**Fix:**
1. Verify the workspace is assigned to a Premium or PPU capacity.
2. Check the `gateway` subnet's NSG — the VNet Data Gateway requires outbound HTTPS (443) to Power BI service endpoints. Remove overly restrictive deny rules.
3. If using a UDR/firewall, ensure Power BI service tags are allowed outbound.
4. Try deleting and recreating the gateway from Power BI Service.

**Verify:** Power BI Service → Settings → Manage gateways → VNet Data Gateways tab → status shows **Online**.

---

## 5. Gateway Can't Reach SQL Server (Connection Timeout)

**Symptom:** Data source connection test on the gateway fails with "Unable to connect" or "Connection timed out". Gateway itself is Online.

**Likely Cause:**
- NSG on the `default` subnet (where the PE lives) is blocking inbound traffic from the `gateway` subnet on port 1433.
- The Private Endpoint NIC is in a different subnet or VNet than expected.
- DNS resolution from the gateway subnet returns a public IP (see issue #1).

**Fix:**
1. Verify DNS resolution from within the VNet resolves to the private IP (see issue #1).
2. Check NSG on the `default` subnet — allow inbound TCP 1433 from `10.0.2.0/24` (gateway subnet).
3. Verify the Private Endpoint NIC is in `10.0.1.0/24` and its IP matches the DNS A record.
4. Test connectivity from a VM in the gateway subnet:
   ```powershell
   Test-NetConnection -ComputerName pbi-pl-demo-sql.database.windows.net -Port 1433
   ```

**Verify:** Data source connection test in Power BI gateway settings succeeds (green checkmark).

---

## 6. Authentication Failure (Entra ID vs SQL Auth Mismatch)

**Symptom:** Connection test or refresh fails with "Login failed for user" or "Authentication failed". Network connectivity is fine.

**Likely Cause:** The data source on the gateway is configured with SQL authentication, but the SQL server only accepts Entra ID (or vice versa). Or the credentials (username/password) are incorrect.

**Fix:**
1. Check SQL Server → **Microsoft Entra ID** blade → verify the authentication mode:
   - "SQL authentication only" → use SQL username/password on the gateway data source.
   - "Microsoft Entra authentication only" → use OAuth2/Entra on the gateway data source.
   - "Both" → either works, but must match the gateway config.
2. In Power BI Service → gateway data source → update credentials to match the SQL server's auth mode.
3. If using Entra ID, ensure the user/service principal has a login and database user created:
   ```sql
   CREATE USER [user@domain.com] FROM EXTERNAL PROVIDER;
   ALTER ROLE db_datareader ADD MEMBER [user@domain.com];
   ```

**Verify:** Gateway data source connection test succeeds. Refresh completes without auth errors.

---

## 7. Power BI Refresh Fails with Credential Error

**Symptom:** Refresh fails with "The credentials provided for the SQL source are invalid" or "Data source credentials need to be updated."

**Likely Cause:**
- Credentials expired or were rotated on the SQL server but not updated on the gateway data source.
- The dataset was republished and lost its gateway binding.
- OAuth2 token expired (for Entra ID auth).

**Fix:**
1. Go to Power BI Service → Dataset → **Settings** → **Gateway and cloud connections**.
2. Verify the dataset is still mapped to `pbi-pl-demo-vnetgw` and the correct data source.
3. Go to **Manage connections and gateways** → find the data source → click **Edit** → re-enter credentials.
4. For OAuth2: click "Edit credentials" and re-authenticate.
5. After updating credentials, trigger a manual refresh to test.

**Verify:** Refresh history shows **Completed** after re-entering credentials.

---

## 8. Power BI Refresh Fails with "Cannot Connect to Data Source"

**Symptom:** Refresh fails with "Unable to connect to the data source" or "Cannot connect to the data source. Please verify the connection information." The gateway is Online.

**Likely Cause:** This is a catch-all error. Root causes include:
- DNS resolution failure (most common — see issue #1).
- Gateway is Online but the SQL server FQDN in the data source config has a typo.
- The database name is wrong (e.g., `ContosoRetail` vs `contoso-retail`).
- The Private Endpoint was deleted or connection was revoked.
- SQL server was paused (if using serverless tier).

**Fix:** Work through this checklist in order:
1. **Verify FQDN** — data source must use exactly `pbi-pl-demo-sql.database.windows.net`.
2. **Verify database name** — must be exactly `ContosoRetail`.
3. **Verify DNS** — from a VM in the VNet, run `nslookup pbi-pl-demo-sql.database.windows.net` and confirm private IP.
4. **Verify PE status** — Private Endpoint connection status must be **Approved**.
5. **Verify SQL server is running** — if serverless, it may be paused. Access the Query Editor in the portal to wake it.
6. **Verify credentials** — test the data source connection from the gateway settings page.
7. **Check gateway logs** — Power BI Service → gateway → **Diagnostics** for detailed error messages.

**Verify:** Fix each item and re-test. Trigger a manual refresh — refresh history should show **Completed**.
