'use client';

import { KnowledgeBase } from '@/types/knowledge-base';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { FileText, Users } from 'lucide-react';
import { cn } from '@/lib/utils/cn';
import { useRouter } from 'next/navigation';

interface KBCardProps {
  knowledgeBase: KnowledgeBase;
}

export function KBCard({ knowledgeBase }: KBCardProps) {
  const router = useRouter();

  return (
    <Card
      className={cn(
        'group cursor-pointer transition-all duration-300',
        'hover:shadow-glass hover:scale-[1.02]',
        'border-2 hover:border-primary/50'
      )}
      onClick={() => router.push(`/knowledge/${knowledgeBase.id}`)}
    >
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="h-12 w-12 rounded-lg bg-gradient-to-br from-primary-start to-primary-end p-3 text-white">
            <FileText className="h-full w-full" />
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              // TODO: 打开设置菜单
            }}
          >
            •••
          </Button>
        </div>
        <CardTitle className="mt-4 line-clamp-1">{knowledgeBase.name}</CardTitle>
        <CardDescription className="line-clamp-2">
          {knowledgeBase.description || '暂无描述'}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <div className="flex items-center gap-1">
            <FileText className="h-4 w-4" />
            <span>{knowledgeBase.fileCount} 个文件</span>
          </div>
          <div className="flex items-center gap-1">
            <Users className="h-4 w-4" />
            <span>{knowledgeBase.members.length + 1} 成员</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
