import { useEffect, useState } from "react";
import {
  AuthenticatedTemplate,
  UnauthenticatedTemplate,
  useMsal
} from "@azure/msal-react";
import { InteractionRequiredAuthError } from "@azure/msal-browser";
import { loginRequest } from "./authConfig";
import {
  AdoStatusResponse,
  getAdoStatus,
  getAdoWorkItems,
  getBackendAccessToken,
  getProfileUsingObo,
  GraphProfile,
  startAdoConnect,
  disconnectAdo
} from "./api";
import "./App.css";

function App() {
  const { instance, accounts } = useMsal();

  const [profile, setProfile] = useState<GraphProfile | null>(null);
  const [adoStatus, setAdoStatus] = useState<AdoStatusResponse | null>(null);
  const [adoWorkItems, setAdoWorkItems] = useState<any>(null);
  const [backendTokenPreview, setBackendTokenPreview] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const account = accounts[0];

  useEffect(() => {
    const url = new URL(window.location.href);

    if (url.pathname === "/ado-connected") {
      setMessage("Azure DevOps connected successfully. You can now load work items.");
      window.history.replaceState({}, document.title, "/");
    }
  }, []);

  async function login() {
    await instance.loginRedirect(loginRequest);
  }

  async function logout() {
    await instance.logoutRedirect();
  }

  async function requestBackendConsent() {
    if (!account) {
      setMessage("Please sign in first.");
      return;
    }

    try {
      setLoading(true);
      setMessage("Opening backend consent popup...");

      await instance.acquireTokenPopup({
        ...loginRequest,
        account,
        prompt: "consent"
      });

      setMessage("Backend access consent completed.");
    } catch (error: any) {
      console.error(error);
      setMessage(error.message || "Failed to request backend consent.");
    } finally {
      setLoading(false);
    }
  }

  async function loadProfile() {
    if (!account) {
      setMessage("Please sign in first.");
      return;
    }

    try {
      setLoading(true);
      setMessage("Calling Spring Boot API. Spring Boot will call Microsoft Graph using OBO...");
      setProfile(null);

      const result = await getProfileUsingObo(instance, account);

      setProfile(result);
      setMessage("Profile loaded using Spring Boot OBO flow.");
    } catch (error: any) {
      console.error(error);
      handleApiError(error);
    } finally {
      setLoading(false);
    }
  }

  async function showBackendTokenPreview() {
    if (!account) {
      setMessage("Please sign in first.");
      return;
    }

    try {
      setLoading(true);

      const token = await getBackendAccessToken(instance, account);

      setBackendTokenPreview(`${token.substring(0, 80)}...`);
      setMessage("Backend token loaded.");
    } catch (error: any) {
      console.error(error);
      setMessage(error.message || "Failed to get backend token.");
    } finally {
      setLoading(false);
    }
  }

  async function checkAdoStatus() {
    if (!account) {
      setMessage("Please sign in first.");
      return;
    }

    try {
      setLoading(true);
      setMessage("Checking Azure DevOps connection...");

      const result = await getAdoStatus(instance, account);

      setAdoStatus(result);

      if (result.connected) {
        setMessage("Azure DevOps is connected.");
      } else {
        setMessage("Azure DevOps is not connected. Please connect ADO.");
      }
    } catch (error: any) {
      console.error(error);
      handleApiError(error);
    } finally {
      setLoading(false);
    }
  }

  async function connectAdo() {
    if (!account) {
      setMessage("Please sign in first.");
      return;
    }

    try {
      setLoading(true);
      setMessage("Starting Azure DevOps connect flow...");

      const result = await startAdoConnect(instance, account);

      if (!result.authorizationUrl) {
        throw new Error("authorizationUrl missing from backend response.");
      }

      window.location.href = result.authorizationUrl;
    } catch (error: any) {
      console.error(error);
      handleApiError(error);
    } finally {
      setLoading(false);
    }
  }

  async function loadAdoWorkItems() {
    if (!account) {
      setMessage("Please sign in first.");
      return;
    }

    try {
      setLoading(true);
      setMessage("Loading Azure DevOps work items...");
      setAdoWorkItems(null);

      const result = await getAdoWorkItems(instance, account);

      setAdoWorkItems(result);
      setMessage("Azure DevOps work items loaded.");
    } catch (error: any) {
      console.error(error);
      handleApiError(error);
    } finally {
      setLoading(false);
    }
  }

  async function disconnectAzureDevOps() {
    if (!account) {
      setMessage("Please sign in first.");
      return;
    }

    try {
      setLoading(true);
      setMessage("Disconnecting Azure DevOps...");

      await disconnectAdo(instance, account);

      setAdoStatus({
        connected: false,
        code: "ADO_CONNECT_REQUIRED"
      });
      setAdoWorkItems(null);
      setMessage("Azure DevOps disconnected.");
    } catch (error: any) {
      console.error(error);
      handleApiError(error);
    } finally {
      setLoading(false);
    }
  }

  function handleApiError(error: any) {
    const code = error?.code || error?.response?.data?.code;
    const errorMessage =
      error?.message ||
      error?.response?.data?.message ||
      "Something went wrong.";

    if (code === "ADO_CONNECT_REQUIRED") {
      setAdoStatus({
        connected: false,
        code: "ADO_CONNECT_REQUIRED"
      });
      setMessage("Azure DevOps connection required. Please click Connect Azure DevOps.");
      return;
    }

    if (code === "ADO_RECONNECT_REQUIRED") {
      setAdoStatus({
        connected: false,
        code: "ADO_RECONNECT_REQUIRED"
      });
      setMessage("Azure DevOps connection expired. Please reconnect Azure DevOps.");
      return;
    }

    if (code === "INVALID_BACKEND_RESPONSE") {
      setMessage(errorMessage);
      return;
    }

    if (error instanceof InteractionRequiredAuthError) {
      setMessage("Consent or login required. Please grant consent.");
      return;
    }

    setMessage(errorMessage);
  }

  return (
    <div className="page">
      <div className="card">
        <h1>React + Spring Boot OBO + Azure DevOps</h1>

        <p className="muted">
          UI gets a token for Spring Boot. Spring Boot validates it, then calls Graph using OBO
          and Azure DevOps using backend-managed ADO token.
        </p>

        <UnauthenticatedTemplate>
          <div className="section">
            <p>You are not signed in.</p>
            <button onClick={login}>Sign in</button>
          </div>
        </UnauthenticatedTemplate>

        <AuthenticatedTemplate>
          <div className="section">
            <p>
              Signed in as: <b>{account?.username}</b>
            </p>

            <div className="button-row">
              <button disabled={loading} onClick={loadProfile}>
                Get Graph Profile
              </button>

              <button disabled={loading} className="secondary" onClick={requestBackendConsent}>
                Grant / Re-consent Backend Access
              </button>

              <button disabled={loading} className="secondary" onClick={showBackendTokenPreview}>
                Show Backend Token Preview
              </button>

              <button disabled={loading} className="danger" onClick={logout}>
                Logout
              </button>
            </div>
          </div>

          <div className="section">
            <h2>Azure DevOps</h2>

            <div className="button-row">
              <button disabled={loading} className="secondary" onClick={checkAdoStatus}>
                Check ADO Status
              </button>

              <button disabled={loading} onClick={connectAdo}>
                Connect Azure DevOps
              </button>

              <button disabled={loading} className="secondary" onClick={loadAdoWorkItems}>
                Load My ADO Work Items
              </button>

              <button disabled={loading} className="danger" onClick={disconnectAzureDevOps}>
                Disconnect ADO
              </button>
            </div>

            {adoStatus && (
              <div className={adoStatus.connected ? "status success" : "status warning"}>
                <b>Status:</b> {adoStatus.connected ? "Connected" : "Not Connected"}
                <br />
                <b>Code:</b> {adoStatus.code || "-"}
                <br />
                <b>ADO Email:</b> {adoStatus.adoEmail || "-"}
                <br />
                <b>Organization:</b> {adoStatus.organization || "-"}
              </div>
            )}
          </div>

          {backendTokenPreview && (
            <div className="section">
              <h2>Backend Token Preview</h2>
              <code>{backendTokenPreview}</code>
              <p className="muted">
                This token is for your Spring Boot API, not Microsoft Graph or Azure DevOps directly.
              </p>
            </div>
          )}

          {profile && (
            <div className="section">
              <h2>Microsoft Graph Profile</h2>

              <table>
                <tbody>
                  <tr>
                    <td>ID</td>
                    <td>{profile.id}</td>
                  </tr>
                  <tr>
                    <td>Display Name</td>
                    <td>{profile.displayName}</td>
                  </tr>
                  <tr>
                    <td>Email</td>
                    <td>{profile.mail || profile.userPrincipalName}</td>
                  </tr>
                  <tr>
                    <td>Given Name</td>
                    <td>{profile.givenName}</td>
                  </tr>
                  <tr>
                    <td>Surname</td>
                    <td>{profile.surname}</td>
                  </tr>
                  <tr>
                    <td>Job Title</td>
                    <td>{profile.jobTitle}</td>
                  </tr>
                  <tr>
                    <td>Office</td>
                    <td>{profile.officeLocation}</td>
                  </tr>
                  <tr>
                    <td>Mobile</td>
                    <td>{profile.mobilePhone}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          )}

          {adoWorkItems && (
            <div className="section">
              <h2>Azure DevOps Work Items Raw Response</h2>
              <pre>{JSON.stringify(adoWorkItems, null, 2)}</pre>
            </div>
          )}

          {message && (
            <div className="section">
              <p className="message">{loading ? "⏳ " : ""}{message}</p>
            </div>
          )}
        </AuthenticatedTemplate>
      </div>
    </div>
  );
}

export default App;