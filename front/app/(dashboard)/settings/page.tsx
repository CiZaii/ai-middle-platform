'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Settings as SettingsIcon, Bell, Lock, Palette, Globe } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function SettingsPage() {
  const settingsSections = [
    {
      title: '通知设置',
      description: '管理系统通知和提醒',
      icon: Bell,
      color: 'from-blue-500 to-indigo-600',
    },
    {
      title: '安全设置',
      description: '密码、双因素认证等安全选项',
      icon: Lock,
      color: 'from-purple-500 to-pink-600',
    },
    {
      title: '外观设置',
      description: '主题、语言等界面个性化',
      icon: Palette,
      color: 'from-green-500 to-teal-600',
    },
    {
      title: '系统配置',
      description: '全局系统参数配置',
      icon: Globe,
      color: 'from-orange-500 to-red-600',
    },
  ];

  return (
    <>
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold bg-gradient-to-r from-primary-start to-primary-end bg-clip-text text-transparent">
          系统设置
        </h1>
        <p className="text-muted-foreground mt-1">
          管理系统配置和偏好设置
        </p>
      </div>

      {/* Settings Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
        {settingsSections.map((section) => {
          const Icon = section.icon;
          return (
            <Card
              key={section.title}
              className="border-2 hover:border-primary/50 transition-all duration-300 hover:shadow-glass cursor-pointer"
            >
              <CardHeader>
                <div className="flex items-center gap-4">
                  <div
                    className={`h-12 w-12 rounded-lg bg-gradient-to-br ${section.color} p-3 text-white`}
                  >
                    <Icon className="h-full w-full" />
                  </div>
                  <div>
                    <CardTitle className="text-lg">{section.title}</CardTitle>
                    <p className="text-sm text-muted-foreground mt-1">
                      {section.description}
                    </p>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <Button variant="ghost" className="w-full">
                  配置
                </Button>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Info Card */}
      <Card className="border-2">
        <CardContent className="pt-6">
          <div className="flex items-center gap-4">
            <SettingsIcon className="h-8 w-8 text-muted-foreground" />
            <div>
              <p className="font-medium">系统设置功能开发中</p>
              <p className="text-sm text-muted-foreground mt-1">
                完整的设置功能将在后续版本中提供
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </>
  );
}
