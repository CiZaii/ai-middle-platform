'use client';

import { useState, useMemo } from 'react';
import { format } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import {
  FileText,
  Plus,
  Search,
  Edit,
  Trash2,
  Activity,
  CheckCircle2,
  XCircle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { Skeleton } from '@/components/ui/skeleton';
import { usePrompts, useTogglePrompt, useDeletePrompt } from '@/lib/hooks/use-prompts';
import { Prompt, BUSINESS_NAME_MAP } from '@/types/prompt';
import { PromptDialog } from '@/components/features/prompts/prompt-dialog';

export default function PromptsPage() {
  const { data: prompts = [], isLoading } = usePrompts();
  const togglePromptMutation = useTogglePrompt();
  const deletePromptMutation = useDeletePrompt();
  
  const [searchTerm, setSearchTerm] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingPrompt, setEditingPrompt] = useState<Prompt | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const filteredPrompts = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase();
    if (!keyword) return prompts;
    return prompts.filter((prompt) => {
      const name = prompt.promptName?.toLowerCase() ?? '';
      const businessCode = prompt.businessCode?.toLowerCase() ?? '';
      const description = prompt.description?.toLowerCase() ?? '';
      return name.includes(keyword) || businessCode.includes(keyword) || description.includes(keyword);
    });
  }, [prompts, searchTerm]);

  const activeCount = prompts.filter(p => p.isActive).length;
  const totalVariables = prompts.reduce((sum, p) => sum + (p.variables?.length || 0), 0);

  const handleAdd = () => {
    setEditingPrompt(null);
    setDialogOpen(true);
  };

  const handleEdit = (prompt: Prompt) => {
    setEditingPrompt(prompt);
    setDialogOpen(true);
  };

  const handleToggleActive = async (prompt: Prompt) => {
    try {
      await togglePromptMutation.mutateAsync({
        id: prompt.id,
        isActive: !prompt.isActive,
      });
    } catch (error) {
      console.error('切换 Prompt 状态失败', error);
    }
  };

  const handleDelete = async (prompt: Prompt) => {
    if (!window.confirm(`确定删除 Prompt「${prompt.promptName}」吗？此操作不可恢复。`)) {
      return;
    }
    setDeletingId(prompt.id);
    try {
      await deletePromptMutation.mutateAsync(prompt.id);
    } catch (error) {
      console.error('删除 Prompt 失败', error);
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold bg-gradient-to-r from-primary-start to-primary-end bg-clip-text text-transparent">
            Prompt 模板管理
          </h1>
          <p className="text-muted-foreground mt-1">
            管理 AI 业务的 Prompt 模板，支持变量替换和版本管理
          </p>
        </div>
        <Button className="gap-2" onClick={handleAdd}>
          <Plus className="h-4 w-4" />
          新增 Prompt
        </Button>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">总 Prompt 数</p>
              <p className="text-2xl font-bold mt-1">{prompts.length}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center">
              <FileText className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">激活中</p>
              <p className="text-2xl font-bold mt-1 text-green-600">{activeCount}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-green-500 to-green-600 flex items-center justify-center">
              <CheckCircle2 className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">停用中</p>
              <p className="text-2xl font-bold mt-1 text-amber-600">{prompts.length - activeCount}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-amber-500 to-amber-600 flex items-center justify-center">
              <XCircle className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">变量总数</p>
              <p className="text-2xl font-bold mt-1">{totalVariables}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-purple-500 to-purple-600 flex items-center justify-center">
              <Activity className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>
      </div>

      {/* 搜索框 */}
      <div className="mb-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="搜索 Prompt 名称、业务代码或描述..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10"
          />
        </div>
      </div>

      {/* Prompt 列表 */}
      {isLoading ? (
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <Card key={i} className="p-6">
              <Skeleton className="h-6 w-48 mb-4" />
              <Skeleton className="h-4 w-full mb-2" />
              <Skeleton className="h-4 w-2/3" />
            </Card>
          ))}
        </div>
      ) : filteredPrompts.length === 0 ? (
        <Card className="p-12">
          <div className="text-center text-muted-foreground">
            <FileText className="h-12 w-12 mx-auto mb-4 opacity-50" />
            <p className="text-lg font-medium">暂无 Prompt 模板</p>
            <p className="text-sm mt-2">
              {searchTerm ? '没有找到匹配的 Prompt，请尝试其他关键词' : '点击上方按钮创建第一个 Prompt 模板'}
            </p>
          </div>
        </Card>
      ) : (
        <div className="space-y-4">
          {filteredPrompts.map((prompt) => (
            <Card key={prompt.id} className="p-6 hover:shadow-lg transition-shadow">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-3">
                    <h3 className="text-lg font-semibold">{prompt.promptName}</h3>
                    <Badge variant="outline" className="text-xs">
                      {BUSINESS_NAME_MAP[prompt.businessCode] || prompt.businessCode}
                    </Badge>
                    <Badge variant={prompt.isActive ? 'default' : 'secondary'} className="text-xs">
                      {prompt.isActive ? '已激活' : '已停用'}
                    </Badge>
                    <Badge variant="outline" className="text-xs">
                      v{prompt.version}
                    </Badge>
                  </div>
                  
                  {prompt.description && (
                    <p className="text-sm text-muted-foreground mb-3">{prompt.description}</p>
                  )}

                  <div className="flex items-center gap-6 text-xs text-muted-foreground">
                    <span>业务代码: {prompt.businessCode}</span>
                    <span>变量: {prompt.variables.length} 个</span>
                    <span>创建时间: {format(new Date(prompt.createdAt), 'yyyy-MM-dd HH:mm', { locale: zhCN })}</span>
                    <span>更新时间: {format(new Date(prompt.updatedAt), 'yyyy-MM-dd HH:mm', { locale: zhCN })}</span>
                  </div>

                  {prompt.variables.length > 0 && (
                    <div className="mt-3 flex items-center gap-2 flex-wrap">
                      <span className="text-xs text-muted-foreground">变量列表:</span>
                      {prompt.variables.map((variable, idx) => (
                        <code
                          key={idx}
                          className="text-xs px-2 py-1 bg-muted rounded-md font-mono"
                        >
                          {'{' + variable + '}'}
                        </code>
                      ))}
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-2 ml-6">
                  <Switch
                    checked={prompt.isActive}
                    onCheckedChange={() => handleToggleActive(prompt)}
                    disabled={togglePromptMutation.isPending}
                  />
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleEdit(prompt)}
                  >
                    <Edit className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDelete(prompt)}
                    disabled={deletingId === prompt.id}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Prompt 编辑对话框 */}
      <PromptDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        prompt={editingPrompt}
      />
    </>
  );
}
