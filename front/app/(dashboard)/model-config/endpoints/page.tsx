'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { format } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import {
  Activity,
  CheckCircle2,
  ChevronRight,
  Globe,
  Key,
  Plus,
  Search,
  Server,
  Trash2,
  XCircle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  useCreateModelEndpoint,
  useDeleteModelEndpoint,
  useModelEndpoints,
  useUpdateModelEndpoint,
} from '@/lib/hooks/use-model-config';
import { Endpoint, Provider } from '@/types/model-config';
import { EndpointDialog, EndpointFormValues } from '@/components/features/model-config/endpoint-dialog';

const providerColors: Record<string, string> = {
  [Provider.OPENAI]: 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300',
  [Provider.CLAUDE]: 'bg-purple-100 text-purple-700 dark:bg-purple-900 dark:text-purple-300',
  [Provider.GEMINI]: 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300',
  [Provider.AZURE]: 'bg-cyan-100 text-cyan-700 dark:bg-cyan-900 dark:text-cyan-300',
  [Provider.CUSTOM]: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
};

export default function EndpointConfigPage() {
  const { data: endpoints = [], isLoading } = useModelEndpoints();
  const createEndpointMutation = useCreateModelEndpoint();
  const [editingEndpoint, setEditingEndpoint] = useState<Endpoint | null>(null);
  const updateEndpointMutation = useUpdateModelEndpoint(editingEndpoint?.id ?? '');
  const deleteEndpointMutation = useDeleteModelEndpoint();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  const filteredEndpoints = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase();
    if (!keyword) return endpoints;
    return endpoints.filter((endpoint) => {
      const name = endpoint.name?.toLowerCase() ?? '';
      const url = endpoint.baseUrl?.toLowerCase() ?? '';
      return name.includes(keyword) || url.includes(keyword);
    });
  }, [endpoints, searchTerm]);

  const enabledCount = endpoints.filter((endpoint) => endpoint.enabled).length;
  const totalApiKeys = endpoints.reduce(
    (sum, endpoint) => sum + (endpoint.stats?.totalApiKeys ?? 0),
    0,
  );
  const totalRequests = endpoints.reduce(
    (sum, endpoint) => sum + (endpoint.stats?.totalRequests ?? 0),
    0,
  );

  const isSaving = createEndpointMutation.isPending || updateEndpointMutation.isPending;

  const handleAdd = () => {
    setEditingEndpoint(null);
    setDialogOpen(true);
  };

  const handleEdit = (endpoint: Endpoint) => {
    setEditingEndpoint(endpoint);
    setDialogOpen(true);
  };

  const handleSave = async (values: EndpointFormValues) => {
    try {
      const payload: Record<string, unknown> = { ...values };
      if (editingEndpoint) {
        await updateEndpointMutation.mutateAsync(payload);
      } else {
        await createEndpointMutation.mutateAsync(payload);
      }
    } catch (error) {
      console.error('保存端点配置失败', error);
      throw error;
    }
  };

  const handleDelete = async (endpoint: Endpoint) => {
    if (!window.confirm(`确定删除端点「${endpoint.name}」吗？`)) {
      return;
    }
    setDeletingId(endpoint.id);
    try {
      await deleteEndpointMutation.mutateAsync(endpoint.id);
    } catch (error) {
      console.error('删除端点失败', error);
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold bg-gradient-to-r from-primary-start to-primary-end bg-clip-text text-transparent">
            端点配置管理
          </h1>
          <p className="text-muted-foreground mt-1">
            配置和管理 AI 模型服务的 API 端点
          </p>
        </div>
        <Button className="gap-2" onClick={handleAdd}>
          <Plus className="h-4 w-4" />
          新增端点
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">总端点数</p>
              <p className="text-2xl font-bold mt-1">{endpoints.length}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center">
              <Server className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">启用中</p>
              <p className="text-2xl font-bold mt-1 text-green-600">{enabledCount}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-green-500 to-green-600 flex items-center justify-center">
              <CheckCircle2 className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">API Keys</p>
              <p className="text-2xl font-bold mt-1">{totalApiKeys}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-purple-500 to-purple-600 flex items-center justify-center">
              <Key className="h-6 w-6 text-white" />
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
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-orange-500 to-orange-600 flex items-center justify-center">
              <Activity className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>
      </div>

      <Card className="p-4 mb-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="搜索端点名称或 URL..."
            className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </Card>

      <div className="space-y-4">
        {isLoading ? (
          Array.from({ length: 3 }).map((_, idx) => (
            <Card key={idx} className="p-6">
              <div className="flex items-start gap-6">
                <Skeleton className="h-16 w-16 rounded-lg" />
                <div className="flex-1 space-y-4">
                  <Skeleton className="h-5 w-1/3" />
                  <Skeleton className="h-4 w-2/3" />
                  <Skeleton className="h-4 w-full" />
                </div>
              </div>
            </Card>
          ))
        ) : filteredEndpoints.length > 0 ? (
          filteredEndpoints.map((endpoint) => (
            <Card key={endpoint.id} className="p-6 hover:shadow-lg transition-shadow">
              <div className="flex items-start gap-6">
                <div className="h-16 w-16 rounded-lg bg-gradient-to-br from-primary-start to-primary-end flex items-center justify-center flex-shrink-0">
                  <Globe className="h-8 w-8 text-white" />
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-3 mb-2 flex-wrap">
                    <h3 className="text-xl font-semibold break-all">{endpoint.name}</h3>
                    <span
                      className={`px-2 py-1 text-xs font-medium rounded-full ${
                        providerColors[endpoint.provider as Provider] ?? providerColors[Provider.CUSTOM]
                      }`}
                    >
                      {endpoint.provider}
                    </span>
                    {endpoint.enabled ? (
                      <span className="px-2 py-1 text-xs font-medium bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300 rounded-full flex items-center gap-1">
                        <CheckCircle2 className="h-3 w-3" />
                        启用
                      </span>
                    ) : (
                      <span className="px-2 py-1 text-xs font-medium bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300 rounded-full flex items-center gap-1">
                        <XCircle className="h-3 w-3" />
                        停用
                      </span>
                    )}
                  </div>

                  <div className="space-y-2 text-sm">
                    <div className="flex items-center gap-2">
                      <Server className="h-4 w-4 text-muted-foreground" />
                      <code className="text-xs bg-muted px-2 py-1 rounded break-all">
                        {endpoint.baseUrl}
                      </code>
                    </div>
                    {endpoint.description && (
                      <p className="text-muted-foreground break-words">
                        {endpoint.description}
                      </p>
                    )}
                  </div>

                  {endpoint.models && endpoint.models.length > 0 && (
                    <div className="mt-4">
                      <p className="text-sm font-medium text-muted-foreground mb-2">支持模型:</p>
                      <div className="flex flex-wrap gap-2">
                        {endpoint.models.map((model) => (
                          <span
                            key={model.id}
                            className="px-2 py-1 text-xs bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 rounded border border-blue-200 dark:border-blue-800"
                          >
                            {model.name}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {endpoint.businesses && endpoint.businesses.length > 0 && (
                    <div className="mt-3">
                      <p className="text-sm font-medium text-muted-foreground mb-2">关联业务:</p>
                      <div className="flex flex-wrap gap-2">
                        {endpoint.businesses.map((business) => (
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

                  {endpoint.stats && (
                    <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 mt-4 pt-4 border-t">
                      <div>
                        <p className="text-xs text-muted-foreground">API Keys</p>
                        <p className="text-lg font-semibold mt-1">
                          {endpoint.stats.activeApiKeys}/{endpoint.stats.totalApiKeys}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">总请求数</p>
                        <p className="text-lg font-semibold mt-1">
                          {endpoint.stats.totalRequests?.toLocaleString?.() ?? endpoint.stats.totalRequests}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">成功率</p>
                        <p className="text-lg font-semibold mt-1 text-green-600">
                          {(endpoint.stats.successRate ?? 0).toFixed(1)}%
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">更新时间</p>
                        <p className="text-sm font-medium mt-1">
                          {endpoint.updatedAt
                            ? format(new Date(endpoint.updatedAt), 'PP', { locale: zhCN })
                            : '-'}
                        </p>
                      </div>
                    </div>
                  )}
                </div>

                <div className="flex flex-col gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    className="gap-2"
                    onClick={() => handleEdit(endpoint)}
                  >
                    编辑
                  </Button>
                  <Button variant="outline" size="sm" asChild>
                    <Link href={`/model-config/api-keys?endpoint=${endpoint.id}`} className="gap-2">
                      管理 Keys
                      <ChevronRight className="h-4 w-4" />
                    </Link>
                  </Button>
                  <Button
                    variant="destructive"
                    size="sm"
                    className="gap-2"
                    onClick={() => handleDelete(endpoint)}
                    disabled={deletingId === endpoint.id || deleteEndpointMutation.isPending}
                  >
                    <Trash2 className="h-4 w-4" />
                    删除
                  </Button>
                </div>
              </div>
            </Card>
          ))
        ) : (
          <div className="text-center py-12">
            <Server className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">未找到匹配的端点</p>
          </div>
        )}
      </div>

      <EndpointDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        endpoint={editingEndpoint}
        onSave={handleSave}
        saving={isSaving}
      />
    </>
  );
}
