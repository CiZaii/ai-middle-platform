# 🎯 **完整技术选型方案**

## 📦 **核心依赖包**

```json
{
  "dependencies": {
    // 基础框架
    "next": "^15.0.0",
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "typescript": "^5.3.0",
    
    // Shadcn/ui 核心
    "@radix-ui/react-accordion": "^1.1.2",
    "@radix-ui/react-alert-dialog": "^1.0.5",
    "@radix-ui/react-dialog": "^1.0.5",
    "@radix-ui/react-dropdown-menu": "^2.0.6",
    "@radix-ui/react-select": "^2.0.0",
    "@radix-ui/react-tabs": "^1.0.4",
    "@radix-ui/react-toast": "^1.1.5",
    "@radix-ui/react-tooltip": "^1.0.7",
    "class-variance-authority": "^0.7.0",
    "clsx": "^2.0.0",
    "tailwind-merge": "^2.0.0",
    
    // Tailwind CSS
    "tailwindcss": "^3.4.0",
    "tailwindcss-animate": "^1.0.7",
    "@tailwindcss/typography": "^0.5.10",
    
    // Markdown 渲染
    "react-markdown": "^9.0.1",
    "remark-gfm": "^4.0.0",
    "remark-math": "^6.0.0",
    "rehype-katex": "^7.0.0",
    "rehype-highlight": "^7.0.0",
    "rehype-raw": "^7.0.0",
    "highlight.js": "^11.9.0",
    "katex": "^0.16.8",
    "mermaid": "^10.6.1",
    
    // 表格渲染
    "@tanstack/react-table": "^8.10.7",
    "@tanstack/react-virtual": "^3.0.0",
    
    // PDF 渲染
    "react-pdf": "^7.5.1",
    "pdfjs-dist": "^3.11.174",
    
    // 知识图谱可视化
    "reactflow": "^11.10.1",
    "@reactflow/node-toolbar": "^1.3.0",
    "@reactflow/node-resizer": "^2.2.0",
    "@reactflow/minimap": "^11.7.0",
    "@reactflow/controls": "^11.2.0",
    "@reactflow/background": "^11.3.0",
    
    // 复杂表单处理
    "react-hook-form": "^7.48.2",
    "@hookform/resolvers": "^3.3.2",
    "zod": "^3.22.4",
    "react-select": "^5.8.0",
    "react-datepicker": "^4.21.0",
    "react-dropzone": "^14.2.3",
    "react-colorful": "^5.6.1",
    
    // 动画与交互
    "framer-motion": "^10.16.16",
    "lottie-react": "^2.4.0",
    
    // 状态管理
    "zustand": "^4.4.7",
    "@tanstack/react-query": "^5.8.4",
    
    // 工具库
    "date-fns": "^3.0.0",
    "lucide-react": "^0.300.0",
    "react-use": "^17.4.0"
  },
  
  "devDependencies": {
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "@types/node": "^20.10.0",
    "@next/bundle-analyzer": "^15.0.0",
    "eslint": "^8.55.0",
    "prettier": "^3.1.0",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.32"
  }
}
```

## 🎨 **功能模块技术选型**

### **Markdown 渲染**
- `react-markdown` - 核心渲染引擎
- `remark-gfm` - GitHub风格Markdown
- `remark-math` + `rehype-katex` - 数学公式支持
- `rehype-highlight` - 代码高亮
- `mermaid` - 图表渲染

### **表格渲染**
- `@tanstack/react-table` - 现代化表格库
- `@tanstack/react-virtual` - 虚拟滚动性能优化

### **PDF 文件渲染**
- `react-pdf` - PDF显示组件
- `pdfjs-dist` - PDF.js核心库

### **知识图谱可视化**
- `reactflow` - 节点图可视化
- `@reactflow/*` - 相关插件包

### **复杂表单处理**
- `react-hook-form` - 表单状态管理
- `zod` - 类型安全验证
- `react-select` - 高级选择器
- `react-datepicker` - 日期选择
- `react-dropzone` - 文件上传
- `react-colorful` - 颜色选择器

### **动画与交互**
- `framer-motion` - 主动画库
- `lottie-react` - Lottie动画支持

### **状态管理**
- `zustand` - 轻量级状态管理
- `@tanstack/react-query` - 服务端状态管理

### **UI基础设施**
- `@radix-ui/*` - 无样式UI原语
- `tailwindcss` - 原子化CSS框架
- `class-variance-authority` - 组件变体管理
- `tailwind-merge` - 类名合并工具

### **开发工具**
- `typescript` - 类型安全
- `eslint` + `prettier` - 代码规范
- `@next/bundle-analyzer` - 包大小分析

## 🎯 **技术栈总结**

| 功能模块 | 主要技术 | 备选方案 |
|---------|---------|---------|
| **UI组件库** | Shadcn/ui + Radix UI | Ant Design, Mantine |
| **样式方案** | Tailwind CSS | Styled Components |
| **Markdown渲染** | react-markdown | @uiw/react-md-editor |
| **表格组件** | TanStack Table | Ant Design Table |
| **PDF渲染** | react-pdf | PDF-lib |
| **图谱可视化** | React Flow | D3.js, Cytoscape.js |
| **表单处理** | React Hook Form + Zod | Formik + Yup |
| **动画库** | Framer Motion | React Spring |
| **状态管理** | Zustand + React Query | Redux Toolkit |
| **图标库** | Lucide React | Heroicons, Tabler Icons |

这套技术选型确保了：
- 🎨 **现代化UI设计**
- 🚀 **优秀性能表现**
- 💫 **流畅交互动画**
- 🔧 **完善开发体验**
- 📱 **全面功能覆盖**

