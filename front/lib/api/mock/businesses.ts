import { Business } from '@/types/model-config';
import { mockUsers } from './users';

export const mockBusinesses: Business[] = [
  {
    id: 'biz-1',
    name: '智能客服',
    code: 'CUSTOMER_SERVICE',
    description: '为客服系统提供AI对话能力',
    createdAt: '2024-01-10T08:00:00Z',
    createdBy: mockUsers[0],
    enabled: true,
  },
  {
    id: 'biz-2',
    name: '内容生成',
    code: 'CONTENT_GEN',
    description: '文章、广告文案等内容自动生成',
    createdAt: '2024-01-12T09:00:00Z',
    createdBy: mockUsers[0],
    enabled: true,
  },
  {
    id: 'biz-3',
    name: '代码助手',
    code: 'CODE_ASSISTANT',
    description: '代码生成、审查和优化',
    createdAt: '2024-01-15T10:00:00Z',
    createdBy: mockUsers[1],
    enabled: true,
  },
  {
    id: 'biz-4',
    name: '数据分析',
    code: 'DATA_ANALYSIS',
    description: '智能数据分析和报告生成',
    createdAt: '2024-01-18T11:00:00Z',
    createdBy: mockUsers[0],
    enabled: true,
  },
  {
    id: 'biz-5',
    name: '翻译服务',
    code: 'TRANSLATION',
    description: '多语言智能翻译',
    createdAt: '2024-01-20T14:00:00Z',
    createdBy: mockUsers[1],
    enabled: false,
  },
];
