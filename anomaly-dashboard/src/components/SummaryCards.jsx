import React from 'react';
import { Activity, AlertTriangle, ShieldAlert, Target } from 'lucide-react';

const SummaryCards = ({ summary }) => {
  if (!summary) return null;

  const cards = [
    {
      title: 'Total Anomalies',
      value: summary.totalAnomalies || 0,
      icon: <Activity className="w-8 h-8 text-blue-400" />,
      color: 'bg-blue-500/10 border-blue-500/20'
    },
    {
      title: 'Critical Alerts',
      value: (summary.severityCounts && summary.severityCounts['CRITICAL']) || 0,
      icon: <ShieldAlert className="w-8 h-8 text-red-500" />,
      color: 'bg-red-500/10 border-red-500/20'
    },
    {
      title: 'High Severity',
      value: (summary.severityCounts && summary.severityCounts['HIGH']) || 0,
      icon: <AlertTriangle className="w-8 h-8 text-orange-400" />,
      color: 'bg-orange-500/10 border-orange-500/20'
    },
    {
      title: 'Affected Endpoints',
      value: summary.uniqueEndpoints || 0,
      icon: <Target className="w-8 h-8 text-purple-400" />,
      color: 'bg-purple-500/10 border-purple-500/20'
    }
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      {cards.map((card, idx) => (
        <div key={idx} className={`p-6 rounded-2xl border ${card.color} backdrop-blur-sm flex items-center justify-between`}>
          <div>
            <p className="text-slate-400 text-sm font-medium mb-1">{card.title}</p>
            <h3 className="text-3xl font-bold text-slate-100">{card.value}</h3>
          </div>
          <div className="p-3 bg-slate-800/50 rounded-xl">
            {card.icon}
          </div>
        </div>
      ))}
    </div>
  );
};

export default SummaryCards;
