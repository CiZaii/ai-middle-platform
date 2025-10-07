'use client';

import { useState } from 'react';
import { useKnowledgeBases } from '@/lib/hooks/use-knowledge-bases';
import { KBCard } from '@/components/features/knowledge-base/kb-card';
import { KBCreateDialog } from '@/components/features/knowledge-base/kb-create-dialog';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { Plus, Search } from 'lucide-react';

export default function KnowledgeBasesPage() {
  const { data: knowledgeBases, isLoading } = useKnowledgeBases();
  const [createDialogOpen, setCreateDialogOpen] = useState(false);

  return (
    <>
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold bg-gradient-to-r from-primary-start to-primary-end bg-clip-text text-transparent">
            知识库
          </h1>
          <p className="text-muted-foreground mt-1">
            管理您的知识库和文档
          </p>
        </div>
        <Button onClick={() => setCreateDialogOpen(true)}>
          <Plus className="mr-2 h-4 w-4" />
          创建知识库
        </Button>
      </div>

      {/* Search */}
      <div className="mb-8">
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="搜索知识库..."
            className="w-full pl-10 pr-4 py-2 rounded-lg border bg-background focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </div>
      </div>

      {/* Knowledge Bases Grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="space-y-3">
              <Skeleton className="h-48 w-full rounded-xl" />
            </div>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {knowledgeBases?.map((kb) => (
            <KBCard key={kb.id} knowledgeBase={kb} />
          ))}
        </div>
      )}

      {!isLoading && knowledgeBases?.length === 0 && (
        <div className="text-center py-12">
          <p className="text-muted-foreground">暂无知识库</p>
          <Button className="mt-4" onClick={() => setCreateDialogOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            创建第一个知识库
          </Button>
        </div>
      )}

      <KBCreateDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
      />
    </>
  );
}
