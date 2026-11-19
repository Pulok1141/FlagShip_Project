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

const getApiBase = (): string => {
  return ((import.meta as any).env?.VITE_API_BASE_URL as string) || localStorage.getItem('flagship_api_base') || '/api';
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

  setApiBase(url: string) {
    if (url) {
      localStorage.setItem('flagship_api_base', url.replace(/\/$/, ''));
    } else {
      localStorage.removeItem('flagship_api_base');
    }
  }

  getApiBase(): string {
    return getApiBase();
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
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

      // 404 indicates backend route is absent (e.g. static hosting on Vercel)
      if (response.status === 404) {
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
            if (text) errMessage = text;
          } catch (_) {}
        }
        throw new Error(errMessage);
      }

      if (response.status === 204) {
        return {} as T;
      }

      return response.json();
    } catch (err: any) {
      // If network failed (e.g. backend down or CORS on static deployment)
      if (
        err.message?.includes('Failed to fetch') ||
        err.message?.includes('NetworkError') ||
        err.message?.includes('404')
      ) {
        return mockStore.handleRequest<T>(endpoint, options);
      }
      throw err;
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
