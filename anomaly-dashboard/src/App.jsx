import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { RefreshCw, BarChart2, LogOut } from 'lucide-react';
import { BrowserRouter, Routes, Route, Link, useNavigate } from 'react-router-dom';
import Dashboard from './components/Dashboard';
import ReportsDashboard from './components/ReportsDashboard';
import LoginPage from './components/LoginPage';
import ProtectedRoute from './components/ProtectedRoute';
import { getCurrentUser, logout, authHeaders } from './utils/auth';

// ─── Logout Button ────────────────────────────────────────────────────────────
function LogoutButton() {
  const navigate = useNavigate();
  const user = getCurrentUser();

  const handleLogout = () => logout(navigate);

  return (
    <div className="flex items-center gap-3">
      {user && (
        <span className="text-xs text-slate-400 hidden sm:inline">
          Signed in as <span className="text-slate-200 font-medium">{user.username}</span>
        </span>
      )}
      <button
        id="logout-btn"
        onClick={handleLogout}
        className="flex items-center gap-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white
                   border border-slate-700 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
        title="Sign out"
      >
        <LogOut className="w-4 h-4" />
        Logout
      </button>
    </div>
  );
}

// ─── Dashboard Wrapper ────────────────────────────────────────────────────────
function DashboardWrapper() {
  const [anomalies, setAnomalies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastRefreshed, setLastRefreshed] = useState(new Date());

  const fetchAnomalies = async () => {
    try {
      // X-User header attached via authHeaders()
      const response = await axios.get('http://localhost:8082/anomalies', {
        headers: authHeaders(),
      });
      const sorted = response.data.sort((a, b) =>
        new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
      );
      setAnomalies(sorted);
      setError(null);
      setLastRefreshed(new Date());
    } catch (err) {
      console.error('Failed to fetch anomalies:', err);
      setError('Connection to Anomaly Service failed. Retrying...');
    } finally {
      if (loading) setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnomalies();
    const interval = setInterval(fetchAnomalies, 5000);
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center text-slate-300">
        <div className="flex flex-col items-center gap-4">
          <RefreshCw className="w-8 h-8 animate-spin text-blue-500" />
          <p className="text-lg font-medium">Loading Anomaly Dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-900 font-sans text-slate-200 selection:bg-blue-500/30 relative">
      {error && (
        <div className="bg-red-500/90 text-white text-sm py-2 px-4 flex justify-center sticky top-0 z-50 shadow-md">
          <div className="flex items-center gap-2">
            <RefreshCw className="w-4 h-4 animate-spin" />
            {error}
          </div>
        </div>
      )}

      {/* Top-right nav: Reports + Logout */}
      <div className="fixed top-4 right-4 z-50 flex items-center gap-2">
        <Link
          to="/reports"
          className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg font-medium transition-colors shadow-lg shadow-blue-500/20 text-sm"
        >
          <BarChart2 className="w-4 h-4" />
          View Reports
        </Link>
        <LogoutButton />
      </div>

      {/* Live indicator */}
      <div className="fixed bottom-4 right-4 text-xs text-slate-500 flex items-center gap-2 z-50 bg-slate-900/80 px-3 py-1.5 rounded-full backdrop-blur-sm border border-slate-800">
        <span className="relative flex h-2 w-2">
          <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${error ? 'bg-red-400' : 'bg-emerald-400'}`} />
          <span className={`relative inline-flex rounded-full h-2 w-2 ${error ? 'bg-red-500' : 'bg-emerald-500'}`} />
        </span>
        <span className="font-medium tracking-wide">Live · Updated {lastRefreshed.toLocaleTimeString()}</span>
      </div>

      <Dashboard anomalies={anomalies} />
    </div>
  );
}

// ─── Reports Wrapper ──────────────────────────────────────────────────────────
function ReportsWrapper() {
  return (
    <div className="min-h-screen bg-slate-900 font-sans text-slate-200 selection:bg-blue-500/30 relative">
      <div className="fixed top-4 right-4 z-50 flex items-center gap-2">
        <Link
          to="/"
          className="flex items-center gap-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 px-4 py-2 rounded-lg font-medium transition-colors shadow-lg text-sm"
        >
          Back to Dashboard
        </Link>
        <LogoutButton />
      </div>
      <ReportsDashboard />
    </div>
  );
}

// ─── App & Routing ────────────────────────────────────────────────────────────
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public route */}
        <Route path="/login" element={<LoginPage />} />

        {/* Protected routes */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <DashboardWrapper />
            </ProtectedRoute>
          }
        />
        <Route
          path="/reports"
          element={
            <ProtectedRoute>
              <ReportsWrapper />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}
