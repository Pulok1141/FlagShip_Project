import React, { useState, useEffect } from 'react';
import { ShieldAlert, CheckCircle2, Trash2, Archive, UserCheck, AlertCircle } from 'lucide-react';
import { api } from '../api/client';
import { Project, StaleFlagReport } from '../types';

interface HygienePageProps {
  project: Project;
  onSelectFlagByKey?: (key: string) => void;
}

export const HygienePage: React.FC<HygienePageProps> = ({ project, onSelectFlagByKey }) => {
  const [reports, setReports] = useState<StaleFlagReport[]>([]);
  const [loading, setLoading] = useState(true);

  const loadHygiene = async () => {
    try {
      setLoading(true);
      const data = await api.getHygieneReport(project.id);
      setReports(data);
    } catch (err) {
      console.error('Failed to load hygiene report:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadHygiene();
  }, [project.id]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight flex items-center space-x-2">
            <ShieldAlert className="w-6 h-6 text-rose-400" />
            <span>Flag Hygiene & Code Debt Dashboard</span>
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Automated staleness detection (FR9): Flags inactive for &ge; 30 days or 100% rolled out for &ge; 14 days.
          </p>
        </div>

        <button
          onClick={loadHygiene}
          className="px-3.5 py-2 bg-slate-900 border border-slate-800 hover:bg-slate-800 text-slate-300 rounded-lg text-sm font-semibold transition-colors"
        >
          Refresh Report
        </button>
      </div>

      {loading ? (
        <div className="text-center py-20 text-slate-500 animate-pulse">Scanning project flags for code debt...</div>
      ) : reports.length === 0 ? (
        <div className="text-center py-16 border border-dashed border-slate-800 rounded-xl bg-slate-900/30">
          <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-white">Clean Bill of Health!</h3>
          <p className="text-sm text-slate-400 mt-1">
            Zero stale flags detected. All flags are either actively evaluated or actively undergoing gradual rollout.
          </p>
        </div>
      ) : (
        <div className="grid gap-4">
          {reports.map((report) => (
            <div
              key={report.flagId}
              className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-3 shadow-sm hover:border-slate-700 transition-colors"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-800">
                <div className="flex items-center space-x-3">
                  <span className="text-xs px-2.5 py-1 rounded-full font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                    STALE FLAG
                  </span>
                  <h3 className="text-base font-bold text-white">{report.flagName}</h3>
                  <span className="font-mono text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                    {report.flagKey}
                  </span>
                </div>

                <div className="text-xs text-slate-400 flex items-center space-x-1.5">
                  <UserCheck className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Owner: <strong className="text-slate-200">{report.ownerEmail}</strong></span>
                </div>
              </div>

              <div>
                <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">
                  Staleness Reason
                </div>
                <p className="text-sm text-rose-300/90 font-medium">{report.reason}</p>
              </div>

              <div className="bg-slate-950 p-3 rounded-lg border border-slate-800/80 flex items-center justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-0.5">
                    Recommended Cleanup
                  </div>
                  <p className="text-xs text-slate-300">{report.recommendation}</p>
                </div>

                {onSelectFlagByKey && (
                  <button
                    onClick={() => onSelectFlagByKey(report.flagKey)}
                    className="flex items-center space-x-1.5 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-indigo-400 border border-slate-700 rounded-md text-xs font-semibold transition-colors flex-shrink-0 ml-4"
                  >
                    <span>View Flag</span>
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
