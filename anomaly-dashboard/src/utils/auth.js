// ─── Auth Utility ────────────────────────────────────────────────────────────
// Minimal localStorage-based auth helpers. No external libraries.

const USER_KEY = "user";

/** Return the stored user object, or null if not logged in. */
export function getCurrentUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

/** True if a user is stored in localStorage. */
export function isAuthenticated() {
  return getCurrentUser() !== null;
}

/** Persist user after successful login. */
export function storeUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

/** Clear session and hard-redirect to /login. */
export function logout() {
  localStorage.removeItem(USER_KEY);
  window.location.replace("/login");
}

/**
 * Returns an Axios-compatible headers object that includes X-User.
 * Usage:  axios.get(url, { headers: authHeaders() })
 */
export function authHeaders() {
  const user = getCurrentUser();
  return user ? { "X-User": user.username } : {};
}
