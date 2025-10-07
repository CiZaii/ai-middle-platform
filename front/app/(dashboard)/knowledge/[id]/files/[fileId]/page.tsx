'use client';

import { use, useState } from 'react';
import { useFile, useTriggerFileProcess } from '@/lib/hooks/use-files';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowLeft, FileText, Network, MessageSquare, Loader2, RefreshCw } from 'lucide-react';
import Link from 'next/link';
import { KnowledgeGraphView } from '@/components/features/knowledge-graph/knowledge-graph-view';
import { QAPairsList } from '@/components/features/qa-pairs/qa-pairs-list';
import { FileViewer } from '@/components/features/file-viewer/file-viewer';
import { OcrEditor } from '@/components/features/ocr-editor/ocr-editor';
import { ProcessingStatus } from '@/types/file';
import { api } from '@/lib/api/client';

const sanitizeOcrContent = (content?: string): string => {
  if (!content) return '';

  const prefixes = [
    '作为一个专业的OCR识别助手，我已识别图片中的所有文字内容，并严格按照您的要求进行格式化。',
    '好的，这是对图片内容的OCR识别结果，已按照您的要求格式化为Markdown。',
  ];

  let sanitized = content.trimStart();

  for (const prefix of prefixes) {
    if (sanitized.startsWith(prefix)) {
      sanitized = sanitized.slice(prefix.length).trimStart();
      break;
    }
  }

  if (sanitized.startsWith('```')) {
    const newlineIndex = sanitized.indexOf('\n');
    if (newlineIndex !== -1) {
      const fenceDescriptor = sanitized.slice(3, newlineIndex).trim().toLowerCase();
      if (!fenceDescriptor || fenceDescriptor === 'markdown' || fenceDescriptor === 'md') {
        sanitized = sanitized.slice(newlineIndex + 1).trim();
        if (sanitized.endsWith('```')) {
          sanitized = sanitized.slice(0, -3).trimEnd();
        }
      }
    }
  }

  return sanitized.trim();
};

export default function FileDetailPage({
  params,
}: {
  params: Promise<{ id: string; fileId: string }>;
}) {
  const { id, fileId } = use(params);
  const { data: file, isLoading } = useFile(fileId);
  const [activeTab, setActiveTab] = useState('ocr');
  const { mutateAsync: triggerProcess } = useTriggerFileProcess();
  const [triggering, setTriggering] = useState<'ocr' | 'qa-pairs' | 'knowledge-graph' | null>(null);

  const handleTabChange = (value: string) => {
    setActiveTab(value);
  };

  const handleProcessTrigger = async (processType: 'ocr' | 'qa-pairs' | 'knowledge-graph') => {
    if (!file) return;
    if (triggering) return;
    if (processType === 'ocr' && file.statuses.ocr === ProcessingStatus.PROCESSING) {
      return;
    }
    if (processType === 'qa-pairs' && file.statuses.qaPairs === ProcessingStatus.PROCESSING) {
      return;
    }
    if (processType === 'knowledge-graph' && file.statuses.knowledgeGraph === ProcessingStatus.PROCESSING) {
      return;
    }

    try {
      setTriggering(processType);
      await triggerProcess({
        knowledgeBaseId: file.knowledgeBaseId || id,
        fileId: file.id,
        processType,
      });
    } catch (error) {
      console.error('处理触发失败', error);
    } finally {
      setTriggering(null);
    }
  };

  if (isLoading) {
    return (
      <>
        <Skeleton className="h-8 w-64 mb-8" />
        <Skeleton className="h-screen w-full" />
      </>
    );
  }

  if (!file) {
    return (
      <>
        <p className="text-muted-foreground">文件不存在</p>
        <Button className="mt-4" asChild>
          <Link href="/knowledge/list">返回列表</Link>
        </Button>
      </>
    );
  }

  const displayedOcrContent = sanitizeOcrContent(file.ocrContent);

  const handleSaveOcrContent = async (newContent: string) => {
    try {
      await api.files.updateOcrContent(fileId, newContent);
      // Refresh file data
      window.location.reload();
    } catch (error) {
      console.error('保存OCR内容失败', error);
      throw error;
    }
  };

  return (
    <>
      <Button variant="ghost" size="sm" className="mb-4" asChild>
        <Link href={`/knowledge/${id}`}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          返回知识库
        </Link>
      </Button>

      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold bg-gradient-to-r from-primary-start to-primary-end bg-clip-text text-transparent">
            {file.name}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            上传于 {new Date(file.uploadedAt).toLocaleDateString()} · {file.type.toUpperCase()} · {(file.size / 1024 / 1024).toFixed(2)} MB
          </p>
        </div>
      </div>

      {/* Two Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 h-[calc(100vh-200px)]">
        {/* Left: File Viewer */}
        <div className="h-full">
          <FileViewer
            fileUrl={file.url}
            fileName={file.name}
            mimeType={file.mimeType}
            fileType={file.type}
          />
        </div>

        {/* Right: Tabs */}
        <div className="h-full flex flex-col overflow-hidden">
          <Tabs value={activeTab} onValueChange={handleTabChange} className="flex flex-col h-full">
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="ocr" className="gap-2">
                {file.statuses.ocr === ProcessingStatus.PROCESSING || triggering === 'ocr' ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <FileText className="h-4 w-4" />
                )}
                OCR内容
              </TabsTrigger>
              <TabsTrigger value="graph" className="gap-2">
                {file.statuses.knowledgeGraph === ProcessingStatus.PROCESSING || triggering === 'knowledge-graph' ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Network className="h-4 w-4" />
                )}
                知识图谱
              </TabsTrigger>
              <TabsTrigger value="qa" className="gap-2">
                {file.statuses.qaPairs === ProcessingStatus.PROCESSING || triggering === 'qa-pairs' ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <MessageSquare className="h-4 w-4" />
                )}
                问答对
              </TabsTrigger>
            </TabsList>

            <div className="flex-1 overflow-auto mt-4">
              <TabsContent value="ocr" className="mt-0 h-full">
                <Card className="h-full flex flex-col">
                  <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <CardTitle className="flex items-center gap-2 text-base">
                      OCR识别内容
                      {file.statuses.ocr === ProcessingStatus.PROCESSING && (
                        <span className="text-sm text-muted-foreground flex items-center gap-1">
                          <Loader2 className="h-3 w-3 animate-spin" />
                          处理中...
                        </span>
                      )}
                      {file.statuses.ocr === ProcessingStatus.PENDING && (
                        <span className="text-sm text-yellow-600">待处理</span>
                      )}
                      {file.statuses.ocr === ProcessingStatus.FAILED && (
                        <span className="text-sm text-red-600">处理失败</span>
                      )}
                    </CardTitle>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleProcessTrigger('ocr')}
                      disabled={triggering === 'ocr' || file.statuses.ocr === ProcessingStatus.PROCESSING}
                    >
                      {triggering === 'ocr' || file.statuses.ocr === ProcessingStatus.PROCESSING ? (
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      ) : (
                        <RefreshCw className="mr-2 h-4 w-4" />
                      )}
                      重新OCR
                    </Button>
                  </CardHeader>
                  <CardContent className="flex-1 overflow-auto">
                    {file.statuses.ocr === ProcessingStatus.COMPLETED ? (
                      <OcrEditor
                        content={displayedOcrContent}
                        onSave={handleSaveOcrContent}
                      />
                    ) : file.statuses.ocr === ProcessingStatus.PROCESSING ? (
                      <div className="flex items-center justify-center py-12">
                        <div className="text-center">
                          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4 text-primary" />
                          <p className="text-muted-foreground">正在进行OCR识别...</p>
                        </div>
                      </div>
                    ) : file.statuses.ocr === ProcessingStatus.FAILED ? (
                      <div className="text-center py-12">
                        <p className="text-red-600">OCR识别失败</p>
                        {file.errorMessage && (
                          <p className="text-sm text-muted-foreground mt-2">{file.errorMessage}</p>
                        )}
                      </div>
                    ) : (
                      <div className="text-center py-12 text-muted-foreground">
                        当前尚未进行OCR识别
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="graph" className="mt-0 h-full">
                <Card className="h-full flex flex-col">
                  <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <CardTitle className="flex items-center gap-2 text-base">
                      知识图谱
                      {file.statuses.knowledgeGraph === ProcessingStatus.PROCESSING && (
                        <span className="text-sm text-muted-foreground flex items-center gap-1">
                          <Loader2 className="h-3 w-3 animate-spin" />
                          处理中...
                        </span>
                      )}
                      {file.statuses.knowledgeGraph === ProcessingStatus.PENDING && (
                        <span className="text-sm text-yellow-600">待处理</span>
                      )}
                      {file.statuses.knowledgeGraph === ProcessingStatus.FAILED && (
                        <span className="text-sm text-red-600">处理失败</span>
                      )}
                    </CardTitle>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleProcessTrigger('knowledge-graph')}
                      disabled={triggering === 'knowledge-graph' || file.statuses.knowledgeGraph === ProcessingStatus.PROCESSING}
                    >
                      {triggering === 'knowledge-graph' || file.statuses.knowledgeGraph === ProcessingStatus.PROCESSING ? (
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      ) : (
                        <RefreshCw className="mr-2 h-4 w-4" />
                      )}
                      重新构建
                    </Button>
                  </CardHeader>
                  <CardContent className="flex-1 overflow-auto">
                    {file.statuses.knowledgeGraph === ProcessingStatus.COMPLETED ? (
                      <KnowledgeGraphView graphData={file.knowledgeGraph} />
                    ) : file.statuses.knowledgeGraph === ProcessingStatus.PROCESSING ? (
                      <div className="flex items-center justify-center py-12">
                        <div className="text-center">
                          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4 text-primary" />
                          <p className="text-muted-foreground">正在构建知识图谱...</p>
                        </div>
                      </div>
                    ) : file.statuses.knowledgeGraph === ProcessingStatus.FAILED ? (
                      <div className="text-center py-12">
                        <p className="text-red-600">知识图谱构建失败</p>
                        {file.errorMessage && (
                          <p className="text-sm text-muted-foreground mt-2">{file.errorMessage}</p>
                        )}
                      </div>
                    ) : (
                      <div className="text-center py-12 text-muted-foreground">
                        当前尚未构建知识图谱
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="qa" className="mt-0 h-full">
                <Card className="h-full flex flex-col">
                  <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <CardTitle className="flex items-center gap-2 text-base">
                      问答对列表
                      {file.statuses.qaPairs === ProcessingStatus.PROCESSING && (
                        <span className="text-sm text-muted-foreground flex items-center gap-1">
                          <Loader2 className="h-3 w-3 animate-spin" />
                          处理中...
                        </span>
                      )}
                      {file.statuses.qaPairs === ProcessingStatus.PENDING && (
                        <span className="text-sm text-yellow-600">待处理</span>
                      )}
                      {file.statuses.qaPairs === ProcessingStatus.FAILED && (
                        <span className="text-sm text-red-600">处理失败</span>
                      )}
                    </CardTitle>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleProcessTrigger('qa-pairs')}
                      disabled={triggering === 'qa-pairs' || file.statuses.qaPairs === ProcessingStatus.PROCESSING}
                    >
                      {triggering === 'qa-pairs' || file.statuses.qaPairs === ProcessingStatus.PROCESSING ? (
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      ) : (
                        <RefreshCw className="mr-2 h-4 w-4" />
                      )}
                      重新生成
                    </Button>
                  </CardHeader>
                  <CardContent className="flex-1 overflow-auto">
                    {file.statuses.qaPairs === ProcessingStatus.COMPLETED ? (
                      <QAPairsList qaPairs={file.qaPairs} />
                    ) : file.statuses.qaPairs === ProcessingStatus.PROCESSING ? (
                      <div className="flex items-center justify-center py-12">
                        <div className="text-center">
                          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4 text-primary" />
                          <p className="text-muted-foreground">正在生成问答对...</p>
                        </div>
                      </div>
                    ) : file.statuses.qaPairs === ProcessingStatus.FAILED ? (
                      <div className="text-center py-12">
                        <p className="text-red-600">问答对生成失败</p>
                        {file.errorMessage && (
                          <p className="text-sm text-muted-foreground mt-2">{file.errorMessage}</p>
                        )}
                      </div>
                    ) : (
                      <div className="text-center py-12 text-muted-foreground">
                        当前尚未生成问答对
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>
            </div>
          </Tabs>
        </div>
      </div>
    </>
  );
}
