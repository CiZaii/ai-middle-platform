import { KnowledgeBase } from '@/types/knowledge-base';
import { mockUsers } from './users';

export const mockKnowledgeBases: KnowledgeBase[] = [
  {
    id: 'kb-1',
    name: '技术文档库',
    description: '存储技术文档、API 文档和架构设计',
    createdAt: '2024-01-15T08:00:00Z',
    updatedAt: '2024-01-20T10:30:00Z',
    fileCount: 45,
    owner: mockUsers[0],
    members: [
      {
        user: mockUsers[1],
        role: 'editor',
        joinedAt: '2024-01-16T09:00:00Z',
      },
    ],
  },
  {
    id: 'kb-2',
    name: '产品需求文档',
    description: 'PRD、用户故事和产品设计文档',
    createdAt: '2024-01-10T08:00:00Z',
    updatedAt: '2024-01-18T14:20:00Z',
    fileCount: 28,
    owner: mockUsers[0],
    members: [],
  },
  {
    id: 'kb-3',
    name: '会议纪要',
    description: '团队会议记录和决策文档',
    createdAt: '2024-01-05T08:00:00Z',
    updatedAt: '2024-01-22T16:45:00Z',
    fileCount: 67,
    owner: mockUsers[1],
    members: [
      {
        user: mockUsers[0],
        role: 'viewer',
        joinedAt: '2024-01-06T10:00:00Z',
      },
    ],
  },
  {
    id: 'kb-4',
    name: '营销素材库',
    description: '品牌素材、海报、视频等营销资源',
    createdAt: '2024-01-12T08:00:00Z',
    updatedAt: '2024-01-19T11:30:00Z',
    fileCount: 156,
    owner: mockUsers[0],
    members: [],
  },
  {
    id: 'kb-5',
    name: '培训资料',
    description: '员工培训、入职文档和教程',
    createdAt: '2024-01-08T08:00:00Z',
    updatedAt: '2024-01-21T09:15:00Z',
    fileCount: 34,
    owner: mockUsers[1],
    members: [],
  },
];
