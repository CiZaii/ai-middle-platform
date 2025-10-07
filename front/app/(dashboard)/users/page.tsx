'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Users as UsersIcon, UserPlus, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function UsersPage() {
  return (
    <>
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold bg-gradient-to-r from-primary-start to-primary-end bg-clip-text text-transparent">
            用户管理
          </h1>
          <p className="text-muted-foreground mt-1">
            管理团队成员和权限
          </p>
        </div>
        <Button>
          <UserPlus className="mr-2 h-4 w-4" />
          邀请成员
        </Button>
      </div>

      {/* Search */}
      <div className="mb-8">
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="搜索用户..."
            className="w-full pl-10 pr-4 py-2 rounded-lg border bg-background focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </div>
      </div>

      {/* Empty State */}
      <Card className="border-2 border-dashed">
        <CardContent className="flex flex-col items-center justify-center py-16">
          <UsersIcon className="h-16 w-16 text-muted-foreground mb-4" />
          <h3 className="text-lg font-semibold mb-2">暂无用户数据</h3>
          <p className="text-muted-foreground text-sm mb-6">
            用户管理功能正在开发中
          </p>
          <Button>
            <UserPlus className="mr-2 h-4 w-4" />
            邀请第一个成员
          </Button>
        </CardContent>
      </Card>
    </>
  );
}
