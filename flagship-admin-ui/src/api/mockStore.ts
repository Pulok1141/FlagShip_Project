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

interface MockState {
  users: Record<string, { user: User; password: string }>;
  projects: Project[];
  flags: Flag[];
  flagVersions: Record<string, FlagVersion[]>;
  changeRequests: ChangeRequest[];
  segments: any[];
}

const INITIAL_USERS: Record<string, { user: User; password: string }> = {
  'admin@flagship.io': {
    user: {
      id: 'usr-admin',
      email: 'admin@flagship.io',
      fullName: 'System Administrator',
      globalRole: 'ADMIN',
    },
    password: 'admin123',
  },
  'approver@flagship.io': {
    user: {
      id: 'usr-approver',
      email: 'approver@flagship.io',
      fullName: 'Production Approver',
      globalRole: 'USER',
    },
    password: 'approver123',
  },
  'editor@flagship.io': {
    user: {
      id: 'usr-editor',
      email: 'editor@flagship.io',
      fullName: 'Feature Flag Editor',
      globalRole: 'USER',
    },
    password: 'editor123',
  },
  'viewer@flagship.io': {
    user: {
      id: 'usr-viewer',
      email: 'viewer@flagship.io',
      fullName: 'Read-Only Viewer',
      globalRole: 'USER',
    },
    password: 'viewer123',
  },
};

const INITIAL_PROJECTS: Project[] = [
  {
    id: 'proj-default',
    projectKey: 'default',
    name: 'Default Project',
    description: 'Primary demonstration project for FlagShip platform',
    myRole: 'ADMIN',
    environments: [
      {
        id: 'env-dev',
        envKey: 'development',
        name: 'Development',
        apiKey: 'flagship_dev_key_12345',
        requireApproval: false,
      },
      {
        id: 'env-stage',
        envKey: 'staging',
        name: 'Staging',
        apiKey: 'flagship_staging_key_12345',
        requireApproval: false,
      },
      {
        id: 'env-prod',
        envKey: 'production',
        name: 'Production',
        apiKey: 'flagship_prod_key_12345',
        requireApproval: true,
      },
    ],
  },
];

const INITIAL_FLAGS: Flag[] = [
  {
    id: 'flag-1',
    projectId: 'proj-default',
    flagKey: 'new-checkout-flow',
    name: 'New One-Click Checkout Flow',
    description: 'Gradual rollout of the redesigned checkout pipeline with 1-click buy',
    flagType: 'BOOLEAN',
    ownerEmail: 'admin@flagship.io',
    tags: ['checkout', 'v2', 'ecommerce'],
    createdAt: '2026-11-19T08:00:00Z',
    updatedAt: '2026-11-19T10:00:00Z',
    lastEvaluatedAt: new Date().toISOString(),
    envConfigs: {
      development: {
        id: 'cfg-dev',
        envKey: 'development',
        environmentName: 'Development',
        enabled: true,
        version: 2,
        defaultVariationId: 'true-var',
        offVariationId: 'false-var',
        requireApproval: false,
        variations: [
          { id: 'true-var', name: 'Enabled (1-Click)', value: true, description: 'New 1-click checkout' },
          { id: 'false-var', name: 'Disabled (Standard)', value: false, description: 'Standard checkout' },
        ],
        targets: [
          { variationId: 'true-var', userKeys: ['alice-vip', 'demo-user'] },
        ],
        rules: [
          {
            id: 'rule-beta',
            variationId: 'true-var',
            ruleTree: {
              type: 'LEAF',
              operator: 'EQUALS',
              attribute: 'betaOptIn',
              values: ['true'],
            },
          },
        ],
        rollout: [
          { variationId: 'true-var', weight: 50 },
          { variationId: 'false-var', weight: 50 },
        ],
      },
      staging: {
        id: 'cfg-stage',
        envKey: 'staging',
        environmentName: 'Staging',
        enabled: true,
        version: 1,
        defaultVariationId: 'false-var',
        offVariationId: 'false-var',
        requireApproval: false,
        variations: [
          { id: 'true-var', name: 'Enabled', value: true },
          { id: 'false-var', name: 'Disabled', value: false },
        ],
        targets: [],
        rules: [],
        rollout: [
          { variationId: 'true-var', weight: 20 },
          { variationId: 'false-var', weight: 80 },
        ],
      },
      production: {
        id: 'cfg-prod',
        envKey: 'production',
        environmentName: 'Production',
        enabled: false,
        version: 1,
        defaultVariationId: 'false-var',
        offVariationId: 'false-var',
        requireApproval: true,
        variations: [
          { id: 'true-var', name: 'Enabled', value: true },
          { id: 'false-var', name: 'Disabled', value: false },
        ],
        targets: [],
        rules: [],
        rollout: [],
      },
    },
  },
  {
    id: 'flag-2',
    projectId: 'proj-default',
    flagKey: 'promo-banner-text',
    name: 'Promotional Banner Text',
    description: 'Dynamic banner messaging on dashboard header',
    flagType: 'STRING',
    ownerEmail: 'editor@flagship.io',
    tags: ['marketing', 'frontend'],
    createdAt: '2026-11-19T08:30:00Z',
    updatedAt: '2026-11-19T09:30:00Z',
    lastEvaluatedAt: new Date().toISOString(),
    envConfigs: {
      development: {
        id: 'cfg-banner-dev',
        envKey: 'development',
        environmentName: 'Development',
        enabled: true,
        version: 1,
        defaultVariationId: 'var-summer',
        offVariationId: 'var-default',
        requireApproval: false,
        variations: [
          { id: 'var-default', name: 'Standard Banner', value: 'Welcome to FlagShip Store!' },
          { id: 'var-summer', name: 'Summer Campaign', value: 'Summer Flash Sale: Use code SUMMER25 for 25% OFF!' },
          { id: 'var-vip', name: 'VIP Exclusive', value: 'Exclusive VIP Offer: Free overnight shipping applied!' },
        ],
        targets: [
          { variationId: 'var-vip', userKeys: ['vip-customer-1'] },
        ],
        rules: [],
        rollout: [],
      },
    },
  },
];

const INITIAL_CHANGE_REQUESTS: ChangeRequest[] = [
  {
    id: 'cr-101',
    projectId: 'proj-default',
    environmentKey: 'production',
    flagKey: 'new-checkout-flow',
    flagName: 'New One-Click Checkout Flow',
    status: 'PENDING_APPROVAL',
    authorEmail: 'editor@flagship.io',
    reason: 'Gradual ramp up to 25% sticky rollout in production for US region customers.',
    createdAt: '2026-11-19T09:15:00Z',
    diffJson: JSON.stringify({
      enabled: { old: false, new: true },
      rollout: {
        old: [],
        new: [
          { variationId: 'true-var', weight: 25 },
          { variationId: 'false-var', weight: 75 },
        ],
      },
    }),
    currentConfigJson: JSON.stringify({
      enabled: false,
      rollout: [],
    }),
    proposedConfigJson: JSON.stringify({
      enabled: true,
      rollout: [
        { variationId: 'true-var', weight: 25 },
        { variationId: 'false-var', weight: 75 },
      ],
    }),
  },
];

class MockStore {
  private state: MockState;

  constructor() {
    this.state = this.loadState();
  }

  private loadState(): MockState {
    try {
      const saved = localStorage.getItem('flagship_mock_state_v2');
      if (saved) {
        return JSON.parse(saved);
      }
    } catch (_) {}

    return {
      users: INITIAL_USERS,
      projects: INITIAL_PROJECTS,
      flags: INITIAL_FLAGS,
      flagVersions: {
        'cfg-dev': [
          {
            id: 'ver-1',
            version: 1,
            authorEmail: 'admin@flagship.io',
            reason: 'Initial flag configuration',
            createdAt: '2026-11-19T08:00:00Z',
            diffJson: '{"initial":"Initial flag creation"}',
            snapshotJson: '{}',
          },
          {
            id: 'ver-2',
            version: 2,
            authorEmail: 'admin@flagship.io',
            reason: 'Promoting 1-click checkout and targeting alice-vip',
            createdAt: '2026-11-19T09:00:00Z',
            diffJson: '{"enabled":{"old":false,"new":true}}',
            snapshotJson: '{}',
          },
        ],
      },
      changeRequests: INITIAL_CHANGE_REQUESTS,
      segments: [
        {
          id: 'seg-1',
          segmentKey: 'beta-testers',
          name: 'Beta Opt-In Users',
          description: 'Users who opted in to beta preview features',
          ruleTreeJson: JSON.stringify({
            type: 'LEAF',
            operator: 'EQUALS',
            attribute: 'betaOptIn',
            values: ['true'],
          }),
        },
        {
          id: 'seg-2',
          segmentKey: 'enterprise-tier',
          name: 'Enterprise Tier Accounts',
          description: 'High-value enterprise customers in US and EU',
          ruleTreeJson: JSON.stringify({
            type: 'LEAF',
            operator: 'IN',
            attribute: 'tier',
            values: ['enterprise', 'platinum'],
          }),
        },
      ],
    };
  }

  private saveState() {
    try {
      localStorage.setItem('flagship_mock_state_v2', JSON.stringify(this.state));
    } catch (_) {}
  }

  public handleRequest<T>(endpoint: string, options: RequestInit = {}): T {
    const method = (options.method || 'GET').toUpperCase();
    const body = options.body ? JSON.parse(options.body as string) : {};

    // 1. Auth Login
    if (endpoint === '/auth/login' && method === 'POST') {
      const email = body.email?.toLowerCase().trim();
      const pass = body.password;

      const userEntry = this.state.users[email];
      if (userEntry && userEntry.password === pass) {
        return {
          token: `mock-jwt-token-for-${userEntry.user.id}-${Date.now()}`,
          user: userEntry.user,
        } as T;
      }

      // Default to admin or requested role
      const fallbackUser: User = {
        id: 'usr-admin',
        email: email || 'admin@flagship.io',
        fullName: 'System Administrator (Demo)',
        globalRole: 'ADMIN',
      };
      return {
        token: `mock-jwt-token-${Date.now()}`,
        user: fallbackUser,
      } as T;
    }

    // 2. Current User
    if (endpoint === '/auth/me') {
      return { user: this.state.users['admin@flagship.io'].user } as T;
    }

    // 3. Projects
    if (endpoint === '/projects' && method === 'GET') {
      return this.state.projects as T;
    }

    // 4. Flags list
    if (endpoint.startsWith('/projects/') && endpoint.endsWith('/flags') && method === 'GET') {
      return this.state.flags as T;
    }

    // 5. Flag Detail
    if (endpoint.startsWith('/flags/flag-') && method === 'GET') {
      const flagId = endpoint.replace('/flags/', '');
      const flag = this.state.flags.find((f) => f.id === flagId) || this.state.flags[0];
      return flag as T;
    }

    // 6. Toggle Flag
    if (endpoint.includes('/toggle') && method === 'PUT') {
      const parts = endpoint.split('/');
      const cfgId = parts[3];
      for (const flag of this.state.flags) {
        if (flag.envConfigs) {
          for (const envKey of Object.keys(flag.envConfigs)) {
            const cfg = flag.envConfigs[envKey];
            if (cfg.id === cfgId) {
              cfg.enabled = body.enabled;
              cfg.version = (cfg.version || 1) + 1;
              this.saveState();
              return { status: 'APPLIED', version: cfg.version } as T;
            }
          }
        }
      }
      return { status: 'APPLIED', version: 2 } as T;
    }

    // 7. Update Flag Config
    if (endpoint.startsWith('/flags/configs/') && method === 'PUT') {
      const parts = endpoint.split('/');
      const cfgId = parts[3];
      for (const flag of this.state.flags) {
        if (flag.envConfigs) {
          for (const envKey of Object.keys(flag.envConfigs)) {
            const cfg = flag.envConfigs[envKey];
            if (cfg.id === cfgId) {
              cfg.enabled = body.enabled;
              cfg.defaultVariationId = body.defaultVariationId;
              cfg.offVariationId = body.offVariationId;
              try {
                cfg.variations = JSON.parse(body.variationsJson || '[]');
                cfg.targets = JSON.parse(body.targetsJson || '[]');
                cfg.rules = JSON.parse(body.rulesJson || '[]');
                cfg.rollout = JSON.parse(body.rolloutJson || '[]');
              } catch (_) {}
              cfg.version = (cfg.version || 1) + 1;
              this.saveState();
              return { status: 'APPLIED', version: cfg.version } as T;
            }
          }
        }
      }
      return { status: 'APPLIED', version: 3 } as T;
    }

    // 8. Flag Versions
    if (endpoint.includes('/versions') && method === 'GET') {
      return (this.state.flagVersions['cfg-dev'] || []) as T;
    }

    // 9. Restore Version
    if (endpoint.includes('/restore/') && method === 'POST') {
      return { status: 'RESTORED' } as T;
    }

    // 10. Live Evaluation Preview (FR2, QA4)
    if (endpoint === '/flags/preview' && method === 'POST') {
      const { userKey, attributes } = body.context || {};
      const isTargeted = userKey === 'alice-vip' || userKey === 'demo-user';
      const isBeta = attributes?.betaOptIn === 'true';

      if (isTargeted) {
        return {
          flagKey: body.flagKey,
          variationId: 'true-var',
          variationName: 'Enabled (1-Click)',
          value: true,
          reason: 'TARGET_MATCH',
          evaluationDurationNanos: 24000,
        } as T;
      }

      if (isBeta) {
        return {
          flagKey: body.flagKey,
          variationId: 'true-var',
          variationName: 'Enabled (1-Click)',
          value: true,
          reason: 'RULE_MATCH',
          evaluationDurationNanos: 31000,
        } as T;
      }

      // Default Murmur3 bucketing simulation
      let hash = 0;
      const str = `${body.flagKey}:${userKey || 'anon'}`;
      for (let i = 0; i < str.length; i++) {
        hash = (hash << 5) - hash + str.charCodeAt(i);
        hash |= 0;
      }
      const bucket = Math.abs(hash) % 10000;
      const inRollout = bucket < 5000;

      return {
        flagKey: body.flagKey,
        variationId: inRollout ? 'true-var' : 'false-var',
        variationName: inRollout ? 'Enabled (1-Click)' : 'Disabled (Standard)',
        value: inRollout,
        reason: inRollout ? 'PERCENTAGE_ROLLOUT' : 'DEFAULT',
        evaluationDurationNanos: 28000,
      } as T;
    }

    // 11. Change Requests
    if (endpoint.includes('/change-requests') && method === 'GET') {
      return this.state.changeRequests as T;
    }

    if (endpoint.includes('/approve') && method === 'POST') {
      const crId = endpoint.split('/')[2];
      const cr = this.state.changeRequests.find((c) => c.id === crId);
      if (cr) {
        cr.status = 'APPROVED';
        cr.approverEmail = 'approver@flagship.io';
        cr.resolvedAt = new Date().toISOString();
        this.saveState();
        return cr as T;
      }
    }

    if (endpoint.includes('/reject') && method === 'POST') {
      const crId = endpoint.split('/')[2];
      const cr = this.state.changeRequests.find((c) => c.id === crId);
      if (cr) {
        cr.status = 'REJECTED';
        cr.approverEmail = 'approver@flagship.io';
        cr.rejectionReason = body.reason || 'Rejected by approver';
        cr.resolvedAt = new Date().toISOString();
        this.saveState();
        return cr as T;
      }
    }

    // 12. Experiments Analytics (FR8)
    if (endpoint.startsWith('/experiments/analyze')) {
      const report: ExperimentReport = {
        flagKey: 'new-checkout-flow',
        baselineVariationId: 'false-var',
        srmChiSquare: 0.072,
        srmPValue: 0.788,
        srmAlert: false,
        summary: 'Significant positive uplift detected (+20.83%). No SRM detected (p=0.788).',
        variationStats: {
          'false-var': {
            variationId: 'false-var',
            exposures: 12450,
            conversions: 1494,
            conversionRate: 0.12,
            zScore: null,
            pValue: null,
            confidenceInterval95: [0.114, 0.126],
            statisticallySignificant: false,
            status: 'BASELINE',
          },
          'true-var': {
            variationId: 'true-var',
            exposures: 12510,
            conversions: 1814,
            conversionRate: 0.145,
            zScore: 3.92,
            pValue: 0.0001,
            confidenceInterval95: [0.138, 0.152],
            statisticallySignificant: true,
            status: 'SIGNIFICANT_POSITIVE',
          },
        },
      };
      return report as T;
    }

    // 13. Hygiene Report (FR9)
    if (endpoint.includes('/hygiene')) {
      const reports: StaleFlagReport[] = [
        {
          flagId: 'flag-stale-1',
          flagKey: 'legacy-payment-v1',
          flagName: 'Legacy Payment Gateway V1',
          ownerEmail: 'admin@flagship.io',
          reason: 'UNUSED_30_DAYS',
          recommendation: 'No evaluation events recorded across all environments in 42 days. Safe to archive or delete.',
        },
        {
          flagId: 'flag-stale-2',
          flagKey: 'dark-mode-beta',
          flagName: 'Dark Mode Beta Toggle',
          ownerEmail: 'editor@flagship.io',
          reason: 'FULLY_ROLLED_OUT_14_DAYS',
          recommendation: 'Flag has been 100% rolled out in production for 18 days. Remove conditional code from repository.',
        },
      ];
      return reports as T;
    }

    // 14. Segments
    if (endpoint.includes('/segments') && method === 'GET') {
      return this.state.segments as T;
    }

    return {} as T;
  }
}

export const mockStore = new MockStore();
