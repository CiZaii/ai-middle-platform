/**
 * Prompt 模板类型定义
 */
export interface Prompt {
  id: number;
  promptId: string;
  businessCode: string;
  promptName: string;
  promptContent: string;
  description?: string;
  variables: string[];
  isActive: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Prompt 创建/更新请求
 */
export interface PromptRequest {
  businessCode: string;
  promptName: string;
  promptContent: string;
  description?: string;
  variables?: string;
  isActive: boolean;
}

/**
 * 业务代码枚举
 */
export enum BusinessCode {
  OCR = 'ocr',
  QA = 'qa',
  KG = 'kg',
  TAG = 'tag',
}

/**
 * 业务名称映射
 */
export const BUSINESS_NAME_MAP: Record<string, string> = {
  ocr: 'OCR识别',
  qa: '问答对生成',
  kg: '知识图谱',
  tag: '标签生成',
};
