import {
  User,
  Project,
  Flag,
  FlagVersion,
  ChangeRequest,
  ExperimentReport,
  StaleFlagReport,
  EvaluationResult,
} from '../types';
import { mockStore } from './mockStore';

const normalizeApiBase = (url: string): string => {
  if (!url) return '/api';
  let clean = url.trim().replace(/\/+$/, '');
  if (!clean.endsWith('/api') && (clean.startsWith('http://') || clean.startsWith('https://'))) {
    clean = `${clean}/api`;
  }
  return clean;
};

export const getCustomBackendUrl = (): string | null => {
  return localStorage.getItem('flagship_api_base');
};

export const isDemoMode = (): boolean => {
  const custom = getCustomBackendUrl();
  const envUrl = (import.meta as any).env?.VITE_API_BASE_URL as string;
  if (custom || envUrl) return false;

  if (typeof window !== 'undefined') {
    const host = window.location.hostname;
    if (host === 'localhost' || host === '127.0.0.1') return false;
    if (window.location.port === '8080') return false;
    // On any external static domain (e.g. *.vercel.app, *.netlify.app, *.github.io)
    return true;
  }
  return false;
};

const getApiBase = (): string => {
  const custom = localStorage.getItem('flagship_api_base');
  const envUrl = (import.meta as any).env?.VITE_API_BASE_URL as string;
  if (custom) return normalizeApiBase(custom);
  if (envUrl) return normalizeApiBase(envUrl);
  return '/api';
};

class ApiClient {
  private token: string | null = null;

  constructor() {
    this.token = localStorage.getItem('flagship_token');
  }

  setToken(token: string | null) {
    this.token = token;
    if (token) {
      localStorage.setItem('flagship_token', token);
    } else {
      localStorage.removeItem('flagship_token');
    }
  }

  getToken(): string | null {
    return this.token;
  }

  setApiBase(url: string | null) {
    if (url && url.trim()) {
      localStorage.setItem('flagship_api_base', normalizeApiBase(url.trim()));
    } else {
      localStorage.removeItem('flagship_api_base');
    }
  }

  getApiBase(): string {
    return getApiBase();
  }

  isDemoMode(): boolean {
    return isDemoMode();
  }

  resetDemoStore() {
    mockStore.resetState();
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    // If on a static cloud domain (e.g. Vercel) and no custom backend URL is configured,
    // immediately serve from the in-browser interactive mock store without failing
    if (isDemoMode()) {
      return mockStore.handleRequest<T>(endpoint, options);
    }

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    const apiBase = getApiBase();

    try {
      const response = await fetch(`${apiBase}${endpoint}`, {
        ...options,
        headers,
      });

      // 404, 405 (Method Not Allowed on static proxy), or 5xx (cold start / server down)
      if (response.status === 404 || response.status === 405 || response.status >= 500) {
        console.warn(`Backend returned HTTP ${response.status} for ${endpoint}. Falling back to demo store.`);
        return mockStore.handleRequest<T>(endpoint, options);
      }

      if (response.status === 401 && !endpoint.includes('/auth/login')) {
        this.setToken(null);
        window.location.href = '/login';
        throw new Error('Session expired. Please log in again.');
      }

      if (!response.ok) {
        let errMessage = `HTTP ${response.status} ${response.statusText}`;
        try {
          const errJson = await response.json();
          if (errJson.message) errMessage = errJson.message;
          else if (errJson.error) errMessage = errJson.error;
        } catch (e) {
          try {
            const text = await response.text();
            if (text && !text.includes('<!doctype') && !text.includes('<html')) errMessage = text;
          } catch (_) {}
        }
        throw new Error(errMessage);
      }

      if (response.status === 204) {
        return {} as T;
      }

      // If static SPA routing returned index.html instead of JSON
      const contentType = response.headers.get('content-type') || '';
      if (contentType.includes('text/html')) {
        console.warn(`Received HTML response for ${endpoint}. Falling back to demo store.`);
        return mockStore.handleRequest<T>(endpoint, options);
      }

      return await response.json();
    } catch (err: any) {
      // If network failed (CORS, offline, unreachable, or HTML SyntaxError)
      console.warn(`Request failed for ${endpoint}: ${err.message}. Falling back to demo store.`);
      return mockStore.handleRequest<T>(endpoint, options);
    }
  }

  // Auth
  async login(email: string, password: string):Promise<{ token: string; user: User }> {
    const data = await this.request<{ token: string; user: User }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    this.setToken(data.token);
    return data;
  }

  logout() {
    this.setToken(null);
  }

  async getCurrentUser(): Promise<{ user: User }> {
    return this.request<{ user: User }>('/auth/me');
  }

  // Projects
  async getProjects(): Promise<Project[]> {
    return this.request<Project[]>('/projects');
  }

  async createProject(projectKey: string, name: string, description: string): Promise<Project> {
    return this.request<Project>('/projects', {
      method: 'POST',
      body: JSON.stringify({ projectKey, name, description }),
    });
  }

  // Flags
  async getFlags(projectId: string): Promise<Flag[]> {
    return this.request<Flag[]>(`/projects/${projectId}/flags`);
  }

  async getFlagDetail(flagId: string): Promise<Flag> {
    return this.request<Flag>(`/flags/${flagId}`);
  }

  async createFlag(projectId: string, payload: {
    flagKey: string;
    name: string;
    description: string;
    flagType: string;
    tags?: string[];
  }): Promise<Flag> {
    return this.request<Flag>(`/projects/${projectId}/flags`, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  }

  async toggleFlag(flagEnvConfigId: string, enabled: boolean, reason?: string): Promise<any> {
    return this.request<any>(`/flags/configs/${flagEnvConfigId}/toggle`, {
      method: 'PUT',
      body: JSON.stringify({ enabled, reason }),
    });
  }

  async updateFlagConfig(flagEnvConfigId: string, payload: {
    enabled: boolean;
    variationsJson: string;
    targetsJson: string;
    rulesJson: string;
    rolloutJson: string;
    defaultVariationId: string;
    offVariationId?: string;
    reason?: string;
  }): Promise<any> {
    return this.request<any>(`/flags/configs/${flagEnvConfigId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  }

  async getFlagVersions(flagEnvConfigId: string): Promise<FlagVersion[]> {
    return this.request<FlagVersion[]>(`/flags/configs/${flagEnvConfigId}/versions`);
  }

  async restoreVersion(flagEnvConfigId: string, version: number, reason?: string): Promise<any> {
    return this.request<any>(`/flags/configs/${flagEnvConfigId}/restore/${version}`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  }

  // Live Evaluation Preview (FR2, QA4)
  async evaluatePreview(projectId: string, flagKey: string, envKey: string, userKey: string, attributes: Record<string, any> = {}): Promise<EvaluationResult> {
    return this.request<EvaluationResult>('/flags/preview', {
      method: 'POST',
      body: JSON.stringify({
        projectId,
        flagKey,
        envKey,
        context: {
          userKey,
          attributes,
        },
      }),
    });
  }

  // Change Requests (FR7, 4-Eyes Principle)
  async getChangeRequests(projectId: string): Promise<ChangeRequest[]> {
    return this.request<ChangeRequest[]>(`/projects/${projectId}/change-requests`);
  }

  async approveChangeRequest(id: string, reason?: string): Promise<ChangeRequest> {
    return this.request<ChangeRequest>(`/change-requests/${id}/approve`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  }

  async rejectChangeRequest(id: string, reason: string): Promise<ChangeRequest> {
    return this.request<ChangeRequest>(`/change-requests/${id}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  }

  // Experiments Analytics (FR8)
  async getExperimentAnalytics(projectId: string, envKey: string, flagKey: string, baselineVariationId?: string, days: number = 30): Promise<ExperimentReport> {
    const params = new URLSearchParams({
      projectId,
      envKey,
      flagKey,
      days: days.toString(),
    });
    if (baselineVariationId) {
      params.append('baselineVariationId', baselineVariationId);
    }
    return this.request<ExperimentReport>(`/experiments/analyze?${params.toString()}`);
  }

  // Flag Hygiene (FR9)
  async getHygieneReport(projectId: string): Promise<StaleFlagReport[]> {
    return this.request<StaleFlagReport[]>(`/projects/${projectId}/hygiene`);
  }

  // Segments
  async getSegments(projectId: string): Promise<any[]> {
    return this.request<any[]>(`/projects/${projectId}/segments`);
  }

  async createSegment(projectId: string, payload: {
    segmentKey: string;
    name: string;
    description: string;
    ruleTreeJson: string;
  }): Promise<any> {
    return this.request<any>(`/projects/${projectId}/segments`, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  }
}

export const api = new ApiClient();
