import React, { useState } from 'react';
import axios from 'axios';
import { Send, Bot } from 'lucide-react';
import { authHeaders } from '../utils/auth';

const RagChat = () => {
  const [question, setQuestion] = useState('');
  const [response, setResponse] = useState('');
  const [loading, setLoading] = useState(false);

  const handleAsk = async () => {
    if (!question.trim()) return;

    setLoading(true);
    setResponse('');
    
    try {
      const res = await axios.post('http://localhost:8082/rag/query', 
        { question: question },
        { 
          headers: {
            'Content-Type': 'application/json',
            ...authHeaders()
          }
        }
      );
      
      setResponse(res.data.response || res.data || 'No response received.');
    } catch (error) {
      console.error('RAG query failed:', error);
      setResponse('Failed to get an answer. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mt-12 bg-slate-800 rounded-xl border border-slate-700 overflow-hidden shadow-lg animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="bg-slate-700/50 p-4 border-b border-slate-700 flex items-center gap-2">
        <Bot size={20} className="text-blue-400" />
        <h3 className="font-semibold text-slate-200">Anomaly Assistant (RAG)</h3>
      </div>
      
      <div className="p-6">
        <div className="flex gap-2 mb-4">
          <input
            type="text"
            className="flex-1 bg-slate-900 border border-slate-700 rounded-lg px-4 py-2 text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all"
            placeholder="Ask about detected anomalies..."
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleAsk()}
          />
          <button
            onClick={handleAsk}
            disabled={loading || !question.trim()}
            className="bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 disabled:text-slate-500 text-white px-4 py-2 rounded-lg font-medium transition-colors flex items-center gap-2"
          >
            {loading ? 'Thinking...' : <><Send size={18} /> Ask</>}
          </button>
        </div>

        {response && (
          <div className="bg-slate-900/50 rounded-lg p-4 border border-slate-700/50 animate-in zoom-in-95 duration-200">
            <p className="text-slate-300 leading-relaxed whitespace-pre-wrap">{response}</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default RagChat;
