import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const EndpointChart = ({ data }) => {
  // Convert object data to array
  const chartData = Object.entries(data || {}).map(([name, count]) => ({
    name,
    count
  })).sort((a, b) => b.count - a.count).slice(0, 10); // Show top 10

  return (
    <div className="bg-slate-800/50 border border-slate-700/50 rounded-2xl p-6 h-[400px]">
      <h3 className="text-lg font-semibold text-slate-200 mb-6 font-display">Top Affected Endpoints</h3>
      {chartData.length > 0 ? (
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} layout="vertical" margin={{ top: 10, right: 30, left: 60, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" horizontal={false} />
            <XAxis type="number" stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
            <YAxis dataKey="name" type="category" stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
            <Tooltip
              contentStyle={{ backgroundColor: '#1e293b', borderColor: '#334155', borderRadius: '0.5rem' }}
            />
            <Bar dataKey="count" fill="#8b5cf6" radius={[0, 4, 4, 0]} barSize={20} />
          </BarChart>
        </ResponsiveContainer>
      ) : (
        <div className="flex h-full items-center justify-center text-slate-500">
          No endpoint data available
        </div>
      )}
    </div>
  );
};

export default EndpointChart;
