'use client';

import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Endpoint, Provider } from '@/types/model-config';
import { useBusinesses } from '@/lib/hooks/use-model-config';
import { Skeleton } from '@/components/ui/skeleton';

export interface EndpointFormValues {
  name: string;
  baseUrl: string;
  provider: Provider;
  description?: string;
  enabled: boolean;
  businessIds: string[];
  modelNames: string[];
}

interface EndpointDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  endpoint?: Endpoint | null;
  onSave?: (values: EndpointFormValues) => Promise<void> | void;
  saving?: boolean;
}

const DEFAULT_FORM: EndpointFormValues = {
  name: '',
  baseUrl: '',
  provider: Provider.OPENAI,
  description: '',
  enabled: true,
  businessIds: [],
  modelNames: [],
};

function extractModelNames(endpoint?: Endpoint | null) {
  if (!endpoint?.models) return [] as string[];
  return endpoint.models.map((model) => model.name).filter(Boolean);
}

export function EndpointDialog({
  open,
  onOpenChange,
  endpoint,
  onSave,
  saving = false,
}: EndpointDialogProps) {
  const { data: businesses = [], isLoading: businessesLoading } = useBusinesses();
  const [formState, setFormState] = useState<EndpointFormValues>(DEFAULT_FORM);
  const [modelsInput, setModelsInput] = useState('');

  useEffect(() => {
    if (!open) {
      return;
    }
    const modelNames = extractModelNames(endpoint);
    setModelsInput(modelNames.join('\n'));
    setFormState({
      name: endpoint?.name ?? '',
      baseUrl: endpoint?.baseUrl ?? '',
      provider: (endpoint?.provider as Provider) ?? Provider.OPENAI,
      description: endpoint?.description ?? '',
      enabled: endpoint?.enabled ?? true,
      businessIds: endpoint?.businesses?.map((biz) => biz.id) ?? [],
      modelNames,
    });
  }, [endpoint, open]);

  const toggleBusiness = (businessId: string) => {
    setFormState((prev) => {
      const isSelected = prev.businessIds.includes(businessId);
      return {
        ...prev,
        businessIds: isSelected
          ? prev.businessIds.filter((id) => id !== businessId)
          : [...prev.businessIds, businessId],
      };
    });
  };

  const handleSave = async () => {
    const modelNames = modelsInput
      .split(/\n|,/)
      .map((item) => item.trim())
      .filter((item) => item.length > 0);

    const payload: EndpointFormValues = {
      ...formState,
      modelNames,
    };

    await Promise.resolve(onSave?.(payload));
    onOpenChange(false);
  };

  const isSubmitDisabled =
    saving || !formState.name.trim() || !formState.baseUrl.trim();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {endpoint ? '编辑端点配置' : '新增端点配置'}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="endpoint-name" className="text-sm font-medium">
              端点名称 <span className="text-red-500">*</span>
            </Label>
            <input
              id="endpoint-name"
              type="text"
              placeholder="请输入端点名称"
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              value={formState.name}
              onChange={(e) =>
                setFormState((prev) => ({ ...prev, name: e.target.value }))
              }
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="endpoint-base-url" className="text-sm font-medium">
              Base URL <span className="text-red-500">*</span>
            </Label>
            <input
              id="endpoint-base-url"
              type="url"
              placeholder="https://api.example.com/v1"
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary font-mono text-sm"
              value={formState.baseUrl}
              onChange={(e) =>
                setFormState((prev) => ({ ...prev, baseUrl: e.target.value }))
              }
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="endpoint-provider" className="text-sm font-medium">
              提供商 <span className="text-red-500">*</span>
            </Label>
            <select
              id="endpoint-provider"
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              value={formState.provider}
              onChange={(e) =>
                setFormState((prev) => ({
                  ...prev,
                  provider: e.target.value as Provider,
                }))
              }
            >
              {Object.values(Provider).map((provider) => (
                <option key={provider} value={provider}>
                  {provider}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="endpoint-models" className="text-sm font-medium">
              支持模型（每行一个或使用逗号分隔）
            </Label>
            <textarea
              id="endpoint-models"
              rows={4}
              placeholder="例如：gpt-4o\ntext-embedding-3-small"
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary resize-none"
              value={modelsInput}
              onChange={(e) => setModelsInput(e.target.value)}
            />
            <p className="text-xs text-muted-foreground">
              将用于文档处理和调用模型时的候选列表，可根据实际需要填写。
            </p>
          </div>

          <div className="space-y-2">
            <Label className="text-sm font-medium">关联业务</Label>
            <div className="border rounded-lg p-3 space-y-2 max-h-40 overflow-y-auto">
              {businessesLoading ? (
                <div className="space-y-2">
                  <Skeleton className="h-8 w-full" />
                  <Skeleton className="h-8 w-full" />
                  <Skeleton className="h-8 w-full" />
                </div>
              ) : businesses.length > 0 ? (
                businesses.map((business) => (
                  <label
                    key={business.id}
                    className="flex items-center gap-2 cursor-pointer hover:bg-muted p-2 rounded"
                  >
                    <input
                      type="checkbox"
                      className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-2 focus:ring-primary"
                      checked={formState.businessIds.includes(business.id)}
                      onChange={() => toggleBusiness(business.id)}
                    />
                    <span className="flex-1">
                      <span className="font-medium">{business.name}</span>
                      {business.description && (
                        <span className="text-xs text-muted-foreground ml-2">
                          - {business.description}
                        </span>
                      )}
                    </span>
                  </label>
                ))
              ) : (
                <p className="text-sm text-muted-foreground text-center py-4">
                  暂无可用业务，请先创建业务配置。
                </p>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="endpoint-description" className="text-sm font-medium">
              描述
            </Label>
            <textarea
              id="endpoint-description"
              rows={3}
              placeholder="请输入端点描述"
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary resize-none"
              value={formState.description}
              onChange={(e) =>
                setFormState((prev) => ({ ...prev, description: e.target.value }))
              }
            />
          </div>

          <div className="flex items-center gap-3">
            <input
              id="endpoint-enabled"
              type="checkbox"
              className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-2 focus:ring-primary"
              checked={formState.enabled}
              onChange={(e) =>
                setFormState((prev) => ({ ...prev, enabled: e.target.checked }))
              }
            />
            <Label htmlFor="endpoint-enabled" className="text-sm font-medium cursor-pointer">
              启用该端点
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
