import React, { useState, useEffect } from 'react';
import {
  GitPullRequest,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  ShieldCheck,
  FileDiff,
} from 'lucide-react';
import { api } from '../api/client';
import { ChangeRequest, Project, User } from '../types';

interface ChangeRequestsPageProps {
  project: Project;
  user: User | null;
}

export const ChangeRequestsPage: React.FC<ChangeRequestsPageProps> = ({
  project,
  user,
}) => {
  const [requests, setRequests] = useState<ChangeRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCr, setSelectedCr] = useState<ChangeRequest | null>(null);
  const [actionReason, setActionReason] = useState('');
  const [processing, setProcessing] = useState(false);

  const loadRequests = async () => {
    try {
      setLoading(true);
      const data = await api.getChangeRequests(project.id);
      setRequests(data);
    } catch (err) {
      console.error('Failed to load change requests:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
  }, [project.id]);

  const handleApprove = async (cr: ChangeRequest) => {
    if (!confirm('Approve and immediately apply this Change Request to Production?')) return;
    setProcessing(true);
    try {
      await api.approveChangeRequest(cr.id, actionReason || 'Approved via Admin UI');
      alert('Change Request APPROVED and applied to Production successfully!');
      setSelectedCr(null);
      setActionReason('');
      await loadRequests();
    } catch (err: any) {
      alert(err.message || 'Approval failed');
    } finally {
      setProcessing(false);
    }
  };

  const handleReject = async (cr: ChangeRequest) => {
    const reason = prompt('Please enter a reason for rejecting this change request:');
    if (!reason) return;
    setProcessing(true);
    try {
      await api.rejectChangeRequest(cr.id, reason);
      alert('Change Request REJECTED.');
      setSelectedCr(null);
      await loadRequests();
    } catch (err: any) {
      alert(err.message || 'Rejection failed');
    } finally {
      setProcessing(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING_APPROVAL':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
      case 'APPROVED':
      case 'APPLIED':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
      case 'REJECTED':
        return 'bg-rose-500/10 text-rose-400 border-rose-500/20';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white tracking-tight flex items-center space-x-2">
          <GitPullRequest className="w-6 h-6 text-amber-400" />
          <span>Change Requests & Approvals</span>
        </h1>
        <p className="text-sm text-slate-400 mt-1">
          Four-Eyes Principle Governance (FR7): Production environment mutations require second-person review.
        </p>
      </div>

      {loading ? (
        <div className="text-center py-20 text-slate-500 animate-pulse">Loading change requests...</div>
      ) : requests.length === 0 ? (
        <div className="text-center py-16 border border-dashed border-slate-800 rounded-xl bg-slate-900/30">
          <ShieldCheck className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-white">No Change Requests</h3>
          <p className="text-sm text-slate-400 mt-1">All production changes are up-to-date and approved.</p>
        </div>
      ) : (
        <div className="grid gap-4">
          {requests.map((cr) => {
            const isAuthor = user?.email?.toLowerCase() === cr.authorEmail?.toLowerCase();
            const isPending = cr.status === 'PENDING_APPROVAL';

            return (
              <div
                key={cr.id}
                className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4 shadow-sm"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-800">
                  <div className="flex items-center space-x-3">
                    <span className={`text-xs px-2.5 py-1 rounded-full font-bold border ${getStatusBadge(cr.status)}`}>
                      {cr.status}
                    </span>
                    <span className="text-sm font-bold text-white font-mono">{cr.id}</span>
                    <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-amber-400 uppercase font-mono">
                      {cr.environmentKey}
                    </span>
                  </div>

                  <div className="flex items-center space-x-4 text-xs text-slate-400">
                    <span>Author: <strong className="text-slate-200">{cr.authorEmail}</strong></span>
                    <span>•</span>
                    <span>{new Date(cr.createdAt).toLocaleString()}</span>
                  </div>
                </div>

                <div>
                  <div className="text-sm text-white font-medium">Reason for change:</div>
                  <p className="text-sm text-slate-300 mt-0.5 italic bg-slate-950 p-2.5 rounded border border-slate-800/80">
                    "{cr.reason}"
                  </p>
                </div>

                {/* Diff View */}
                {cr.diffJson && cr.diffJson !== '{}' && (
                  <div>
                    <div className="text-xs font-semibold text-slate-400 flex items-center space-x-1.5 mb-1.5">
                      <FileDiff className="w-3.5 h-3.5 text-indigo-400" />
                      <span>Configuration JSON Diff</span>
                    </div>
                    <pre className="bg-slate-950 border border-slate-800 text-xs font-mono text-indigo-300 p-3 rounded-lg overflow-x-auto max-h-48">
                      {cr.diffJson}
                    </pre>
                  </div>
                )}

                {/* Approver & Resolution Details */}
                {cr.approverEmail && (
                  <div className="text-xs text-slate-400 flex items-center space-x-2 pt-2 border-t border-slate-800">
                    <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                    <span>Approved & Applied by <strong>{cr.approverEmail}</strong> at {new Date(cr.resolvedAt || '').toLocaleString()}</span>
                  </div>
                )}

                {cr.rejectionReason && (
                  <div className="text-xs text-rose-400 flex items-center space-x-2 pt-2 border-t border-slate-800">
                    <XCircle className="w-4 h-4 text-rose-400" />
                    <span>Rejected: {cr.rejectionReason}</span>
                  </div>
                )}

                {/* Actions for Pending Requests */}
                {isPending && (
                  <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-3 border-t border-slate-800">
                    {isAuthor ? (
                      <div className="flex items-center space-x-2 text-xs text-amber-400 bg-amber-500/10 px-3 py-1.5 rounded-lg border border-amber-500/20">
                        <AlertTriangle className="w-4 h-4 flex-shrink-0" />
                        <span>Four-Eyes Principle: You authored this request and cannot self-approve it.</span>
                      </div>
                    ) : (
                      <div className="text-xs text-slate-400">
                        Second-person approval authorized. Review the diff above before approving.
                      </div>
                    )}

                    <div className="flex items-center space-x-3 w-full sm:w-auto justify-end">
                      <button
                        onClick={() => handleReject(cr)}
                        disabled={processing}
                        className="px-3.5 py-1.5 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/20 rounded-lg text-xs font-semibold transition-colors disabled:opacity-50"
                      >
                        Reject
                      </button>

                      <button
                        onClick={() => handleApprove(cr)}
                        disabled={isAuthor || processing}
                        className="flex items-center space-x-1.5 px-4 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold transition-colors shadow-md shadow-emerald-600/30 disabled:opacity-40 disabled:cursor-not-allowed"
                        title={isAuthor ? 'Author cannot self-approve' : 'Approve and apply to Production'}
                      >
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>Approve & Apply</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
