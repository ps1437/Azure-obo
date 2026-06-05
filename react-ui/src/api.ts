import axios from "axios";
import {
  AccountInfo,
  InteractionRequiredAuthError,
  IPublicClientApplication
} from "@azure/msal-browser";
import { loginRequest } from "./authConfig";

const BACKEND_BASE_URL = "http://localhost:8080";

export type GraphProfile = {
  id?: string;
  displayName?: string;
  givenName?: string;
  surname?: string;
  userPrincipalName?: string;
  mail?: string;
  jobTitle?: string;
  mobilePhone?: string;
  officeLocation?: string;
};

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
      await instance.acquireTokenRedirect({
        ...loginRequest,
        account
      });

      throw new Error("Redirecting for interactive token acquisition.");
    }

    throw error;
  }
}

export async function getProfileUsingObo(
  instance: IPublicClientApplication,
  account: AccountInfo
): Promise<GraphProfile> {
  const backendToken = await getBackendAccessToken(instance, account);

  const response = await axios.get<GraphProfile>(
    `${BACKEND_BASE_URL}/api/profile/me`,
    {
      headers: {
        Authorization: `Bearer ${backendToken}`
      }
    }
  );

  return response.data;
}
