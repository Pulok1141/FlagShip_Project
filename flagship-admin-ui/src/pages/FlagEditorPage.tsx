import React, { useState, useEffect } from 'react';
import {
  ArrowLeft,
  Save,
  Play,
  RotateCcw,
  Plus,
  Trash2,
  Users,
  Percent,
  Sliders,
  History,
  AlertCircle,
  CheckCircle2,
  GitPullRequest,
} from 'lucide-react';
import { api } from '../api/client';
import { Flag, Project, Variation, TargetRule, SegmentRule, RolloutBucket, FlagVersion } from '../types';
import { LiveEvaluationModal } from '../components/LiveEvaluationModal';

interface FlagEditorPageProps {
  project: Project;
  currentEnv: string;
  flagId: string;
  onBack: () => void;
}

export const FlagEditorPage: React.FC<FlagEditorPageProps> = ({
  project,
  currentEnv,
  flagId,
  onBack,
}) => {
  const [flag, setFlag] = useState<Flag | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<'targeting' | 'versions'>('targeting');
  const [showPreview, setShowPreview] = useState(false);

  // Form states for the current environment
  const [enabled, setEnabled] = useState(false);
  const [variations, setVariations] = useState<Variation[]>([]);
  const [targets, setTargets] = useState<TargetRule[]>([]);
  const [rules, setRules] = useState<SegmentRule[]>([]);
  const [rollout, setRollout] = useState<RolloutBucket[]>([]);
  const [defaultVarId, setDefaultVarId] = useState('');
  const [offVarId, setOffVarId] = useState('');

  // Version history
  const [versions, setVersions] = useState<FlagVersion[]>([]);
  const [versionsLoading, setVersionsLoading] = useState(false);

  // Change request modal (for production approval)
  const [showCrPrompt, setShowCrPrompt] = useState(false);
  const [crReason, setCrReason] = useState('');

  const currentConfig = flag?.envConfigs ? flag.envConfigs[currentEnv] : null;

  const loadFlag = async () => {
    try {
      setLoading(true);
      const data = await api.getFlagDetail(flagId);
      setFlag(data);

      const cfg = data.envConfigs ? data.envConfigs[currentEnv] : null;
      if (cfg) {
        setEnabled(cfg.enabled);
        setVariations(cfg.variations || []);
        setTargets(cfg.targets || []);
        setRules(cfg.rules || []);
        setRollout(cfg.rollout || []);
        setDefaultVarId(cfg.defaultVariationId || (cfg.variations?.[0]?.id || ''));
        setOffVarId(cfg.offVariationId || (cfg.variations?.[0]?.id || ''));
      }
    } catch (err) {
      console.error('Failed to load flag details:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadVersions = async () => {
    if (!currentConfig) return;
    try {
      setVersionsLoading(true);
      const data = await api.getFlagVersions(currentConfig.id);
      setVersions(data);
    } catch (err) {
      console.error('Failed to load versions:', err);
    } finally {
      setVersionsLoading(false);
    }
  };

  useEffect(() => {
    loadFlag();
  }, [flagId, currentEnv]);

  useEffect(() => {
    if (activeTab === 'versions') {
      loadVersions();
    }
  }, [activeTab]);

  const handleSave = async (reason?: string) => {
    if (!currentConfig) return;
    setSaving(true);
    try {
      const payload = {
        enabled,
        variationsJson: JSON.stringify(variations),
        targetsJson: JSON.stringify(targets),
        rulesJson: JSON.stringify(rules),
        rolloutJson: JSON.stringify(rollout),
        defaultVariationId: defaultVarId,
        offVariationId: offVarId,
        reason: reason || 'Updated configuration via Admin UI',
      };

      const result = await api.updateFlagConfig(currentConfig.id, payload);

      if (result.status === 'PENDING_APPROVAL') {
        alert(`Change Request created for Production! ID: ${result.changeRequestId}. Pending second-person approval.`);
      } else {
        alert('Configuration saved and applied successfully!');
      }

      setShowCrPrompt(false);
      setCrReason('');
      await loadFlag();
    } catch (err: any) {
      alert(err.message || 'Failed to save configuration');
    } finally {
      setSaving(false);
    }
  };

  const handleSaveClick = () => {
    if (currentConfig?.requireApproval) {
      setShowCrPrompt(true);
    } else {
      handleSave();
    }
  };

  const handleRestoreVersion = async (version: number) => {
    if (!currentConfig) return;
    if (!confirm(`Are you sure you want to restore configuration to Version ${version}?`)) return;

    try {
      await api.restoreVersion(currentConfig.id, version, `Restored to version ${version} via Admin UI`);
      alert(`Restored to Version ${version} successfully!`);
      await loadFlag();
      await loadVersions();
    } catch (err: any) {
      alert(err.message || 'Failed to restore version');
    }
  };

  if (loading || !flag) {
    return <div className="text-center py-20 text-slate-500 animate-pulse">Loading flag editor...</div>;
  }

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div className="flex items-center space-x-4">
          <button
            onClick={onBack}
            className="p-2 rounded-lg bg-slate-900 border border-slate-800 hover:bg-slate-800 text-slate-300 transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <div className="flex items-center space-x-3">
              <h1 className="text-2xl font-bold text-white tracking-tight">{flag.name}</h1>
              <span className="font-mono text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                {flag.flagKey}
              </span>
              <span className="text-xs px-2 py-0.5 rounded font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                {flag.flagType}
              </span>
            </div>
            <p className="text-sm text-slate-400 mt-1">{flag.description}</p>
          </div>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={() => setShowPreview(true)}
            className="flex items-center space-x-2 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-indigo-400 border border-slate-700 rounded-lg text-sm font-semibold transition-colors"
          >
            <Play className="w-4 h-4 fill-indigo-400" />
            <span>Test Preview</span>
          </button>

          <button
            onClick={handleSaveClick}
            disabled={saving}
            className="flex items-center space-x-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-semibold transition-colors shadow-lg shadow-indigo-600/30 disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            <span>{saving ? 'Saving...' : currentConfig?.requireApproval ? 'Request Change' : 'Save Changes'}</span>
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex space-x-4 border-b border-slate-800">
        <button
          onClick={() => setActiveTab('targeting')}
          className={`flex items-center space-x-2 py-2 px-1 border-b-2 text-sm font-semibold transition-colors ${
            activeTab === 'targeting'
              ? 'border-indigo-500 text-indigo-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Sliders className="w-4 h-4" />
          <span>Targeting & Rollout ({currentEnv})</span>
        </button>

        <button
          onClick={() => setActiveTab('versions')}
          className={`flex items-center space-x-2 py-2 px-1 border-b-2 text-sm font-semibold transition-colors ${
            activeTab === 'versions'
              ? 'border-indigo-500 text-indigo-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <History className="w-4 h-4" />
          <span>Version History & Rollback</span>
        </button>
      </div>

      {activeTab === 'targeting' ? (
        <div className="space-y-6">
          {/* Section 1: Kill Switch Toggle */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 flex items-center justify-between">
            <div>
              <h3 className="text-base font-bold text-white flex items-center space-x-2">
                <span>Flag Status (Kill Switch)</span>
                {enabled ? (
                  <span className="text-xs px-2 py-0.5 rounded font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                    Active / Enabled
                  </span>
                ) : (
                  <span className="text-xs px-2 py-0.5 rounded font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                    Kill Switch Active (Disabled)
                  </span>
                )}
              </h3>
              <p className="text-xs text-slate-400 mt-1">
                Step 1 of Evaluation Pipeline: When disabled, returns the kill-switch off variation immediately.
              </p>
            </div>

            <button
              onClick={() => setEnabled(!enabled)}
              className={`relative inline-flex h-7 w-12 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                enabled ? 'bg-emerald-500' : 'bg-slate-700'
              }`}
            >
              <span
                className={`pointer-events-none inline-block h-6 w-6 transform rounded-full bg-white shadow-lg ring-0 transition duration-200 ease-in-out ${
                  enabled ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {/* Section 2: Variations */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-white">Variations</h3>
                <p className="text-xs text-slate-400">Values returned by this feature flag.</p>
              </div>

              <button
                type="button"
                onClick={() => {
                  const newId = `var-${Date.now().toString().slice(-4)}`;
                  setVariations([...variations, { id: newId, name: `Variation ${newId}`, value: true }]);
                }}
                className="flex items-center space-x-1.5 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg text-xs font-semibold"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Variation</span>
              </button>
            </div>

            <div className="space-y-3">
              {variations.map((v, idx) => (
                <div key={v.id} className="flex items-center space-x-3 bg-slate-950 p-3 rounded-lg border border-slate-800">
                  <span className="text-xs font-mono text-slate-500 w-6 text-center">{idx + 1}</span>
                  <input
                    type="text"
                    value={v.name}
                    onChange={(e) => {
                      const updated = [...variations];
                      updated[idx].name = e.target.value;
                      setVariations(updated);
                    }}
                    placeholder="Variation name"
                    className="bg-slate-900 border border-slate-700 rounded px-2.5 py-1 text-sm text-white w-48 focus:outline-none"
                  />

                  <input
                    type="text"
                    value={typeof v.value === 'object' ? JSON.stringify(v.value) : String(v.value)}
                    onChange={(e) => {
                      const updated = [...variations];
                      let val: any = e.target.value;
                      if (flag.flagType === 'BOOLEAN') val = e.target.value === 'true';
                      if (flag.flagType === 'NUMBER') val = Number(e.target.value) || 0;
                      updated[idx].value = val;
                      setVariations(updated);
                    }}
                    placeholder="Value"
                    className="bg-slate-900 border border-slate-700 rounded px-2.5 py-1 text-sm font-mono text-emerald-400 flex-1 focus:outline-none"
                  />

                  <div className="flex items-center space-x-2 text-xs">
                    <label className="flex items-center space-x-1 cursor-pointer">
                      <input
                        type="radio"
                        name="defaultVar"
                        checked={defaultVarId === v.id}
                        onChange={() => setDefaultVarId(v.id)}
                        className="text-indigo-600"
                      />
                      <span className="text-slate-400">Default</span>
                    </label>

                    <label className="flex items-center space-x-1 cursor-pointer">
                      <input
                        type="radio"
                        name="offVar"
                        checked={offVarId === v.id}
                        onChange={() => setOffVarId(v.id)}
                        className="text-rose-600"
                      />
                      <span className="text-slate-400">Off / Kill</span>
                    </label>
                  </div>

                  {variations.length > 2 && (
                    <button
                      type="button"
                      onClick={() => setVariations(variations.filter((x) => x.id !== v.id))}
                      className="text-slate-500 hover:text-rose-400 p-1"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Section 3: Individual User Targets */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-white flex items-center space-x-2">
                  <Users className="w-4 h-4 text-purple-400" />
                  <span>Step 2: Individual User Targets</span>
                </h3>
                <p className="text-xs text-slate-400">Exact match on context.userKey directly delivers variation.</p>
              </div>

              <button
                type="button"
                onClick={() => {
                  if (variations.length > 0) {
                    setTargets([...targets, { variationId: variations[0].id, userKeys: [] }]);
                  }
                }}
                className="flex items-center space-x-1.5 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg text-xs font-semibold"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Target Rule</span>
              </button>
            </div>

            {targets.length === 0 ? (
              <div className="text-xs text-slate-500 italic py-2">No individual user targets defined.</div>
            ) : (
              <div className="space-y-3">
                {targets.map((t, idx) => (
                  <div key={idx} className="bg-slate-950 p-3 rounded-lg border border-slate-800 flex items-center space-x-3">
                    <span className="text-xs text-slate-400">Deliver</span>
                    <select
                      value={t.variationId}
                      onChange={(e) => {
                        const updated = [...targets];
                        updated[idx].variationId = e.target.value;
                        setTargets(updated);
                      }}
                      className="bg-slate-900 border border-slate-700 text-white rounded px-2 py-1 text-sm"
                    >
                      {variations.map((v) => (
                        <option key={v.id} value={v.id}>
                          {v.name}
                        </option>
                      ))}
                    </select>
                    <span className="text-xs text-slate-400">to userKeys:</span>
                    <input
                      type="text"
                      placeholder="comma-separated user keys (e.g. user-1, user-2)"
                      value={t.userKeys.join(', ')}
                      onChange={(e) => {
                        const updated = [...targets];
                        updated[idx].userKeys = e.target.value
                          .split(',')
                          .map((s) => s.trim())
                          .filter(Boolean);
                        setTargets(updated);
                      }}
                      className="bg-slate-900 border border-slate-700 rounded px-2.5 py-1 text-sm font-mono text-white flex-1 focus:outline-none"
                    />
                    <button
                      type="button"
                      onClick={() => setTargets(targets.filter((_, i) => i !== idx))}
                      className="text-slate-500 hover:text-rose-400 p-1"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Section 4: Percentage Rollout */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-white flex items-center space-x-2">
                  <Percent className="w-4 h-4 text-emerald-400" />
                  <span>Step 4: Sticky Percentage Rollout (FR3, MurmurHash3)</span>
                </h3>
                <p className="text-xs text-slate-400">
                  Deterministic bucketing [0, 9999]. Ramping preserves 100% of previously enrolled users.
                </p>
              </div>
            </div>

            {/* Visual allocation bar */}
            <div className="h-4 w-full bg-slate-950 rounded-full flex overflow-hidden border border-slate-800">
              {rollout.map((b, idx) => {
                const colors = ['bg-emerald-500', 'bg-indigo-500', 'bg-amber-500', 'bg-purple-500'];
                const color = colors[idx % colors.length];
                return (
                  <div
                    key={b.variationId}
                    style={{ width: `${b.weight}%` }}
                    className={`${color} h-full transition-all`}
                    title={`${b.variationId}: ${b.weight}%`}
                  />
                );
              })}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {variations.map((v) => {
                const existing = rollout.find((r) => r.variationId === v.id);
                const weight = existing ? existing.weight : 0;

                return (
                  <div key={v.id} className="bg-slate-950 p-3 rounded-lg border border-slate-800 space-y-2">
                    <div className="flex justify-between items-center text-sm">
                      <span className="font-semibold text-white">{v.name}</span>
                      <span className="font-mono text-emerald-400 font-bold">{weight}%</span>
                    </div>

                    <input
                      type="range"
                      min="0"
                      max="100"
                      step="5"
                      value={weight}
                      onChange={(e) => {
                        const newWeight = parseInt(e.target.value);
                        const otherVariations = rollout.filter((r) => r.variationId !== v.id);
                        setRollout([...otherVariations, { variationId: v.id, weight: newWeight }]);
                      }}
                      className="w-full accent-emerald-500"
                    />
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      ) : (
        /* Version History Tab */
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-800">
            <div>
              <h3 className="text-base font-bold text-white">Version History & 1-Click Rollback (FR7)</h3>
              <p className="text-xs text-slate-400">Full audit trail with JSON diffs and instant restore.</p>
            </div>

            <button
              onClick={loadVersions}
              className="text-xs font-semibold px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg transition-colors"
            >
              Refresh
            </button>
          </div>

          {versionsLoading ? (
            <div className="py-10 text-center text-slate-500 animate-pulse">Loading version history...</div>
          ) : versions.length === 0 ? (
            <div className="py-10 text-center text-slate-500">No version history found.</div>
          ) : (
            <div className="space-y-4">
              {versions.map((ver) => (
                <div key={ver.id} className="bg-slate-950 border border-slate-800 rounded-lg p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <span className="font-mono text-sm px-2.5 py-0.5 rounded bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 font-bold">
                        v{ver.version}
                      </span>
                      <span className="text-sm font-medium text-white">{ver.reason}</span>
                    </div>

                    <button
                      onClick={() => handleRestoreVersion(ver.version)}
                      className="flex items-center space-x-1.5 px-3 py-1 text-xs font-semibold bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/20 rounded-md transition-colors"
                    >
                      <RotateCcw className="w-3.5 h-3.5" />
                      <span>Restore to v{ver.version}</span>
                    </button>
                  </div>

                  <div className="flex items-center space-x-4 text-xs text-slate-400">
                    <span>Author: {ver.authorEmail}</span>
                    <span>•</span>
                    <span>Created: {new Date(ver.createdAt).toLocaleString()}</span>
                  </div>

                  {ver.diffJson && ver.diffJson !== '{}' && (
                    <div className="mt-2 bg-slate-900 border border-slate-800 p-2.5 rounded font-mono text-xs text-slate-300 overflow-x-auto">
                      <pre>{ver.diffJson}</pre>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Production Change Request Prompt Modal */}
      {showCrPrompt && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center space-x-3 text-amber-400">
              <GitPullRequest className="w-6 h-6" />
              <h3 className="text-lg font-bold text-white">Production Change Request</h3>
            </div>

            <p className="text-sm text-slate-400">
              This environment enforces the <strong>Four-Eyes Principle</strong>. Your change will be submitted
              as a Change Request and requires approval from another team member before applying to Production.
            </p>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                Reason for Change (Required)
              </label>
              <textarea
                rows={3}
                required
                placeholder="Describe why this change is necessary..."
                value={crReason}
                onChange={(e) => setCrReason(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div className="flex justify-end space-x-3 pt-3 border-t border-slate-800">
              <button
                type="button"
                onClick={() => setShowCrPrompt(false)}
                className="px-4 py-2 text-sm text-slate-400 hover:text-white"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={!crReason.trim()}
                onClick={() => handleSave(crReason.trim())}
                className="px-4 py-2 text-sm font-semibold bg-amber-600 hover:bg-amber-500 text-white rounded-lg transition-colors disabled:opacity-50"
              >
                Submit Change Request
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Live Preview Modal */}
      {showPreview && (
        <LiveEvaluationModal
          projectId={project.id}
          flagKey={flag.flagKey}
          envKey={currentEnv}
          onClose={() => setShowPreview(false)}
        />
      )}
    </div>
  );
};
