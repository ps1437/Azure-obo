import { useState } from "react";
import {
  AuthenticatedTemplate,
  UnauthenticatedTemplate,
  useMsal
} from "@azure/msal-react";
import { loginRequest } from "./authConfig";
import { getProfileUsingObo, GraphProfile } from "./api";

function App() {
  const { instance, accounts } = useMsal();
  const [profile, setProfile] = useState<GraphProfile | null>(null);
  const [backendTokenPreview, setBackendTokenPreview] = useState("");
  const [message, setMessage] = useState("");

  const account = accounts[0];

  async function login() {
    await instance.loginRedirect(loginRequest);
  }

  async function logout() {
    await instance.logoutRedirect();
  }

  async function loadProfile() {
    if (!account) {
      setMessage("Please sign in first.");
      return;
    }

    try {
      setMessage("Calling Spring Boot API. Spring Boot will call Microsoft Graph using OBO...");
      setProfile(null);

      const result = await getProfileUsingObo(instance, account);
      setProfile(result);
      setMessage("Profile loaded using Spring Boot OBO flow.");
    } catch (error: any) {
      console.error(error);
      setMessage(error?.response?.data?.message || error.message || "Failed to load profile.");
    }
  }

  async function showBackendTokenPreview() {
    if (!account) {
      setMessage("Please sign in first.");
      return;
    }

    const result = await instance.acquireTokenSilent({
      ...loginRequest,
      account
    });

    setBackendTokenPreview(`${result.accessToken.substring(0, 80)}...`);
  }

  return (
    <div className="page">
      <div className="card">
        <h1>React + Spring Boot OBO Demo</h1>
        <p className="muted">
          UI gets a token for Spring Boot. Spring Boot exchanges it using OBO and calls Microsoft Graph /me.
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

            <button onClick={loadProfile}>Get Profile Using Spring Boot OBO</button>
            <button className="secondary" onClick={showBackendTokenPreview}>
              Show Backend Token Preview
            </button>
            <button className="danger" onClick={logout}>Logout</button>
          </div>

          {backendTokenPreview && (
            <div className="section">
              <h2>Backend Token Preview</h2>
              <code>{backendTokenPreview}</code>
              <p className="muted">
                This token is for your Spring Boot API, not Microsoft Graph directly.
              </p>
            </div>
          )}

          {profile && (
            <div className="section">
              <h2>Microsoft Graph Profile</h2>
              <table>
                <tbody>
                  <tr><td>ID</td><td>{profile.id}</td></tr>
                  <tr><td>Display Name</td><td>{profile.displayName}</td></tr>
                  <tr><td>Email</td><td>{profile.mail || profile.userPrincipalName}</td></tr>
                  <tr><td>Given Name</td><td>{profile.givenName}</td></tr>
                  <tr><td>Surname</td><td>{profile.surname}</td></tr>
                  <tr><td>Job Title</td><td>{profile.jobTitle}</td></tr>
                  <tr><td>Office</td><td>{profile.officeLocation}</td></tr>
                  <tr><td>Mobile</td><td>{profile.mobilePhone}</td></tr>
                </tbody>
              </table>
            </div>
          )}

          {message && <p className="message">{message}</p>}
        </AuthenticatedTemplate>
      </div>
    </div>
  );
}

export default App;
