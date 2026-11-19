import React, { useState, useEffect } from 'react';
import {
  Plus,
  Search,
  Filter,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Play,
  Settings,
  Layers,
  ChevronRight,
  ShieldCheck,
  Tag,
} from 'lucide-react';
import { api } from '../api/client';
import { Flag, Project, FlagType } from '../types';
import { LiveEvaluationModal } from '../components/LiveEvaluationModal';

interface FlagsPageProps {
  project: Project;
  currentEnv: string;
  onSelectFlag: (flagId: string) => void;
}

export const FlagsPage: React.FC<FlagsPageProps> = ({
  project,
  currentEnv,
  onSelectFlag,
}) => {
  const [flags, setFlags] = useState<Flag[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [activePreviewFlag, setActivePreviewFlag] = useState<string | null>(null);

  // New Flag Modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newKey, setNewKey] = useState('');
  const [newName, setNewName] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [newType, setNewType] = useState<FlagType>('BOOLEAN');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const loadFlags = async () => {
    try {
      setLoading(true);
      const data = await api.getFlags(project.id);
      setFlags(data);
    } catch (err) {
      console.error('Failed to load flags:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFlags();
  }, [project.id]);

  const handleToggle = async (e: React.MouseEvent, flag: Flag) => {
    e.stopPropagation();
    const config = flag.envConfigs ? flag.envConfigs[currentEnv] : null;
    if (!config) return;

    try {
      await api.toggleFlag(config.id, !config.enabled, config.enabled ? 'Trip kill switch' : 'Enable flag');
      await loadFlags();
    } catch (err: any) {
      alert(err.message || 'Failed to toggle flag');
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreating(true);
    setCreateError(null);
    try {
      await api.createFlag(project.id, {
        flagKey: newKey.trim(),
        name: newName.trim(),
        description: newDesc.trim(),
        flagType: newType,
      });
      setShowCreateModal(false);
      setNewKey('');
      setNewName('');
      setNewDesc('');
      await loadFlags();
    } catch (err: any) {
      setCreateError(err.message || 'Failed to create flag');
    } finally {
      setCreating(false);
    }
  };

  const filteredFlags = flags.filter((f) => {
    const matchesSearch =
      f.flagKey.toLowerCase().includes(search.toLowerCase()) ||
      f.name.toLowerCase().includes(search.toLowerCase()) ||
      f.description.toLowerCase().includes(search.toLowerCase());
    const matchesType = typeFilter === 'ALL' || f.flagType === typeFilter;
    return matchesSearch && matchesType;
  });

  return (
    <div className="space-y-6">
      {/* Top action bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Feature Flags</h1>
          <p className="text-sm text-slate-400 mt-1">
            Managing flags in <span className="font-semibold text-slate-200">{project.name}</span> for{' '}
            <span className="capitalize font-mono px-2 py-0.5 rounded bg-slate-800 text-indigo-400 border border-slate-700">
              {currentEnv}
            </span>
          </p>
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center space-x-2 bg-indigo-600 hover:bg-indigo-500 text-white font-medium px-4 py-2.5 rounded-lg transition-colors shadow-lg shadow-indigo-600/30"
        >
          <Plus className="w-4 h-4" />
          <span>Create Feature Flag</span>
        </button>
      </div>

      {/* Filters & Search */}
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Search flags by key, name, or description..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full bg-slate-900 border border-slate-800 rounded-lg pl-9 pr-4 py-2 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        <div className="flex items-center space-x-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="bg-slate-900 text-slate-300 border border-slate-800 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="ALL">All Types</option>
            <option value="BOOLEAN">Boolean</option>
            <option value="STRING">String</option>
            <option value="NUMBER">Number</option>
            <option value="JSON">JSON</option>
          </select>
        </div>
      </div>

      {/* Flag List */}
      {loading ? (
        <div className="text-center py-16 text-slate-500 animate-pulse">Loading feature flags...</div>
      ) : filteredFlags.length === 0 ? (
        <div className="text-center py-16 border border-dashed border-slate-800 rounded-xl bg-slate-900/30">
          <div className="mx-auto w-12 h-12 rounded-full bg-slate-800 flex items-center justify-center text-slate-400 mb-3">
            <Plus className="w-6 h-6" />
          </div>
          <h3 className="text-base font-semibold text-white">No feature flags found</h3>
          <p className="text-sm text-slate-400 mt-1">Get started by creating your first feature flag.</p>
        </div>
      ) : (
        <div className="grid gap-4">
          {filteredFlags.map((flag) => {
            const config = flag.envConfigs ? flag.envConfigs[currentEnv] : null;
            const isEnabled = config ? config.enabled : false;

            return (
              <div
                key={flag.id}
                onClick={() => onSelectFlag(flag.id)}
                className="bg-slate-900/80 hover:bg-slate-900 border border-slate-800 hover:border-slate-700 rounded-xl p-5 transition-all cursor-pointer shadow-sm group"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-start space-x-4">
                    {/* Kill-switch toggle */}
                    <button
                      onClick={(e) => handleToggle(e, flag)}
                      title={isEnabled ? 'Click to trip Kill Switch (Disable flag)' : 'Click to Enable flag'}
                      className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                        isEnabled ? 'bg-emerald-500' : 'bg-slate-700'
                      }`}
                    >
                      <span
                        className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-lg ring-0 transition duration-200 ease-in-out ${
                          isEnabled ? 'translate-x-5' : 'translate-x-0'
                        }`}
                      />
                    </button>

                    <div>
                      <div className="flex items-center space-x-2.5">
                        <h3 className="text-base font-semibold text-white group-hover:text-indigo-400 transition-colors">
                          {flag.name}
                        </h3>
                        <span className="font-mono text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                          {flag.flagKey}
                        </span>
                        <span className="text-xs px-2 py-0.5 rounded font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                          {flag.flagType}
                        </span>
                      </div>

                      <p className="text-sm text-slate-400 mt-1 line-clamp-1">
                        {flag.description || 'No description provided.'}
                      </p>

                      <div className="flex items-center space-x-4 mt-2 text-xs text-slate-500">
                        <span>Owner: {flag.ownerEmail}</span>
                        <span>•</span>
                        <span>Variations: {config?.variations?.length || 2}</span>
                        {flag.lastEvaluatedAt && (
                          <>
                            <span>•</span>
                            <span>Last evaluated: {new Date(flag.lastEvaluatedAt).toLocaleDateString()}</span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center space-x-3">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setActivePreviewFlag(flag.flagKey);
                      }}
                      className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-indigo-400 border border-slate-700 transition-colors"
                      title="Run Live Evaluation Preview"
                    >
                      <Play className="w-3.5 h-3.5 fill-indigo-400" />
                      <span>Live Preview</span>
                    </button>

                    <ChevronRight className="w-5 h-5 text-slate-600 group-hover:text-slate-400 transition-colors" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create Flag Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-lg w-full p-6 shadow-2xl">
            <h2 className="text-lg font-bold text-white mb-4">Create New Feature Flag</h2>

            {createError && (
              <div className="mb-4 bg-rose-500/10 border border-rose-500/20 rounded-lg p-3 text-sm text-rose-400">
                {createError}
              </div>
            )}

            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                  Flag Key (Unique identifier)
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. new-checkout-flow"
                  value={newKey}
                  onChange={(e) => setNewKey(e.target.value.toLowerCase().replace(/[^a-z0-9-_]/g, ''))}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white font-mono focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                  Display Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Modern Checkout Flow"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                  Value Type
                </label>
                <select
                  value={newType}
                  onChange={(e) => setNewType(e.target.value as FlagType)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                >
                  <option value="BOOLEAN">Boolean (true / false)</option>
                  <option value="STRING">String</option>
                  <option value="NUMBER">Number</option>
                  <option value="JSON">JSON Object</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                  Description
                </label>
                <textarea
                  rows={3}
                  placeholder="Explain why this flag exists and its rollout plan..."
                  value={newDesc}
                  onChange={(e) => setNewDesc(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div className="flex justify-end space-x-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-sm font-medium text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="px-4 py-2 text-sm font-medium bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg transition-colors disabled:opacity-50"
                >
                  {creating ? 'Creating...' : 'Create Flag'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Live Evaluation Preview Modal */}
      {activePreviewFlag && (
        <LiveEvaluationModal
          projectId={project.id}
          flagKey={activePreviewFlag}
          envKey={currentEnv}
          onClose={() => setActivePreviewFlag(null)}
        />
      )}
    </div>
  );
};
