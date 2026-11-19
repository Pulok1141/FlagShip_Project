import React, { useState } from 'react';
import { X, Play, Clock, CheckCircle2, AlertCircle, ShieldAlert } from 'lucide-react';
import { api } from '../api/client';
import { EvaluationResult } from '../types';

interface LiveEvaluationModalProps {
  projectId: string;
  flagKey: string;
  envKey: string;
  onClose: () => void;
}

export const LiveEvaluationModal: React.FC<LiveEvaluationModalProps> = ({
  projectId,
  flagKey,
  envKey,
  onClose,
}) => {
  const [userKey, setUserKey] = useState('user-12345');
  const [attributesJson, setAttributesJson] = useState('{\n  "country": "US",\n  "tier": "enterprise",\n  "appVersion": "2.4.0"\n}');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<EvaluationResult | null>(null);

  const handleEvaluate = async () => {
    setLoading(true);
    setError(null);
    try {
      let parsedAttrs = {};
      try {
        parsedAttrs = JSON.parse(attributesJson);
      } catch (e) {
        throw new Error('Attributes JSON is invalid format');
      }

      const res = await api.evaluatePreview(projectId, flagKey, envKey, userKey, parsedAttrs);
      setResult(res);
    } catch (err: any) {
      setError(err.message || 'Evaluation failed');
    } finally {
      setLoading(false);
    }
  };

  const getReasonColor = (reason?: string) => {
    switch (reason) {
      case 'KILL_SWITCH': return 'bg-rose-500/10 text-rose-400 border-rose-500/20';
      case 'TARGET_MATCH': return 'bg-purple-500/10 text-purple-400 border-purple-500/20';
      case 'RULE_MATCH': return 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20';
      case 'PERCENTAGE_ROLLOUT': return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
      case 'DEFAULT': return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
      default: return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-2xl w-full overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/50">
          <div>
            <h2 className="text-lg font-bold text-white flex items-center space-x-2">
              <span>Live Evaluation Preview</span>
              <span className="text-xs px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 font-mono">
                {flagKey}
              </span>
              <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-300 uppercase font-mono">
                {envKey}
              </span>
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Deterministic Chain-of-Responsibility evaluation preview with MurmurHash3 bucketing (FR2, QA4).
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 space-y-4 overflow-y-auto">
          {error && (
            <div className="bg-rose-500/10 border border-rose-500/20 rounded-lg p-3 text-sm text-rose-400 flex items-center space-x-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
              User Key (context.userKey)
            </label>
            <input
              type="text"
              value={userKey}
              onChange={(e) => setUserKey(e.target.value)}
              placeholder="e.g. user-12345 or email"
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <p className="text-xs text-slate-500 mt-1">
              Used for exact user target matching and sticky percentage rollout bucketing via MurmurHash3.
            </p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
              Context Attributes (JSON)
            </label>
            <textarea
              rows={4}
              value={attributesJson}
              onChange={(e) => setAttributesJson(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-sm font-mono text-emerald-400 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <p className="text-xs text-slate-500 mt-1">
              Evaluated against segment rule conditions (SemVer, Regex, Date, Set membership).
            </p>
          </div>

          <button
            onClick={handleEvaluate}
            disabled={loading}
            className="w-full flex items-center justify-center space-x-2 bg-indigo-600 hover:bg-indigo-500 text-white font-medium py-2.5 rounded-lg transition-colors shadow-lg shadow-indigo-600/30 disabled:opacity-50"
          >
            <Play className="w-4 h-4 fill-white" />
            <span>{loading ? 'Evaluating...' : 'Run Evaluation'}</span>
          </button>

          {/* Results Output */}
          {result && (
            <div className="mt-6 border border-slate-800 rounded-xl bg-slate-950/70 p-4 space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                  Evaluation Outcome
                </span>
                <span className={`text-xs px-2.5 py-1 rounded-full font-semibold border ${getReasonColor(result.reason)}`}>
                  Pipeline Step: {result.reason}
                </span>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-xs text-slate-400">Selected Variation</div>
                  <div className="text-lg font-bold text-white mt-0.5">
                    {result.variationName || result.variationId}
                  </div>
                  <div className="text-xs text-slate-500 font-mono">ID: {result.variationId}</div>
                </div>

                <div>
                  <div className="text-xs text-slate-400">Resolved Value</div>
                  <div className="text-sm font-mono bg-slate-900 border border-slate-800 p-2 rounded text-emerald-400 mt-1 break-all">
                    {typeof result.value === 'object' ? JSON.stringify(result.value, null, 2) : String(result.value)}
                  </div>
                </div>
              </div>

              {result.matchedRuleId && (
                <div className="text-xs text-slate-400 flex items-center space-x-1.5">
                  <CheckCircle2 className="w-3.5 h-3.5 text-cyan-400" />
                  <span>Matched Segment Rule: <span className="font-mono text-slate-300">{result.matchedRuleId}</span></span>
                </div>
              )}

              <div className="text-xs text-slate-500 flex items-center space-x-1 pt-2 border-t border-slate-800">
                <Clock className="w-3.5 h-3.5" />
                <span>Evaluation duration: <strong className="text-slate-300">{(result.evaluationDurationNanos / 1000).toFixed(2)} µs</strong> (budget: ≤ 50 µs p99)</span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
