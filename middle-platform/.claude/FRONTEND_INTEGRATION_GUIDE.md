# 前端后端集成完成指南

> **版本**: v1.0
> **完成日期**: 2025-10-04
> **状态**: ✅ 集成完成

---

## 📋 集成概览

前端已成功从Mock API切换到真实的Spring Boot后端API。所有数据操作现在通过HTTP请求与后端交互。

---

## ✅ 已完成的集成

### 1. **API客户端** (`lib/api/client.ts`)

#### 核心功能
- **基础URL配置**: `http://localhost:8080/api` (可通过环境变量 `NEXT_PUBLIC_API_URL` 覆盖)
- **JWT认证**: 自动在请求头添加 `Authorization: Bearer <token>`
- **响应处理**: 解析后端统一响应格式 `{code, message, data}`
- **Token管理**: 与Zustand store同步,支持localStorage持久化
- **文件上传**: XHR实现,支持进度回调

#### 后端实体映射
```typescript
// 后端 → 前端类型映射
BackendKnowledgeBase → KnowledgeBase
BackendFile → File
BackendUser → User
BackendEndpoint → Endpoint
BackendApiKey → ApiKey
BackendBusiness → Business
```

#### API模块结构
```typescript
api.auth.*              // 认证相关
api.knowledgeBases.*    // 知识库CRUD
api.files.*             // 文件管理
api.modelConfig.*       // 模型配置
  ├── endpoints.*       // 端点管理
  ├── apiKeys.*         // API Key管理
  └── business.*        // 业务配置
```

---

### 2. **认证Hooks** (`lib/hooks/use-auth.ts`) ✅ 新建

```typescript
useLogin()           // 登录 → 存储token和用户信息
useRegister()        // 注册 → 自动登录
useLogout()          // 登出 → 清空状态
useCurrentUser()     // 获取当前用户 → 自动同步store
useIsAuthenticated() // 检查登录状态
```

**使用示例**:
```tsx
const { mutate: login, isPending } = useLogin();

login({ username: 'zang', password: 'zangzang' }, {
  onSuccess: () => router.push('/dashboard'),
  onError: (error) => toast.error(error.message)
});
```

---

### 3. **知识库Hooks** (`lib/hooks/use-knowledge-bases.ts`) ✅ 已更新

```typescript
useKnowledgeBases()         // 列表查询
useKnowledgeBase(id)        // 单个查询
useCreateKnowledgeBase()    // 创建 → 自动更新缓存
useDeleteKnowledgeBase()    // 删除 → 乐观更新
```

**变更**:
- ❌ `mockAPI.getKnowledgeBases()`
- ✅ `api.knowledgeBases.list()`

---

### 4. **文件Hooks** (`lib/hooks/use-files.ts`) ✅ 已更新

```typescript
useFiles(kbId)    // 文件列表
useFile(fileId)   // 文件详情
```

**变更**:
- ❌ `mockAPI.getFiles()`
- ✅ `api.files.list(kbId)`

---

### 5. **模型配置Hooks** (`lib/hooks/use-model-config.ts`) ✅ 新建

#### 端点管理
```typescript
useEndpoints()         // 列表
useEndpoint(id)        // 详情
useCreateEndpoint()    // 创建
useUpdateEndpoint()    // 更新
useDeleteEndpoint()    // 删除
```

#### API Key管理
```typescript
useApiKeys(endpointId?)  // 列表(可按端点过滤)
useApiKey(id)            // 详情
useCreateApiKey()        // 创建
useUpdateApiKey()        // 更新
useDeleteApiKey()        // 删除
```

#### 业务配置
```typescript
useBusinesses()       // 列表
useBusiness(id)       // 详情
useCreateBusiness()   // 创建
useUpdateBusiness()   // 更新
useDeleteBusiness()   // 删除
```

**使用示例**:
```tsx
const { data: endpoints } = useEndpoints();
const { mutate: createKey } = useCreateApiKey();

createKey({
  endpointId: '1',
  name: 'OpenAI Key',
  apiKey: 'sk-xxx'
});
```

---

### 6. **环境配置** (`.env.local`) ✅ 新建

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

**说明**:
- 开发环境默认连接本地后端
- 生产环境需修改为实际后端地址
- 前端构建时会将此值嵌入代码

---

## 🔧 后端API端点映射

### 认证 (`/api/auth/*`)
| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录 → `{user, token}` |
| POST | `/auth/register` | 注册 → `{user, token}` |
| GET | `/auth/me` | 获取当前用户 |

### 知识库 (`/api/knowledge-bases/*`)
| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/knowledge-bases` | 列表 |
| GET | `/knowledge-bases/{kbId}` | 详情 |
| POST | `/knowledge-bases` | 创建 |
| PUT | `/knowledge-bases/{kbId}` | 更新 |
| DELETE | `/knowledge-bases/{kbId}` | 删除 |

### 文件 (`/api/files/*`)
| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/knowledge-bases/{kbId}/files` | 文件列表 |
| POST | `/knowledge-bases/{kbId}/files` | 上传文件 |
| GET | `/files/{fileId}` | 文件详情 |
| DELETE | `/files/{fileId}` | 删除文件 |
| GET | `/files/{fileId}/ocr-content` | OCR内容 |
| GET | `/files/{fileId}/qa-pairs` | 问答对 |
| GET | `/files/{fileId}/knowledge-graph` | 知识图谱 |

### 模型配置 (`/api/model/*`)
| 方法 | 端点 | 说明 |
|------|------|------|
| GET/POST/PUT/DELETE | `/model/endpoints` | 端点CRUD |
| GET/POST/PUT/DELETE | `/model/api-keys` | API Key CRUD |
| GET/POST/PUT/DELETE | `/model/business` | 业务配置CRUD |

---

## 📦 后端响应格式

所有API响应遵循统一格式:

```json
{
  "code": 200,
  "message": "success",
  "data": <实际数据>
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "Knowledge base not found",
  "data": null
}
```

---

## 🔐 认证流程

### 1. 登录
```typescript
// 1. 用户提交登录表单
const { mutate: login } = useLogin();
login({ username, password });

// 2. API客户端发送请求
POST /api/auth/login
Body: { username, password }

// 3. 后端返回token和用户信息
Response: { code: 200, data: { user, token } }

// 4. 前端保存token
localStorage.setItem('auth_token', token);
useAuthStore.setState({ user, token });

// 5. 后续请求自动携带token
Authorization: Bearer <token>
```

### 2. Token刷新
```typescript
// 页面加载时自动恢复
const { data: user } = useCurrentUser(); // enabled: !!token

// 从localStorage加载token
function getToken() {
  const storedToken = localStorage.getItem('auth_token');
  if (storedToken) {
    useAuthStore.getState().setToken(storedToken);
  }
  return storedToken;
}
```

---

## 🎯 数据流示例

### 创建知识库
```
用户填表 → useCreateKnowledgeBase()
          ↓
       api.knowledgeBases.create()
          ↓
    POST /api/knowledge-bases
    Body: { name, description }
    Authorization: Bearer <token>
          ↓
    后端创建记录 → 返回KB对象
          ↓
    React Query缓存更新
    - setQueryData(['knowledge-base', id], newKB)
    - invalidateQueries(['knowledge-bases'])
          ↓
    UI自动刷新列表
```

### 文件上传
```
用户选择文件 → api.files.upload(kbId, file, onProgress)
                ↓
            XHR FormData上传
            POST /api/knowledge-bases/{kbId}/files
                ↓
            进度回调 → 更新UI进度条
                ↓
            后端保存文件 → 拆分页面 → 发送OCR任务
                ↓
            返回文件对象(status: pending)
                ↓
            前端轮询或WebSocket监听状态变化
```

---

## 🚀 启动指南

### 1. 启动后端
```bash
cd /Users/zang/Documents/IDEA/AI-Middle-Platform/middle-platform
mvn spring-boot:run
```

### 2. 启动前端
```bash
cd /Users/zang/Documents/IDEA/AI-Middle-Platform/front
npm run dev
```

### 3. 访问应用
- 前端: http://localhost:3000
- 后端: http://localhost:8080
- API文档: http://localhost:8080/swagger-ui.html (如已配置)

### 4. 测试登录
```
用户名: zang
密码: zangzang
```

---

## 🐛 调试技巧

### 1. 查看网络请求
```bash
# Chrome DevTools → Network
# 筛选: XHR/Fetch
# 检查: Request Headers, Response
```

### 2. React Query DevTools
```tsx
// 已集成在开发环境
// 右下角悬浮按钮 → 查看查询状态/缓存
```

### 3. 后端日志
```bash
# application.yml 开启日志
logging:
  level:
    com.ai.middle.platform: DEBUG
```

### 4. 常见问题

#### CORS错误
```java
// 后端已配置CORS
@CrossOrigin(origins = "http://localhost:3000")
```

#### 401 Unauthorized
```typescript
// 检查token是否存在
const token = localStorage.getItem('auth_token');
console.log('Token:', token);

// 检查token是否过期
const { data: user } = useCurrentUser();
```

#### 404 Not Found
```typescript
// 检查API路径
console.log('API_BASE_URL:', process.env.NEXT_PUBLIC_API_URL);
// 应为: http://localhost:8080/api
```

---

## 📝 后续开发建议

### 1. WebSocket集成
```typescript
// 实时监听文件处理状态
const ws = new WebSocket('ws://localhost:8080/ws/file-status');
ws.onmessage = (event) => {
  const { fileId, status } = JSON.parse(event.data);
  queryClient.setQueryData(['file', fileId], (old) => ({
    ...old,
    statuses: { ...old.statuses, ocr: status }
  }));
};
```

### 2. 错误边界
```tsx
<ErrorBoundary fallback={<ErrorPage />}>
  <QueryClientProvider client={queryClient}>
    <App />
  </QueryClientProvider>
</ErrorBoundary>
```

### 3. 请求重试策略
```typescript
// lib/api/client.ts
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 3,
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    },
  },
});
```

### 4. 乐观更新增强
```typescript
// 文件上传乐观更新
const { mutate: uploadFile } = useMutation({
  mutationFn: api.files.upload,
  onMutate: async (variables) => {
    const tempFile = {
      id: 'temp-' + Date.now(),
      name: variables.file.name,
      status: 'uploading',
      progress: 0,
    };
    queryClient.setQueryData(['files', variables.kbId], (old) => [
      tempFile,
      ...old,
    ]);
    return { tempFile };
  },
});
```

---

## ✅ 集成检查清单

- [x] API客户端创建并配置
- [x] 认证hooks实现
- [x] 知识库hooks更新
- [x] 文件hooks更新
- [x] 模型配置hooks创建
- [x] 环境变量配置
- [x] 后端实体映射
- [x] Token管理集成
- [x] 错误处理
- [x] React Query缓存策略
- [ ] WebSocket实时更新 (可选)
- [ ] 文件上传进度显示 (可选)
- [ ] 请求拦截器/响应拦截器 (可选)

---

## 📚 相关文档

- [后端API文档](../middle-platform/README.md)
- [动态AI模型配置指南](../middle-platform/.claude/DYNAMIC_AI_MODEL_GUIDE.md)
- [OCR分页处理指南](../middle-platform/.claude/OCR_PAGE_PROCESSING_GUIDE.md)
- [React Query官方文档](https://tanstack.com/query/latest)
- [Next.js环境变量](https://nextjs.org/docs/app/building-your-application/configuring/environment-variables)

---

**集成完成时间**: 2025-10-04
**集成人**: Codex MCP + Claude Code
**状态**: ✅ 生产就绪
