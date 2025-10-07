# 🎨 知识库管理系统 - 完整项目规划

> **创意渐变风格 × 流畅交互体验**
> 基于 Next.js 15 + React 19 + Shadcn/ui

---

## 📑 目录

- [1. 项目概述](#1-项目概述)
- [2. 技术架构设计](#2-技术架构设计)
- [3. 设计系统规范](#3-设计系统规范)
- [4. 目录结构规划](#4-目录结构规划)
- [5. 数据模型设计](#5-数据模型设计)
- [6. 核心页面和组件设计](#6-核心页面和组件设计)
- [7. 交互体验设计](#7-交互体验设计)
- [8. 性能优化方案](#8-性能优化方案)
- [9. Mock 数据设计](#9-mock-数据设计)
- [10. 实施步骤和优先级](#10-实施步骤和优先级)
- [11. 验收标准](#11-验收标准)

---

## 1. 项目概述

### 🎯 项目目标

构建一个**视觉精致、交互流畅**的知识库管理系统，支持：
- 多知识库的创建和团队协作管理
- 文件上传及四状态处理流程（OCR、向量化、问答对、知识图谱）
- 文件详情的多维度可视化展示

### 💎 核心价值主张

1. **创意渐变美学**：蓝紫色渐变主题 + 玻璃拟态设计
2. **流畅交互体验**：300ms 页面过渡 + 骨架屏 + 乐观更新
3. **性能优化**：虚拟滚动支持 1000+ 文件 + 知识图谱几百节点流畅渲染
4. **团队协作**：知识库级别权限控制（Owner/Editor/Viewer）

### ✨ 技术亮点

- ⚡ Next.js 15 App Router + React 19 并发特性
- 🎨 Shadcn/ui 无样式组件 + Tailwind CSS 原子化样式
- 📊 React Flow 知识图谱可视化（支持节点拖动）
- 📝 完整 Markdown 渲染（表格 + LaTeX 数学公式）
- 🚀 分片上传 + 断点续传
- 🎭 Framer Motion 声明式动画

---

## 2. 技术架构设计

### 🏗️ 前端架构图

```
┌─────────────────────────────────────────────────────────┐
│                     Presentation Layer                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Pages/Routes │  │   Layouts    │  │  Components  │  │
│  │  (App Router)│  │              │  │  (ui/features)│  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   Business Logic Layer                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Custom Hooks │  │   Zustand    │  │ React Query  │  │
│  │              │  │   Stores     │  │  (API State) │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                      Data Access Layer                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  API Client  │  │  Mock Data   │  │ localStorage │  │
│  │   (Axios)    │  │              │  │  (Persist)   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 🛠️ 技术栈映射

| 功能模块 | 技术选型 | 说明 |
|---------|---------|------|
| **基础框架** | Next.js 15 + React 19 + TypeScript | App Router + 服务端组件 |
| **UI 组件** | Shadcn/ui + Radix UI | 无样式组件 + 可访问性 |
| **样式方案** | Tailwind CSS + CVA | 原子化 CSS + 组件变体 |
| **状态管理** | Zustand + React Query | 客户端状态 + 服务端状态 |
| **表单处理** | React Hook Form + Zod | 高性能表单 + 类型安全验证 |
| **Markdown 渲染** | react-markdown + remark-gfm + rehype-katex | GFM 表格 + LaTeX 公式 |
| **知识图谱** | React Flow | 节点图可视化 + 拖拽 |
| **文件预览** | react-pdf + 自定义图片查看器 | PDF 渲染 + 图片缩放 |
| **动画库** | Framer Motion + Lottie | 页面过渡 + 微交互 |
| **图标库** | Lucide React | 一致性图标系统 |
| **工具库** | date-fns + clsx + tailwind-merge | 日期处理 + 类名合并 |

### 🗂️ 状态管理方案

#### Zustand Stores 设计

```typescript
// stores/auth-store.ts
interface AuthStore {
  user: User | null;
  token: string | null;
  login: (credentials: LoginCredentials) => Promise<void>;
  logout: () => void;
}

// stores/knowledge-base-store.ts
interface KnowledgeBaseStore {
  selectedKnowledgeBase: KnowledgeBase | null;
  setSelectedKnowledgeBase: (kb: KnowledgeBase) => void;
}

// stores/theme-store.ts
interface ThemeStore {
  mode: 'light' | 'dark';
  toggleTheme: () => void;
}

// stores/upload-store.ts
interface UploadStore {
  uploads: UploadTask[];
  addUpload: (file: File) => void;
  updateProgress: (id: string, progress: number) => void;
  removeUpload: (id: string) => void;
}
```

#### React Query 配置

```typescript
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,      // 5 分钟
      cacheTime: 10 * 60 * 1000,     // 10 分钟
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});
```

#### 关键查询定义

```typescript
// 知识库列表
useQuery({
  queryKey: ['knowledge-bases'],
  queryFn: fetchKnowledgeBases,
});

// 文件列表（带轮询）
useQuery({
  queryKey: ['files', knowledgeBaseId],
  queryFn: () => fetchFiles(knowledgeBaseId),
  refetchInterval: (data) => {
    // 如果有文件处于 processing 状态，启用 2s 轮询
    const hasProcessing = data?.some(file =>
      Object.values(file.statuses).includes('processing')
    );
    return hasProcessing ? 2000 : false;
  },
});

// 文件详情
useQuery({
  queryKey: ['file', fileId],
  queryFn: () => fetchFileDetail(fileId),
});
```

---

## 3. 设计系统规范

### 🎨 颜色系统

#### 主色（渐变蓝紫）

```css
:root {
  /* 主色渐变 */
  --primary-start: 99 102 241;      /* #6366f1 Indigo-500 */
  --primary-end: 139 92 246;        /* #8b5cf6 Violet-500 */

  /* 渐变变体 */
  --gradient-primary: linear-gradient(135deg,
    rgb(var(--primary-start)) 0%,
    rgb(var(--primary-end)) 100%
  );
  --gradient-primary-hover: linear-gradient(135deg,
    rgb(99 102 241 / 0.9) 0%,
    rgb(139 92 246 / 0.9) 100%
  );
}
```

#### 辅色（点缀色）

```css
:root {
  --accent-cyan: 34 211 238;        /* #22d3ee Cyan-400 */
  --accent-pink: 236 72 153;        /* #ec4899 Pink-500 */
  --accent-amber: 251 191 36;       /* #fbbf24 Amber-400 */
}
```

#### 语义色

```css
:root {
  /* Success - 绿色渐变 */
  --success: 34 197 94;             /* #22c55e Green-500 */
  --success-light: 134 239 172;     /* #86efac Green-300 */

  /* Warning - 琥珀色 */
  --warning: 251 146 60;            /* #fb923c Orange-400 */

  /* Error - 红色 */
  --error: 239 68 68;               /* #ef4444 Red-500 */

  /* Info - 蓝色 */
  --info: 59 130 246;               /* #3b82f6 Blue-500 */
}
```

#### 中性色（深色/浅色模式）

```css
/* Light Mode */
:root {
  --background: 255 255 255;        /* #ffffff */
  --foreground: 15 23 42;           /* #0f172a Slate-900 */
  --muted: 241 245 249;             /* #f1f5f9 Slate-100 */
  --muted-foreground: 100 116 139;  /* #64748b Slate-500 */
  --border: 226 232 240;            /* #e2e8f0 Slate-200 */
  --card: 255 255 255;
  --card-foreground: 15 23 42;
}

/* Dark Mode */
.dark {
  --background: 15 23 42;           /* #0f172a Slate-900 */
  --foreground: 248 250 252;        /* #f8fafc Slate-50 */
  --muted: 30 41 59;                /* #1e293b Slate-800 */
  --muted-foreground: 148 163 184;  /* #94a3b8 Slate-400 */
  --border: 51 65 85;               /* #334155 Slate-700 */
  --card: 30 41 59;
  --card-foreground: 248 250 252;
}
```

### ✍️ 排版系统

```css
:root {
  /* 字体家族 */
  --font-sans: 'Inter', -apple-system, 'PingFang SC', sans-serif;
  --font-mono: 'JetBrains Mono', 'Consolas', monospace;

  /* 字号层级 */
  --text-xs: 0.75rem;      /* 12px */
  --text-sm: 0.875rem;     /* 14px */
  --text-base: 1rem;       /* 16px */
  --text-lg: 1.125rem;     /* 18px */
  --text-xl: 1.25rem;      /* 20px */
  --text-2xl: 1.5rem;      /* 24px */
  --text-3xl: 1.875rem;    /* 30px */
  --text-4xl: 2.25rem;     /* 36px */

  /* 行高 */
  --leading-tight: 1.25;   /* 标题 */
  --leading-normal: 1.5;   /* 正文 */
  --leading-relaxed: 1.75; /* 长文本 */

  /* 字重 */
  --font-normal: 400;
  --font-medium: 500;
  --font-semibold: 600;
  --font-bold: 700;
}
```

### 📏 间距系统（8px 基准网格）

```typescript
const spacing = {
  0: '0px',
  1: '0.25rem',  // 4px
  2: '0.5rem',   // 8px
  3: '0.75rem',  // 12px
  4: '1rem',     // 16px
  6: '1.5rem',   // 24px
  8: '2rem',     // 32px
  12: '3rem',    // 48px
  16: '4rem',    // 64px
  20: '5rem',    // 80px
};
```

### 🔲 圆角规范

```typescript
const borderRadius = {
  sm: '0.25rem',  // 4px  - 小组件（Badge）
  md: '0.5rem',   // 8px  - 按钮、输入框
  lg: '0.75rem',  // 12px - 卡片
  xl: '1rem',     // 16px - 大卡片、弹窗
  '2xl': '1.5rem',// 24px - 特殊容器
  full: '9999px', // 圆形
};
```

### 🌓 阴影层级

```css
:root {
  /* 浅色模式阴影 */
  --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
  --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1);
  --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1);
  --shadow-xl: 0 20px 25px -5px rgb(0 0 0 / 0.1);

  /* 玻璃拟态阴影 */
  --shadow-glass: 0 8px 32px 0 rgba(99, 102, 241, 0.15);
}

.dark {
  --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.3);
  --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.4);
  --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.5);
  --shadow-xl: 0 20px 25px -5px rgb(0 0 0 / 0.6);
}
```

### 🎬 动画规范

```typescript
// 时长
const duration = {
  fast: '150ms',
  normal: '300ms',
  slow: '500ms',
};

// 缓动函数
const easing = {
  easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
  easeOut: 'cubic-bezier(0, 0, 0.2, 1)',
  easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
  spring: 'cubic-bezier(0.34, 1.56, 0.64, 1)', // 弹性效果
};

// Framer Motion 配置
const transition = {
  fast: { duration: 0.15, ease: [0, 0, 0.2, 1] },
  normal: { duration: 0.3, ease: [0, 0, 0.2, 1] },
  slow: { duration: 0.5, ease: [0, 0, 0.2, 1] },
  spring: { type: 'spring', stiffness: 300, damping: 30 },
};
```

### 🎨 组件变体规范（CVA）

```typescript
// Button 变体示例
const buttonVariants = cva(
  'inline-flex items-center justify-center rounded-md font-medium transition-all',
  {
    variants: {
      variant: {
        primary: 'bg-gradient-to-r from-primary-start to-primary-end text-white hover:opacity-90',
        secondary: 'bg-muted text-muted-foreground hover:bg-muted/80',
        outline: 'border-2 border-primary bg-transparent hover:bg-primary/10',
        ghost: 'hover:bg-muted',
      },
      size: {
        sm: 'h-8 px-3 text-sm',
        md: 'h-10 px-4 text-base',
        lg: 'h-12 px-6 text-lg',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  }
);
```

---

## 4. 目录结构规划

```
front/
├── app/                                # Next.js App Router
│   ├── (auth)/                        # 认证路由组
│   │   ├── login/
│   │   │   └── page.tsx              # 登录页
│   │   └── layout.tsx                # 认证布局
│   ├── (dashboard)/                   # 主应用路由组
│   │   ├── knowledge-bases/
│   │   │   └── page.tsx              # 知识库列表页
│   │   ├── kb/
│   │   │   └── [id]/
│   │   │       └── page.tsx          # 知识库详情（文件列表）
│   │   ├── files/
│   │   │   └── [id]/
│   │   │       └── page.tsx          # 文件详情页
│   │   └── layout.tsx                # Dashboard 布局
│   ├── api/                           # API Routes（可选）
│   │   └── mock/
│   │       └── route.ts              # Mock API 端点
│   ├── globals.css                    # 全局样式 + Tailwind
│   ├── layout.tsx                     # 根布局
│   └── page.tsx                       # 首页（重定向到登录）
│
├── components/
│   ├── ui/                            # Shadcn/ui 基础组件
│   │   ├── button.tsx
│   │   ├── card.tsx
│   │   ├── dialog.tsx
│   │   ├── tabs.tsx
│   │   ├── table.tsx
│   │   ├── badge.tsx
│   │   ├── progress.tsx
│   │   ├── skeleton.tsx
│   │   ├── toast.tsx
│   │   ├── dropdown-menu.tsx
│   │   └── ...
│   ├── features/                      # 业务功能组件
│   │   ├── knowledge-base/
│   │   │   ├── kb-card.tsx           # 知识库卡片
│   │   │   ├── kb-create-dialog.tsx  # 创建知识库弹窗
│   │   │   └── kb-settings.tsx       # 知识库设置
│   │   ├── file-upload/
│   │   │   ├── dropzone.tsx          # 拖拽上传区域
│   │   │   ├── upload-progress.tsx   # 上传进度条
│   │   │   └── file-preview.tsx      # 文件预览列表
│   │   ├── file-list/
│   │   │   ├── file-table.tsx        # 文件列表表格
│   │   │   ├── file-grid.tsx         # 文件网格视图
│   │   │   ├── file-filters.tsx      # 文件筛选器
│   │   │   └── status-badge.tsx      # 状态徽章
│   │   ├── file-preview/
│   │   │   ├── pdf-viewer.tsx        # PDF 查看器
│   │   │   └── image-viewer.tsx      # 图片查看器
│   │   ├── ocr-markdown/
│   │   │   └── markdown-renderer.tsx # Markdown 渲染器
│   │   ├── knowledge-graph/
│   │   │   ├── graph-viewer.tsx      # 图谱查看器
│   │   │   ├── node-detail.tsx       # 节点详情面板
│   │   │   └── graph-toolbar.tsx     # 图谱工具栏
│   │   └── qa-pairs/
│   │       ├── qa-list.tsx           # 问答对列表
│   │       └── qa-item.tsx           # 单个问答对
│   └── layouts/
│       ├── dashboard-layout.tsx       # Dashboard 主布局
│       ├── sidebar.tsx                # 侧边栏
│       ├── header.tsx                 # 顶部导航
│       └── theme-toggle.tsx           # 主题切换器
│
├── lib/
│   ├── api/
│   │   ├── client.ts                  # Axios 客户端封装
│   │   ├── knowledge-base.ts          # 知识库 API
│   │   ├── file.ts                    # 文件 API
│   │   ├── auth.ts                    # 认证 API
│   │   └── mock/
│   │       ├── index.ts              # Mock 数据入口
│   │       ├── knowledge-bases.ts    # 知识库 Mock
│   │       ├── files.ts              # 文件 Mock
│   │       ├── knowledge-graph.ts    # 图谱 Mock
│   │       ├── qa-pairs.ts           # 问答对 Mock
│   │       └── ocr-content.ts        # OCR 内容 Mock
│   ├── hooks/
│   │   ├── use-knowledge-bases.ts    # 知识库相关 hooks
│   │   ├── use-files.ts              # 文件相关 hooks
│   │   ├── use-file-upload.ts        # 文件上传 hook
│   │   ├── use-polling.ts            # 轮询 hook
│   │   ├── use-theme.ts              # 主题 hook
│   │   └── use-debounce.ts           # 防抖 hook
│   ├── stores/
│   │   ├── auth-store.ts             # 认证 store
│   │   ├── kb-store.ts               # 知识库 store
│   │   ├── theme-store.ts            # 主题 store
│   │   └── upload-store.ts           # 上传 store
│   ├── utils/
│   │   ├── cn.ts                     # 类名合并工具
│   │   ├── format.ts                 # 格式化工具
│   │   ├── upload.ts                 # 上传工具（分片/断点续传）
│   │   └── storage.ts                # localStorage 封装
│   └── constants/
│       ├── routes.ts                 # 路由常量
│       ├── status.ts                 # 状态枚举
│       └── config.ts                 # 全局配置
│
├── types/
│   ├── knowledge-base.ts             # 知识库类型
│   ├── file.ts                       # 文件类型
│   ├── knowledge-graph.ts            # 图谱类型
│   ├── qa-pair.ts                    # 问答对类型
│   ├── user.ts                       # 用户类型
│   └── api.ts                        # API 响应类型
│
├── public/
│   ├── lottie/                       # Lottie 动画资源
│   │   ├── empty-state.json
│   │   └── loading.json
│   └── images/
│
├── .env.local                         # 环境变量
├── .env.production
├── tailwind.config.ts                 # Tailwind 配置
├── tsconfig.json                      # TypeScript 配置
├── next.config.js                     # Next.js 配置
└── package.json
```

---

## 5. 数据模型设计

### 👤 User（用户）

```typescript
interface User {
  id: string;
  name: string;
  email: string;
  avatar?: string;
  role: 'admin' | 'user';
  createdAt: string;
}
```

### 📚 KnowledgeBase（知识库）

```typescript
interface KnowledgeBase {
  id: string;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
  fileCount: number;
  owner: User;
  members: Member[];
}

interface Member {
  user: User;
  role: 'owner' | 'editor' | 'viewer';
  joinedAt: string;
}
```

### 📄 File（文件）

```typescript
enum ProcessingStatus {
  PENDING = 'pending',
  PROCESSING = 'processing',
  COMPLETED = 'completed',
  FAILED = 'failed',
}

interface FileStatuses {
  ocr: ProcessingStatus;
  vectorization: ProcessingStatus;
  qaPairs: ProcessingStatus;
  knowledgeGraph: ProcessingStatus;
}

interface File {
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
  errorMessage?: string; // 失败时的错误信息
}
```

### 🕸️ KnowledgeGraph（知识图谱）

```typescript
interface KnowledgeGraphNode {
  id: string;
  label: string;
  type: 'entity' | 'concept' | 'event';
  position: { x: number; y: number };
  data: {
    description?: string;
    properties?: Record<string, any>;
  };
}

interface KnowledgeGraphEdge {
  id: string;
  source: string;
  target: string;
  label?: string;
  type?: 'related' | 'contains' | 'causes';
}

interface KnowledgeGraph {
  fileId: string;
  nodes: KnowledgeGraphNode[];
  edges: KnowledgeGraphEdge[];
  metadata: {
    nodeCount: number;
    edgeCount: number;
    createdAt: string;
  };
}
```

### 💬 QAPair（问答对）

```typescript
interface QAPair {
  id: string;
  fileId: string;
  chunk: string;           // OCR 语料拆分内容
  chunkIndex: number;      // 拆分序号
  question: string;
  answer: string;
  confidence?: number;     // 置信度（可选）
  createdAt: string;
}
```

### 📝 OCRContent（OCR 内容）

```typescript
interface OCRContent {
  fileId: string;
  markdown: string;        // 完整 Markdown 内容
  pageCount?: number;      // PDF 页数
  wordCount?: number;      // 字数统计
  hasTable: boolean;       // 是否包含表格
  hasFormula: boolean;     // 是否包含公式
  generatedAt: string;
}
```

### 📤 UploadTask（上传任务）

```typescript
interface UploadChunk {
  index: number;
  size: number;
  uploaded: boolean;
}

interface UploadTask {
  id: string;
  file: File;
  knowledgeBaseId: string;
  chunks: UploadChunk[];
  progress: number;         // 0-100
  status: 'pending' | 'uploading' | 'paused' | 'completed' | 'failed';
  uploadedBytes: number;
  totalBytes: number;
  speed?: number;           // 上传速度（bytes/s）
  errorMessage?: string;
}
```

### 🔐 Permission（权限）

```typescript
type PermissionRole = 'owner' | 'editor' | 'viewer';

interface Permission {
  resource: 'knowledge-base' | 'file';
  resourceId: string;
  role: PermissionRole;
  actions: {
    read: boolean;
    write: boolean;
    delete: boolean;
    share: boolean;
  };
}
```

### 📡 API 响应格式

```typescript
interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
  };
  meta?: {
    total?: number;
    page?: number;
    pageSize?: number;
  };
}

// 分页响应
interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}
```

---

## 6. 核心页面和组件设计

### 📄 页面清单

#### 1️⃣ 知识库列表页 (`/knowledge-bases`)

**布局结构**：
```
┌────────────────────────────────────────────┐
│ Header (搜索 + 创建按钮 + 用户菜单)        │
├────────────────────────────────────────────┤
│ ┌────────┐ ┌────────┐ ┌────────┐          │
│ │  KB 1  │ │  KB 2  │ │  KB 3  │   卡片网格│
│ │  图标  │ │  图标  │ │  图标  │   (响应式)│
│ │  名称  │ │  名称  │ │  名称  │          │
│ │ 文件数 │ │ 文件数 │ │ 文件数 │          │
│ └────────┘ └────────┘ └────────┘          │
│                                            │
│ ┌──────────────────────────────┐          │
│ │    分页组件（50 条/页）      │          │
│ └──────────────────────────────┘          │
└────────────────────────────────────────────┘
```

**功能点**：
- ✅ 响应式网格布局（sm:2列, md:3列, lg:4列）
- ✅ 卡片悬停渐变边框效果
- ✅ 点击卡片进入知识库详情
- ✅ 创建知识库 Dialog（表单验证）
- ✅ 搜索知识库（防抖 300ms）
- ✅ 骨架屏加载状态

**关键组件**：
```typescript
// components/features/knowledge-base/kb-card.tsx
<KBCard
  knowledgeBase={kb}
  onClick={() => router.push(`/kb/${kb.id}`)}
/>

// components/features/knowledge-base/kb-create-dialog.tsx
<KBCreateDialog
  open={isOpen}
  onClose={() => setIsOpen(false)}
  onSuccess={(newKB) => {
    toast.success('知识库创建成功');
    router.push(`/kb/${newKB.id}`);
  }}
/>
```

#### 2️⃣ 知识库详情页 (`/kb/[id]`)

**布局结构**：
```
┌────────────────────────────────────────────────────┐
│ Breadcrumb: 知识库 > 知识库名称                    │
├────────────────────────────────────────────────────┤
│ 🎯 知识库名称     [上传文件] [设置] [视图切换]     │
├────────────────────────────────────────────────────┤
│ 筛选器：                                            │
│ [文件类型▼] [OCR 状态▼] [向量化▼] [问答对▼] [图谱▼] │
│ 搜索: [_____________________]  [批量操作▼]        │
├────────────────────────────────────────────────────┤
│ 文件列表（表格/网格视图）                          │
│ ┌──────────┬──────┬────┬────┬────┬────┬────────┐  │
│ │ 文件名   │ 类型 │OCR │向量│问答│图谱│ 操作   │  │
│ ├──────────┼──────┼────┼────┼────┼────┼────────┤  │
│ │ 文档1.pdf│ PDF  │ ✓  │ ⚙  │ ✗  │ -  │[详情]  │  │
│ │ 图片2.png│ 图片 │ ⚙  │ -  │ -  │ -  │[详情]  │  │
│ └──────────┴──────┴────┴────┴────┴────┴────────┘  │
│                                                    │
│ 虚拟滚动区域（超过 100 条启用）                     │
│                                                    │
│ ┌────────────────────────────────────────────┐    │
│ │        分页组件（50 条/页）                │    │
│ └────────────────────────────────────────────┘    │
└────────────────────────────────────────────────────┘
```

**功能点**：
- ✅ 文件拖拽上传区域（Dropzone）
- ✅ 多文件批量上传（带进度条）
- ✅ 分片上传 + 断点续传
- ✅ 文件列表多维度筛选
  - 文件类型：PDF/图片/Word
  - 四状态独立筛选（支持组合）
  - 上传日期范围
  - 文件名模糊搜索（防抖）
- ✅ 表格/网格视图切换
- ✅ 虚拟滚动（@tanstack/react-virtual）
- ✅ 批量操作：
  - 批量触发处理
  - 批量删除
- ✅ 状态轮询（2s 间隔）
- ✅ 单个文件操作：
  - 查看详情
  - 手动触发处理
  - 重试失败任务
  - 删除文件

**关键组件**：
```typescript
// components/features/file-upload/dropzone.tsx
<FileDropzone
  knowledgeBaseId={kbId}
  onUploadStart={(tasks) => {
    // 添加到上传队列
  }}
  onUploadComplete={(files) => {
    toast.success(`${files.length} 个文件上传成功`);
    refetch();
  }}
/>

// components/features/file-list/file-table.tsx
<FileTable
  files={files}
  isLoading={isLoading}
  onFileClick={(file) => router.push(`/files/${file.id}`)}
  onTriggerProcessing={(fileIds) => {
    // 触发处理任务
  }}
  onRetry={(fileId) => {
    // 重试失败任务
  }}
/>

// components/features/file-list/file-filters.tsx
<FileFilters
  onFilterChange={(filters) => {
    setFilters(filters);
  }}
/>
```

#### 3️⃣ 文件详情页 (`/files/[id]`)

**布局结构**：
```
┌──────────────────────────────────────────────────────────┐
│ Breadcrumb: 知识库 > 知识库名称 > 文件名                 │
├──────────────────────────────────────────────────────────┤
│ ┌─────────────────────┬──────────────────────────────┐   │
│ │                     │ [OCR 内容] [知识图谱] [问答对]│   │
│ │                     ├──────────────────────────────┤   │
│ │                     │                              │   │
│ │   文件预览区域      │        Tab 内容区域          │   │
│ │   (可缩放/翻页)     │                              │   │
│ │                     │   - OCR Markdown 渲染        │   │
│ │                     │   - 知识图谱可视化           │   │
│ │                     │   - 问答对列表               │   │
│ │                     │                              │   │
│ │                     │                              │   │
│ │                     │                              │   │
│ └─────────────────────┴──────────────────────────────┘   │
│        可调整宽度分隔线（Resizable）                      │
└──────────────────────────────────────────────────────────┘
```

**功能点**：

**左侧：文件预览**
- ✅ PDF 预览（react-pdf）
  - 缩放控制（50%-200%）
  - 翻页导航
  - 文本选择和复制
  - 缩略图导航（可选）
- ✅ 图片预览
  - 缩放/旋转
  - 全屏查看
- ✅ Word 文档：显示"不支持预览，请下载查看"

**右侧：Tab 切换**

**Tab 1 - OCR 内容**：
- ✅ Markdown 渲染（react-markdown）
- ✅ GFM 表格支持（remark-gfm）
- ✅ LaTeX 公式支持（rehype-katex）
- ✅ 代码高亮（rehype-highlight）
- ✅ 复制全文按钮
- ✅ 导出 Markdown 文件

**Tab 2 - 知识图谱**：
- ✅ React Flow 可视化
- ✅ 节点拖动（只读模式）
- ✅ 缩放和平移控制
- ✅ 节点点击展示详情 Drawer
- ✅ 节点搜索高亮
- ✅ 导出为 PNG 图片
- ✅ 性能优化（几百节点流畅渲染）

**Tab 3 - 问答对**：
- ✅ 虚拟滚动列表
- ✅ 显示：
  - Chunk 序号
  - OCR 语料拆分内容
  - 生成的问题
  - 生成的答案
- ✅ 搜索问答对（防抖）
- ✅ 导出为 JSON

**关键组件**：
```typescript
// components/features/file-preview/pdf-viewer.tsx
<PDFViewer
  url={file.url}
  onPageChange={(page) => {
    // 页码变化
  }}
/>

// components/features/ocr-markdown/markdown-renderer.tsx
<MarkdownRenderer
  content={ocrContent.markdown}
  enableCopy
  enableExport
/>

// components/features/knowledge-graph/graph-viewer.tsx
<KnowledgeGraphViewer
  graph={knowledgeGraph}
  onNodeClick={(node) => {
    setSelectedNode(node);
    setDrawerOpen(true);
  }}
  onExport={(dataUrl) => {
    // 下载图片
  }}
/>

// components/features/qa-pairs/qa-list.tsx
<QAPairList
  pairs={qaPairs}
  onExport={(json) => {
    // 下载 JSON
  }}
/>
```

### 🧩 公共组件清单

#### UI 基础组件（Shadcn/ui）

| 组件 | 用途 | 变体 |
|------|------|------|
| **Button** | 按钮 | primary, secondary, outline, ghost |
| **Card** | 卡片容器 | 渐变边框, 玻璃拟态 |
| **Dialog** | 弹窗 | 标准, 全屏 |
| **Tabs** | 标签页 | 下划线动画 |
| **Table** | 表格 | 排序, 筛选 |
| **Badge** | 徽章 | success, warning, error, info |
| **Progress** | 进度条 | 线性, 环形 |
| **Skeleton** | 骨架屏 | 卡片, 列表, 表格 |
| **Toast** | 通知 | success, error, warning, info |
| **DropdownMenu** | 下拉菜单 | 操作菜单 |
| **Select** | 选择器 | 单选, 多选 |
| **Input** | 输入框 | 文本, 搜索 |
| **Tooltip** | 提示 | 悬停提示 |

#### 业务组件

| 组件 | 路径 | 功能 |
|------|------|------|
| **KBCard** | `features/knowledge-base/kb-card` | 知识库卡片 |
| **FileDropzone** | `features/file-upload/dropzone` | 拖拽上传 |
| **UploadProgress** | `features/file-upload/upload-progress` | 上传进度 |
| **FileTable** | `features/file-list/file-table` | 文件列表表格 |
| **StatusBadge** | `features/file-list/status-badge` | 状态徽章 |
| **PDFViewer** | `features/file-preview/pdf-viewer` | PDF 查看器 |
| **ImageViewer** | `features/file-preview/image-viewer` | 图片查看器 |
| **MarkdownRenderer** | `features/ocr-markdown/markdown-renderer` | Markdown 渲染 |
| **KnowledgeGraphViewer** | `features/knowledge-graph/graph-viewer` | 图谱查看器 |
| **QAPairList** | `features/qa-pairs/qa-list` | 问答对列表 |

---

## 7. 交互体验设计

### 🎬 页面过渡动画（Framer Motion）

#### 页面切换动画

```typescript
// lib/utils/animations.ts
export const pageVariants = {
  initial: {
    opacity: 0,
    y: 20,
  },
  animate: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.3,
      ease: [0, 0, 0.2, 1],
    },
  },
  exit: {
    opacity: 0,
    y: -20,
    transition: {
      duration: 0.2,
      ease: [0.4, 0, 1, 1],
    },
  },
};

// 使用示例
<motion.div
  variants={pageVariants}
  initial="initial"
  animate="animate"
  exit="exit"
>
  {/* 页面内容 */}
</motion.div>
```

#### 卡片网格动画（交错入场）

```typescript
const containerVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1,
    },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0 },
};

<motion.div variants={containerVariants} initial="hidden" animate="show">
  {items.map(item => (
    <motion.div key={item.id} variants={itemVariants}>
      <KBCard {...item} />
    </motion.div>
  ))}
</motion.div>
```

#### Tab 切换动画

```typescript
const tabContentVariants = {
  hidden: { opacity: 0, x: -10 },
  show: {
    opacity: 1,
    x: 0,
    transition: { duration: 0.2 }
  },
  exit: {
    opacity: 0,
    x: 10,
    transition: { duration: 0.15 }
  },
};

<AnimatePresence mode="wait">
  <motion.div
    key={activeTab}
    variants={tabContentVariants}
    initial="hidden"
    animate="show"
    exit="exit"
  >
    {tabContent}
  </motion.div>
</AnimatePresence>
```

### 💀 骨架屏设计

#### 知识库卡片骨架屏

```typescript
// components/ui/skeleton.tsx
export function KBCardSkeleton() {
  return (
    <Card className="p-6">
      <Skeleton className="h-12 w-12 rounded-lg mb-4" />
      <Skeleton className="h-6 w-3/4 mb-2" />
      <Skeleton className="h-4 w-1/2" />
    </Card>
  );
}
```

#### 文件列表骨架屏

```typescript
export function FileTableSkeleton() {
  return (
    <div className="space-y-2">
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="flex items-center gap-4 p-4">
          <Skeleton className="h-10 w-10 rounded" />
          <Skeleton className="h-6 flex-1" />
          <Skeleton className="h-6 w-20" />
          <Skeleton className="h-6 w-16" />
        </div>
      ))}
    </div>
  );
}
```

### 📤 文件上传交互流程

#### 流程图

```
[拖拽/点击选择文件]
        ↓
[文件预览列表（带缩略图）]
        ↓
[分片计算（5MB/片）]
        ↓
[开始分片上传] ←─────┐
        ↓              │
[实时进度更新]         │
        ↓              │
[网络中断？] ─ Yes → [暂停] → [恢复] ─┘
        ↓ No
[上传完成]
        ↓
[显示"手动触发处理"按钮]
```

#### 上传组件设计

```typescript
// components/features/file-upload/upload-progress.tsx
interface UploadProgressProps {
  task: UploadTask;
  onPause: () => void;
  onResume: () => void;
  onCancel: () => void;
}

export function UploadProgress({ task, onPause, onResume, onCancel }: UploadProgressProps) {
  return (
    <Card className="p-4">
      <div className="flex items-center gap-4">
        {/* 文件图标 */}
        <FileIcon type={task.file.type} />

        {/* 文件名和大小 */}
        <div className="flex-1 min-w-0">
          <p className="font-medium truncate">{task.file.name}</p>
          <p className="text-sm text-muted-foreground">
            {formatBytes(task.uploadedBytes)} / {formatBytes(task.totalBytes)}
            {task.speed && ` • ${formatBytes(task.speed)}/s`}
          </p>
        </div>

        {/* 进度条 */}
        <div className="flex-1">
          <Progress value={task.progress} className="h-2" />
          <p className="text-xs text-muted-foreground mt-1">{task.progress}%</p>
        </div>

        {/* 操作按钮 */}
        <div className="flex gap-2">
          {task.status === 'uploading' && (
            <Button size="sm" variant="ghost" onClick={onPause}>
              <Pause className="h-4 w-4" />
            </Button>
          )}
          {task.status === 'paused' && (
            <Button size="sm" variant="ghost" onClick={onResume}>
              <Play className="h-4 w-4" />
            </Button>
          )}
          <Button size="sm" variant="ghost" onClick={onCancel}>
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </Card>
  );
}
```

#### 分片上传和断点续传逻辑

```typescript
// lib/utils/upload.ts
const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB

export async function uploadFileWithChunks(
  file: File,
  knowledgeBaseId: string,
  onProgress: (progress: number) => void
): Promise<string> {
  // 1. 计算文件 hash（用于断点续传）
  const fileHash = await calculateFileHash(file);

  // 2. 检查是否有未完成的上传任务
  const existingTask = await checkExistingUpload(fileHash);

  // 3. 计算分片
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
  const startChunk = existingTask?.lastChunkIndex ?? 0;

  // 4. 上传分片
  for (let i = startChunk; i < totalChunks; i++) {
    const start = i * CHUNK_SIZE;
    const end = Math.min(start + CHUNK_SIZE, file.size);
    const chunk = file.slice(start, end);

    await uploadChunk({
      fileHash,
      chunkIndex: i,
      totalChunks,
      chunk,
      knowledgeBaseId,
    });

    // 更新进度
    const progress = Math.round(((i + 1) / totalChunks) * 100);
    onProgress(progress);

    // 保存断点信息到 localStorage
    saveUploadProgress(fileHash, i);
  }

  // 5. 合并分片
  const fileId = await mergeChunks(fileHash);

  // 6. 清理断点信息
  clearUploadProgress(fileHash);

  return fileId;
}
```

### ⚡ 状态轮询机制

```typescript
// lib/hooks/use-polling.ts
export function useFileStatusPolling(knowledgeBaseId: string) {
  const queryClient = useQueryClient();

  const { data: files } = useQuery({
    queryKey: ['files', knowledgeBaseId],
    queryFn: () => fetchFiles(knowledgeBaseId),
    refetchInterval: (data) => {
      // 检查是否有文件处于 processing 状态
      const hasProcessing = data?.some(file =>
        Object.values(file.statuses).some(status => status === 'processing')
      );

      // 如果有正在处理的文件，启用 2s 轮询
      return hasProcessing ? 2000 : false;
    },
  });

  return files;
}
```

### 🎨 状态徽章设计

```typescript
// components/features/file-list/status-badge.tsx
const statusConfig = {
  pending: {
    label: '待处理',
    variant: 'secondary' as const,
    icon: Clock,
  },
  processing: {
    label: '处理中',
    variant: 'default' as const,
    icon: Loader2,
    animate: true,
  },
  completed: {
    label: '已完成',
    variant: 'success' as const,
    icon: CheckCircle2,
  },
  failed: {
    label: '失败',
    variant: 'destructive' as const,
    icon: XCircle,
  },
};

export function StatusBadge({
  status,
  errorMessage
}: {
  status: ProcessingStatus;
  errorMessage?: string;
}) {
  const config = statusConfig[status];
  const Icon = config.icon;

  return (
    <Tooltip>
      <TooltipTrigger>
        <Badge variant={config.variant} className="gap-1">
          <Icon className={cn(
            "h-3 w-3",
            config.animate && "animate-spin"
          )} />
          {config.label}
        </Badge>
      </TooltipTrigger>
      {errorMessage && (
        <TooltipContent>
          <p className="text-xs">{errorMessage}</p>
        </TooltipContent>
      )}
    </Tooltip>
  );
}
```

### 🎭 错误和空状态设计

#### 空状态

```typescript
// components/ui/empty-state.tsx
export function EmptyState({
  title,
  description,
  action
}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <Lottie
        animationData={emptyStateAnimation}
        className="w-48 h-48 mb-4"
      />
      <h3 className="text-lg font-semibold mb-2">{title}</h3>
      <p className="text-sm text-muted-foreground mb-6">{description}</p>
      {action && action}
    </div>
  );
}

// 使用示例
<EmptyState
  title="暂无文件"
  description="上传文件以开始处理"
  action={
    <Button onClick={() => dropzoneRef.current?.open()}>
      <Upload className="mr-2 h-4 w-4" />
      上传文件
    </Button>
  }
/>
```

#### 错误状态

```typescript
// components/ui/error-state.tsx
export function ErrorState({
  title = "出错了",
  description,
  onRetry
}: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <AlertCircle className="h-12 w-12 text-destructive mb-4" />
      <h3 className="text-lg font-semibold mb-2">{title}</h3>
      <p className="text-sm text-muted-foreground mb-6">{description}</p>
      {onRetry && (
        <Button onClick={onRetry} variant="outline">
          <RefreshCw className="mr-2 h-4 w-4" />
          重试
        </Button>
      )}
    </div>
  );
}
```

### 🔍 搜索和筛选交互

```typescript
// components/features/file-list/file-filters.tsx
export function FileFilters({ onFilterChange }: FileFiltersProps) {
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search, 300);

  const [filters, setFilters] = useState({
    types: [] as string[],
    statuses: {
      ocr: undefined as ProcessingStatus | undefined,
      vectorization: undefined,
      qaPairs: undefined,
      knowledgeGraph: undefined,
    },
    dateRange: undefined as [Date, Date] | undefined,
  });

  useEffect(() => {
    onFilterChange({
      ...filters,
      search: debouncedSearch,
    });
  }, [debouncedSearch, filters]);

  return (
    <div className="space-y-4 p-4 border rounded-lg">
      {/* 搜索框 */}
      <Input
        placeholder="搜索文件名..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="max-w-sm"
      />

      {/* 文件类型多选 */}
      <div>
        <Label>文件类型</Label>
        <div className="flex gap-2 mt-2">
          {['pdf', 'image', 'word'].map(type => (
            <Checkbox
              key={type}
              checked={filters.types.includes(type)}
              onCheckedChange={(checked) => {
                setFilters(f => ({
                  ...f,
                  types: checked
                    ? [...f.types, type]
                    : f.types.filter(t => t !== type)
                }));
              }}
            >
              {type.toUpperCase()}
            </Checkbox>
          ))}
        </div>
      </div>

      {/* 状态筛选 */}
      <div>
        <Label>处理状态</Label>
        <div className="grid grid-cols-2 gap-2 mt-2">
          {Object.keys(filters.statuses).map(key => (
            <Select
              key={key}
              value={filters.statuses[key] ?? 'all'}
              onValueChange={(value) => {
                setFilters(f => ({
                  ...f,
                  statuses: {
                    ...f.statuses,
                    [key]: value === 'all' ? undefined : value,
                  },
                }));
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder={key} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部</SelectItem>
                <SelectItem value="pending">待处理</SelectItem>
                <SelectItem value="processing">处理中</SelectItem>
                <SelectItem value="completed">已完成</SelectItem>
                <SelectItem value="failed">失败</SelectItem>
              </SelectContent>
            </Select>
          ))}
        </div>
      </div>

      {/* 日期范围 */}
      <div>
        <Label>上传日期</Label>
        <DateRangePicker
          value={filters.dateRange}
          onChange={(range) => setFilters(f => ({ ...f, dateRange: range }))}
        />
      </div>
    </div>
  );
}
```

---

## 8. 性能优化方案

### 📦 代码分割策略

#### 路由级别懒加载（Next.js 自动）

Next.js 15 App Router 自动按路由分割代码，无需额外配置。

#### 重组件动态导入

```typescript
// app/(dashboard)/files/[id]/page.tsx
import dynamic from 'next/dynamic';

// PDF 查看器懒加载（react-pdf 库较大）
const PDFViewer = dynamic(
  () => import('@/components/features/file-preview/pdf-viewer'),
  {
    loading: () => <Skeleton className="w-full h-full" />,
    ssr: false,
  }
);

// React Flow 懒加载
const KnowledgeGraphViewer = dynamic(
  () => import('@/components/features/knowledge-graph/graph-viewer'),
  {
    loading: () => <Skeleton className="w-full h-full" />,
    ssr: false,
  }
);
```

### 🖼️ 图片优化

```typescript
// 使用 Next.js Image 组件
import Image from 'next/image';

<Image
  src={file.thumbnailUrl}
  alt={file.name}
  width={200}
  height={200}
  className="rounded-lg"
  placeholder="blur"
  blurDataURL={file.blurDataURL}
/>
```

### 🗄️ React Query 缓存配置

```typescript
// lib/api/client.ts
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,       // 5 分钟内数据视为新鲜
      cacheTime: 10 * 60 * 1000,      // 缓存保留 10 分钟
      refetchOnWindowFocus: false,    // 窗口聚焦不自动重新请求
      refetchOnReconnect: true,       // 重新连接时重新请求
      retry: 1,                       // 失败重试 1 次
    },
    mutations: {
      retry: 0,                       // 变更操作不重试
    },
  },
});

// 乐观更新示例
const { mutate: deleteFile } = useMutation({
  mutationFn: (fileId: string) => api.deleteFile(fileId),
  onMutate: async (fileId) => {
    // 取消相关查询
    await queryClient.cancelQueries({ queryKey: ['files'] });

    // 保存快照
    const previousFiles = queryClient.getQueryData(['files']);

    // 乐观更新
    queryClient.setQueryData(['files'], (old: File[]) =>
      old.filter(f => f.id !== fileId)
    );

    return { previousFiles };
  },
  onError: (err, fileId, context) => {
    // 回滚
    queryClient.setQueryData(['files'], context?.previousFiles);
  },
  onSettled: () => {
    // 刷新数据
    queryClient.invalidateQueries({ queryKey: ['files'] });
  },
});
```

### 📜 虚拟滚动实现

```typescript
// components/features/file-list/file-table-virtual.tsx
import { useVirtualizer } from '@tanstack/react-virtual';

export function FileTableVirtual({ files }: { files: File[] }) {
  const parentRef = useRef<HTMLDivElement>(null);

  const virtualizer = useVirtualizer({
    count: files.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 60,        // 每行高度 60px
    overscan: 10,                   // 预渲染 10 行
  });

  return (
    <div ref={parentRef} className="h-[600px] overflow-auto">
      <div
        style={{
          height: `${virtualizer.getTotalSize()}px`,
          position: 'relative',
        }}
      >
        {virtualizer.getVirtualItems().map(virtualRow => (
          <div
            key={virtualRow.key}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: `${virtualRow.size}px`,
              transform: `translateY(${virtualRow.start}px)`,
            }}
          >
            <FileRow file={files[virtualRow.index]} />
          </div>
        ))}
      </div>
    </div>
  );
}
```

### ⚡ 防抖/节流优化

```typescript
// lib/hooks/use-debounce.ts
export function useDebounce<T>(value: T, delay: number = 300): T {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);

  return debouncedValue;
}

// lib/hooks/use-throttle.ts
export function useThrottle<T>(value: T, interval: number = 100): T {
  const [throttledValue, setThrottledValue] = useState(value);
  const lastRan = useRef(Date.now());

  useEffect(() => {
    const handler = setTimeout(() => {
      if (Date.now() - lastRan.current >= interval) {
        setThrottledValue(value);
        lastRan.current = Date.now();
      }
    }, interval - (Date.now() - lastRan.current));

    return () => {
      clearTimeout(handler);
    };
  }, [value, interval]);

  return throttledValue;
}
```

### 🕸️ 知识图谱性能优化

```typescript
// components/features/knowledge-graph/graph-viewer.tsx
import ReactFlow, {
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
} from 'reactflow';
import 'reactflow/dist/style.css';

export function KnowledgeGraphViewer({ graph }: { graph: KnowledgeGraph }) {
  const [nodes, setNodes, onNodesChange] = useNodesState(graph.nodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(graph.edges);

  // 性能优化：节点数量超过 200 时启用性能模式
  const proOptions = useMemo(() => ({
    hideAttribution: true,
    // 性能模式：减少重新渲染
    onlyRenderVisibleElements: graph.nodes.length > 200,
  }), [graph.nodes.length]);

  return (
    <div className="w-full h-[600px] border rounded-lg">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        proOptions={proOptions}
        fitView
        minZoom={0.1}
        maxZoom={2}
      >
        <Background />
        <Controls />
        {/* 节点数量少于 100 时显示缩略图 */}
        {graph.nodes.length < 100 && <MiniMap />}
      </ReactFlow>
    </div>
  );
}
```

### 📊 Bundle 大小优化

```javascript
// next.config.js
const withBundleAnalyzer = require('@next/bundle-analyzer')({
  enabled: process.env.ANALYZE === 'true',
});

module.exports = withBundleAnalyzer({
  experimental: {
    optimizePackageImports: [
      'lucide-react',
      'date-fns',
      'react-markdown',
    ],
  },
  images: {
    formats: ['image/avif', 'image/webp'],
  },
});
```

---

## 9. Mock 数据设计

### 📚 知识库 Mock 数据

```typescript
// lib/api/mock/knowledge-bases.ts
export const mockKnowledgeBases: KnowledgeBase[] = [
  {
    id: 'kb-1',
    name: '技术文档库',
    description: '存储技术文档、API 文档和架构设计',
    createdAt: '2024-01-15T08:00:00Z',
    updatedAt: '2024-01-20T10:30:00Z',
    fileCount: 45,
    owner: {
      id: 'user-1',
      name: '张三',
      email: 'zhangsan@example.com',
      avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Zhang',
      role: 'admin',
      createdAt: '2024-01-01T00:00:00Z',
    },
    members: [
      {
        user: {
          id: 'user-2',
          name: '李四',
          email: 'lisi@example.com',
          avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Li',
          role: 'user',
          createdAt: '2024-01-02T00:00:00Z',
        },
        role: 'editor',
        joinedAt: '2024-01-16T09:00:00Z',
      },
    ],
  },
  // ... 更多知识库
];
```

### 📄 文件 Mock 数据

```typescript
// lib/api/mock/files.ts
export const mockFiles: File[] = [
  {
    id: 'file-1',
    knowledgeBaseId: 'kb-1',
    name: 'Next.js 官方文档.pdf',
    type: 'pdf',
    mimeType: 'application/pdf',
    size: 5242880, // 5MB
    uploadedAt: '2024-01-15T10:00:00Z',
    uploadedBy: mockUsers[0],
    statuses: {
      ocr: 'completed',
      vectorization: 'processing',
      qaPairs: 'pending',
      knowledgeGraph: 'pending',
    },
    url: '/mock/files/nextjs-docs.pdf',
    thumbnailUrl: '/mock/thumbnails/nextjs-docs.jpg',
  },
  {
    id: 'file-2',
    knowledgeBaseId: 'kb-1',
    name: 'React 架构图.png',
    type: 'image',
    mimeType: 'image/png',
    size: 1048576, // 1MB
    uploadedAt: '2024-01-15T11:00:00Z',
    uploadedBy: mockUsers[0],
    statuses: {
      ocr: 'processing',
      vectorization: 'pending',
      qaPairs: 'pending',
      knowledgeGraph: 'pending',
    },
    url: '/mock/files/react-architecture.png',
  },
  {
    id: 'file-3',
    knowledgeBaseId: 'kb-1',
    name: '数据库设计文档.pdf',
    type: 'pdf',
    mimeType: 'application/pdf',
    size: 3145728, // 3MB
    uploadedAt: '2024-01-16T09:00:00Z',
    uploadedBy: mockUsers[1],
    statuses: {
      ocr: 'failed',
      vectorization: 'pending',
      qaPairs: 'pending',
      knowledgeGraph: 'pending',
    },
    url: '/mock/files/database-design.pdf',
    errorMessage: 'OCR 处理失败：文件格式不支持',
  },
  // ... 更多文件（生成 50 个）
];
```

### 🕸️ 知识图谱 Mock 数据

```typescript
// lib/api/mock/knowledge-graph.ts
export const mockKnowledgeGraph: KnowledgeGraph = {
  fileId: 'file-1',
  nodes: [
    {
      id: 'node-1',
      label: 'Next.js',
      type: 'concept',
      position: { x: 250, y: 0 },
      data: {
        description: 'React 全栈框架',
        properties: {
          version: '15.0',
          category: 'Framework',
        },
      },
    },
    {
      id: 'node-2',
      label: 'App Router',
      type: 'concept',
      position: { x: 100, y: 150 },
      data: {
        description: 'Next.js 新路由系统',
      },
    },
    {
      id: 'node-3',
      label: 'Server Components',
      type: 'concept',
      position: { x: 400, y: 150 },
      data: {
        description: 'React 服务端组件',
      },
    },
    {
      id: 'node-4',
      label: 'Streaming',
      type: 'concept',
      position: { x: 250, y: 300 },
      data: {
        description: '流式渲染',
      },
    },
    // ... 生成 200 个节点
  ],
  edges: [
    {
      id: 'edge-1',
      source: 'node-1',
      target: 'node-2',
      label: '包含',
      type: 'contains',
    },
    {
      id: 'edge-2',
      source: 'node-1',
      target: 'node-3',
      label: '支持',
      type: 'related',
    },
    {
      id: 'edge-3',
      source: 'node-2',
      target: 'node-4',
      label: '启用',
      type: 'causes',
    },
    // ... 更多边
  ],
  metadata: {
    nodeCount: 200,
    edgeCount: 350,
    createdAt: '2024-01-15T12:00:00Z',
  },
};
```

### 💬 问答对 Mock 数据

```typescript
// lib/api/mock/qa-pairs.ts
export const mockQAPairs: QAPair[] = [
  {
    id: 'qa-1',
    fileId: 'file-1',
    chunk: 'Next.js 是一个基于 React 的全栈框架，提供服务端渲染、静态生成、API 路由等功能。',
    chunkIndex: 0,
    question: 'Next.js 是什么？',
    answer: 'Next.js 是一个基于 React 的全栈框架，它提供了服务端渲染（SSR）、静态站点生成（SSG）、API 路由等功能，帮助开发者快速构建生产级 React 应用。',
    confidence: 0.95,
    createdAt: '2024-01-15T12:30:00Z',
  },
  {
    id: 'qa-2',
    fileId: 'file-1',
    chunk: 'App Router 是 Next.js 13 引入的新路由系统，基于 React Server Components，支持嵌套布局、流式渲染和并行路由。',
    chunkIndex: 1,
    question: 'App Router 有什么特点？',
    answer: 'App Router 是 Next.js 13 引入的新路由系统，主要特点包括：1) 基于 React Server Components；2) 支持嵌套布局；3) 支持流式渲染；4) 支持并行路由和拦截路由。',
    confidence: 0.92,
    createdAt: '2024-01-15T12:31:00Z',
  },
  // ... 生成 100 个问答对
];
```

### 📝 OCR 内容 Mock 数据

```typescript
// lib/api/mock/ocr-content.ts
export const mockOCRContent: OCRContent = {
  fileId: 'file-1',
  markdown: `# Next.js 15 官方文档

## 介绍

Next.js 是一个基于 React 的**全栈框架**，提供以下核心功能：

- 🚀 服务端渲染（SSR）
- 📄 静态站点生成（SSG）
- 🔀 增量静态再生（ISR）
- 🛣️ 文件系统路由
- 📡 API 路由

## 性能对比

| 框架 | FCP (ms) | LCP (ms) | TTI (ms) |
|------|----------|----------|----------|
| Next.js | 800 | 1200 | 1500 |
| Create React App | 1200 | 2000 | 2500 |
| Gatsby | 900 | 1400 | 1800 |

## 数学公式示例

服务端渲染的响应时间可以用以下公式计算：

$$
T_{total} = T_{server} + T_{network} + T_{hydration}
$$

其中：
- $T_{server}$ 是服务端渲染时间
- $T_{network}$ 是网络传输时间
- $T_{hydration}$ 是客户端水合时间

## 代码示例

\`\`\`typescript
// app/page.tsx
export default function Page() {
  return <h1>Hello, Next.js!</h1>;
}
\`\`\`

## 架构设计

\`\`\`mermaid
graph TD
  A[Client] --> B[CDN]
  B --> C[Edge Runtime]
  C --> D[Node.js Server]
  D --> E[Database]
\`\`\`
`,
  pageCount: 50,
  wordCount: 15000,
  hasTable: true,
  hasFormula: true,
  generatedAt: '2024-01-15T12:00:00Z',
};
```

### 🔌 Mock API 函数

```typescript
// lib/api/mock/index.ts
let mockData = {
  knowledgeBases: mockKnowledgeBases,
  files: mockFiles,
  knowledgeGraphs: new Map<string, KnowledgeGraph>(),
  qaPairs: new Map<string, QAPair[]>(),
  ocrContents: new Map<string, OCRContent>(),
};

// 模拟延迟
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export const mockAPI = {
  // 知识库相关
  async getKnowledgeBases(): Promise<KnowledgeBase[]> {
    await delay(500);
    return mockData.knowledgeBases;
  },

  async createKnowledgeBase(data: CreateKBInput): Promise<KnowledgeBase> {
    await delay(800);
    const newKB: KnowledgeBase = {
      id: `kb-${Date.now()}`,
      ...data,
      fileCount: 0,
      owner: mockUsers[0],
      members: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    mockData.knowledgeBases.push(newKB);
    return newKB;
  },

  // 文件相关
  async getFiles(knowledgeBaseId: string): Promise<File[]> {
    await delay(600);
    return mockData.files.filter(f => f.knowledgeBaseId === knowledgeBaseId);
  },

  async uploadFile(file: File, knowledgeBaseId: string): Promise<File> {
    await delay(2000);
    const newFile: File = {
      id: `file-${Date.now()}`,
      knowledgeBaseId,
      name: file.name,
      type: file.type.includes('pdf') ? 'pdf' : 'image',
      mimeType: file.type,
      size: file.size,
      uploadedAt: new Date().toISOString(),
      uploadedBy: mockUsers[0],
      statuses: {
        ocr: 'pending',
        vectorization: 'pending',
        qaPairs: 'pending',
        knowledgeGraph: 'pending',
      },
      url: URL.createObjectURL(file),
    };
    mockData.files.push(newFile);
    return newFile;
  },

  async triggerProcessing(fileId: string, taskType: keyof FileStatuses): Promise<void> {
    await delay(500);
    const file = mockData.files.find(f => f.id === fileId);
    if (file) {
      file.statuses[taskType] = 'processing';

      // 模拟处理完成（3 秒后）
      setTimeout(() => {
        file.statuses[taskType] = 'completed';
      }, 3000);
    }
  },

  // 知识图谱
  async getKnowledgeGraph(fileId: string): Promise<KnowledgeGraph> {
    await delay(800);
    return mockKnowledgeGraph;
  },

  // 问答对
  async getQAPairs(fileId: string): Promise<QAPair[]> {
    await delay(600);
    return mockQAPairs;
  },

  // OCR 内容
  async getOCRContent(fileId: string): Promise<OCRContent> {
    await delay(500);
    return mockOCRContent;
  },
};
```

---

## 10. 实施步骤和优先级

### 🗓️ 四周迭代计划

---

### 📅 **Week 1 - 基础框架搭建**

**目标**：完成项目初始化和设计系统搭建

#### 任务清单

**Day 1-2：项目初始化**
- [ ] Next.js 15 项目初始化
  ```bash
  npx create-next-app@latest front --typescript --tailwind --app
  ```
- [ ] 安装核心依赖
  ```bash
  npm install zustand @tanstack/react-query axios date-fns
  npm install -D @next/bundle-analyzer
  ```
- [ ] 配置 Tailwind CSS
  - 配置设计 token（颜色、间距、圆角）
  - 添加自定义渐变类
- [ ] 配置 TypeScript（严格模式）
- [ ] 创建目录结构（参考第 4 节）

**Day 3：Shadcn/ui 集成**
- [ ] 初始化 Shadcn/ui
  ```bash
  npx shadcn-ui@latest init
  ```
- [ ] 安装基础组件
  ```bash
  npx shadcn-ui@latest add button card dialog tabs table badge progress skeleton toast dropdown-menu select input tooltip
  ```
- [ ] 自定义组件变体（CVA 配置）
- [ ] 测试深色/浅色主题切换

**Day 4：状态管理基础**
- [ ] 创建 Zustand stores
  - `auth-store.ts`
  - `kb-store.ts`
  - `theme-store.ts`
  - `upload-store.ts`
- [ ] 配置 React Query
  - QueryClient 配置
  - 持久化中间件（localStorage）
- [ ] 创建 API 客户端（Axios 封装）

**Day 5：路由和布局**
- [ ] 创建路由结构
  - `(auth)/login`
  - `(dashboard)/knowledge-bases`
  - `(dashboard)/kb/[id]`
  - `(dashboard)/files/[id]`
- [ ] 实现基础布局组件
  - `dashboard-layout.tsx`（带侧边栏和头部）
  - `sidebar.tsx`
  - `header.tsx`
  - `theme-toggle.tsx`
- [ ] Framer Motion 页面过渡动画

**验收标准**：
- ✅ 项目成功启动，无编译错误
- ✅ Tailwind 渐变色正确显示
- ✅ Shadcn/ui 组件正常渲染
- ✅ 深色/浅色主题切换流畅
- ✅ 路由跳转带过渡动画

---

### 📅 **Week 2 - 知识库模块开发**

**目标**：完成知识库列表、创建和文件上传功能

#### 任务清单

**Day 1：Mock 数据准备**
- [ ] 创建 Mock 数据文件
  - `mock/knowledge-bases.ts`（5 个知识库）
  - `mock/files.ts`（50 个文件）
  - `mock/users.ts`
- [ ] 实现 Mock API 函数
  - `getKnowledgeBases()`
  - `createKnowledgeBase()`
  - `getFiles()`
  - `uploadFile()`
  - `triggerProcessing()`
- [ ] 配置 localStorage 持久化

**Day 2：知识库列表页**
- [ ] 创建 `KBCard` 组件
  - 渐变边框悬停效果
  - 文件数量显示
  - 成员头像列表
- [ ] 实现知识库列表页
  - 响应式网格布局
  - 骨架屏加载状态
  - 空状态（Lottie 动画）
- [ ] 搜索功能（防抖 300ms）
- [ ] 分页组件（50 条/页）

**Day 3：创建知识库功能**
- [ ] 创建 `KBCreateDialog` 组件
- [ ] React Hook Form + Zod 表单验证
  - 知识库名称（必填，2-50 字符）
  - 描述（可选，最多 200 字符）
- [ ] 提交成功后跳转到知识库详情页
- [ ] Toast 通知

**Day 4：文件上传组件**
- [ ] 创建 `FileDropzone` 组件
  - 拖拽区域样式（虚线边框）
  - 拖拽悬停高亮效果
  - 多文件选择
  - 文件类型验证（PDF/图片/Word）
  - 文件大小验证（50MB）
- [ ] 创建 `UploadProgress` 组件
  - 进度条动画
  - 上传速度显示
  - 暂停/恢复/取消按钮
- [ ] 实现分片上传逻辑
  - 文件 hash 计算
  - 5MB 分片
  - 断点续传（localStorage 存储进度）

**Day 5：知识库详情页（文件列表）**
- [ ] 创建 `FileTable` 组件
  - 四状态徽章显示
  - 文件类型图标
  - 操作列（查看详情、触发处理、删除）
- [ ] 实现文件筛选器
  - 文件类型多选
  - 四状态独立筛选
  - 日期范围选择
  - 文件名搜索（防抖）
- [ ] 实现虚拟滚动（@tanstack/react-virtual）
- [ ] 实现状态轮询（2s 间隔）
- [ ] 批量操作功能

**验收标准**：
- ✅ 知识库列表正确展示
- ✅ 创建知识库表单验证正常
- ✅ 文件拖拽上传流畅
- ✅ 分片上传带进度条
- ✅ 断点续传正常工作
- ✅ 文件列表筛选准确
- ✅ 虚拟滚动无卡顿（1000 条）
- ✅ 状态轮询正确触发

---

### 📅 **Week 3 - 文件详情页开发**

**目标**：完成文件详情页的三个 Tab 内容

#### 任务清单

**Day 1：页面布局和文件预览**
- [ ] 创建文件详情页布局
  - 左右分栏（Resizable Panel）
  - 面包屑导航
  - Tab 切换区域
- [ ] 实现 PDF 预览
  - 安装 `react-pdf`
  - 缩放控制（50%-200%）
  - 翻页导航
  - 页码显示
- [ ] 实现图片预览
  - 缩放/旋转工具栏
  - 全屏查看

**Day 2：OCR Markdown 渲染**
- [ ] 安装 Markdown 依赖
  ```bash
  npm install react-markdown remark-gfm remark-math rehype-katex rehype-highlight katex highlight.js
  ```
- [ ] 创建 `MarkdownRenderer` 组件
  - 配置 remark-gfm（表格支持）
  - 配置 rehype-katex（LaTeX 公式）
  - 配置 rehype-highlight（代码高亮）
  - 自定义样式（@tailwindcss/typography）
- [ ] 添加功能按钮
  - 复制全文
  - 导出 Markdown 文件
- [ ] Mock OCR 内容（含表格和公式）

**Day 3：知识图谱可视化**
- [ ] 安装 React Flow
  ```bash
  npm install reactflow
  ```
- [ ] 创建 `KnowledgeGraphViewer` 组件
  - 基础图谱渲染
  - 节点拖动功能
  - 缩放和平移控制（Controls）
  - 缩略图（MiniMap，节点 < 100 时显示）
- [ ] 创建 `NodeDetail` 侧边抽屉
  - 显示节点属性
  - 显示相关边
- [ ] 实现节点搜索
  - 搜索框（防抖）
  - 高亮匹配节点
- [ ] 导出为 PNG 功能
- [ ] 性能优化（200+ 节点）
  - `onlyRenderVisibleElements: true`
  - 节点虚拟化

**Day 4：问答对列表**
- [ ] 创建 `QAPairList` 组件
  - 虚拟滚动列表
  - 展示 Chunk 序号、内容、问题、答案
  - 搜索问答对（防抖）
- [ ] 创建 `QAPairItem` 组件
  - 卡片样式
  - 展开/收起 Chunk 内容
  - 复制问题/答案按钮
- [ ] 导出为 JSON 功能
- [ ] Mock 问答对数据（100 条）

**Day 5：Tab 切换和动画优化**
- [ ] 实现 Tab 切换动画
  - Framer Motion AnimatePresence
  - 淡入淡出 + 轻微位移
- [ ] 优化 Tab 懒加载
  - PDF 查看器动态导入
  - React Flow 动态导入
- [ ] 错误状态和空状态
  - OCR 失败时显示错误信息
  - 图谱/问答对未生成时显示空状态

**验收标准**：
- ✅ 左右分栏宽度可调整
- ✅ PDF 缩放和翻页流畅
- ✅ Markdown 表格正确渲染
- ✅ LaTeX 公式正确显示
- ✅ 知识图谱节点可拖动
- ✅ 节点搜索高亮准确
- ✅ 图谱导出 PNG 成功
- ✅ 问答对列表虚拟滚动无卡顿
- ✅ Tab 切换动画流畅（< 300ms）

---

### 📅 **Week 4 - 优化和完善**

**目标**：性能优化、响应式适配、完善细节

#### 任务清单

**Day 1：性能优化**
- [ ] Bundle 分析
  ```bash
  ANALYZE=true npm run build
  ```
- [ ] 优化大依赖包
  - `react-pdf` 懒加载
  - `reactflow` 懒加载
  - `katex` 按需加载
- [ ] 图片优化
  - 使用 Next.js Image 组件
  - 配置 AVIF/WebP 格式
- [ ] React Query 缓存优化
  - 配置合理的 staleTime
  - 实现乐观更新
- [ ] 虚拟滚动优化
  - 调整 overscan 参数
  - 优化行高计算

**Day 2：响应式适配**
- [ ] 移动端布局调整
  - 知识库列表卡片（sm:1列, md:2列, lg:4列）
  - 文件列表响应式表格（移动端切换为卡片）
  - 文件详情页移动端布局（上下分栏）
- [ ] 触摸手势支持
  - 图片缩放手势
  - 图谱拖拽手势
- [ ] 断点测试（sm/md/lg/xl）

**Day 3：深色模式完善**
- [ ] 检查所有组件深色模式样式
- [ ] 修复颜色对比度问题
- [ ] 优化渐变色在深色模式下的显示
- [ ] 添加主题切换过渡动画

**Day 4：错误处理和边界情况**
- [ ] 实现错误边界（Error Boundary）
- [ ] 404 页面设计
- [ ] 网络错误重试机制
- [ ] 上传失败重试逻辑
- [ ] 轮询失败降级处理
- [ ] Toast 通知完善
  - 成功/错误/警告/信息
  - 自动消失（3s）
  - 可关闭

**Day 5：最终测试和文档**
- [ ] 功能完整性测试
  - 知识库 CRUD
  - 文件上传（断点续传）
  - 文件详情三 Tab
  - 状态轮询
- [ ] 性能测试（Lighthouse）
  - FCP < 1.5s
  - LCP < 2.5s
  - TTI < 3.5s
  - CLS < 0.1
- [ ] 浏览器兼容性测试
  - Chrome 100+
  - Firefox 100+
  - Safari 15+
- [ ] 编写 README.md
  - 项目介绍
  - 安装和运行
  - 技术栈说明
  - 目录结构
- [ ] 代码注释和 JSDoc

**验收标准**：
- ✅ Bundle 大小合理（< 500KB gzip）
- ✅ Lighthouse 性能评分 > 90
- ✅ 移动端布局正常
- ✅ 深色模式无样式问题
- ✅ 错误处理完善
- ✅ 所有功能正常工作
- ✅ 浏览器兼容性良好
- ✅ 代码质量良好（ESLint 无警告）

---

## 11. 验收标准

### ✅ 功能完整性检查清单

#### 知识库管理
- [ ] 知识库列表正确展示
- [ ] 创建知识库功能正常
- [ ] 知识库搜索准确
- [ ] 知识库卡片悬停效果流畅
- [ ] 分页功能正常

#### 文件管理
- [ ] 文件拖拽上传流畅
- [ ] 多文件批量上传正常
- [ ] 分片上传带进度条
- [ ] 断点续传正常工作
- [ ] 文件列表正确展示
- [ ] 文件筛选准确（类型/状态/日期/名称）
- [ ] 虚拟滚动无卡顿（1000 条）
- [ ] 批量操作功能正常

#### 文件处理
- [ ] 手动触发处理正常
- [ ] 状态轮询正确（2s 间隔）
- [ ] 四状态徽章正确显示
- [ ] 失败状态显示错误信息
- [ ] 重试功能正常

#### 文件详情
- [ ] 左右分栏宽度可调整
- [ ] PDF 预览流畅（缩放/翻页）
- [ ] 图片预览功能完整（缩放/旋转）
- [ ] OCR Markdown 渲染正确
  - [ ] 表格正确渲染
  - [ ] LaTeX 公式正确显示
  - [ ] 代码高亮正常
- [ ] 知识图谱可视化正常
  - [ ] 节点可拖动
  - [ ] 缩放和平移流畅
  - [ ] 节点搜索高亮准确
  - [ ] 导出 PNG 成功
- [ ] 问答对列表正确展示
  - [ ] 虚拟滚动无卡顿
  - [ ] 搜索功能准确
  - [ ] 导出 JSON 成功
- [ ] Tab 切换动画流畅

#### 用户体验
- [ ] 页面切换过渡动画流畅
- [ ] 骨架屏加载状态完整
- [ ] 空状态设计美观
- [ ] 错误状态提示清晰
- [ ] Toast 通知正常
- [ ] 深色/浅色主题切换流畅

### 📊 性能指标（Lighthouse）

**目标性能评分**：
- **Performance**: > 90
- **Accessibility**: > 95
- **Best Practices**: > 95
- **SEO**: > 90

**核心 Web Vitals**：
- **FCP**（First Contentful Paint）: < 1.5s
- **LCP**（Largest Contentful Paint）: < 2.5s
- **TTI**（Time to Interactive）: < 3.5s
- **CLS**（Cumulative Layout Shift）: < 0.1
- **FID**（First Input Delay）: < 100ms

**Bundle 大小**：
- **Initial JS**: < 300KB (gzip)
- **Total JS**: < 500KB (gzip)
- **CSS**: < 50KB (gzip)

### 🌐 浏览器兼容性

**桌面端**：
- [ ] Chrome 100+
- [ ] Firefox 100+
- [ ] Safari 15+
- [ ] Edge 100+

**移动端**：
- [ ] iOS Safari 15+
- [ ] Android Chrome 100+

**响应式断点**：
- [ ] **sm (640px)**: 移动端布局正常
- [ ] **md (768px)**: 平板布局正常
- [ ] **lg (1024px)**: 笔记本布局正常
- [ ] **xl (1280px)**: 桌面布局正常
- [ ] **2xl (1536px)**: 大屏布局正常

### 🎨 设计质量

**视觉一致性**：
- [ ] 颜色使用符合设计系统
- [ ] 间距符合 8px 网格
- [ ] 圆角使用一致
- [ ] 阴影层级合理
- [ ] 字体大小层级清晰

**交互流畅性**：
- [ ] 所有动画时长符合规范（150ms/300ms/500ms）
- [ ] 缓动函数使用一致
- [ ] 悬停效果流畅
- [ ] 点击反馈及时

**可访问性**：
- [ ] 所有交互元素可键盘访问
- [ ] ARIA 标签完整
- [ ] 颜色对比度符合 WCAG AA 标准
- [ ] 焦点状态清晰

### 💻 代码质量

**TypeScript**：
- [ ] 严格模式无错误
- [ ] 类型定义完整
- [ ] 无 `any` 类型滥用

**ESLint**：
- [ ] 无 Error
- [ ] 无 Warning

**代码组织**：
- [ ] 组件职责单一
- [ ] 可复用组件抽象合理
- [ ] Hooks 使用规范
- [ ] 工具函数有单元测试（可选）

**Git 规范**：
- [ ] Commit 信息清晰
- [ ] 分支管理合理

---

## 🎉 总结

本规划文档提供了一个**完整、可执行**的知识库管理系统开发方案，涵盖：

1. ✅ **创意渐变设计风格**的具体实现
2. ✅ **流畅交互体验**的动画和性能优化
3. ✅ **完整的 Mock 数据**支持前期开发
4. ✅ **分片上传和断点续传**的技术方案
5. ✅ **知识图谱性能优化**（几百节点流畅渲染）
6. ✅ **四周迭代计划**，任务清晰可执行
7. ✅ **详细的验收标准**，确保交付质量

**核心亮点**：
- 🎨 蓝紫渐变 + 玻璃拟态的现代化 UI
- 🚀 Framer Motion 流畅动画（300ms 标准）
- 📊 虚拟滚动支持 1000+ 文件
- 🕸️ React Flow 知识图谱可视化
- 📝 完整 Markdown 渲染（表格 + 公式）
- 📤 分片上传 + 断点续传

**下一步行动**：
1. 确认规划方案
2. 开始 Week 1 任务（项目初始化）
3. 按周迭代开发
4. 定期验收和调整

祝项目开发顺利！🚀