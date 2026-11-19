import React, { useState } from 'react';
import { Flag, ShieldCheck, ArrowRight, Lock, Mail, AlertCircle, Key } from 'lucide-react';
import { api } from '../api/client';
import { User } from '../types';

interface LoginPageProps {
  onLoginSuccess: (user: User) => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('admin@flagship.io');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const data = await api.login(email.trim(), password);
      onLoginSuccess(data.user);
    } catch (err: any) {
      setError(err.message || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickLogin = (roleEmail: string, rolePass: string) => {
    setEmail(roleEmail);
    setPassword(rolePass);
    // Submit with selected credentials
    setLoading(true);
    setError(null);
    api.login(roleEmail, rolePass)
      .then((data) => onLoginSuccess(data.user))
      .catch((err) => setError(err.message || 'Quick login failed'))
      .finally(() => setLoading(false));
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="inline-flex items-center justify-center bg-gradient-to-tr from-indigo-600 to-emerald-500 p-3 rounded-2xl shadow-xl shadow-indigo-500/20 mb-4">
          <Flag className="w-8 h-8 text-white" />
        </div>
        <h2 className="text-3xl font-extrabold text-white tracking-tight">
          FlagShip Platform
        </h2>
        <p className="mt-2 text-sm text-slate-400">
          Production-grade Self-Hosted Feature Flags & Gradual Rollouts
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-slate-900 border border-slate-800 py-8 px-6 shadow-2xl rounded-2xl sm:px-10 space-y-6">
          {error && (
            <div className="bg-rose-500/10 border border-rose-500/20 rounded-xl p-3.5 text-sm text-rose-400 flex items-center space-x-2.5">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                Email Address
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center space-x-2 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold py-2.5 rounded-lg transition-colors shadow-lg shadow-indigo-600/30 disabled:opacity-50"
            >
              <span>{loading ? 'Signing in...' : 'Sign In'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* Quick Demo Logins */}
          <div className="pt-4 border-t border-slate-800">
            <span className="block text-xs font-semibold text-slate-400 uppercase tracking-wider text-center mb-3">
              1-Click Demo Logins
            </span>

            <div className="grid grid-cols-2 gap-2 text-xs">
              <button
                type="button"
                onClick={() => handleQuickLogin('admin@flagship.io', 'admin123')}
                className="p-2 rounded-lg bg-slate-950 border border-slate-800 hover:border-indigo-500/50 hover:bg-slate-800 text-slate-300 transition-all text-left"
              >
                <div className="font-bold text-white flex items-center space-x-1">
                  <ShieldCheck className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Admin</span>
                </div>
                <div className="text-[10px] text-slate-500 mt-0.5">admin@flagship.io</div>
              </button>

              <button
                type="button"
                onClick={() => handleQuickLogin('approver@flagship.io', 'approver123')}
                className="p-2 rounded-lg bg-slate-950 border border-slate-800 hover:border-amber-500/50 hover:bg-slate-800 text-slate-300 transition-all text-left"
              >
                <div className="font-bold text-white flex items-center space-x-1">
                  <Key className="w-3.5 h-3.5 text-amber-400" />
                  <span>Approver</span>
                </div>
                <div className="text-[10px] text-slate-500 mt-0.5">approver@flagship.io</div>
              </button>

              <button
                type="button"
                onClick={() => handleQuickLogin('editor@flagship.io', 'editor123')}
                className="p-2 rounded-lg bg-slate-950 border border-slate-800 hover:border-cyan-500/50 hover:bg-slate-800 text-slate-300 transition-all text-left"
              >
                <div className="font-bold text-white flex items-center space-x-1">
                  <Flag className="w-3.5 h-3.5 text-cyan-400" />
                  <span>Editor</span>
                </div>
                <div className="text-[10px] text-slate-500 mt-0.5">editor@flagship.io</div>
              </button>

              <button
                type="button"
                onClick={() => handleQuickLogin('viewer@flagship.io', 'viewer123')}
                className="p-2 rounded-lg bg-slate-950 border border-slate-800 hover:border-slate-600 hover:bg-slate-800 text-slate-300 transition-all text-left"
              >
                <div className="font-bold text-white flex items-center space-x-1">
                  <span className="text-slate-400">👁️</span>
                  <span>Viewer</span>
                </div>
                <div className="text-[10px] text-slate-500 mt-0.5">viewer@flagship.io</div>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
