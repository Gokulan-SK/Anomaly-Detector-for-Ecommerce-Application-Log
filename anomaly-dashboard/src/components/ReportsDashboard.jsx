import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { RefreshCw, Download, AlertCircle } from 'lucide-react';
import SummaryCards from './SummaryCards';
import TrendChart from './TrendChart';
import EndpointChart from './EndpointChart';

const ReportsDashboard = () => {
  const [data, setData] = useState({
    summary: null,
    trend: [],
    endpoints: {},
    critical: []
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastRefreshed, setLastRefreshed] = useState(new Date());

  const fetchReports = async () => {
    // Visibility safeguard: skip polling if tab is inactive
    if (document.visibilityState !== 'visible') {
      return;
    }

    try {
      const [summaryRes, trendRes, endpointsRes, criticalRes] = await Promise.all([
        axios.get('http://localhost:8082/reports/summary'),
        axios.get('http://localhost:8082/reports/trend?interval=minute'),
        axios.get('http://localhost:8082/reports/endpoints'),
        axios.get('http://localhost:8082/reports/critical')
      ]);

      setData({
        summary: summaryRes.data,
        trend: trendRes.data,
        endpoints: endpointsRes.data,
        critical: criticalRes.data
      });
      setError(null);
      setLastRefreshed(new Date());
    } catch (err) {
      console.error('Failed to fetch reports:', err);
      setError('Connection to Reports Service failed. Retrying...');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReports();
    const interval = setInterval(fetchReports, 10000); // 10s isolated polling
    return () => clearInterval(interval);
  }, []);

  const handleExport = () => {
    window.location.href = 'http://localhost:8082/reports/export';
  };

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center text-slate-300 min-h-[50vh]">
        <div className="flex flex-col items-center gap-4">
          <RefreshCw className="w-8 h-8 animate-spin text-blue-500" />
          <p className="text-lg">Loading Analytics...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold font-display text-slate-100 tracking-tight">Security Analytics</h1>
          <p className="text-slate-400 mt-2">Aggregated insights and threat distribution</p>
        </div>
        <div className="flex items-center gap-4">
          <div className="text-xs text-slate-500 tracking-wide font-medium bg-slate-800/50 px-3 py-1.5 rounded-full border border-slate-700/50">
            Updated: {lastRefreshed.toLocaleTimeString()}
          </div>
          <button 
            onClick={handleExport}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-lg transition-colors font-medium text-sm shadow-lg shadow-blue-500/20"
          >
            <Download className="w-4 h-4" />
            Export JSON
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/50 text-red-400 px-4 py-3 rounded-xl flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <p>{error}</p>
        </div>
      )}

      {/* Summary layer */}
      <SummaryCards summary={data.summary} />

      {/* Charts layer */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
        <TrendChart data={data.trend} />
        <EndpointChart data={data.endpoints} />
      </div>

      {/* Critical Events Layer */}
      <div className="bg-slate-800/50 border border-slate-700/50 rounded-2xl overflow-hidden">
        <div className="px-6 py-5 border-b border-slate-700/50 border-dashed">
          <h3 className="text-lg font-semibold text-slate-200">Recent Critical Anomalies</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm whitespace-nowrap">
            <thead className="bg-slate-800/80 text-slate-400">
              <tr>
                <th className="px-6 py-4 font-medium tracking-wide">Time</th>
                <th className="px-6 py-4 font-medium tracking-wide">Type</th>
                <th className="px-6 py-4 font-medium tracking-wide">Endpoint</th>
                <th className="px-6 py-4 font-medium tracking-wide">Description</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700/50">
              {data.critical.length > 0 ? data.critical.map((item) => (
                <tr key={item.id} className="hover:bg-slate-700/30 transition-colors">
                  <td className="px-6 py-4 text-slate-300">
                    {new Date(item.createdAt).toLocaleTimeString()}
                  </td>
                  <td className="px-6 py-4">
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-500/10 text-red-400 border border-red-500/20">
                      {item.anomalyType}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-slate-300 font-mono text-xs">{item.sourceLayer}</td>
                  <td className="px-6 py-4 text-slate-400">{item.description}</td>
                </tr>
              )) : (
                <tr>
                  <td colSpan="4" className="px-6 py-8 text-center text-slate-500">
                    No critical events found recently.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default ReportsDashboard;
