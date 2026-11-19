import React, { useState, useEffect } from 'react';
import { Layers, Plus, Search, Tag, CheckCircle2 } from 'lucide-react';
import { api } from '../api/client';
import { Project } from '../types';

interface SegmentsPageProps {
  project: Project;
}

export const SegmentsPage: React.FC<SegmentsPageProps> = ({ project }) => {
  const [segments, setSegments] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newKey, setNewKey] = useState('');
  const [newName, setNewName] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [newRuleJson, setNewRuleJson] = useState('{\n  "type": "COMPOSITE",\n  "logicalOperator": "AND",\n  "children": [\n    {\n      "type": "LEAF",\n      "attribute": "country",\n      "operator": "IN",\n      "values": ["US", "CA", "GB"]\n    }\n  ]\n}');
  const [creating, setCreating] = useState(false);

  const loadSegments = async () => {
    try {
      setLoading(true);
      const data = await api.getSegments(project.id);
      setSegments(data);
    } catch (err) {
      console.error('Failed to load segments:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSegments();
  }, [project.id]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreating(true);
    try {
      await api.createSegment(project.id, {
        segmentKey: newKey.trim(),
        name: newName.trim(),
        description: newDesc.trim(),
        ruleTreeJson: newRuleJson,
      });
      setShowCreateModal(false);
      setNewKey('');
      setNewName('');
      setNewDesc('');
      await loadSegments();
    } catch (err: any) {
      alert(err.message || 'Failed to create segment');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight flex items-center space-x-2">
            <Layers className="w-6 h-6 text-cyan-400" />
            <span>Reusable Segments</span>
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Define reusable user cohorts and targeting rules (Composite AST) across all project flags.
          </p>
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center space-x-2 bg-indigo-600 hover:bg-indigo-500 text-white font-medium px-4 py-2.5 rounded-lg transition-colors shadow-lg shadow-indigo-600/30"
        >
          <Plus className="w-4 h-4" />
          <span>Create Segment</span>
        </button>
      </div>

      {loading ? (
        <div className="text-center py-20 text-slate-500 animate-pulse">Loading segments...</div>
      ) : segments.length === 0 ? (
        <div className="text-center py-16 border border-dashed border-slate-800 rounded-xl bg-slate-900/30">
          <Layers className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-white">No Segments Defined</h3>
          <p className="text-sm text-slate-400 mt-1">Create your first reusable segment rule cohort.</p>
        </div>
      ) : (
        <div className="grid gap-4">
          {segments.map((seg) => (
            <div
              key={seg.id}
              className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-3 shadow-sm"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <h3 className="text-base font-bold text-white">{seg.name}</h3>
                  <span className="font-mono text-xs px-2 py-0.5 rounded bg-slate-800 text-cyan-400 border border-slate-700">
                    {seg.segmentKey}
                  </span>
                </div>
                <span className="text-xs text-slate-500">
                  Created {new Date(seg.createdAt).toLocaleDateString()}
                </span>
              </div>

              <p className="text-sm text-slate-400">{seg.description || 'No description provided.'}</p>

              {seg.ruleTreeJson && (
                <div className="bg-slate-950 border border-slate-800 p-3 rounded-lg font-mono text-xs text-slate-300 overflow-x-auto">
                  <pre>{seg.ruleTreeJson}</pre>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Create Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <h2 className="text-lg font-bold text-white">Create Reusable Segment</h2>

            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                  Segment Key
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. beta-testers"
                  value={newKey}
                  onChange={(e) => setNewKey(e.target.value.toLowerCase().replace(/[^a-z0-9-_]/g, ''))}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                  Display Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Beta Program Users"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                  Description
                </label>
                <input
                  type="text"
                  placeholder="e.g. Early access beta testing cohort"
                  value={newDesc}
                  onChange={(e) => setNewDesc(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                  Rule Tree (Composite AST JSON)
                </label>
                <textarea
                  rows={6}
                  value={newRuleJson}
                  onChange={(e) => setNewRuleJson(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs font-mono text-emerald-400 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="flex justify-end space-x-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-sm text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="px-4 py-2 text-sm font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg transition-colors disabled:opacity-50"
                >
                  {creating ? 'Creating...' : 'Create Segment'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
