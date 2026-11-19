import React from 'react';
import { Flag, GitPullRequest, BarChart3, ShieldAlert, Layers, LogOut, CheckCircle2 } from 'lucide-react';
import { Project, User } from '../types';

interface NavbarProps {
  user: User | null;
  projects: Project[];
  currentProject: Project | null;
  onSelectProject: (p: Project) => void;
  currentEnv: string;
  onSelectEnv: (env: string) => void;
  currentPage: string;
  onNavigate: (page: string) => void;
  onLogout: () => void;
  pendingCrCount: number;
}

export const Navbar: React.FC<NavbarProps> = ({
  user,
  projects,
  currentProject,
  onSelectProject,
  currentEnv,
  onSelectEnv,
  currentPage,
  onNavigate,
  onLogout,
  pendingCrCount,
}) => {
  const envs = ['development', 'staging', 'production'];

  return (
    <header className="bg-slate-900 border-b border-slate-800 sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Project Selection */}
          <div className="flex items-center space-x-6">
            <div
              className="flex items-center space-x-2.5 cursor-pointer"
              onClick={() => onNavigate('flags')}
            >
              <div className="bg-gradient-to-tr from-indigo-600 to-emerald-500 p-2 rounded-lg text-white shadow-md shadow-indigo-500/20">
                <Flag className="w-5 h-5" />
              </div>
              <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-white via-slate-100 to-slate-400 bg-clip-text text-transparent">
                FlagShip
              </span>
            </div>

            {/* Project Switcher */}
            {projects.length > 0 && currentProject && (
              <div className="relative">
                <select
                  value={currentProject.id}
                  onChange={(e) => {
                    const found = projects.find((p) => p.id === e.target.value);
                    if (found) onSelectProject(found);
                  }}
                  className="bg-slate-800 text-slate-200 border border-slate-700 rounded-md px-3 py-1.5 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent cursor-pointer"
                >
                  {projects.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.projectKey})
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Environment Tabs */}
            <div className="flex bg-slate-950 p-1 rounded-lg border border-slate-800">
              {envs.map((env) => {
                const isActive = currentEnv === env;
                let activeColor = 'bg-indigo-600 text-white';
                if (env === 'production') activeColor = 'bg-amber-600 text-white';
                if (env === 'staging') activeColor = 'bg-blue-600 text-white';

                return (
                  <button
                    key={env}
                    onClick={() => onSelectEnv(env)}
                    className={`px-3 py-1 text-xs font-semibold rounded-md transition-all capitalize ${
                      isActive
                        ? `${activeColor} shadow-sm`
                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
                    }`}
                  >
                    {env}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="flex items-center space-x-1">
            <button
              onClick={() => onNavigate('flags')}
              className={`flex items-center space-x-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                currentPage === 'flags' || currentPage === 'flag-detail'
                  ? 'bg-slate-800 text-white'
                  : 'text-slate-300 hover:bg-slate-800/60 hover:text-white'
              }`}
            >
              <Flag className="w-4 h-4 text-indigo-400" />
              <span>Flags</span>
            </button>

            <button
              onClick={() => onNavigate('change-requests')}
              className={`flex items-center space-x-2 px-3 py-2 rounded-md text-sm font-medium transition-colors relative ${
                currentPage === 'change-requests'
                  ? 'bg-slate-800 text-white'
                  : 'text-slate-300 hover:bg-slate-800/60 hover:text-white'
              }`}
            >
              <GitPullRequest className="w-4 h-4 text-amber-400" />
              <span>Change Requests</span>
              {pendingCrCount > 0 && (
                <span className="inline-flex items-center justify-center px-1.5 py-0.5 text-xs font-bold leading-none text-white bg-amber-500 rounded-full animate-pulse">
                  {pendingCrCount}
                </span>
              )}
            </button>

            <button
              onClick={() => onNavigate('experiments')}
              className={`flex items-center space-x-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                currentPage === 'experiments'
                  ? 'bg-slate-800 text-white'
                  : 'text-slate-300 hover:bg-slate-800/60 hover:text-white'
              }`}
            >
              <BarChart3 className="w-4 h-4 text-emerald-400" />
              <span>Experiments</span>
            </button>

            <button
              onClick={() => onNavigate('hygiene')}
              className={`flex items-center space-x-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                currentPage === 'hygiene'
                  ? 'bg-slate-800 text-white'
                  : 'text-slate-300 hover:bg-slate-800/60 hover:text-white'
              }`}
            >
              <ShieldAlert className="w-4 h-4 text-rose-400" />
              <span>Hygiene</span>
            </button>

            <button
              onClick={() => onNavigate('segments')}
              className={`flex items-center space-x-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                currentPage === 'segments'
                  ? 'bg-slate-800 text-white'
                  : 'text-slate-300 hover:bg-slate-800/60 hover:text-white'
              }`}
            >
              <Layers className="w-4 h-4 text-cyan-400" />
              <span>Segments</span>
            </button>
          </nav>

          {/* User profile & Logout */}
          <div className="flex items-center space-x-4">
            {user && (
              <div className="flex items-center space-x-3">
                <div className="text-right">
                  <div className="text-sm font-medium text-slate-200">{user.fullName}</div>
                  <div className="text-xs text-slate-400 font-mono">{user.email}</div>
                </div>
                <div className="h-8 w-8 rounded-full bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center font-bold text-sm">
                  {user.fullName.charAt(0).toUpperCase()}
                </div>
              </div>
            )}

            <button
              onClick={onLogout}
              title="Log out"
              className="p-2 text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition-colors"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
