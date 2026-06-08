import { AccountInfo, IPublicClientApplication, InteractionRequiredAuthError } from "@azure/msal-browser";
import { loginRequest } from "./authConfig";

export const API_BASE_URL = "http://localhost:8080";

export interface GraphProfile {
  id: string;
  displayName: string;
  mail?: string;
  userPrincipalName?: string;
  givenName?: string;
  surname?: string;
  jobTitle?: string;
  officeLocation?: string;
  mobilePhone?: string;
}

export interface AdoStatusResponse {
  connected: boolean;
  code?: string;
  adoEmail?: string;
  organization?: string;
}

export interface AdoConnectResponse {
  authorizationUrl: string;
}

export interface ApiErrorResponse {
  code?: string;
  message?: string;
}

export async function getBackendAccessToken(
  instance: IPublicClientApplication,
  account: AccountInfo
): Promise<string> {
  try {
    const result = await instance.acquireTokenSilent({
      ...loginRequest,
      account
    });

    return result.accessToken;
  } catch (error) {
    if (error instanceof InteractionRequiredAuthError) {
      const result = await instance.acquireTokenPopup({
        ...loginRequest,
        account,
        prompt: "consent"
      });

      return result.accessToken;
    }

    throw error;
  }
}

async function parseResponse<T>(response: Response): Promise<T> {
  const contentType = response.headers.get("content-type") || "";

  if (!contentType.includes("application/json")) {
    const text = await response.text();

    console.error("Expected JSON but received:", text);

    throw {
      code: "INVALID_BACKEND_RESPONSE",
      message:
        "Backend did not return JSON. Check API URL, backend server, CORS, or proxy configuration."
    };
  }

  const data = await response.json();

  if (!response.ok) {
    throw data;
  }

  return data as T;
}

export async function apiGet<T>(
  path: string,
  instance: IPublicClientApplication,
  account: AccountInfo
): Promise<T> {
  const token = await getBackendAccessToken(instance, account);

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json"
    }
  });

  return parseResponse<T>(response);
}

export async function apiDelete<T>(
  path: string,
  instance: IPublicClientApplication,
  account: AccountInfo
): Promise<T> {
  const token = await getBackendAccessToken(instance, account);

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json"
    }
  });

  return parseResponse<T>(response);
}

export async function getProfileUsingObo(
  instance: IPublicClientApplication,
  account: AccountInfo
): Promise<GraphProfile> {
  return apiGet<GraphProfile>("/api/profile/me", instance, account);
}

export async function getAdoStatus(
  instance: IPublicClientApplication,
  account: AccountInfo
): Promise<AdoStatusResponse> {
  return apiGet<AdoStatusResponse>("/api/ado/status", instance, account);
}

export async function startAdoConnect(
  instance: IPublicClientApplication,
  account: AccountInfo
): Promise<AdoConnectResponse> {
  return apiGet<AdoConnectResponse>("/api/ado/connect", instance, account);
}

export async function getAdoWorkItems(
  instance: IPublicClientApplication,
  account: AccountInfo
): Promise<any> {
  return apiGet<any>("/api/ado/workitems", instance, account);
}

export async function disconnectAdo(
  instance: IPublicClientApplication,
  account: AccountInfo
): Promise<any> {
  return apiDelete<any>("/api/ado/connection", instance, account);
}