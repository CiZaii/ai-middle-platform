'use client';

import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Upload, X, FileText } from 'lucide-react';
import { useUploadFile } from '@/lib/hooks/use-files';

interface FileUploadDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  knowledgeBaseId: string;
}

export function FileUploadDialog({
  open,
  onOpenChange,
  knowledgeBaseId,
}: FileUploadDialogProps) {
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const uploadFile = useUploadFile();

  const handleDialogChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      setSelectedFiles([]);
      setError(null);
      setIsDragging(false);
      setIsUploading(false);
    }
    onOpenChange(nextOpen);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const files = Array.from(e.dataTransfer.files);
    setSelectedFiles((prev) => [...prev, ...files]);
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const files = Array.from(e.target.files);
      setSelectedFiles((prev) => [...prev, ...files]);
    }
  };

  const removeFile = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleUpload = async () => {
    if (!knowledgeBaseId) {
      setError('缺少知识库标识，无法上传文件');
      return;
    }

    setError(null);
    setIsUploading(true);

    try {
      for (const file of selectedFiles) {
        await uploadFile.mutateAsync({ knowledgeBaseId, file });
      }
      handleDialogChange(false);
    } catch (uploadError) {
      const message =
        uploadError instanceof Error
          ? uploadError.message
          : '文件上传失败，请稍后重试';
      setError(message);
    } finally {
      setIsUploading(false);
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  return (
    <Dialog open={open} onOpenChange={handleDialogChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>上传文件</DialogTitle>
          <DialogDescription>
            支持 PDF, DOCX, TXT, MD 等格式,单个文件不超过 50MB
          </DialogDescription>
        </DialogHeader>

        {/* 拖拽上传区域 */}
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          className={`
            border-2 border-dashed rounded-lg p-8 text-center transition-all
            ${
              isDragging
                ? 'border-primary bg-primary/5'
                : 'border-muted-foreground/25 hover:border-primary/50'
            }
          `}
        >
          <Upload
            className={`mx-auto h-12 w-12 mb-4 ${
              isDragging ? 'text-primary' : 'text-muted-foreground'
            }`}
          />
          <p className="text-sm font-medium mb-2">
            拖拽文件到此处,或点击选择文件
          </p>
          <p className="text-xs text-muted-foreground mb-4">
            支持多个文件同时上传
          </p>
          <input
            type="file"
            multiple
            onChange={handleFileSelect}
            className="hidden"
            id="file-input"
            accept=".pdf,.doc,.docx,.txt,.md"
          />
          <Button asChild variant="outline">
            <label htmlFor="file-input" className="cursor-pointer">
              选择文件
            </label>
          </Button>
        </div>

        {/* 已选文件列表 */}
        {selectedFiles.length > 0 && (
          <div className="mt-4">
            <h3 className="text-sm font-medium mb-3">
              已选择 {selectedFiles.length} 个文件
            </h3>
            <div className="space-y-2 max-h-60 overflow-y-auto">
              {selectedFiles.map((file, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between p-3 bg-muted/30 rounded-lg"
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <FileText className="h-5 w-5 text-primary flex-shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate">
                        {file.name}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {formatFileSize(file.size)}
                      </p>
                    </div>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeFile(index)}
                    className="flex-shrink-0"
                  >
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              ))}
            </div>
          </div>
        )}

        {error && <p className="text-sm text-red-500">{error}</p>}

        {/* 操作按钮 */}
        <div className="flex justify-end gap-3 mt-6">
          <Button
            variant="outline"
            onClick={() => handleDialogChange(false)}
            disabled={isUploading}
          >
            取消
          </Button>
          <Button
            onClick={handleUpload}
            disabled={selectedFiles.length === 0 || isUploading}
          >
            {isUploading ? '上传中...' : '上传'}
            {selectedFiles.length > 0 && !isUploading && ` (${selectedFiles.length})`}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
