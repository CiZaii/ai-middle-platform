'use client';

import { useMemo, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { format } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Copy,
  Eye,
  EyeOff,
  Key,
  Plus,
  Search,
  TrendingUp,
  Trash2,
  XCircle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  useApiKeys,
  useCreateApiKey,
  useDeleteApiKey,
  useModelEndpoints,
  useUpdateApiKey,
} from '@/lib/hooks/use-model-config';
import { ApiKey } from '@/types/model-config';
import { ApiKeyDialog, ApiKeyFormValues } from '@/components/features/model-config/api-key-dialog';

export default function ApiKeyConfigPage() {
  const searchParams = useSearchParams();
  const endpointFilter = searchParams.get('endpoint');
  const [selectedEndpoint, setSelectedEndpoint] = useState(endpointFilter ?? 'all');
  const [searchTerm, setSearchTerm] = useState('');
  const [visibleKeys, setVisibleKeys] = useState<Set<string>>(new Set());
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingApiKey, setEditingApiKey] = useState<ApiKey | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const { data: endpoints = [] } = useModelEndpoints();
  const filterEndpointId = selectedEndpoint === 'all' ? undefined : selectedEndpoint;
  const { data: apiKeys = [], isLoading } = useApiKeys(filterEndpointId);
  const createApiKeyMutation = useCreateApiKey();
  const updateApiKeyMutation = useUpdateApiKey(editingApiKey?.id ?? '');
  const deleteApiKeyMutation = useDeleteApiKey();

  const filteredKeys = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase();
    if (!keyword) return apiKeys;
    return apiKeys.filter((keyItem) => keyItem.name.toLowerCase().includes(keyword));
  }, [apiKeys, searchTerm]);

  const totalKeys = apiKeys.length;
  const activeKeys = apiKeys.filter((key) => key.enabled).length;
  const totalRequests = apiKeys.reduce(
    (sum, key) => sum + (key.stats?.totalRequests ?? 0),
    0,
  );
  const avgSuccessRate = apiKeys.length
    ? apiKeys.reduce((sum, key) => {
        const stats = key.stats;
        if (!stats || !stats.totalRequests) return sum;
        return sum + (stats.successRequests / stats.totalRequests) * 100;
      }, 0) / apiKeys.length
    : 0;

  const isSaving = createApiKeyMutation.isPending || updateApiKeyMutation.isPending;

  const toggleKeyVisibility = (keyId: string) => {
    setVisibleKeys((prev) => {
      const next = new Set(prev);
      if (next.has(keyId)) {
        next.delete(keyId);
      } else {
        next.add(keyId);
      }
      return next;
    });
  };

  const handleCopy = (text: string) => {
    if (!text) return;
    navigator.clipboard.writeText(text).catch((error) => {
      console.error('复制 API Key 失败', error);
    });
  };

  const handleAdd = () => {
    setEditingApiKey(null);
    setDialogOpen(true);
  };

  const handleEdit = (apiKey: ApiKey) => {
    setEditingApiKey(apiKey);
    setDialogOpen(true);
  };

  const handleSave = async (values: ApiKeyFormValues) => {
    try {
      const payload: Record<string, unknown> = { ...values };
      if (editingApiKey) {
        await updateApiKeyMutation.mutateAsync(payload);
      } else {
        await createApiKeyMutation.mutateAsync(payload);
      }
    } catch (error) {
      console.error('保存 API Key 失败', error);
      throw error;
    }
  };

  const handleDelete = async (apiKey: ApiKey) => {
    if (!window.confirm(`确定删除 API Key「${apiKey.name}」吗？`)) {
      return;
    }
    setDeletingId(apiKey.id);
    try {
      await deleteApiKeyMutation.mutateAsync(apiKey.id);
    } catch (error) {
      console.error('删除 API Key 失败', error);
    } finally {
      setDeletingId(null);
    }
  };

  const handleDialogOpenChange = (open: boolean) => {
    setDialogOpen(open);
    if (!open) {
      setEditingApiKey(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold bg-gradient-to-r from-primary-start to-primary-end bg-clip-text text-transparent">
            API Key 管理
          </h1>
          <p className="text-muted-foreground mt-1">管理各端点的 API Keys 及其使用情况</p>
        </div>
        <Button className="gap-2" onClick={handleAdd}>
          <Plus className="h-4 w-4" />
          新增 API Key
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">总 Keys</p>
              <p className="text-2xl font-bold mt-1">{totalKeys}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center">
              <Key className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">激活中</p>
              <p className="text-2xl font-bold mt-1 text-green-600">{activeKeys}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-green-500 to-green-600 flex items-center justify-center">
              <CheckCircle2 className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">总请求数</p>
              <p className="text-2xl font-bold mt-1">
                {totalRequests >= 1000 ? `${(totalRequests / 1000).toFixed(1)}K` : totalRequests}
              </p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-purple-500 to-purple-600 flex items-center justify-center">
              <TrendingUp className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">平均成功率</p>
              <p className="text-2xl font-bold mt-1 text-green-600">{avgSuccessRate.toFixed(1)}%</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-orange-500 to-orange-600 flex items-center justify-center">
              <Activity className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>
      </div>

      <Card className="p-4 space-y-4">
        <div className="flex items-center gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <input
              type="text"
              placeholder="搜索 API Key 名称..."
              className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <select
            className="px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            value={selectedEndpoint}
            onChange={(e) => setSelectedEndpoint(e.target.value)}
          >
            <option value="all">全部端点</option>
            {endpoints.map((endpoint) => (
              <option key={endpoint.id} value={endpoint.id}>
                {endpoint.name}
              </option>
            ))}
          </select>
        </div>
      </Card>

      <div className="space-y-4">
        {isLoading ? (
          Array.from({ length: 3 }).map((_, idx) => (
            <Card key={idx} className="p-6">
              <Skeleton className="h-6 w-1/3 mb-4" />
              <Skeleton className="h-4 w-full mb-2" />
              <Skeleton className="h-4 w-2/3" />
            </Card>
          ))
        ) : filteredKeys.length > 0 ? (
          filteredKeys.map((apiKey) => {
            const stats = apiKey.stats;
            return (
              <Card key={apiKey.id} className="p-6 hover:shadow-lg transition-shadow">
                <div className="space-y-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-3 mb-2 flex-wrap">
                        <h3 className="text-lg font-semibold break-all">{apiKey.name}</h3>
                        {apiKey.enabled ? (
                          <span className="px-2 py-1 text-xs font-medium bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300 rounded-full flex items-center gap-1">
                            <CheckCircle2 className="h-3 w-3" />
                            激活
                          </span>
                        ) : (
                          <span className="px-2 py-1 text-xs font-medium bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300 rounded-full flex items-center gap-1">
                            <XCircle className="h-3 w-3" />
                            停用
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-muted-foreground break-all">
                        端点: {apiKey.endpoint?.name ?? '未知端点'}
                      </p>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleEdit(apiKey)}
                        className="gap-1"
                      >
                        编辑
                      </Button>
                      <Button
                        variant="destructive"
                        size="sm"
                        className="gap-1"
                        onClick={() => handleDelete(apiKey)}
                        disabled={deletingId === apiKey.id || deleteApiKeyMutation.isPending}
                      >
                        <Trash2 className="h-4 w-4" />
                        删除
                      </Button>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 p-3 bg-muted rounded-lg">
                    <Key className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                    <code className="flex-1 text-sm font-mono break-all">
                      {visibleKeys.has(apiKey.id) ? apiKey.key ?? '' : apiKey.displayKey ?? ''}
                    </code>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 p-0"
                      onClick={() => toggleKeyVisibility(apiKey.id)}
                    >
                      {visibleKeys.has(apiKey.id) ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 p-0"
                      onClick={() => handleCopy(apiKey.key ?? '')}
                    >
                      <Copy className="h-4 w-4" />
                    </Button>
                  </div>

                  {apiKey.endpoint?.businesses && apiKey.endpoint.businesses.length > 0 && (
                    <div>
                      <p className="text-sm font-medium text-muted-foreground mb-2">关联业务 (通过端点):</p>
                      <div className="flex flex-wrap gap-2">
                        {apiKey.endpoint.businesses.map((business) => (
                          <span
                            key={business.id}
                            className="px-2 py-1 text-xs bg-purple-50 dark:bg-purple-950 text-purple-700 dark:text-purple-300 rounded border border-purple-200 dark:border-purple-800"
                          >
                            {business.name}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {(apiKey.rateLimit?.requestsPerMinute || apiKey.rateLimit?.requestsPerDay) && (
                    <div className="grid grid-cols-2 gap-4 p-3 bg-blue-50 dark:bg-blue-950 rounded-lg">
                      <div>
                        <p className="text-xs text-muted-foreground">每分钟请求</p>
                        <p className="text-sm font-semibold mt-1">
                          {apiKey.rateLimit?.requestsPerMinute ?? 0} 次
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">每日请求</p>
                        <p className="text-sm font-semibold mt-1">
                          {(apiKey.rateLimit?.requestsPerDay ?? 0).toLocaleString()}
                        </p>
                      </div>
                    </div>
                  )}

                  {stats && (
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-4 border-t">
                      <div>
                        <p className="text-xs text-muted-foreground">总请求</p>
                        <p className="text-lg font-semibold mt-1">
                          {(stats.totalRequests ?? 0).toLocaleString()}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">成功</p>
                        <p className="text-lg font-semibold mt-1 text-green-600">
                          {(stats.successRequests ?? 0).toLocaleString()}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">失败</p>
                        <p className="text-lg font-semibold mt-1 text-red-600">
                          {(stats.failedRequests ?? 0).toLocaleString()}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">成功率</p>
                        <p className="text-lg font-semibold mt-1 text-green-600">
                          {stats.totalRequests
                            ? (((stats.successRequests ?? 0) / stats.totalRequests) * 100).toFixed(1)
                            : '0.0'}
                          %
                        </p>
                      </div>
                    </div>
                  )}

                  {stats?.lastError && (
                    <div className="flex items-start gap-2 p-3 bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 rounded-lg">
                      <AlertTriangle className="h-4 w-4 text-red-600 mt-0.5 flex-shrink-0" />
                      <div className="flex-1">
                        <p className="text-sm font-medium text-red-900 dark:text-red-300">最后错误</p>
                        <p className="text-sm text-red-700 dark:text-red-400 mt-1 break-words">
                          {stats.lastError}
                        </p>
                      </div>
                    </div>
                  )}

                  <div className="flex items-center justify-between text-sm text-muted-foreground pt-2 border-t">
                    <div className="flex items-center gap-4 flex-wrap">
                      <span>创建: {format(new Date(apiKey.createdAt), 'PPp', { locale: zhCN })}</span>
                      {apiKey.lastUsedAt && (
                        <span className="flex items-center gap-1">
                          <Clock className="h-3 w-3" />
                          最后使用: {format(new Date(apiKey.lastUsedAt), 'PPp', { locale: zhCN })}
                        </span>
                      )}
                    </div>
                    {apiKey.expiresAt && (
                      <span className="text-orange-600">
                        过期时间: {format(new Date(apiKey.expiresAt), 'PPP', { locale: zhCN })}
                      </span>
                    )}
                  </div>
                </div>
              </Card>
            );
          })
        ) : (
          <div className="text-center py-12">
            <Key className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">未找到匹配的 API Key</p>
          </div>
        )}
      </div>

      <ApiKeyDialog
        open={dialogOpen}
        onOpenChange={handleDialogOpenChange}
        apiKey={editingApiKey}
        defaultEndpointId={selectedEndpoint !== 'all' ? selectedEndpoint : undefined}
        onSave={handleSave}
        saving={isSaving}
      />
    </div>
  );
}
