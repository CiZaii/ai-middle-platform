import { KnowledgeBase } from '@/types/knowledge-base';
import { File as KnowledgeFile, ProcessingStatus } from '@/types/file';
import { ApiKey, Business, Endpoint, Model, Provider } from '@/types/model-config';
import { User } from '@/types/user';
import { Prompt, PromptRequest } from '@/types/prompt';
import { useAuthStore } from '@/lib/stores/auth-store';

// API基础配置
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

// API响应包装类型
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// 获取token
function getToken(): string | null {
  const state = useAuthStore.getState();
  if (state.token) {
    return state.token;
  }

  if (typeof window === 'undefined') return null;

  const storedToken = localStorage.getItem('auth_token');
  if (storedToken) {
    state.setToken(storedToken);
  }
  return storedToken;
}

// 通用请求函数
async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({
      code: response.status,
      message: response.statusText,
    }));
    throw new Error(error.message || `HTTP error! status: ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const result = await response
    .json()
    .catch(() => ({ code: 200, message: 'success', data: undefined }));

  if (typeof result.code === 'number' && result.code !== 200) {
    throw new Error(result.message || 'API request failed');
  }

  return (result.data ?? result) as T;
}

// 文件上传专用函数
async function uploadFile<T>(
  endpoint: string,
  file: globalThis.File,
  onProgress?: (progress: number) => void
): Promise<T> {
  const token = getToken();

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('file', file);

    xhr.upload.addEventListener('progress', (e) => {
      if (e.lengthComputable && onProgress) {
        onProgress((e.loaded / e.total) * 100);
      }
    });

    xhr.addEventListener('load', () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const result: ApiResponse<T> = JSON.parse(xhr.responseText);
          if (result.code === 200) {
            resolve(result.data);
          } else {
            reject(new Error(result.message || 'Upload failed'));
          }
        } catch (error) {
          reject(new Error('Failed to parse response'));
        }
      } else {
        reject(new Error(`Upload failed with status: ${xhr.status}`));
      }
    });

    xhr.addEventListener('error', () => {
      reject(new Error('Upload failed'));
    });

    xhr.open('POST', `${API_BASE_URL}${endpoint}`);
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }
    xhr.send(formData);
  });
}

// 后端实体映射到前端类型
interface BackendUser {
  id: number | string;
  username?: string;
  name?: string;
  email?: string;
  avatar?: string;
  role?: string;
  createdAt?: string;
  created_at?: string;
}

type BackendUserLike = BackendUser | number | string | null | undefined;

interface BackendKnowledgeBaseMember {
  id?: number | string;
  userId?: number | string;
  user?: BackendUserLike;
  role?: string;
  joinedAt?: string;
  joined_at?: string;
}

interface BackendKnowledgeBase {
  id: number | string;
  kbId?: string;
  name: string;
  description?: string;
  ownerId?: number | string;
  fileCount: number;
  enabled?: boolean;
  createdAt: string;
  updatedAt: string;
  owner?: BackendUserLike;
  members?: BackendKnowledgeBaseMember[];
}

interface BackendGraphNode {
  id?: string | number;
  label?: string;
  type?: string;
}

interface BackendGraphEdge {
  source?: string | number;
  target?: string | number;
  label?: string;
}

interface BackendQaPair {
  id?: string;
  qaId?: string;
  question?: string;
  answer?: string;
  sourceText?: string;
  source_text?: string;
}

interface BackendFile {
  id: string;
  knowledgeBaseId?: string;
  kbId?: number | string;
  name: string;
  originalName?: string;
  type?: string;
  fileType?: string;
  mimeType: string;
  size: number;
  storagePath?: string;
  url: string;
  thumbnailUrl?: string;
  uploadedBy: number | string | BackendUserLike;
  uploadedAt: string;
  statuses?: {
    ocr?: string;
    vectorization?: string;
    qaPairs?: string;
    knowledgeGraph?: string;
  };
  ocrStatus?: string;
  vectorizationStatus?: string;
  qaPairsStatus?: string;
  knowledgeGraphStatus?: string;
  errorMessage?: string;
  uploader?: BackendUserLike;
  ocrContent?: string;
  knowledgeGraph?: {
    nodes?: BackendGraphNode[];
    edges?: BackendGraphEdge[];
  };
  qaPairs?: BackendQaPair[];
}

interface BackendModel {
  id: number | string;
  name: string;
  provider?: string;
  description?: string;
}

interface BackendEndpointStats {
  totalApiKeys?: number;
  activeApiKeys?: number;
  totalRequests?: number;
  successRate?: number;
}

interface BackendBusiness {
  id: number | string;
  name: string;
  code: string;
  description?: string;
  createdAt: string;
  enabled?: boolean;
  createdBy?: BackendUserLike;
}

interface BackendEndpoint {
  id: number | string;
  name: string;
  baseUrl: string;
  provider?: string;
  description?: string;
  enabled?: boolean;
  createdAt: string;
  updatedAt?: string;
  createdBy?: BackendUserLike;
  models?: (BackendModel | Model)[];
  businesses?: BackendBusiness[];
  stats?: BackendEndpointStats;
}

interface BackendEndpointSummary {
  id?: number | string;
  name?: string;
  baseUrl?: string;
  provider?: string;
  description?: string;
  createdAt?: string;
  createdBy?: BackendUserLike;
  stats?: BackendEndpointStats;
  models?: (BackendModel | Model)[];
  businesses?: BackendBusiness[];
}

interface BackendApiKey {
  id: number | string;
  endpointId: number | string;
  endpoint?: BackendEndpoint | BackendEndpointSummary;
  key?: string;
  apiKey?: string;
  displayKey?: string;
  name: string;
  enabled?: boolean;
  createdAt: string;
  createdBy?: BackendUserLike;
  expiresAt?: string;
  lastUsedAt?: string;
  rateLimit?: {
    requestsPerMinute?: number;
    requestsPerDay?: number;
  };
  rateLimitPerMinute?: number;
  rateLimitPerDay?: number;
  stats?: {
    totalRequests?: number;
    successRequests?: number;
    failedRequests?: number;
    lastError?: string;
  };
  totalRequests?: number;
  successRequests?: number;
  failedRequests?: number;
  lastError?: string;
}

const MEMBER_ROLE_MAP: Record<string, 'owner' | 'editor' | 'viewer'> = {
  owner: 'owner',
  editor: 'editor',
  viewer: 'viewer',
};

const PROCESSING_STATUS_MAP: Record<string, ProcessingStatus> = {
  pending: ProcessingStatus.PENDING,
  processing: ProcessingStatus.PROCESSING,
  completed: ProcessingStatus.COMPLETED,
  success: ProcessingStatus.COMPLETED,
  failed: ProcessingStatus.FAILED,
  error: ProcessingStatus.FAILED,
};

function mapMemberRole(role?: string): 'owner' | 'editor' | 'viewer' {
  if (!role) return 'viewer';
  const normalized = role.toLowerCase();
  return MEMBER_ROLE_MAP[normalized] || 'viewer';
}

function mapProcessingStatus(status?: string): ProcessingStatus {
  if (!status) return ProcessingStatus.PENDING;
  const normalized = status.toLowerCase();
  return PROCESSING_STATUS_MAP[normalized] || ProcessingStatus.PENDING;
}

function mapProvider(provider?: string): Provider {
  if (!provider) return Provider.CUSTOM;
  const normalized = provider.toLowerCase();
  switch (normalized) {
    case 'openai':
    case 'open_ai':
      return Provider.OPENAI;
    case 'claude':
      return Provider.CLAUDE;
    case 'gemini':
      return Provider.GEMINI;
    case 'azure':
    case 'azure-openai':
    case 'azure_openai':
      return Provider.AZURE;
    default:
      return Provider.CUSTOM;
  }
}

function mapBackendUser(user: BackendUserLike, fallback?: Partial<User>): User {
  const defaults: User = {
    id: fallback?.id ?? 'unknown',
    name: fallback?.name ?? 'Unknown',
    email: fallback?.email ?? '',
    avatar: fallback?.avatar,
    role: (fallback?.role as User['role']) ?? 'user',
    createdAt: fallback?.createdAt ?? new Date().toISOString(),
  };

  if (user === null || user === undefined) {
    return defaults;
  }

  if (typeof user === 'number' || typeof user === 'string') {
    return {
      ...defaults,
      id: String(user),
    };
  }

  const createdAt = user.createdAt ?? user.created_at ?? defaults.createdAt;
  const role = (user.role as User['role']) ?? defaults.role;

  return {
    id: String(user.id ?? defaults.id),
    name: user.name ?? user.username ?? defaults.name,
    email: user.email ?? defaults.email,
    avatar: user.avatar ?? defaults.avatar,
    role,
    createdAt,
  };
}

function mapBackendKBToFrontend(kb: BackendKnowledgeBase): KnowledgeBase {
  const owner = mapBackendUser(kb.owner ?? kb.ownerId, {
    id: kb.ownerId ? String(kb.ownerId) : undefined,
    createdAt: kb.createdAt,
  });

  const resolvedId = kb.kbId ?? (kb.id !== undefined && kb.id !== null ? String(kb.id) : undefined);

  return {
    id: resolvedId ?? '',
    name: kb.name,
    description: kb.description,
    fileCount: kb.fileCount,
    owner,
    members: Array.isArray(kb.members)
      ? kb.members.map((member) => ({
          user: mapBackendUser(member.user ?? member.userId, {
            id: member.userId ? String(member.userId) : undefined,
            createdAt: member.joinedAt ?? member.joined_at ?? kb.createdAt,
          }),
          role: mapMemberRole(member.role),
          joinedAt: member.joinedAt ?? member.joined_at ?? kb.createdAt,
        }))
      : [],
    createdAt: kb.createdAt,
    updatedAt: kb.updatedAt,
  };
}

function mapFileType(fileType?: string, mimeType?: string): KnowledgeFile['type'] {
  const value = (fileType || mimeType || '').toLowerCase();
  if (value.includes('pdf')) return 'pdf';
  if (value.includes('doc') || value.includes('word')) return 'word';
  return 'image';
}

function mapBackendFileToFrontend(file: BackendFile): KnowledgeFile {
  const ocrContent = (file as any).ocrContent ?? (file as any).ocr_content ?? file.ocrContent;

  const rawQaPairs = (file as any).qaPairs ?? (file as any).qa_pairs ?? file.qaPairs;
  const qaPairs = Array.isArray(rawQaPairs)
    ? rawQaPairs.map((pair: BackendQaPair) => ({
        id: String(pair.id ?? pair.qaId ?? ''),
        question: pair.question ?? '',
        answer: pair.answer ?? '',
        sourceText: pair.sourceText ?? pair.source_text ?? '',
      }))
    : undefined;

  const rawKnowledgeGraph = (file as any).knowledgeGraph ?? (file as any).knowledge_graph ?? file.knowledgeGraph;
  const knowledgeGraph = rawKnowledgeGraph && Array.isArray(rawKnowledgeGraph.nodes) && Array.isArray(rawKnowledgeGraph.edges)
    ? {
        nodes: rawKnowledgeGraph.nodes.map((node: BackendGraphNode) => ({
          id: String(node.id ?? ''),
          label: node.label ?? '',
          type: node.type ?? undefined,
        })),
        edges: rawKnowledgeGraph.edges.map((edge: BackendGraphEdge) => ({
          source: String(edge.source ?? ''),
          target: String(edge.target ?? ''),
          label: edge.label ?? undefined,
        })),
      }
    : undefined;

  return {
    id: file.id,
    knowledgeBaseId: String(file.knowledgeBaseId ?? file.kbId),
    name: file.name,
    type: mapFileType(file.type ?? file.fileType, file.mimeType),
    mimeType: file.mimeType,
    size: file.size,
    uploadedAt: file.uploadedAt,
    uploadedBy: mapBackendUser(file.uploader ?? file.uploadedBy, {
      id: String(file.uploadedBy),
      createdAt: file.uploadedAt,
    }),
    statuses: file.statuses ? {
      ocr: mapProcessingStatus(file.statuses.ocr ?? file.ocrStatus),
      vectorization: mapProcessingStatus(file.statuses.vectorization ?? file.vectorizationStatus),
      qaPairs: mapProcessingStatus(file.statuses.qaPairs ?? file.qaPairsStatus),
      knowledgeGraph: mapProcessingStatus(file.statuses.knowledgeGraph ?? file.knowledgeGraphStatus),
    } : {
      ocr: mapProcessingStatus(file.ocrStatus),
      vectorization: mapProcessingStatus(file.vectorizationStatus),
      qaPairs: mapProcessingStatus(file.qaPairsStatus),
      knowledgeGraph: mapProcessingStatus(file.knowledgeGraphStatus),
    },
    url: file.url,
    thumbnailUrl: file.thumbnailUrl,
    errorMessage: file.errorMessage,
    ocrContent: typeof ocrContent === 'string' ? ocrContent : undefined,
    knowledgeGraph,
    qaPairs,
  };
}

function maskApiKey(key?: string, displayKey?: string): string {
  if (displayKey) return displayKey;
  if (!key) return '';
  if (key.length <= 4) {
    return `${key.slice(0, 2)}****`;
  }
  if (key.length <= 8) {
    return `${key.slice(0, 4)}****`;
  }
  return `${key.slice(0, 4)}****${key.slice(-4)}`;
}

function mapBackendModel(model: BackendModel | Model): Model {
  return {
    id: String(model.id ?? 'unknown-model'),
    name: model.name ?? 'Unknown Model',
    provider: mapProvider(model.provider),
    description: model.description,
  };
}

function mapBackendBusiness(business: BackendBusiness): Business {
  const createdAt = business.createdAt ?? new Date().toISOString();
  return {
    id: String(business.id),
    name: business.name ?? 'Unknown Business',
    code: business.code ?? String(business.id),
    description: business.description,
    createdAt,
    createdBy: mapBackendUser(business.createdBy, {
      createdAt,
    }),
    enabled: business.enabled ?? true,
  };
}

function mapBackendEndpoint(endpoint: BackendEndpoint): Endpoint {
  const createdAt = endpoint.createdAt ?? new Date().toISOString();
  return {
    id: String(endpoint.id),
    name: endpoint.name ?? 'Unknown Endpoint',
    baseUrl: endpoint.baseUrl ?? '',
    provider: mapProvider(endpoint.provider),
    models: Array.isArray(endpoint.models)
      ? endpoint.models.map((model) => mapBackendModel(model))
      : [],
    businesses: Array.isArray(endpoint.businesses)
      ? endpoint.businesses.map((business) => mapBackendBusiness(business))
      : [],
    enabled: endpoint.enabled ?? true,
    createdAt,
    createdBy: mapBackendUser(endpoint.createdBy, {
      createdAt,
    }),
    updatedAt: endpoint.updatedAt,
    description: endpoint.description,
    stats: endpoint.stats
      ? {
          totalApiKeys: endpoint.stats.totalApiKeys ?? 0,
          activeApiKeys: endpoint.stats.activeApiKeys ?? 0,
          totalRequests: endpoint.stats.totalRequests ?? 0,
          successRate: endpoint.stats.successRate ?? 0,
        }
      : undefined,
  };
}

function mapBackendApiKey(apiKey: BackendApiKey): ApiKey {
  const createdAt = apiKey.createdAt ?? new Date().toISOString();
  const rawKey = apiKey.apiKey ?? apiKey.key ?? '';

  const endpointData = apiKey.endpoint;
  let endpoint: Endpoint | undefined;

  if (endpointData) {
    const hasFullShape = 'models' in endpointData || 'businesses' in endpointData || 'stats' in endpointData;

    if (hasFullShape) {
      endpoint = mapBackendEndpoint(endpointData as BackendEndpoint);
    } else {
      const summary = endpointData as BackendEndpointSummary;
      const summaryCreatedAt = summary.createdAt ?? createdAt;
      endpoint = {
        id: String(summary.id ?? apiKey.endpointId ?? 'unknown-endpoint'),
        name: summary.name ?? '未命名端点',
        baseUrl: summary.baseUrl ?? '',
        provider: mapProvider(summary.provider),
        models: Array.isArray(summary.models)
          ? summary.models.map((model) => mapBackendModel(model))
          : [],
        businesses: Array.isArray(summary.businesses)
          ? summary.businesses.map((business) => mapBackendBusiness(business))
          : [],
        enabled: true,
        createdAt: summaryCreatedAt,
        createdBy: mapBackendUser(summary.createdBy, {
          createdAt: summaryCreatedAt,
        }),
        updatedAt: undefined,
        description: summary.description,
        stats: summary.stats
          ? {
              totalApiKeys: summary.stats.totalApiKeys ?? 0,
              activeApiKeys: summary.stats.activeApiKeys ?? 0,
              totalRequests: summary.stats.totalRequests ?? 0,
              successRate: summary.stats.successRate ?? 0,
            }
          : undefined,
      };
    }
  }

  const rpm = apiKey.rateLimit?.requestsPerMinute ?? apiKey.rateLimitPerMinute;
  const rpd = apiKey.rateLimit?.requestsPerDay ?? apiKey.rateLimitPerDay;
  const totalRequests = apiKey.stats?.totalRequests ?? apiKey.totalRequests ?? 0;
  const successRequests = apiKey.stats?.successRequests ?? apiKey.successRequests ?? 0;
  const failedRequests = apiKey.stats?.failedRequests ?? apiKey.failedRequests ?? 0;
  const lastError = apiKey.stats?.lastError ?? apiKey.lastError;

  const hasRateLimit = rpm !== undefined || rpd !== undefined;

  const stats = {
    totalRequests,
    successRequests,
    failedRequests,
    lastError,
  };

  return {
    id: String(apiKey.id),
    endpointId: String(apiKey.endpointId),
    endpoint,
    key: rawKey,
    displayKey: maskApiKey(rawKey, apiKey.displayKey),
    name: apiKey.name ?? 'API Key',
    enabled: apiKey.enabled ?? true,
    createdAt,
    createdBy: mapBackendUser(apiKey.createdBy, {
      createdAt,
    }),
    expiresAt: apiKey.expiresAt,
    lastUsedAt: apiKey.lastUsedAt,
    rateLimit: hasRateLimit
      ? {
          requestsPerMinute: rpm ?? 0,
          requestsPerDay: rpd ?? 0,
        }
      : undefined,
    stats,
  };
}

function toNumber(value: unknown): number | undefined {
  if (typeof value === 'number' && !Number.isNaN(value)) {
    return value;
  }
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) return undefined;
    const parsed = Number(trimmed);
    return Number.isNaN(parsed) ? undefined : parsed;
  }
  return undefined;
}

function normalizeEndpointPayload(data: Record<string, unknown>) {
  const payload = data as {
    name: string;
    baseUrl: string;
    provider: string;
    description?: string;
    enabled?: boolean;
    businessIds?: Array<string | number>;
    modelNames?: Array<string>;
  };

  const businessIds = Array.isArray(payload.businessIds)
    ? payload.businessIds
        .map((id) => toNumber(id))
        .filter((id): id is number => id !== undefined)
    : [];

  const modelNames = Array.isArray(payload.modelNames)
    ? payload.modelNames
        .map((name) => String(name).trim())
        .filter((name) => name.length > 0)
    : [];

  return {
    name: payload.name,
    baseUrl: payload.baseUrl,
    provider: payload.provider,
    description: payload.description,
    enabled: payload.enabled ?? true,
    businessIds,
    modelNames,
  };
}

function normalizeApiKeyPayload(data: Record<string, unknown>) {
  const payload = data as {
    endpointId: string | number;
    name: string;
    apiKey?: string;
    key?: string;
    enabled?: boolean;
    requestsPerMinute?: number | string;
    requestsPerDay?: number | string;
    expiresAt?: string | Date;
  };

  const endpointId = toNumber(payload.endpointId);
  if (endpointId === undefined) {
    throw new Error('endpointId is required');
  }

  const requestsPerMinute = toNumber(payload.requestsPerMinute);
  const requestsPerDay = toNumber(payload.requestsPerDay);
  const expiresAt = payload.expiresAt instanceof Date
    ? payload.expiresAt.toISOString().split('T')[0]
    : payload.expiresAt;

  const apiKeySource = payload.apiKey ?? payload.key;
  const apiKey = typeof apiKeySource === 'string' && apiKeySource.trim().length > 0
    ? apiKeySource.trim()
    : undefined;

  return {
    endpointId,
    name: payload.name,
    apiKey,
    enabled: payload.enabled ?? true,
    requestsPerMinute,
    requestsPerDay,
    expiresAt,
  };
}

function normalizeBusinessPayload(data: Record<string, unknown>) {
  const payload = data as {
    name: string;
    code: string;
    description?: string;
    enabled?: boolean;
  };

  return {
    name: payload.name,
    code: typeof payload.code === 'string' ? payload.code.trim() : payload.code,
    description: payload.description,
    enabled: payload.enabled ?? true,
  };
}

// API客户端
export const api = {
  // ============================================================================
  // 认证相关
  // ============================================================================
  auth: {
    async login(email: string, password: string): Promise<{ user: User; token: string }> {
      const response = await request<{ user: BackendUserLike; token: string }>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });

      // 保存token到localStorage
      if (typeof window !== 'undefined' && response.token) {
        localStorage.setItem('auth_token', response.token);
      }

      const mappedUser = mapBackendUser(response.user);
      const authStore = useAuthStore.getState();
      authStore.setUser(mappedUser);
      authStore.setToken(response.token ?? null);

      return {
        token: response.token,
        user: mappedUser,
      };
    },

    async register(data: {
      username: string;
      email: string;
      password: string;
      name: string;
    }): Promise<{ user: User; token: string }> {
      const response = await request<{ user: BackendUserLike; token: string }>('/auth/register', {
        method: 'POST',
        body: JSON.stringify(data),
      });

      if (typeof window !== 'undefined' && response.token) {
        localStorage.setItem('auth_token', response.token);
      }

      const mappedUser = mapBackendUser(response.user);
      const authStore = useAuthStore.getState();
      authStore.setUser(mappedUser);
      authStore.setToken(response.token ?? null);

      return {
        token: response.token,
        user: mappedUser,
      };
    },

    async logout(): Promise<void> {
      if (typeof window !== 'undefined') {
        localStorage.removeItem('auth_token');
      }
      useAuthStore.getState().logout();
    },

    async getCurrentUser(): Promise<User> {
      const backendUser = await request<BackendUserLike>('/auth/me');
      const user = mapBackendUser(backendUser);
      useAuthStore.getState().setUser(user);
      return user;
    },
  },

  // ============================================================================
  // 知识库相关
  // ============================================================================
  knowledgeBases: {
    async list(): Promise<KnowledgeBase[]> {
      const backendList = await request<BackendKnowledgeBase[]>('/knowledge-bases');
      return backendList.map(mapBackendKBToFrontend);
    },

    async get(kbId: string): Promise<KnowledgeBase | null> {
      try {
        const kb = await request<BackendKnowledgeBase>(`/knowledge-bases/${kbId}`);
        return mapBackendKBToFrontend(kb);
      } catch (error) {
        return null;
      }
    },

    async create(data: {
      name: string;
      description?: string;
    }): Promise<KnowledgeBase> {
      const kb = await request<BackendKnowledgeBase>('/knowledge-bases', {
        method: 'POST',
        body: JSON.stringify(data),
      });
      return mapBackendKBToFrontend(kb);
    },

    async update(
      kbId: string,
      data: { name?: string; description?: string }
    ): Promise<KnowledgeBase> {
      const kb = await request<BackendKnowledgeBase>(`/knowledge-bases/${kbId}`, {
        method: 'PUT',
        body: JSON.stringify(data),
      });
      return mapBackendKBToFrontend(kb);
    },

    async delete(kbId: string): Promise<void> {
      await request(`/knowledge-bases/${kbId}`, {
        method: 'DELETE',
      });
    },
  },

  // ============================================================================
  // 文件相关
  // ============================================================================
  files: {
    async list(kbId: string): Promise<KnowledgeFile[]> {
      const backendFiles = await request<BackendFile[]>(`/knowledge-bases/${kbId}/files`);
      return backendFiles.map(mapBackendFileToFrontend);
    },

    async get(fileId: string): Promise<KnowledgeFile | null> {
      try {
        const file = await request<BackendFile>(`/files/${fileId}`);
        return mapBackendFileToFrontend(file);
      } catch (error) {
        return null;
      }
    },

    async upload(
      kbId: string,
      file: globalThis.File,
      onProgress?: (progress: number) => void
    ): Promise<KnowledgeFile> {
      const backendFile = await uploadFile<BackendFile>(
        `/knowledge-bases/${kbId}/files`,
        file,
        onProgress
      );
      return mapBackendFileToFrontend(backendFile);
    },

    async delete(fileId: string): Promise<void> {
      await request(`/files/${fileId}`, {
        method: 'DELETE',
      });
    },

    async getOcrContent(fileId: string): Promise<string> {
      return request(`/files/${fileId}/ocr-content`);
    },

    async getQAPairs(fileId: string): Promise<any[]> {
      return request(`/files/${fileId}/qa-pairs`);
    },

    async getKnowledgeGraph(fileId: string): Promise<any> {
      return request(`/files/${fileId}/knowledge-graph`);
    },

    async triggerProcess(fileId: string, type: 'ocr' | 'vectorization' | 'qa-pairs' | 'knowledge-graph'): Promise<void> {
      await request(`/files/${fileId}/process/${type}`, {
        method: 'POST',
      });
    },
    async updateOcrContent(fileId: string, content: string): Promise<void> {
      await request(`/files/${fileId}/ocr-content`, {
        method: 'PUT',
        body: JSON.stringify({ content }),
      });
    },
  },

  // ============================================================================
  // 模型配置相关
  // ============================================================================
  modelConfig: {
    // 端点管理
    endpoints: {
      async list(): Promise<Endpoint[]> {
        const endpoints = await request<BackendEndpoint[]>('/model/endpoints');
        return endpoints.map(mapBackendEndpoint);
      },

      async get(id: number | string): Promise<Endpoint> {
        const endpoint = await request<BackendEndpoint>(`/model/endpoints/${id}`);
        return mapBackendEndpoint(endpoint);
      },

      async create(data: Record<string, unknown>): Promise<Endpoint> {
        const endpoint = await request<BackendEndpoint>('/model/endpoints', {
          method: 'POST',
          body: JSON.stringify(normalizeEndpointPayload(data)),
        });
        return mapBackendEndpoint(endpoint);
      },

      async update(id: number | string, data: Record<string, unknown>): Promise<Endpoint> {
        const endpoint = await request<BackendEndpoint>(`/model/endpoints/${id}`, {
          method: 'PUT',
          body: JSON.stringify(normalizeEndpointPayload(data)),
        });
        return mapBackendEndpoint(endpoint);
      },

      async delete(id: number | string): Promise<void> {
        await request(`/model/endpoints/${id}`, {
          method: 'DELETE',
        });
      },
    },

    // API Key管理
    apiKeys: {
      async list(endpointId?: number | string): Promise<ApiKey[]> {
        const endpointParam = endpointId !== undefined && endpointId !== null
          ? toNumber(endpointId)
          : undefined;
        const url = endpointParam !== undefined
          ? `/model/api-keys?endpointId=${endpointParam}`
          : '/model/api-keys';
        const apiKeys = await request<BackendApiKey[]>(url);
        return apiKeys.map(mapBackendApiKey);
      },

      async get(id: number | string): Promise<ApiKey> {
        const apiKey = await request<BackendApiKey>(`/model/api-keys/${id}`);
        return mapBackendApiKey(apiKey);
      },

      async create(data: Record<string, unknown>): Promise<ApiKey> {
        const apiKey = await request<BackendApiKey>('/model/api-keys', {
          method: 'POST',
          body: JSON.stringify(normalizeApiKeyPayload(data)),
        });
        return mapBackendApiKey(apiKey);
      },

      async update(id: number | string, data: Record<string, unknown>): Promise<ApiKey> {
        const apiKey = await request<BackendApiKey>(`/model/api-keys/${id}`, {
          method: 'PUT',
          body: JSON.stringify(normalizeApiKeyPayload(data)),
        });
        return mapBackendApiKey(apiKey);
      },

      async delete(id: number | string): Promise<void> {
        await request(`/model/api-keys/${id}`, {
          method: 'DELETE',
        });
      },
    },

    // 业务配置
    business: {
      async list(): Promise<Business[]> {
        const businesses = await request<BackendBusiness[]>('/model/business');
        return businesses.map(mapBackendBusiness);
      },

      async get(id: number | string): Promise<Business> {
        const business = await request<BackendBusiness>(`/model/business/${id}`);
        return mapBackendBusiness(business);
      },

      async create(data: Record<string, unknown>): Promise<Business> {
        const business = await request<BackendBusiness>('/model/business', {
          method: 'POST',
          body: JSON.stringify(normalizeBusinessPayload(data)),
        });
        return mapBackendBusiness(business);
      },

      async update(id: number | string, data: Record<string, unknown>): Promise<Business> {
        const business = await request<BackendBusiness>(`/model/business/${id}`, {
          method: 'PUT',
          body: JSON.stringify(normalizeBusinessPayload(data)),
        });
        return mapBackendBusiness(business);
      },

      async delete(id: number | string): Promise<void> {
        await request(`/model/business/${id}`, {
          method: 'DELETE',
        });
      },
    },
  },

  // ============================================================================
  // Prompt 模板管理
  // ============================================================================
  prompts: {
    /**
     * 获取所有 Prompt 列表
     */
    async list(): Promise<Prompt[]> {
      return request<Prompt[]>('/prompts');
    },

    /**
     * 根据 ID 获取 Prompt
     */
    async get(id: number): Promise<Prompt> {
      return request<Prompt>(`/prompts/${id}`);
    },

    /**
     * 根据业务代码获取 Prompt
     */
    async getByBusinessCode(businessCode: string): Promise<Prompt> {
      return request<Prompt>(`/prompts/business/${businessCode}`);
    },

    /**
     * 创建 Prompt
     */
    async create(data: PromptRequest): Promise<Prompt> {
      return request<Prompt>('/prompts', {
        method: 'POST',
        body: JSON.stringify(data),
      });
    },

    /**
     * 更新 Prompt
     */
    async update(id: number, data: PromptRequest): Promise<Prompt> {
      return request<Prompt>(`/prompts/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data),
      });
    },

    /**
     * 删除 Prompt
     */
    async delete(id: number): Promise<void> {
      await request(`/prompts/${id}`, {
        method: 'DELETE',
      });
    },

    /**
     * 激活/停用 Prompt
     */
    async toggleActive(id: number, isActive: boolean): Promise<void> {
      await request(`/prompts/${id}/toggle?isActive=${isActive}`, {
        method: 'POST',
      });
    },
  },
};

// 兼容旧代码的mockAPI导出
export const mockAPI = {
  getKnowledgeBases: api.knowledgeBases.list,
  getKnowledgeBase: api.knowledgeBases.get,
  createKnowledgeBase: api.knowledgeBases.create,
  deleteKnowledgeBase: api.knowledgeBases.delete,
  getFiles: api.files.list,
  getFile: api.files.get,
};
