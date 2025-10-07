'use client';

import { useMemo, useState } from 'react';
import { format } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import { Briefcase, Edit, Plus, Power, PowerOff, Search, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  useBusinesses,
  useCreateBusiness,
  useDeleteBusiness,
  useUpdateBusiness,
} from '@/lib/hooks/use-model-config';
import { Business } from '@/types/model-config';
import { BusinessDialog, BusinessFormValues } from '@/components/features/model-config/business-dialog';

export default function BusinessConfigPage() {
  const { data: businesses = [], isLoading } = useBusinesses();
  const createBusinessMutation = useCreateBusiness();
  const [editingBusiness, setEditingBusiness] = useState<Business | null>(null);
  const updateBusinessMutation = useUpdateBusiness(editingBusiness?.id ?? '');
  const deleteBusinessMutation = useDeleteBusiness();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const filteredBusinesses = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase();
    if (!keyword) return businesses;
    return businesses.filter((biz) => biz.name.toLowerCase().includes(keyword));
  }, [businesses, searchTerm]);

  const enabledCount = businesses.filter((biz) => biz.enabled).length;
  const disabledCount = businesses.filter((biz) => !biz.enabled).length;

  const isSaving = createBusinessMutation.isPending || updateBusinessMutation.isPending;

  const handleAdd = () => {
    setEditingBusiness(null);
    setDialogOpen(true);
  };

  const handleEdit = (business: Business) => {
    setEditingBusiness(business);
    setDialogOpen(true);
  };

  const handleSave = async (values: BusinessFormValues) => {
    try {
      const payload: Record<string, unknown> = { ...values };
      if (editingBusiness) {
        await updateBusinessMutation.mutateAsync(payload);
      } else {
        await createBusinessMutation.mutateAsync(payload);
      }
    } catch (error) {
      console.error('保存业务配置失败', error);
      throw error;
    }
  };

  const handleDelete = async (business: Business) => {
    if (!window.confirm(`确定删除业务「${business.name}」吗？`)) {
      return;
    }
    setDeletingId(business.id);
    try {
      await deleteBusinessMutation.mutateAsync(business.id);
    } catch (error) {
      console.error('删除业务失败', error);
    } finally {
      setDeletingId(null);
    }
  };

  const handleDialogOpenChange = (open: boolean) => {
    setDialogOpen(open);
    if (!open) {
      setEditingBusiness(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold bg-gradient-to-r from-primary-start to-primary-end bg-clip-text text-transparent">
            业务配置管理
          </h1>
          <p className="text-muted-foreground mt-1">
            管理系统中的业务类型, 用于组织和分类 API 调用
          </p>
        </div>
        <Button className="gap-2" onClick={handleAdd}>
          <Plus className="h-4 w-4" />
          新增业务
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">总业务数</p>
              <p className="text-2xl font-bold mt-1">{businesses.length}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center">
              <Briefcase className="h-6 w-6 text-white" />
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
              <Power className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">已停用</p>
              <p className="text-2xl font-bold mt-1 text-gray-600">{disabledCount}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-gray-500 to-gray-600 flex items-center justify-center">
              <PowerOff className="h-6 w-6 text-white" />
            </div>
          </div>
        </Card>
      </div>

      <Card className="p-4 mb-2">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="搜索业务名称..."
            className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {isLoading ? (
          Array.from({ length: 6 }).map((_, idx) => (
            <Card key={idx} className="p-6">
              <Skeleton className="h-5 w-1/2 mb-3" />
              <Skeleton className="h-4 w-1/3 mb-2" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-3/4 mt-4" />
            </Card>
          ))
        ) : filteredBusinesses.length > 0 ? (
          filteredBusinesses.map((business) => (
            <Card
              key={business.id}
              className="p-6 hover:shadow-lg transition-shadow flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-semibold text-lg break-all">{business.name}</h3>
                      {business.enabled ? (
                        <span className="px-2 py-0.5 text-xs font-medium bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300 rounded-full">
                          启用
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 text-xs font-medium bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300 rounded-full">
                          停用
                        </span>
                      )}
                    </div>
                    <code className="text-xs font-mono bg-muted px-2 py-1 rounded break-all">
                      {business.code}
                    </code>
                    <p className="text-sm text-muted-foreground line-clamp-2 mt-2 break-words">
                      {business.description || '暂无描述'}
                    </p>
                  </div>
                </div>

                <div className="space-y-2 pt-4 border-t">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">创建时间</span>
                    <span className="font-medium">
                      {format(new Date(business.createdAt), 'PPP', { locale: zhCN })}
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">创建人</span>
                    <span className="font-medium">{business.createdBy?.name ?? '未知'}</span>
                  </div>
                </div>
              </div>

              <div className="flex gap-2 mt-4">
                <Button
                  variant="outline"
                  size="sm"
                  className="flex-1 gap-2"
                  onClick={() => handleEdit(business)}
                >
                  <Edit className="h-3 w-3" />
                  编辑
                </Button>
                <Button
                  variant="destructive"
                  size="sm"
                  className="flex-1 gap-2"
                  onClick={() => handleDelete(business)}
                  disabled={deletingId === business.id || deleteBusinessMutation.isPending}
                >
                  <Trash2 className="h-3 w-3" />
                  删除
                </Button>
              </div>
            </Card>
          ))
        ) : (
          <div className="col-span-full text-center py-12">
            <Briefcase className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">未找到匹配的业务</p>
          </div>
        )}
      </div>

      <BusinessDialog
        open={dialogOpen}
        onOpenChange={handleDialogOpenChange}
        business={editingBusiness}
        onSave={handleSave}
        saving={isSaving}
      />
    </div>
  );
}
