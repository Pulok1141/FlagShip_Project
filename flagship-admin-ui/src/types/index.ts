export type FlagType = 'BOOLEAN' | 'STRING' | 'NUMBER' | 'JSON';

export type GlobalRole = 'ADMIN' | 'USER';

export type ProjectRole = 'VIEWER' | 'EDITOR' | 'APPROVER' | 'ADMIN';

export type EvaluationReason =
  | 'KILL_SWITCH'
  | 'TARGET_MATCH'
  | 'RULE_MATCH'
  | 'PERCENTAGE_ROLLOUT'
  | 'DEFAULT'
  | 'ERROR';

export interface User {
  id: string;
  email: string;
  fullName: string;
  globalRole: GlobalRole;
}

export interface Environment {
  id: string;
  envKey: 'development' | 'staging' | 'production' | string;
  name: string;
  apiKey: string;
  requireApproval: boolean;
}

export interface Project {
  id: string;
  projectKey: string;
  name: string;
  description: string;
  environments?: Environment[];
  myRole?: ProjectRole;
}

export interface Variation {
  id: string;
  name: string;
  value: any;
  description?: string;
}

export interface TargetRule {
  variationId: string;
  userKeys: string[];
}

export interface RolloutBucket {
  variationId: string;
  weight: number; // e.g. 50%
}

export interface LeafConditionNode {
  type: 'LEAF';
  attribute: string;
  operator:
    | 'EQUALS'
    | 'NOT_EQUALS'
    | 'IN'
    | 'NOT_IN'
    | 'CONTAINS'
    | 'NOT_CONTAINS'
    | 'STARTS_WITH'
    | 'ENDS_WITH'
    | 'SEMVER_EQ'
    | 'SEMVER_GT'
    | 'SEMVER_GTE'
    | 'SEMVER_LT'
    | 'SEMVER_LTE'
    | 'REGEX'
    | 'DATE_BEFORE'
    | 'DATE_AFTER';
  values: string[];
}

export interface CompositeRuleNode {
  type: 'COMPOSITE';
  logicalOperator: 'AND' | 'OR';
  children: (CompositeRuleNode | LeafConditionNode)[];
}

export type RuleNode = CompositeRuleNode | LeafConditionNode;

export interface SegmentRule {
  id: string;
  variationId: string;
  ruleTree: RuleNode;
}

export interface FlagEnvConfig {
  id: string;
  envKey: string;
  environmentName: string;
  enabled: boolean;
  version: number;
  variations: Variation[];
  targets: TargetRule[];
  rules: SegmentRule[];
  rollout: RolloutBucket[];
  defaultVariationId: string;
  offVariationId?: string;
  requireApproval: boolean;
}

export interface Flag {
  id: string;
  projectId: string;
  flagKey: string;
  name: string;
  description: string;
  flagType: FlagType;
  ownerEmail: string;
  tags?: string[];
  lastEvaluatedAt?: string;
  fullyRolledOutSince?: string;
  createdAt: string;
  updatedAt: string;
  envConfigs?: Record<string, FlagEnvConfig>;
}

export interface ChangeRequest {
  id: string;
  projectId: string;
  environmentKey: string;
  flagKey?: string;
  flagName?: string;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'APPLIED';
  authorEmail: string;
  approverEmail?: string;
  reason: string;
  rejectionReason?: string;
  proposedConfigJson: string;
  currentConfigJson: string;
  diffJson: string;
  createdAt: string;
  resolvedAt?: string;
}

export interface FlagVersion {
  id: string;
  version: number;
  snapshotJson: string;
  diffJson: string;
  authorEmail: string;
  reason: string;
  createdAt: string;
}

export interface VariationStats {
  variationId: string;
  exposures: number;
  conversions: number;
  conversionRate: number;
  zScore: number | null;
  pValue: number | null;
  confidenceInterval95: [number, number];
  statisticallySignificant: boolean;
  status: string;
}

export interface ExperimentReport {
  flagKey: string;
  baselineVariationId: string;
  variationStats: Record<string, VariationStats>;
  srmChiSquare: number;
  srmPValue: number;
  srmAlert: boolean;
  summary: string;
}

export interface StaleFlagReport {
  flagId: string;
  flagKey: string;
  flagName: string;
  ownerEmail: string;
  reason: string;
  recommendation: string;
}

export interface EvaluationResult {
  flagKey: string;
  variationId: string;
  variationName: string;
  value: any;
  reason: EvaluationReason;
  matchedRuleId?: string;
  evaluationDurationNanos: number;
}
