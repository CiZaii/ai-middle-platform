'use client';

import { use, useState } from 'react';
import { useKnowledgeBase } from '@/lib/hooks/use-knowledge-bases';
import { useFiles, useTriggerFileProcess } from '@/lib/hooks/use-files';
import { FileCard } from '@/components/features/file-list/file-card';
import { FileDetailPanel } from '@/components/features/file-list/file-detail-panel';
import { FileUploadDialog } from '@/components/features/file-upload/file-upload-dialog';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Upload,
  FolderPlus,
  ChevronDown,
  Grid3x3,
  List,
} from 'lucide-react';
import { useRouter } from 'next/navigation';
import type { File } from '@/types/file';

export default function KnowledgeBaseDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const { data: kb, isLoading: kbLoading } = useKnowledgeBase(id);
  const { data: files, isLoading: filesLoading } = useFiles(id);
  const { mutateAsync: triggerProcess, isPending: triggerLoading } = useTriggerFileProcess();
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

  type ProcessType = 'ocr' | 'vectorization' | 'qa-pairs' | 'knowledge-graph';

  // 获取最近文件（按更新时间排序，取前4个）
  const recentFiles = files
    ?.slice()
    .sort((a, b) => new Date(b.uploadedAt).getTime() - new Date(a.uploadedAt).getTime())
    .slice(0, 4) || [];

  const handleStartProcess = async (
    fileId: string,
    processType: ProcessType
  ) => {
    const processLabels: Record<ProcessType, string> = {
      ocr: 'OCR',
      vectorization: '向量化',
      'qa-pairs': '问答对',
      'knowledge-graph': '知识图谱',
    };

    try {
      console.log(`开始${processLabels[processType]}处理:`, fileId);
      await triggerProcess({
        knowledgeBaseId: id,
        fileId,
        processType,
      });
      console.log(`${processLabels[processType]}处理已触发`);
    } catch (error) {
      const message =
        error instanceof Error ? error.message : '处理任务触发失败';
      console.error(`启动${processLabels[processType]}失败:`, message);
    }
  };

  const handleDeleteFile = (fileId: string) => {
    console.log('删除文件:', fileId);
    // TODO: 实际调用删除接口
  };

  const handleFileClick = (file: File) => {
    setSelectedFile(file);
  };

  const handleViewDetails = (fileId: string) => {
    router.push(`/knowledge/${id}/files/${fileId}`);
  };

  if (kbLoading) {
    return (
      <div className="flex-1 flex">
        <div className="flex-1">
          <Skeleton className="h-8 w-64 mb-8" />
          <Skeleton className="h-96 w-full" />
        </div>
      </div>
    );
  }

  if (!kb) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="text-center">
          <p className="text-gray-500 mb-4">知识库不存在</p>
          <Button onClick={() => router.push('/knowledge/list')}>
            返回列表
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-1 overflow-hidden">
      <div className="flex-1 overflow-y-auto">
        {/* 筛选工具栏 */}
        <div className="flex justify-between items-center mb-6">
          <div className="flex items-center space-x-4">
            <Button
              onClick={() => setUploadDialogOpen(true)}
              className="bg-blue-600 text-white px-4 py-2 rounded-lg flex items-center hover:bg-blue-700 transition-colors"
            >
              <Upload className="mr-1.5 h-4 w-4" />
              上传
            </Button>
            <Button
              variant="outline"
              className="text-gray-700 px-4 py-2 rounded-lg flex items-center border hover:bg-gray-50 transition-colors"
            >
              <FolderPlus className="mr-1.5 h-4 w-4" />
              新建文件夹
            </Button>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center">
              <span className="text-sm text-gray-500 mr-2">排序:</span>
              <button className="text-gray-800 bg-white border rounded-lg px-3 py-1.5 flex items-center text-sm">
                <span>修改时间</span>
                <ChevronDown className="ml-1 h-4 w-4" />
              </button>
            </div>

            <div className="flex border rounded-lg overflow-hidden">
              <button
                onClick={() => setViewMode('grid')}
                className={`p-2 flex items-center justify-center border-r ${
                  viewMode === 'grid' ? 'bg-white' : 'bg-gray-50'
                }`}
              >
                <Grid3x3 className={`h-5 w-5 ${viewMode === 'grid' ? 'text-blue-600' : 'text-gray-500'}`} />
              </button>
              <button
                onClick={() => setViewMode('list')}
                className={`p-2 flex items-center justify-center ${
                  viewMode === 'list' ? 'bg-white' : 'bg-gray-50'
                }`}
              >
                <List className={`h-5 w-5 ${viewMode === 'list' ? 'text-blue-600' : 'text-gray-500'}`} />
              </button>
            </div>
          </div>
        </div>

        {/* 最近使用的文件 */}
        {!filesLoading && recentFiles.length > 0 && (
          <div className="mb-8">
            <h2 className="text-lg font-semibold mb-3">最近使用的文件</h2>
            <div className="grid grid-cols-4 gap-4">
              {recentFiles.map((file) => (
                <FileCard
                  key={file.id}
                  id={file.id}
                  name={file.name}
                  size={file.size}
                  type={file.name.split('.').pop() || ''}
                  updatedAt={file.uploadedAt}
                  onClick={() => handleFileClick(file)}
                  isSelected={selectedFile?.id === file.id}
                />
              ))}
            </div>
          </div>
        )}

        {/* 所有文件 */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-lg font-semibold">所有文件</h2>
            {files && files.length > 4 && (
              <button className="text-sm text-blue-600 hover:text-blue-800 transition">
                查看全部
              </button>
            )}
          </div>

          {/* 文件网格 */}
          {filesLoading ? (
            <div className="grid grid-cols-4 gap-4">
              {Array.from({ length: 8 }).map((_, i) => (
                <Skeleton key={i} className="h-48 w-full rounded-xl" />
              ))}
            </div>
          ) : files && files.length > 0 ? (
            <div className="grid grid-cols-4 gap-4">
              {files.map((file) => (
                <FileCard
                  key={file.id}
                  id={file.id}
                  name={file.name}
                  size={file.size}
                  type={file.name.split('.').pop() || ''}
                  updatedAt={file.uploadedAt}
                  onClick={() => handleFileClick(file)}
                  isSelected={selectedFile?.id === file.id}
                />
              ))}
            </div>
          ) : (
            <div className="text-center py-12 bg-white rounded-xl border">
              <Upload className="mx-auto h-12 w-12 text-gray-400 mb-4" />
              <p className="text-gray-500 mb-4">暂无文件</p>
              <Button
                onClick={() => setUploadDialogOpen(true)}
                className="bg-blue-600 hover:bg-blue-700"
              >
                <Upload className="mr-2 h-4 w-4" />
                上传第一个文件
              </Button>
            </div>
          )}
        </div>
      </div>

      {/* 右侧详情面板 */}
      {selectedFile && (
        <FileDetailPanel
          file={selectedFile}
          onClose={() => setSelectedFile(null)}
          onDelete={handleDeleteFile}
          onStartProcess={handleStartProcess}
          onViewDetails={handleViewDetails}
        />
      )}

      {/* Upload Dialog */}
      <FileUploadDialog
        open={uploadDialogOpen}
        onOpenChange={setUploadDialogOpen}
        knowledgeBaseId={id}
      />
    </div>
  );
}
