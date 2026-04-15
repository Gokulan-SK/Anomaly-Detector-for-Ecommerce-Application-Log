import React from 'react';
import { Navigate } from 'react-router-dom';
import { isAuthenticated } from '../utils/auth';

/**
 * Wrap any route element with this to require login.
 *
 * Usage in App.jsx:
 *   <Route path="/" element={<ProtectedRoute><DashboardWrapper /></ProtectedRoute>} />
 */
export default function ProtectedRoute({ children }) {
  if (!isAuthenticated()) {
    // Redirect to /login, replacing history so Back button doesn't loop
    return <Navigate to="/login" replace />;
  }
  return children;
}
