# React + Spring Boot Azure Entra ID OBO User Profile Demo

This zip contains two projects:

```text
react-ui/       React TypeScript UI using MSAL
springboot-api/ Spring Boot API using OBO flow to call Microsoft Graph /me
```

## Flow

```text
React UI
  ↓ login with Entra ID
  ↓ gets access token for Backend API scope
Spring Boot API
  ↓ validates incoming JWT
  ↓ exchanges incoming token using OBO
Microsoft Graph
  ↓ /v1.0/me
Spring Boot returns profile to UI
```

## Azure Portal setup

You need 2 app registrations.

---

## 1. Backend API app registration

Create app registration:

```text
Name: obo-springboot-api
Account type: Single tenant
Redirect URI: leave empty
```

Copy:

```text
Tenant ID
Backend API Client ID
```

### Expose API

Go to:

```text
Expose an API
```

Set Application ID URI:

```text
api://<BACKEND_API_CLIENT_ID>
```

Add scope:

```text
Scope name: access_as_user
Who can consent: Admins and users
Admin consent display name: Access Spring Boot API
Admin consent description: Allows the app to access Spring Boot API as signed-in user
User consent display name: Access Spring Boot API
User consent description: Allows this app to access Spring Boot API as you
State: Enabled
```

Final scope:

```text
api://<BACKEND_API_CLIENT_ID>/access_as_user
```

### Add client secret

Go to:

```text
Certificates & secrets
→ New client secret
```

Copy the secret VALUE.

### Add Microsoft Graph permission to backend app

Go to:

```text
API permissions
→ Add a permission
→ Microsoft Graph
→ Delegated permissions
```

Add:

```text
User.Read
```

Then click:

```text
Grant admin consent
```

---

## 2. UI app registration

Create app registration:

```text
Name: obo-react-ui
Account type: Single tenant
Platform: Single-page application
Redirect URI: http://localhost:3000
```

Copy:

```text
UI Client ID
Tenant ID
```

### Add backend API permission to UI app

Go to UI app registration:

```text
API permissions
→ Add a permission
→ My APIs
→ Select obo-springboot-api
```

Select:

```text
access_as_user
```

Then click:

```text
Grant admin consent
```

---

## Configure React UI

Open:

```text
react-ui/src/authConfig.ts
```

Replace:

```ts
const UI_CLIENT_ID = "REPLACE_WITH_UI_CLIENT_ID";
const TENANT_ID = "REPLACE_WITH_TENANT_ID";
const BACKEND_API_CLIENT_ID = "REPLACE_WITH_BACKEND_API_CLIENT_ID";
```

Run:

```bash
cd react-ui
npm install
npm start
```

UI runs on:

```text
http://localhost:3000
```

---

## Configure Spring Boot API

Open:

```text
springboot-api/src/main/resources/application.yml
```

Replace:

```yaml
tenant-id: REPLACE_WITH_TENANT_ID
client-id: REPLACE_WITH_BACKEND_API_CLIENT_ID
client-secret: REPLACE_WITH_BACKEND_API_CLIENT_SECRET
```

Run:

```bash
cd springboot-api
mvn spring-boot:run
```

Backend runs on:

```text
http://localhost:8080
```

---

## Test

1. Start Spring Boot
2. Start React
3. Click **Sign in**
4. Click **Get Profile Using Spring Boot OBO**

You should see Microsoft Graph `/me` profile returned from Spring Boot.

## Important

UI scope must be:

```text
api://<BACKEND_API_CLIENT_ID>/access_as_user
```

Spring Boot OBO downstream scope is:

```text
https://graph.microsoft.com/User.Read
```

For Azure DevOps later, the backend downstream scope changes to:

```text
499b84ac-1321-427f-aa17-267ca6975798/.default
```
