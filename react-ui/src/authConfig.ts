import { Configuration, LogLevel } from "@azure/msal-browser";

const UI_CLIENT_ID = "33c7aac9-d494-424c-8c95-f28eae06494d";
const TENANT_ID = "c46898be-6f62-415a-8c78-bf712f1a22b5";
const BACKEND_API_CLIENT_ID = "3d45a735-08fc-40e2-9eb4-0ba1a817f35b";

export const msalConfig: Configuration = {
  auth: {
    clientId: UI_CLIENT_ID,
    authority: `https://login.microsoftonline.com/${TENANT_ID}`,
    redirectUri: "http://localhost:3000",
    postLogoutRedirectUri: "http://localhost:3000"
  },
  cache: {
    cacheLocation: "sessionStorage",
    storeAuthStateInCookie: false
  },
  system: {
    loggerOptions: {
      loggerCallback: (level, message, containsPii) => {
        if (containsPii) return;

        switch (level) {
          case LogLevel.Error:
            console.error(message);
            break;
          case LogLevel.Warning:
            console.warn(message);
            break;
          case LogLevel.Info:
            console.info(message);
            break;
          case LogLevel.Verbose:
            console.debug(message);
            break;
        }
      }
    }
  }
};

export const loginRequest = {
  scopes: [`api://${BACKEND_API_CLIENT_ID}/access_as_user`]
};
