import axios from "axios";
import { API_BASE_URL } from "../config";

const CART_API_URL = `${API_BASE_URL}/cart`;
const OAUTH_CONNECT_URL =
  `${API_BASE_URL}/oauth2/authorization/swiggy-mcp`;

const authConfig = (token) => ({
  headers: { Authorization: `Bearer ${token}` },
  withCredentials: true,
});

export const cartService = {
  /**
   * Redirects the browser to initiate the Swiggy OAuth 2.1 authorization code flow.
   */
  connectSwiggy: () => {
    window.location.href = OAUTH_CONNECT_URL;
  },

  getAddresses: async (token) =>
    (await axios.get(`${CART_API_URL}/addresses`, authConfig(token))).data,
  previewCart: async (addressId, items, token) =>
    (
      await axios.post(
        `${CART_API_URL}/preview`,
        { addressId, items },
        authConfig(token),
      )
    ).data,
  syncCart: async (addressId, lines, token) =>
    (
      await axios.post(
        `${CART_API_URL}/sync`,
        { addressId, items: lines },
        authConfig(token),
      )
    ).data,

  // Fetches the current Swiggy connection status from the backend.
  // This endpoint checks if the user has an active Swiggy session and returns relevant details.
  getSwiggyStatus: async (token) => {
    const res = await axios.get(`${CART_API_URL}/swiggy-status`, {
      headers: { Authorization: `Bearer ${token}` },
      withCredentials: true,
    });
    return res.data;
  },
};
