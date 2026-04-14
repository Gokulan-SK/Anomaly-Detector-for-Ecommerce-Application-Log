import React from 'react';
import { format } from 'date-fns';

const severityColors = {
  LOW: 'bg-slate-500/20 text-slate-400',
  MEDIUM: 'bg-yellow-500/20 text-yellow-500',
  HIGH: 'bg-orange-500/20 text-orange-500',
  CRITICAL: 'bg-red-500/20 text-red-500',
};

export default function AnomalyTable({ anomalies }) {
  if (anomalies.length === 0) {
    return (
      <div className="bg-slate-800 rounded-xl border border-slate-700 p-8 text-center text-slate-400">
        No anomalies found matching your criteria.
      </div>
    );
  }

  return (
    <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-800/50 text-slate-400 border-b border-slate-700">
            <tr>
              <th className="px-6 py-4 font-medium">Timestamp</th>
              <th className="px-6 py-4 font-medium">Type</th>
              <th className="px-6 py-4 font-medium">Endpoint</th>
              <th className="px-6 py-4 font-medium">Severity</th>
              <th className="px-6 py-4 font-medium">Impact</th>
              <th className="px-6 py-4 font-medium">Score</th>
              <th className="px-6 py-4 font-medium">Description</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-700">
            {anomalies.map((a) => {
              const impactMapping = {
                CRITICAL: 'High User Impact',
                HIGH: 'Significant Impact',
                MEDIUM: 'Moderate Impact',
                LOW: 'Low Impact'
              };
              const impactText = impactMapping[a.severity] || 'Unknown Impact';

              return (
                <tr key={a.id || Math.random().toString()} className={`hover:bg-slate-700/50 transition-colors ${a.severity === 'CRITICAL' ? 'bg-red-900/10' : ''}`}>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {format(new Date(a.timestamp || new Date()), 'MMM dd HH:mm:ss')}
                  </td>
                  <td className="px-6 py-4">{a.anomalyType}</td>
                  <td className="px-6 py-4 font-mono text-xs text-slate-400">{a.sourceLayer}</td>
                  <td className="px-6 py-4">
                    <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${severityColors[a.severity] || severityColors.LOW}`}>
                      {a.severity}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-xs font-medium text-slate-300">
                    {impactText}
                  </td>
                  <td className="px-6 py-4 font-mono">{a.score}</td>
                  <td className="px-6 py-4 truncate max-w-xs" title={a.description}>{a.description}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
