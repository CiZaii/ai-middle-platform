'use client';

import { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils/cn';
import {
  LayoutDashboard,
  BookOpen,
  Users,
  Settings,
  Cpu,
  ChevronDown,
  Server,
  Key,
  Briefcase,
  FileText,
} from 'lucide-react';

interface MenuItem {
  title: string;
  icon: any;
  href: string;
  color?: string;
  children?: MenuItem[];
}

const menuItems: MenuItem[] = [
  {
    title: '仪表盘',
    icon: LayoutDashboard,
    href: '/dashboard',
    color: 'text-blue-600',
  },
  {
    title: '知识库',
    icon: BookOpen,
    href: '/knowledge',
    color: 'text-green-600',
  },
  {
    title: '模型配置',
    icon: Cpu,
    href: '/model-config',
    color: 'text-purple-600',
    children: [
      {
        title: '端点配置',
        icon: Server,
        href: '/model-config/endpoints',
        color: 'text-blue-500',
      },
      {
        title: 'API Key',
        icon: Key,
        href: '/model-config/api-keys',
        color: 'text-yellow-500',
      },
      {
        title: '业务配置',
        icon: Briefcase,
        href: '/model-config/business',
        color: 'text-green-500',
      },
      {
        title: 'Prompt 管理',
        icon: FileText,
        href: '/prompts',
        color: 'text-pink-600',
      },
    ],
  },
  {
    title: '用户管理',
    icon: Users,
    href: '/users',
    color: 'text-orange-600',
  },
  {
    title: '系统设置',
    icon: Settings,
    href: '/settings',
    color: 'text-gray-600',
  },
];

export function Sidebar() {
  const pathname = usePathname();
  const [expandedMenus, setExpandedMenus] = useState<Set<string>>(new Set(['/model-config']));

  const toggleMenu = (href: string) => {
    const newExpanded = new Set(expandedMenus);
    if (newExpanded.has(href)) {
      newExpanded.delete(href);
    } else {
      newExpanded.add(href);
    }
    setExpandedMenus(newExpanded);
  };

  const isActive = (href: string) => {
    if (href === '/dashboard') {
      return pathname === href;
    }
    if (href === '/knowledge') {
      return pathname === href || pathname.startsWith('/knowledge');
    }
    return pathname.startsWith(href);
  };

  return (
    <aside className="w-64 bg-white border-r border-gray-200 h-[calc(100vh-4rem)] flex flex-col">
      {/* 导航菜单 */}
      <div className="p-4 flex-1 overflow-y-auto">
        <div className="mb-2">
          <h2 className="text-xs uppercase text-gray-500 font-semibold mb-3 px-2">
            主要功能
          </h2>
          <ul>
            {menuItems.map((item) => {
              const Icon = item.icon;
              const hasChildren = item.children && item.children.length > 0;
              const childActive = hasChildren ? item.children.some((child) => isActive(child.href)) : false;
              const active = isActive(item.href) || childActive;
              const isExpanded = expandedMenus.has(item.href);

              return (
                <li key={item.href} className="mb-1">
                  {/* 父菜单项 */}
                  {hasChildren ? (
                    <button
                      onClick={() => toggleMenu(item.href)}
                      className={cn(
                        'w-full sidebar-item rounded-lg px-3 py-2.5 transition-all flex items-center justify-between',
                        active && 'bg-blue-50 border-l-3 border-blue-600'
                      )}
                    >
                      <div className="flex items-center">
                        <Icon className={cn('mr-3 h-5 w-5', active ? 'text-blue-600' : item.color)} />
                        <span className={cn('font-medium', active && 'text-blue-600')}>
                          {item.title}
                        </span>
                      </div>
                      <ChevronDown
                        className={cn(
                          'h-4 w-4 text-gray-500 transition-transform',
                          isExpanded && 'rotate-180'
                        )}
                      />
                    </button>
                  ) : (
                    <Link
                      href={item.href}
                      className={cn(
                        'sidebar-item rounded-lg px-3 py-2.5 transition-all flex items-center',
                        active && 'bg-blue-50 border-l-3 border-blue-600'
                      )}
                    >
                      <Icon className={cn('mr-3 h-5 w-5', active ? 'text-blue-600' : item.color)} />
                      <span className={cn('font-medium', active && 'text-blue-600')}>
                        {item.title}
                      </span>
                    </Link>
                  )}

                  {/* 子菜单项 */}
                  {hasChildren && isExpanded && (
                    <ul className="ml-4 mt-1 space-y-1">
                      {item.children!.map((child) => {
                        const ChildIcon = child.icon;
                        const childActive = isActive(child.href);

                        return (
                          <li key={child.href}>
                            <Link
                              href={child.href}
                              className={cn(
                                'sidebar-item rounded-lg px-3 py-2 transition-all flex items-center text-sm',
                                childActive && 'bg-blue-50 text-blue-600'
                              )}
                            >
                              <ChildIcon className={cn('mr-3 h-4 w-4', childActive ? 'text-blue-600' : child.color)} />
                              <span>{child.title}</span>
                            </Link>
                          </li>
                        );
                      })}
                    </ul>
                  )}
                </li>
              );
            })}
          </ul>
        </div>
      </div>

      {/* 存储使用情况 */}
      <div className="mt-auto p-4 border-t border-gray-200">
        <div className="mb-2">
          <div className="flex justify-between items-center mb-1">
            <span className="text-sm text-gray-600">存储空间</span>
            <span className="text-sm font-medium">12.5GB / 15GB</span>
          </div>
          <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
            <div className="h-full bg-blue-500 rounded-full" style={{ width: '83%' }}></div>
          </div>
        </div>
        <button className="text-sm text-blue-600 hover:text-blue-800 transition">
          升级存储空间
        </button>
      </div>
    </aside>
  );
}
