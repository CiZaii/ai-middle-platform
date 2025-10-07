'use client';

import { Card } from '@/components/ui/card';
import { MessageSquare, FileText } from 'lucide-react';

interface QAPair {
  id: string;
  question: string;
  answer: string;
  sourceText: string;
}

interface QAPairsListProps {
  qaPairs?: QAPair[];
}

export function QAPairsList({ qaPairs }: QAPairsListProps) {
  if (!qaPairs || qaPairs.length === 0) {
    return (
      <div className="text-center py-12">
        <MessageSquare className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
        <p className="text-muted-foreground">暂无问答对数据</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {qaPairs.map((pair) => (
        <Card key={pair.id} className="p-6 hover:shadow-md transition-shadow">
          <div className="space-y-4">
            {/* 问题 */}
            <div>
              <div className="flex items-center gap-2 mb-2">
                <div className="h-6 w-6 rounded-full bg-blue-100 dark:bg-blue-900 flex items-center justify-center">
                  <span className="text-xs font-bold text-blue-600 dark:text-blue-400">
                    Q
                  </span>
                </div>
                <h3 className="font-semibold text-foreground">问题</h3>
              </div>
              <p className="ml-8 text-foreground">{pair.question}</p>
            </div>

            {/* 答案 */}
            <div>
              <div className="flex items-center gap-2 mb-2">
                <div className="h-6 w-6 rounded-full bg-green-100 dark:bg-green-900 flex items-center justify-center">
                  <span className="text-xs font-bold text-green-600 dark:text-green-400">
                    A
                  </span>
                </div>
                <h3 className="font-semibold text-foreground">答案</h3>
              </div>
              <p className="ml-8 text-foreground">{pair.answer}</p>
            </div>

            {/* 原文引用 */}
            <div className="border-t pt-4">
              <div className="flex items-center gap-2 mb-2">
                <FileText className="h-4 w-4 text-muted-foreground" />
                <h4 className="text-sm font-medium text-muted-foreground">
                  原文引用
                </h4>
              </div>
              <p className="ml-6 text-sm text-muted-foreground italic bg-muted/30 p-3 rounded">
                {pair.sourceText}
              </p>
            </div>
          </div>
        </Card>
      ))}
    </div>
  );
}
