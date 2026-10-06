import axios from "axios";

const CART_API_URL = "http://localhost:8080/api/cart";
const OAUTH_CONNECT_URL =
  "http://localhost:8080/api/oauth2/authorization/swiggy-mcp";

export const cartService = {
  /**
   * Redirects the browser to initiate the Swiggy OAuth 2.1 authorization code flow.
   */
  connectSwiggy: () => {
    window.location.href = OAUTH_CONNECT_URL;
  },

  /**
   * Dispatches the structured grocery DTO to the backend Swiggy MCP orchestrator.
   * withCredentials is required so the session cookie linked to the OAuth token is sent.
   */
  syncToInstamart: async (structuredGroceryList, token) => {
    try {
      const response = await axios.post(
        `${CART_API_URL}/sync`,
        structuredGroceryList,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
          withCredentials: true,
        },
      );
      return response.data;
    } catch (error) {
      console.error("Frontend log: Failed to sync cart via Swiggy MCP.", error);
      throw error;
    }
  },
};
