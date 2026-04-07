import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { RefreshCw } from 'lucide-react';
import Dashboard from './components/Dashboard';

function App() {
  const [anomalies, setAnomalies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastRefreshed, setLastRefreshed] = useState(new Date());

  const fetchAnomalies = async () => {
    try {
      const response = await axios.get('http://localhost:8082/anomalies');
      // Sort newest first
      const sorted = response.data.sort((a, b) => {
        return new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime();
      });
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
    // Initial fetch
    fetchAnomalies();

    // Setup polling every 5s
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
    <div className="min-h-screen bg-slate-900 font-sans text-slate-200 selection:bg-blue-500/30">
      {/* Optional Top Error Bar */}
      {error && (
        <div className="bg-red-500/90 text-white text-sm py-2 px-4 flex justify-center sticky top-0 z-50 shadow-md">
          <div className="flex items-center gap-2">
            <RefreshCw className="w-4 h-4 animate-spin" />
            {error}
          </div>
        </div>
      )}
      
      {/* Subtle Polling Indicator */}
      <div className="fixed bottom-4 right-4 text-xs text-slate-500 flex items-center gap-2 z-50 bg-slate-900/80 px-3 py-1.5 rounded-full backdrop-blur-sm border border-slate-800">
        <span className="relative flex h-2 w-2">
          <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${error ? 'bg-red-400' : 'bg-emerald-400'}`}></span>
          <span className={`relative inline-flex rounded-full h-2 w-2 ${error ? 'bg-red-500' : 'bg-emerald-500'}`}></span>
        </span>
        <span className="font-medium tracking-wide">Live · Updated {lastRefreshed.toLocaleTimeString()}</span>
      </div>

      <Dashboard anomalies={anomalies} />
    </div>
  );
}

export default App;
