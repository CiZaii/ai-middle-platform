import { User } from './user';

// 业务配置
export interface Business {
  id: string;
  name: string;
  code: string; // 唯一业务CODE
  description?: string;
  createdAt: string;
  createdBy: User;
  enabled: boolean;
}

// 模型信息
export interface Model {
  id: string;
  name: string;
  provider: string; // OpenAI, Claude, Gemini, etc.
  description?: string;
}

// 端点配置
export interface Endpoint {
  id: string;
  name: string;
  baseUrl: string;
  provider: string;
  models: Model[];
  businesses: Business[];
  enabled: boolean;
  createdAt: string;
  createdBy: User;
  updatedAt?: string;
  description?: string;
  // 统计信息
  stats?: {
    totalApiKeys: number;
    activeApiKeys: number;
    totalRequests: number;
    successRate: number;
  };
}

// API Key 配置
export interface ApiKey {
  id: string;
  endpointId: string;
  endpoint?: Endpoint;
  key: string; // 实际使用时会加密
  displayKey: string; // 显示用的脱敏 key
  name: string;
  enabled: boolean;
  createdAt: string;
  createdBy: User;
  expiresAt?: string;
  lastUsedAt?: string;
  // 配额和限制
  rateLimit?: {
    requestsPerMinute: number;
    requestsPerDay: number;
  };
  // 使用统计
  stats?: {
    totalRequests: number;
    successRequests: number;
    failedRequests: number;
    lastError?: string;
  };
}

// 提供商枚举
export enum Provider {
  OPENAI = 'OpenAI',
  CLAUDE = 'Claude',
  GEMINI = 'Gemini',
  AZURE = 'Azure OpenAI',
  CUSTOM = 'Custom',
}
