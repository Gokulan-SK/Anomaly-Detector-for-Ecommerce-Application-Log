import React, { useMemo } from 'react';
import { format } from 'date-fns';
import { 
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend
} from 'recharts';

const COLORS = {
  LOW: '#64748b',    // slate-500
  MEDIUM: '#eab308', // yellow-500
  HIGH: '#f97316',   // orange-500
  CRITICAL: '#ef4444' // red-500
};

export default function Charts({ anomalies }) {
  const lineData = useMemo(() => {
    // Group anomalies by minute for a clean trend line
    const grouped = {};
    anomalies.forEach((a) => {
      // Use HH:mm format for grouping, which is useful for recent trend
      const timeLabel = format(new Date(a.timestamp || new Date()), 'HH:mm');
      grouped[timeLabel] = (grouped[timeLabel] || 0) + 1;
    });
    
    // Sort chronologically using keys
    const sortedKeys = Object.keys(grouped).sort();
    return sortedKeys.map(key => ({
      time: key,
      count: grouped[key]
    }));
  }, [anomalies]);

  const pieData = useMemo(() => {
    const counts = { LOW: 0, MEDIUM: 0, HIGH: 0, CRITICAL: 0 };
    anomalies.forEach(a => {
      if (counts[a.severity] !== undefined) {
        counts[a.severity]++;
      } else {
        counts.LOW++;
      }
    });

    return [
      { name: 'CRITICAL', value: counts.CRITICAL },
      { name: 'HIGH', value: counts.HIGH },
      { name: 'MEDIUM', value: counts.MEDIUM },
      { name: 'LOW', value: counts.LOW }
    ].filter(d => d.value > 0);
  }, [anomalies]);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
      {/* Line Chart */}
      <div className="lg:col-span-2 bg-slate-800 p-6 rounded-xl border border-slate-700 shadow-sm">
        <h3 className="text-lg font-medium text-slate-200 mb-6">Anomaly Trend</h3>
        <div className="h-64">
          {lineData.length > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={lineData} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" vertical={false} />
                <XAxis dataKey="time" stroke="#94a3b8" tick={{fill: '#94a3b8', fontSize: 12}} tickLine={false} axisLine={false} />
                <YAxis stroke="#94a3b8" tick={{fill: '#94a3b8', fontSize: 12}} tickLine={false} axisLine={false} allowDecimals={false} />
                <RechartsTooltip 
                  contentStyle={{ backgroundColor: '#1e293b', borderColor: '#334155', color: '#f8fafc', borderRadius: '0.5rem' }}
                  itemStyle={{ color: '#38bdf8' }}
                />
                <Line type="monotone" dataKey="count" name="Anomalies" stroke="#38bdf8" strokeWidth={3} dot={{ fill: '#0ea5e9', r: 4, strokeWidth: 0 }} activeDot={{ r: 6 }} />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex h-full items-center justify-center text-slate-400">No trend data available</div>
          )}
        </div>
      </div>

      {/* Pie Chart */}
      <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 shadow-sm">
        <h3 className="text-lg font-medium text-slate-200 mb-6">Severity Distribution</h3>
        <div className="h-64">
          {pieData.length > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                  stroke="none"
                >
                  {pieData.map((entry, index) => (
                     <Cell key={`cell-${index}`} fill={COLORS[entry.name]} />
                  ))}
                </Pie>
                <RechartsTooltip 
                  contentStyle={{ backgroundColor: '#1e293b', borderColor: '#334155', color: '#f8fafc', borderRadius: '0.5rem' }}
                  itemStyle={{ color: '#fff' }}
                />
                <Legend verticalAlign="bottom" height={36} wrapperStyle={{ paddingTop: '20px' }} />
              </PieChart>
            </ResponsiveContainer>
          ) : (
             <div className="flex h-full items-center justify-center text-slate-400">No distribution data</div>
          )}
        </div>
      </div>
    </div>
  );
}
