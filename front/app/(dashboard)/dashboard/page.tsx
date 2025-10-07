'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { FileText, BookOpen, Users, TrendingUp } from 'lucide-react';

export default function DashboardPage() {
  const stats = [
    {
      title: '知识库总数',
      value: '12',
      icon: BookOpen,
      trend: '+2 本周',
      color: 'from-blue-500 to-indigo-600',
    },
    {
      title: '文件总数',
      value: '156',
      icon: FileText,
      trend: '+18 本周',
      color: 'from-purple-500 to-pink-600',
    },
    {
      title: '团队成员',
      value: '24',
      icon: Users,
      trend: '+3 本月',
      color: 'from-green-500 to-teal-600',
    },
    {
      title: '处理任务',
      value: '89',
      icon: TrendingUp,
      trend: '+12 今日',
      color: 'from-orange-500 to-red-600',
    },
  ];

  return (
    <>
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold bg-gradient-to-r from-primary-start to-primary-end bg-clip-text text-transparent">
          仪表盘
        </h1>
        <p className="text-muted-foreground mt-1">
          欢迎回来,查看您的系统概览
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <Card
              key={stat.title}
              className="overflow-hidden border-2 hover:border-primary/50 transition-all duration-300 hover:shadow-glass"
            >
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <CardTitle className="text-sm font-medium text-muted-foreground">
                    {stat.title}
                  </CardTitle>
                  <div
                    className={`h-10 w-10 rounded-lg bg-gradient-to-br ${stat.color} p-2 text-white`}
                  >
                    <Icon className="h-full w-full" />
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-3xl font-bold">{stat.value}</div>
                <p className="text-xs text-muted-foreground mt-1">
                  {stat.trend}
                </p>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="border-2">
          <CardHeader>
            <CardTitle>最近活动</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[1, 2, 3, 4].map((i) => (
                <div
                  key={i}
                  className="flex items-center gap-4 p-3 rounded-lg bg-muted/30 hover:bg-muted/50 transition-colors"
                >
                  <div className="h-10 w-10 rounded-full bg-gradient-to-br from-primary-start to-primary-end flex items-center justify-center text-white text-sm font-medium">
                    A
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-medium">上传了新文件</p>
                    <p className="text-xs text-muted-foreground">2 小时前</p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card className="border-2">
          <CardHeader>
            <CardTitle>处理进度</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[
                { name: 'OCR 处理', progress: 75, color: 'bg-blue-500' },
                { name: '向量化', progress: 60, color: 'bg-purple-500' },
                { name: '问答对生成', progress: 45, color: 'bg-green-500' },
                { name: '知识图谱', progress: 30, color: 'bg-orange-500' },
              ].map((item) => (
                <div key={item.name} className="space-y-2">
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-medium">{item.name}</span>
                    <span className="text-muted-foreground">
                      {item.progress}%
                    </span>
                  </div>
                  <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                    <div
                      className={`h-full ${item.color} transition-all duration-500`}
                      style={{ width: `${item.progress}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </>
  );
}
