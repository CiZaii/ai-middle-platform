'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Bell, Search, FolderOpen, ChevronDown } from 'lucide-react';
import Link from 'next/link';
import { Breadcrumb } from './breadcrumb';

export function Header() {
  const [searchQuery, setSearchQuery] = useState('');

  return (
    <header className="sticky top-0 z-50 w-full border-b bg-white shadow-sm">
      <div className="flex h-16 items-center justify-between px-6">
        {/* Logo和面包屑 */}
        <div className="flex items-center">
          <Link href="/dashboard" className="flex items-center gap-2 mr-8">
            <div className="h-8 w-8 rounded-lg bg-gradient-to-br from-blue-500 to-purple-600" />
            <h1 className="text-xl font-semibold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
              AI中台管理系统
            </h1>
          </Link>
          
          {/* 面包屑 */}
          <Breadcrumb />
        </div>

        {/* 搜索框和用户操作区 */}
        <div className="flex items-center gap-4">
          {/* 搜索框 */}
          <div className="relative">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-gray-500" />
            <Input
              type="text"
              placeholder="搜索文件..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10 pr-4 py-2 w-64 bg-gray-100 rounded-full border-0 focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:bg-white transition-all"
            />
          </div>

          {/* 通知 */}
          <Button
            variant="ghost"
            size="icon"
            className="h-9 w-9 rounded-full bg-gray-100 hover:bg-gray-200 transition-colors"
          >
            <Bell className="h-5 w-5 text-gray-600" />
          </Button>

          {/* 用户信息 */}
          <div className="flex items-center gap-2 cursor-pointer hover:opacity-80 transition-opacity">
            <div className="w-9 h-9 rounded-full bg-blue-500 flex items-center justify-center text-white font-medium">
              JD
            </div>
            <span className="text-sm font-medium">John Doe</span>
            <ChevronDown className="h-4 w-4 text-gray-500" />
          </div>
        </div>
      </div>
    </header>
  );
}
