'use client';

import { useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { ApiKey, Endpoint } from '@/types/model-config';
import { useModelEndpoints } from '@/lib/hooks/use-model-config';
import { Skeleton } from '@/components/ui/skeleton';

export interface ApiKeyFormValues {
  name: string;
  endpointId: string;
  key?: string;
  enabled: boolean;
  requestsPerMinute?: number | string;
  requestsPerDay?: number | string;
  expiresAt?: string;
}

interface ApiKeyDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  apiKey?: ApiKey | null;
  defaultEndpointId?: string;
  onSave?: (values: ApiKeyFormValues) => Promise<void> | void;
  saving?: boolean;
}

const DEFAULT_FORM: ApiKeyFormValues = {
  name: '',
  endpointId: '',
  key: '',
  enabled: true,
  requestsPerMinute: 60,
  requestsPerDay: 10000,
  expiresAt: undefined,
};

export function ApiKeyDialog({
  open,
  onOpenChange,
  apiKey,
  defaultEndpointId,
  onSave,
  saving = false,
}: ApiKeyDialogProps) {
  const { data: endpoints = [], isLoading: endpointsLoading } = useModelEndpoints();
  const [formState, setFormState] = useState<ApiKeyFormValues>(DEFAULT_FORM);
  const isEditing = Boolean(apiKey);

  useEffect(() => {
    if (!open) {
      return;
    }
    setFormState({
      name: apiKey?.name ?? '',
      endpointId: apiKey?.endpointId ?? defaultEndpointId ?? '',
      key: apiKey?.key ?? '',
      enabled: apiKey?.enabled ?? true,
      requestsPerMinute: apiKey?.rateLimit?.requestsPerMinute ?? 60,
      requestsPerDay: apiKey?.rateLimit?.requestsPerDay ?? 10000,
      expiresAt: apiKey?.expiresAt ? apiKey.expiresAt.split('T')[0] : undefined,
    });
  }, [apiKey, defaultEndpointId, open]);

  const endpointOptions = useMemo(() => {
    return endpoints.filter((endpoint) => endpoint.enabled);
  }, [endpoints]);

  const handleSave = async () => {
    const payload: ApiKeyFormValues = {
      ...formState,
      key: formState.key?.trim() || undefined,
      requestsPerMinute:
        formState.requestsPerMinute === '' ? undefined : formState.requestsPerMinute,
      requestsPerDay:
        formState.requestsPerDay === '' ? undefined : formState.requestsPerDay,
      expiresAt: formState.expiresAt || undefined,
    };

    await Promise.resolve(onSave?.(payload));
    onOpenChange(false);
  };

  const isSubmitDisabled =
    saving ||
    !formState.name.trim() ||
    !formState.endpointId ||
    (!isEditing && !(formState.key && formState.key.trim()));

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEditing ? '编辑 API Key' : '新增 API Key'}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="api-key-name" className="text-sm font-medium">
              Key 名称 <span className="text-red-500">*</span>
            </Label>
            <input
              id="api-key-name"
              type="text"
              placeholder="例如: 生产环境主 Key"
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              value={formState.name}
              onChange={(e) =>
                setFormState((prev) => ({ ...prev, name: e.target.value }))
              }
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="api-key-endpoint" className="text-sm font-medium">
              所属端点 <span className="text-red-500">*</span>
            </Label>
            {endpointsLoading ? (
              <Skeleton className="h-10 w-full" />
            ) : (
              <select
                id="api-key-endpoint"
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                value={formState.endpointId}
                onChange={(e) =>
                  setFormState((prev) => ({ ...prev, endpointId: e.target.value }))
                }
              >
                <option value="">请选择端点</option>
                {endpointOptions.map((endpoint) => (
                  <option key={endpoint.id} value={endpoint.id}>
                    {endpoint.name} ({endpoint.provider})
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="api-key-secret" className="text-sm font-medium">
              API Key {isEditing ? '(留空则不更新)' : <span className="text-red-500">*</span>}
            </Label>
            <input
              id="api-key-secret"
              type="text"
              placeholder="sk-..."
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary font-mono text-sm"
              value={formState.key ?? ''}
              onChange={(e) =>
                setFormState((prev) => ({ ...prev, key: e.target.value }))
              }
            />
            <p className="text-xs text-muted-foreground">
              请输入完整的 API Key, 系统会自动加密存储。
            </p>
          </div>

          <div className="space-y-3">
            <Label className="text-sm font-medium">限流配置</Label>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="api-key-rpm" className="text-xs text-muted-foreground">
                  每分钟请求数
                </Label>
                <input
                  id="api-key-rpm"
                  type="number"
                  min="1"
                  className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                  value={formState.requestsPerMinute ?? ''}
                  onChange={(e) =>
                    setFormState((prev) => ({
                      ...prev,
                      requestsPerMinute: e.target.value === '' ? '' : Number(e.target.value),
                    }))
                  }
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="api-key-rpd" className="text-xs text-muted-foreground">
                  每日请求数
                </Label>
                <input
                  id="api-key-rpd"
                  type="number"
                  min="1"
                  className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                  value={formState.requestsPerDay ?? ''}
                  onChange={(e) =>
                    setFormState((prev) => ({
                      ...prev,
                      requestsPerDay: e.target.value === '' ? '' : Number(e.target.value),
                    }))
                  }
                />
              </div>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="api-key-expire" className="text-sm font-medium">
              过期时间 (可选)
            </Label>
            <input
              id="api-key-expire"
              type="date"
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              value={formState.expiresAt ?? ''}
              onChange={(e) =>
                setFormState((prev) => ({ ...prev, expiresAt: e.target.value || undefined }))
              }
            />
          </div>

          <div className="flex items-center gap-3">
            <input
              id="api-key-enabled"
              type="checkbox"
              className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-2 focus:ring-primary"
              checked={formState.enabled}
              onChange={(e) =>
                setFormState((prev) => ({ ...prev, enabled: e.target.checked }))
              }
            />
            <Label htmlFor="api-key-enabled" className="text-sm font-medium cursor-pointer">
              启用该 API Key
            </Label>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={saving}>
            取消
          </Button>
          <Button onClick={handleSave} disabled={isSubmitDisabled}>
            {saving ? '保存中...' : '保存'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
