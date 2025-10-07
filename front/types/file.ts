import { User } from './user';

export enum ProcessingStatus {
  PENDING = 'pending',
  PROCESSING = 'processing',
  COMPLETED = 'completed',
  FAILED = 'failed',
}

export interface FileStatuses {
  ocr: ProcessingStatus;
  vectorization: ProcessingStatus;
  qaPairs: ProcessingStatus;
  knowledgeGraph: ProcessingStatus;
}

export interface QAPair {
  id: string;
  question: string;
  answer: string;
  sourceText: string;
}

export interface KnowledgeGraphNode {
  id: string;
  label: string;
  type?: string;
}

export interface KnowledgeGraphEdge {
  source: string;
  target: string;
  label?: string;
}

export interface KnowledgeGraph {
  nodes: KnowledgeGraphNode[];
  edges: KnowledgeGraphEdge[];
}

export interface File {
  id: string;
  knowledgeBaseId: string;
  name: string;
  type: 'pdf' | 'image' | 'word';
  mimeType: string;
  size: number;
  uploadedAt: string;
  uploadedBy: User;
  statuses: FileStatuses;
  url: string;
  thumbnailUrl?: string;
  errorMessage?: string;
  // 新增字段
  ocrContent?: string;
  knowledgeGraph?: KnowledgeGraph;
  qaPairs?: QAPair[];
}
