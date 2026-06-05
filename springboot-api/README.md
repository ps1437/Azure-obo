# Spring Boot Backend API - Azure Entra ID OBO Flow

This backend API receives an access token from the React UI, validates it, then uses the **On-Behalf-Of flow** to get a Microsoft Graph token and call `/me`.

## Architecture

```text
React UI
  ↓
Gets access token for Spring Boot API
  ↓
Calls Spring Boot API with Authorization header
  ↓
Spring Boot validates token
  ↓
Spring Boot uses OBO flow
  ↓
Microsoft Graph /me
  ↓
Returns user profile to React UI
````

---

## Required Azure App Registrations

You need two app registrations:

```text
1. UI App Registration
2. Backend API App Registration
```

This README is for the **Backend API App Registration** and Spring Boot API.

---

# 1. Backend API App Registration

Go to:

```text
Azure Portal
→ Microsoft Entra ID
→ App registrations
→ New registration
```

Use:

```text
Name: syscho-api
Supported account types: Single tenant
Redirect URI: Leave empty
```

After creating, copy:

```text
Application / Client ID
Directory / Tenant ID
```

---

# 2. Expose Backend API

Open your backend app registration:

```text
App registrations
→ syscho-api
→ Expose an API
```

Set **Application ID URI**:

```text
api://<BACKEND_API_CLIENT_ID>
```

Example:

```text
api://3d45a735-08fc-40e2-9eb4-0ba1a817f35b
```

Then click:

```text
Add a scope
```

Add:

```text
Scope name: access_as_user
Who can consent: Admins and users
Admin consent display name: Access Spring Boot API
Admin consent description: Allows the app to access Spring Boot API as the signed-in user.
User consent display name: Access Spring Boot API
User consent description: Allows this app to access Spring Boot API as you.
State: Enabled
```

Final scope:

```text
api://<BACKEND_API_CLIENT_ID>/access_as_user
```

React UI must request this scope.

---

# 3. Create Backend Client Secret

Open backend app registration:

```text
Certificates & secrets
→ Client secrets
→ New client secret
```

Copy the **Value** immediately.

You need this in Spring Boot:

```yaml
client-secret: <BACKEND_API_CLIENT_SECRET>
```

Do not use secret ID. Use the secret **Value**.

---

# 4. Add Microsoft Graph Permission

Open backend app registration:

```text
API permissions
→ Add a permission
→ Microsoft Graph
→ Delegated permissions
```

Search and add:

```text
User.Read
```

Then click:

```text
Grant admin consent
```

This is required because Spring Boot calls:

```text
https://graph.microsoft.com/v1.0/me
```

using OBO flow.

---

# 5. Spring Boot Configuration

Open:

```text
src/main/resources/application.yml
```

Use:

```yaml
server:
  port: 8080

app:
  cors:
    allowed-origin: http://localhost:3000

  azure:
    tenant-id: <TENANT_ID>
    client-id: <BACKEND_API_CLIENT_ID>
    client-secret: <BACKEND_API_CLIENT_SECRET>

    # Match the aud claim in the token.
    # Usually this is:
    audience: api://<BACKEND_API_CLIENT_ID>

    # Microsoft Graph delegated scope for OBO
    graph-scope: https://graph.microsoft.com/User.Read

logging:
  level:
    org.springframework.security: DEBUG
```

Example:

```yaml
app:
  azure:
    tenant-id: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
    client-id: 3d45a735-08fc-40e2-9eb4-0ba1a817f35b
    client-secret: your-secret-value
    audience: api://3d45a735-08fc-40e2-9eb4-0ba1a817f35b
    graph-scope: https://graph.microsoft.com/User.Read
```

---

# 6. Run Backend API

From backend project folder:

```bash
mvn clean spring-boot:run
```

Backend starts on:

```text
http://localhost:8080
```

---

# 7. Backend Endpoints

## Health Check

No token required.

```http
GET http://localhost:8080/api/health
```

Response:

```json
{
  "status": "UP"
}
```

---

## Token Debug

Requires token from React UI.

```http
GET http://localhost:8080/api/token/debug
Authorization: Bearer <TOKEN_FROM_REACT_UI>
```

This shows token claims like:

```json
{
  "subject": "...",
  "issuer": "...",
  "audience": ["api://..."],
  "scopes": "access_as_user",
  "name": "...",
  "preferredUsername": "..."
}
```

---

## Get User Profile Using OBO

Requires token from React UI.

```http
GET http://localhost:8080/api/profile/me
Authorization: Bearer <TOKEN_FROM_REACT_UI>
```

Spring Boot will:

```text
1. Validate incoming token
2. Use incoming token as assertion
3. Request Graph token using OBO
4. Call Microsoft Graph /me
5. Return profile
```

Response example:

```json
{
  "id": "123",
  "displayName": "Test User",
  "givenName": "Test",
  "surname": "User",
  "userPrincipalName": "testuser@tenant.onmicrosoft.com",
  "mail": "testuser@tenant.onmicrosoft.com",
  "jobTitle": null,
  "mobilePhone": null,
  "officeLocation": null
}
```

---

# 8. Common Errors

## Error: `iss claim is not valid`

Cause:

```text
Token issuer does not match backend validation config.
```

Fix:

Use custom JWT decoder that accepts both:

```text
https://login.microsoftonline.com/<TENANT_ID>/v2.0
https://sts.windows.net/<TENANT_ID>/
```

Also verify the token using:

```text
https://jwt.ms
```

Check:

```text
iss
aud
scp
ver
```

---

## Error: `AADSTS65001 consent_required`

Cause:

```text
Backend API app does not have consent for Microsoft Graph User.Read.
```

Fix:

```text
Backend API app registration
→ API permissions
→ Microsoft Graph
→ Delegated permissions
→ User.Read
→ Grant admin consent
```

---

## Error: `invalid audience`

Cause:

```text
The aud claim in token does not match application.yml audience.
```

Fix:

Paste token in:

```text
https://jwt.ms
```

Check `aud`.

If token has:

```text
aud: api://<BACKEND_API_CLIENT_ID>
```

use:

```yaml
audience: api://<BACKEND_API_CLIENT_ID>
```

If token has:

```text
aud: <BACKEND_API_CLIENT_ID>
```

use:

```yaml
audience: <BACKEND_API_CLIENT_ID>
```

---

# 9. React UI Must Send This Header

React should call backend like this:

```http
GET /api/profile/me
Authorization: Bearer <ACCESS_TOKEN_FOR_BACKEND_API>
```

React scope must be:

```ts
scopes: ["api://<BACKEND_API_CLIENT_ID>/access_as_user"]
```

Do not send Microsoft Graph token from React.

---

# 10. Final Checklist

Backend API app registration:

```text
✅ Expose an API
✅ Application ID URI: api://<BACKEND_API_CLIENT_ID>
✅ Scope: access_as_user
✅ Client secret created
✅ Microsoft Graph delegated permission: User.Read
✅ Admin consent granted
```

Spring Boot:

```text
✅ tenant-id is correct
✅ client-id is backend API client ID
✅ client-secret is backend API secret value
✅ audience matches token aud
✅ graph-scope is https://graph.microsoft.com/User.Read
✅ CORS allows http://localhost:3000
```

React UI:

```text
✅ Uses UI app client ID
✅ Requests backend API scope
✅ Sends Authorization Bearer token to backend
```
