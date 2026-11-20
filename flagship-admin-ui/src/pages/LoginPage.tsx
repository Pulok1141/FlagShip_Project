import React, { useState } from 'react';
import { Flag, ShieldCheck, ArrowRight, Lock, Mail, AlertCircle, Key, Server, Sparkles, Check, Globe } from 'lucide-react';
import { api, getCustomBackendUrl, isDemoMode } from '../api/client';
import { User } from '../types';

interface LoginPageProps {
  onLoginSuccess: (user: User) => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('admin@flagship.io');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [backendUrl, setBackendUrl] = useState(getCustomBackendUrl() || '');
  const [showServerConfig, setShowServerConfig] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const isDemo = isDemoMode();

  const handleSaveBackendUrl = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    api.setApiBase(backendUrl.trim());
    setSaveSuccess(true);
    setTimeout(() => setSaveSuccess(false), 2000);
  };

  const handleResetToDemo = () => {
    setBackendUrl('');
    api.setApiBase(null);
    setSaveSuccess(true);
    setTimeout(() => setSaveSuccess(false), 2000);
  };

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

        {/* Backend Connection Pill */}
        <div className="mt-4 flex items-center justify-center">
          <button
            type="button"
            onClick={() => setShowServerConfig(!showServerConfig)}
            className="inline-flex items-center space-x-2 px-3 py-1.5 rounded-full text-xs font-medium bg-slate-900 border border-slate-800 text-slate-300 hover:border-slate-700 hover:text-white transition-all shadow-sm"
          >
            {isDemo ? (
              <>
                <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                <span>Mode: <strong className="text-amber-300">Live Interactive Demo</strong></span>
              </>
            ) : (
              <>
                <Globe className="w-3.5 h-3.5 text-emerald-400" />
                <span>Server: <strong className="text-emerald-300 font-mono truncate max-w-[200px]">{api.getApiBase()}</strong></span>
              </>
            )}
            <span className="text-[10px] text-indigo-400 ml-1 underline">Configure</span>
          </button>
        </div>
      </div>

      <div className="mt-6 sm:mx-auto sm:w-full sm:max-w-md">
        {/* Expandable Server Config Modal/Card */}
        {showServerConfig && (
          <div className="mb-4 bg-slate-900 border border-indigo-500/30 p-4 rounded-xl shadow-xl space-y-3 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-white flex items-center space-x-1.5">
                <Server className="w-4 h-4 text-indigo-400" />
                <span>Backend Server Connection</span>
              </span>
              <button
                type="button"
                onClick={() => setShowServerConfig(false)}
                className="text-xs text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed">
              By default on static hosting (Vercel), FlagShip runs in zero-setup <strong>Interactive Live Demo Mode</strong>. To connect a live Spring Boot backend (e.g. Render), paste its URL below.
            </p>
            <form onSubmit={handleSaveBackendUrl} className="space-y-2">
              <div className="relative">
                <input
                  type="text"
                  placeholder="https://flagship-server.onrender.com"
                  value={backendUrl}
                  onChange={(e) => setBackendUrl(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-white placeholder-slate-600 focus:outline-none focus:ring-1 focus:ring-indigo-500 font-mono"
                />
              </div>
              <div className="flex items-center space-x-2 pt-1">
                <button
                  type="submit"
                  className="flex-1 bg-indigo-600 hover:bg-indigo-500 text-white font-medium py-1.5 px-3 rounded-lg text-xs transition-colors flex items-center justify-center space-x-1"
                >
                  {saveSuccess ? (
                    <>
                      <Check className="w-3.5 h-3.5 text-emerald-300" />
                      <span>Saved!</span>
                    </>
                  ) : (
                    <span>Save & Connect</span>
                  )}
                </button>
                <button
                  type="button"
                  onClick={handleResetToDemo}
                  className="bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium py-1.5 px-3 rounded-lg text-xs transition-colors"
                >
                  Use Demo Mode
                </button>
              </div>
            </form>
          </div>
        )}

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
