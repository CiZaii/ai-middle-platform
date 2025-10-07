import { File, ProcessingStatus } from '@/types/file';
import { mockUsers } from './users';

export const mockFiles: File[] = [
  {
    id: 'file-1',
    knowledgeBaseId: 'kb-1',
    name: 'Next.js 官方文档.pdf',
    type: 'pdf',
    mimeType: 'application/pdf',
    size: 5242880,
    uploadedAt: '2024-01-15T10:00:00Z',
    uploadedBy: mockUsers[0],
    statuses: {
      ocr: ProcessingStatus.COMPLETED,
      vectorization: ProcessingStatus.PROCESSING,
      qaPairs: ProcessingStatus.PENDING,
      knowledgeGraph: ProcessingStatus.PENDING,
    },
    url: '/mock/files/nextjs-docs.pdf',
    thumbnailUrl: '/mock/thumbnails/nextjs-docs.jpg',
    ocrContent: `# Next.js 简介

Next.js 是一个 React 框架,用于构建全栈 Web 应用程序。

## 主要特性

### 1. 服务端渲染 (SSR)
Next.js 提供了开箱即用的服务端渲染支持,可以显著提升首屏加载速度和 SEO 性能。

\`\`\`javascript
export async function getServerSideProps(context) {
  return {
    props: {}, // 将作为 props 传递给页面组件
  }
}
\`\`\`

### 2. 静态网站生成 (SSG)
通过 \`getStaticProps\` 在构建时生成静态页面。

| 特性 | SSR | SSG |
|------|-----|-----|
| 构建时间 | 短 | 长 |
| 运行时性能 | 中等 | 优秀 |
| 适用场景 | 动态内容 | 静态内容 |

### 3. API 路由
Next.js 允许在 \`pages/api\` 目录下创建 API 端点。

## 数学公式示例

勾股定理: $a^2 + b^2 = c^2$

积分公式:

$$\\int_{a}^{b} f(x)dx = F(b) - F(a)$$

## 总结

Next.js 是现代 Web 开发的优秀选择,结合了 React 的灵活性和服务端渲染的性能优势。`,
  },
  {
    id: 'file-2',
    knowledgeBaseId: 'kb-1',
    name: 'React 架构图.png',
    type: 'image',
    mimeType: 'image/png',
    size: 1048576,
    uploadedAt: '2024-01-15T11:00:00Z',
    uploadedBy: mockUsers[0],
    statuses: {
      ocr: ProcessingStatus.PROCESSING,
      vectorization: ProcessingStatus.PENDING,
      qaPairs: ProcessingStatus.PENDING,
      knowledgeGraph: ProcessingStatus.PENDING,
    },
    url: '/mock/files/react-architecture.png',
  },
  {
    id: 'file-3',
    knowledgeBaseId: 'kb-1',
    name: '数据库设计文档.pdf',
    type: 'pdf',
    mimeType: 'application/pdf',
    size: 3145728,
    uploadedAt: '2024-01-16T09:00:00Z',
    uploadedBy: mockUsers[1],
    statuses: {
      ocr: ProcessingStatus.FAILED,
      vectorization: ProcessingStatus.PENDING,
      qaPairs: ProcessingStatus.PENDING,
      knowledgeGraph: ProcessingStatus.PENDING,
    },
    url: '/mock/files/database-design.pdf',
    errorMessage: 'OCR 处理失败：文件格式不支持',
  },
  {
    id: 'file-4',
    knowledgeBaseId: 'kb-1',
    name: 'API 接口文档.pdf',
    type: 'pdf',
    mimeType: 'application/pdf',
    size: 2097152,
    uploadedAt: '2024-01-16T10:30:00Z',
    uploadedBy: mockUsers[0],
    statuses: {
      ocr: ProcessingStatus.COMPLETED,
      vectorization: ProcessingStatus.COMPLETED,
      qaPairs: ProcessingStatus.COMPLETED,
      knowledgeGraph: ProcessingStatus.COMPLETED,
    },
    url: '/mock/files/api-docs.pdf',
    ocrContent: `# API 接口文档

## 用户认证 API

### POST /api/auth/login
用户登录接口

**请求参数:**
- email: string (必填)
- password: string (必填)

**响应示例:**
\`\`\`json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "123",
    "name": "张三"
  }
}
\`\`\``,
    knowledgeGraph: {
      nodes: [
        { id: '1', label: 'API 接口文档', type: 'document' },
        { id: '2', label: '用户认证', type: 'module' },
        { id: '3', label: 'POST /api/auth/login', type: 'endpoint' },
        { id: '4', label: '请求参数', type: 'schema' },
        { id: '5', label: '响应格式', type: 'schema' },
        { id: '6', label: 'Token', type: 'field' },
      ],
      edges: [
        { source: '1', target: '2', label: '包含' },
        { source: '2', target: '3', label: '定义' },
        { source: '3', target: '4', label: '接收' },
        { source: '3', target: '5', label: '返回' },
        { source: '5', target: '6', label: '包含' },
      ],
    },
    qaPairs: [
      {
        id: 'qa-1',
        question: '用户登录接口的 URL 是什么?',
        answer: '用户登录接口的 URL 是 POST /api/auth/login',
        sourceText: 'POST /api/auth/login 用户登录接口',
      },
      {
        id: 'qa-2',
        question: '登录接口需要哪些必填参数?',
        answer: '登录接口需要两个必填参数：email (邮箱地址) 和 password (密码)',
        sourceText: '请求参数: email: string (必填), password: string (必填)',
      },
      {
        id: 'qa-3',
        question: '登录成功后会返回什么数据?',
        answer: '登录成功后会返回一个 token (认证令牌) 和 user 对象 (包含用户 id 和 name)',
        sourceText: '响应示例: { "token": "eyJhbGciOiJIUzI1NiIs...", "user": { "id": "123", "name": "张三" } }',
      },
    ],
  },
  {
    id: 'file-5',
    knowledgeBaseId: 'kb-1',
    name: '用户手册.pdf',
    type: 'pdf',
    mimeType: 'application/pdf',
    size: 4194304,
    uploadedAt: '2024-01-17T14:00:00Z',
    uploadedBy: mockUsers[0],
    statuses: {
      ocr: ProcessingStatus.COMPLETED,
      vectorization: ProcessingStatus.COMPLETED,
      qaPairs: ProcessingStatus.PROCESSING,
      knowledgeGraph: ProcessingStatus.PENDING,
    },
    url: '/mock/files/user-manual.pdf',
    ocrContent: `# 系统使用手册

## 快速开始

1. 注册账号
2. 创建知识库
3. 上传文档

## 功能说明

系统支持 **PDF**、**Word** 和 **图片** 格式的文档上传。`,
  },
];
