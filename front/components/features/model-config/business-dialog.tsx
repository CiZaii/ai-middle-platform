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
import { Business } from '@/types/model-config';

export interface BusinessFormValues {
  name: string;
  code: string;
  description?: string;
  enabled: boolean;
}

interface BusinessDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  business?: Business | null;
  onSave?: (values: BusinessFormValues) => Promise<void> | void;
  saving?: boolean;
}

const DEFAULT_FORM: BusinessFormValues = {
  name: '',
  code: '',
  description: '',
  enabled: true,
};

export function BusinessDialog({
  open,
  onOpenChange,
  business,
  onSave,
  saving = false,
}: BusinessDialogProps) {
  const [formData, setFormData] = useState<BusinessFormValues>(DEFAULT_FORM);

  useEffect(() => {
    if (!open) {
      return;
    }
    setFormData({
      name: business?.name ?? '',
      code: business?.code ?? '',
      description: business?.description ?? '',
      enabled: business?.enabled ?? true,
    });
  }, [business, open]);

  const handleSave = async () => {
    const payload: BusinessFormValues = {
      name: formData.name.trim(),
      code: formData.code.trim(),
      description: formData.description?.trim() || undefined,
      enabled: formData.enabled,
    };

    await Promise.resolve(onSave?.(payload));
    onOpenChange(false);
  };

  const isDisabled = saving || !formData.name.trim() || !formData.code.trim();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{business ? '编辑业务配置' : '新增业务配置'}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="business-name" className="text-sm font-medium">
              业务名称 <span className="text-red-500">*</span>
            </Label>
            <input
              id="business-name"
              type="text"
              placeholder="请输入业务名称"
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              value={formData.name}
              onChange={(e) => setFormData((prev) => ({ ...prev, name: e.target.value }))}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="business-code" className="text-sm font-medium">
              业务CODE <span className="text-red-500">*</span>
            </Label>
            <input
              id="business-code"
              type="text"
              placeholder="请输入唯一业务CODE (如: CUSTOMER_SERVICE)"
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary font-mono"
              value={formData.code}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, code: e.target.value.toUpperCase() }))
              }
            />
            <p className="text-xs text-muted-foreground">
              建议使用大写字母和下划线, 如: CUSTOMER_SERVICE
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="business-description" className="text-sm font-medium">
              业务描述
            </Label>
            <textarea
              id="business-description"
              placeholder="请输入业务描述"
              rows={4}
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary resize-none"
              value={formData.description}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, description: e.target.value }))
              }
            />
          </div>

          <div className="flex items-center gap-3">
            <input
              id="business-enabled"
              type="checkbox"
              className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-2 focus:ring-primary"
              checked={formData.enabled}
              onChange={(e) => setFormData((prev) => ({ ...prev, enabled: e.target.checked }))}
            />
            <Label htmlFor="business-enabled" className="text-sm font-medium cursor-pointer">
              启用该业务
            </Label>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={saving}>
            取消
          </Button>
          <Button onClick={handleSave} disabled={isDisabled}>
            {saving ? '保存中...' : '保存'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
