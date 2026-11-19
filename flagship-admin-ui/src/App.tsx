import React, { useState, useEffect } from 'react';
import { Navbar } from './components/Navbar';
import { FlagsPage } from './pages/FlagsPage';
import { FlagEditorPage } from './pages/FlagEditorPage';
import { ChangeRequestsPage } from './pages/ChangeRequestsPage';
import { ExperimentsPage } from './pages/ExperimentsPage';
import { HygienePage } from './pages/HygienePage';
import { SegmentsPage } from './pages/SegmentsPage';
import { LoginPage } from './pages/LoginPage';
import { api } from './api/client';
import { Project, User } from './types';

export const App: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [authChecked, setAuthChecked] = useState(false);

  const [projects, setProjects] = useState<Project[]>([]);
  const [currentProject, setCurrentProject] = useState<Project | null>(null);
  const [currentEnv, setCurrentEnv] = useState<string>('development');

  const [currentPage, setCurrentPage] = useState<string>('flags');
  const [selectedFlagId, setSelectedFlagId] = useState<string | null>(null);
  const [pendingCrCount, setPendingCrCount] = useState<number>(0);

  // Check auth session
  useEffect(() => {
    const checkAuth = async () => {
      const token = api.getToken();
      if (token) {
        try {
          const res = await api.getCurrentUser();
          setUser(res.user);
        } catch (e) {
          api.setToken(null);
          setUser(null);
        }
      }
      setAuthChecked(true);
    };
    checkAuth();
  }, []);

  // Load projects
  const loadProjects = async () => {
    if (!user) return;
    try {
      const projs = await api.getProjects();
      setProjects(projs);
      if (projs.length > 0 && !currentProject) {
        setCurrentProject(projs[0]);
      }
    } catch (err) {
      console.error('Failed to load projects:', err);
    }
  };

  useEffect(() => {
    if (user) {
      loadProjects();
    }
  }, [user]);

  // Load pending change requests count
  const loadPendingCr = async () => {
    if (!currentProject) return;
    try {
      const crs = await api.getChangeRequests(currentProject.id);
      const pending = crs.filter((c) => c.status === 'PENDING_APPROVAL');
      setPendingCrCount(pending.length);
    } catch (_) {}
  };

  useEffect(() => {
    if (currentProject) {
      loadPendingCr();
    }
  }, [currentProject, currentPage]);

  const handleLogout = () => {
    api.logout();
    setUser(null);
    setCurrentProject(null);
    setCurrentPage('flags');
  };

  if (!authChecked) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center text-slate-500">
        Initializing FlagShip...
      </div>
    );
  }

  if (!user) {
    return <LoginPage onLoginSuccess={(u) => setUser(u)} />;
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar
        user={user}
        projects={projects}
        currentProject={currentProject}
        onSelectProject={(p) => {
          setCurrentProject(p);
          setSelectedFlagId(null);
        }}
        currentEnv={currentEnv}
        onSelectEnv={(env) => setCurrentEnv(env)}
        currentPage={currentPage}
        onNavigate={(page) => {
          setCurrentPage(page);
          if (page !== 'flag-detail') {
            setSelectedFlagId(null);
          }
        }}
        onLogout={handleLogout}
        pendingCrCount={pendingCrCount}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {!currentProject ? (
          <div className="text-center py-20 text-slate-500">Loading project...</div>
        ) : currentPage === 'flags' && !selectedFlagId ? (
          <FlagsPage
            project={currentProject}
            currentEnv={currentEnv}
            onSelectFlag={(id) => {
              setSelectedFlagId(id);
              setCurrentPage('flag-detail');
            }}
          />
        ) : currentPage === 'flag-detail' && selectedFlagId ? (
          <FlagEditorPage
            project={currentProject}
            currentEnv={currentEnv}
            flagId={selectedFlagId}
            onBack={() => {
              setSelectedFlagId(null);
              setCurrentPage('flags');
            }}
          />
        ) : currentPage === 'change-requests' ? (
          <ChangeRequestsPage project={currentProject} user={user} />
        ) : currentPage === 'experiments' ? (
          <ExperimentsPage project={currentProject} currentEnv={currentEnv} />
        ) : currentPage === 'hygiene' ? (
          <HygienePage
            project={currentProject}
            onSelectFlagByKey={(flagKey) => {
              // Lookup flag id by key
              api.getFlags(currentProject.id).then((flags) => {
                const found = flags.find((f) => f.flagKey === flagKey);
                if (found) {
                  setSelectedFlagId(found.id);
                  setCurrentPage('flag-detail');
                }
              });
            }}
          />
        ) : currentPage === 'segments' ? (
          <SegmentsPage project={currentProject} />
        ) : (
          <div className="text-center py-20 text-slate-500">Page not found</div>
        )}
      </main>
    </div>
  );
};

export default App;
