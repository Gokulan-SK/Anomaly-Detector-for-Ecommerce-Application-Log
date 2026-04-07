import React, { useState, useMemo } from 'react';
import { AlertCircle, Activity, ShieldAlert } from 'lucide-react';
import Filters from './Filters';
import Charts from './Charts';
import AnomalyTable from './AnomalyTable';

export default function Dashboard({ anomalies }) {
  const [textFilter, setTextFilter] = useState('');
  const [severityFilter, setSeverityFilter] = useState('ALL');

  // Filter anomalies based on controls
  const filteredAnomalies = useMemo(() => {
    return anomalies.filter((a) => {
      const matchesText = textFilter === '' || 
        (a.sourceLayer || '').toLowerCase().includes(textFilter.toLowerCase()) ||
        (a.anomalyType || '').toLowerCase().includes(textFilter.toLowerCase());
      
      const matchesSeverity = severityFilter === 'ALL' || a.severity === severityFilter;
      
      return matchesText && matchesSeverity;
    });
  }, [anomalies, textFilter, severityFilter]);

  // Use raw anomalies for top-level stats context, but you can also use filteredAnomalies if you want the dashboard stats to respond to filters
  const totalCount = anomalies.length;
  const highCriticalCount = anomalies.filter(a => a.severity === 'HIGH' || a.severity === 'CRITICAL').length;
  const uniqueEndpoints = new Set(anomalies.map(a => a.sourceLayer)).size;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent inline-block">
          Anomaly Detection Dashboard
        </h1>
        <p className="text-slate-400 mt-2">Real-time monitoring of system health and anomalous behavior</p>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 flex items-center justify-between">
          <div>
            <p className="text-slate-400 text-sm font-medium mb-1">Total Anomalies</p>
            <p className="text-3xl font-bold text-slate-100">{totalCount}</p>
          </div>
          <div className="w-12 h-12 bg-blue-500/20 rounded-lg flex items-center justify-center text-blue-500">
            <Activity size={24} />
          </div>
        </div>
        
        <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 flex items-center justify-between">
          <div>
            <p className="text-slate-400 text-sm font-medium mb-1">High & Critical</p>
            <p className="text-3xl font-bold text-red-500">{highCriticalCount}</p>
          </div>
          <div className="w-12 h-12 bg-red-500/20 rounded-lg flex items-center justify-center text-red-500">
            <AlertCircle size={24} />
          </div>
        </div>

        <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 flex items-center justify-between">
          <div>
            <p className="text-slate-400 text-sm font-medium mb-1">Affected Endpoints</p>
            <p className="text-3xl font-bold text-slate-100">{uniqueEndpoints}</p>
          </div>
          <div className="w-12 h-12 bg-emerald-500/20 rounded-lg flex items-center justify-center text-emerald-500">
            <ShieldAlert size={24} />
          </div>
        </div>
      </div>

      {/* Charts Section */}
      <Charts anomalies={filteredAnomalies} />

      {/* Table Section */}
      <div className="mb-4">
        <h2 className="text-xl font-semibold text-slate-200 mb-4">Anomaly Details</h2>
        <Filters 
          textFilter={textFilter}
          setTextFilter={setTextFilter}
          severityFilter={severityFilter}
          setSeverityFilter={setSeverityFilter}
        />
        <AnomalyTable anomalies={filteredAnomalies} />
      </div>
    </div>
  );
}
