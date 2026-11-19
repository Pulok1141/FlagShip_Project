import React, { useState, useEffect } from 'react';
import {
  BarChart3,
  AlertTriangle,
  CheckCircle2,
  TrendingUp,
  Percent,
  Clock,
  ArrowUpRight,
  Info,
} from 'lucide-react';
import { api } from '../api/client';
import { ExperimentReport, Flag, Project } from '../types';

interface ExperimentsPageProps {
  project: Project;
  currentEnv: string;
}

export const ExperimentsPage: React.FC<ExperimentsPageProps> = ({
  project,
  currentEnv,
}) => {
  const [flags, setFlags] = useState<Flag[]>([]);
  const [selectedFlagKey, setSelectedFlagKey] = useState<string>('');
  const [report, setReport] = useState<ExperimentReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [days, setDays] = useState(30);

  useEffect(() => {
    const loadFlags = async () => {
      try {
        const data = await api.getFlags(project.id);
        setFlags(data);
        if (data.length > 0) {
          setSelectedFlagKey(data[0].flagKey);
        }
      } catch (err) {
        console.error('Failed to load flags:', err);
      }
    };
    loadFlags();
  }, [project.id]);

  useEffect(() => {
    if (!selectedFlagKey) return;

    const loadAnalytics = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await api.getExperimentAnalytics(project.id, currentEnv, selectedFlagKey, undefined, days);
        setReport(res);
      } catch (err: any) {
        setError(err.message || 'Failed to load experiment analytics');
      } finally {
        setLoading(false);
      }
    };

    loadAnalytics();
  }, [project.id, currentEnv, selectedFlagKey, days]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight flex items-center space-x-2">
            <BarChart3 className="w-6 h-6 text-emerald-400" />
            <span>A/B Experiment Analytics</span>
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Frequentist statistics engine (FR8): Conversion rates, Chi-square Sample-Ratio Mismatch (SRM) checks, and Two-proportion Z-tests.
          </p>
        </div>

        {/* Flag Selector & Window */}
        <div className="flex items-center space-x-3">
          <select
            value={selectedFlagKey}
            onChange={(e) => setSelectedFlagKey(e.target.value)}
            className="bg-slate-900 border border-slate-800 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            {flags.map((f) => (
              <option key={f.id} value={f.flagKey}>
                {f.name} ({f.flagKey})
              </option>
            ))}
          </select>

          <select
            value={days}
            onChange={(e) => setDays(Number(e.target.value))}
            className="bg-slate-900 border border-slate-800 text-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="7">Last 7 Days</option>
            <option value="14">Last 14 Days</option>
            <option value="30">Last 30 Days</option>
            <option value="90">Last 90 Days</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="text-center py-20 text-slate-500 animate-pulse">Computing experiment statistics...</div>
      ) : error ? (
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-8 text-center text-slate-400">
          <Info className="w-8 h-8 text-slate-500 mx-auto mb-2" />
          <p>{error}</p>
        </div>
      ) : !report ? null : (
        <div className="space-y-6">
          {/* SRM Alert Banner */}
          {report.srmAlert ? (
            <div className="bg-rose-500/10 border border-rose-500/20 rounded-xl p-4 flex items-start space-x-3 text-rose-300">
              <AlertTriangle className="w-5 h-5 text-rose-400 flex-shrink-0 mt-0.5" />
              <div>
                <h4 className="font-bold text-sm text-rose-200">Sample Ratio Mismatch (SRM) Detected!</h4>
                <p className="text-xs text-rose-400/90 mt-1">
                  The observed traffic ratio deviates significantly from the expected allocation (Chi-square ={' '}
                  {report.srmChiSquare.toFixed(2)}, p-value = {report.srmPValue.toFixed(5)} &lt; 0.001). Results may be invalid due to uneven sampling or bucketing errors.
                </p>
              </div>
            </div>
          ) : (
            <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-4 flex items-center justify-between text-emerald-300">
              <div className="flex items-center space-x-2.5">
                <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                <span className="text-sm font-medium">
                  SRM Check Passed (Chi-square = {report.srmChiSquare.toFixed(2)}, p-value = {report.srmPValue.toFixed(4)} &ge; 0.001). Traffic distribution is balanced.
                </span>
              </div>
            </div>
          )}

          {/* Variations Table */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
            <div className="px-6 py-4 border-b border-slate-800 bg-slate-900/50 flex items-center justify-between">
              <h3 className="text-base font-bold text-white">Variation Performance & Z-Test Significance</h3>
              <span className="text-xs text-slate-400">
                Baseline Variation: <strong className="text-white font-mono">{report.baselineVariationId}</strong>
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-950 text-xs uppercase text-slate-400 tracking-wider">
                  <tr>
                    <th className="px-6 py-3">Variation</th>
                    <th className="px-6 py-3">Exposures</th>
                    <th className="px-6 py-3">Conversions</th>
                    <th className="px-6 py-3">Conversion Rate</th>
                    <th className="px-6 py-3">Z-Score</th>
                    <th className="px-6 py-3">p-value</th>
                    <th className="px-6 py-3">Statistical Significance</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {Object.entries(report.variationStats).map(([varId, stats]) => {
                    const isBaseline = varId === report.baselineVariationId;

                    return (
                      <tr key={varId} className="hover:bg-slate-800/40 transition-colors">
                        <td className="px-6 py-4">
                          <div className="font-semibold text-white">{varId}</div>
                          {isBaseline && (
                            <span className="text-xs px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">
                              Baseline
                            </span>
                          )}
                        </td>

                        <td className="px-6 py-4 text-slate-300 font-mono">
                          {stats.exposures.toLocaleString()}
                        </td>

                        <td className="px-6 py-4 text-slate-300 font-mono">
                          {stats.conversions.toLocaleString()}
                        </td>

                        <td className="px-6 py-4 font-mono font-bold text-white">
                          {(stats.conversionRate * 100).toFixed(2)}%
                        </td>

                        <td className="px-6 py-4 font-mono text-slate-300">
                          {isBaseline ? '—' : stats.zScore !== null ? stats.zScore.toFixed(3) : 'N/A'}
                        </td>

                        <td className="px-6 py-4 font-mono text-slate-300">
                          {isBaseline ? '—' : stats.pValue !== null ? stats.pValue.toFixed(4) : 'N/A'}
                        </td>

                        <td className="px-6 py-4">
                          {isBaseline ? (
                            <span className="text-xs text-slate-500">Baseline control</span>
                          ) : stats.statisticallySignificant ? (
                            <span className="inline-flex items-center space-x-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              <span>Statistically Significant (p &lt; 0.05)</span>
                            </span>
                          ) : (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-slate-800 text-slate-400 border border-slate-700">
                              Not Significant
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
