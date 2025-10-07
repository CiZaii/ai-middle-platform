'use client';

import { useState } from 'react';
import { X, Download, Share2, FileText, Play, Eye, Trash2, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils/cn';
import type { File } from '@/types/file';

type ProcessType = 'ocr' | 'vectorization' | 'qa-pairs' | 'knowledge-graph';

interface FileDetailPanelProps {
  file: File | null;
  onClose: () => void;
  onDelete?: (fileId: string) => void;
  onDownload?: (fileId: string) => void;
  onShare?: (fileId: string) => void;
  onStartProcess?: (fileId: string, processType: ProcessType) => void | Promise<void>;
  onViewDetails?: (fileId: string) => void;
}

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
};

const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
};

const getFileIcon = (type: string) => {
  return FileText;
};

const getStatusColor = (status: string) => {
  switch (status) {
    case 'completed':
      return 'bg-green-100 text-green-700';
    case 'processing':
      return 'bg-blue-100 text-blue-700';
    case 'failed':
      return 'bg-red-100 text-red-700';
    case 'pending':
      return 'bg-yellow-100 text-yellow-700';
    default:
      return 'bg-gray-100 text-gray-700';
  }
};

const getStatusText = (status: string) => {
  switch (status) {
    case 'completed':
      return '已完成';
    case 'processing':
      return '处理中';
    case 'failed':
      return '失败';
    case 'pending':
      return '待处理';
    default:
      return '未知';
  }
};

export function FileDetailPanel({
  file,
  onClose,
  onDelete,
  onDownload,
  onShare,
  onStartProcess,
  onViewDetails,
}: FileDetailPanelProps) {
  const [tags] = useState(['产品', '文档', '重要']);

  if (!file) {
    return null;
  }

  const Icon = getFileIcon(file.name);

  return (
    <div
      className={cn(
        'w-80 bg-white border-l border-gray-200 p-5 overflow-y-auto transition-transform duration-300'
      )}
    >
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h3 className="font-semibold text-gray-800">文件详情</h3>
        <button
          onClick={onClose}
          className="text-gray-500 hover:text-gray-700 transition-colors"
        >
          <X className="h-5 w-5" />
        </button>
      </div>

      {/* File Preview */}
      <div className="mb-6 flex flex-col items-center">
        <div className="w-24 h-24 rounded-xl bg-blue-50 flex items-center justify-center mb-3">
          <Icon className="h-12 w-12 text-blue-500" />
        </div>
        <h3 className="text-lg font-bold text-center">{file.name}</h3>
        <p className="text-gray-500 text-sm">文档类型</p>
      </div>

      {/* File Info */}
      <div className="border-t border-gray-100 pt-4 mb-6">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className="text-xs text-gray-500 mb-1">创建时间</p>
            <p className="text-sm">{formatDate(file.uploadedAt)}</p>
          </div>
          <div>
            <p className="text-xs text-gray-500 mb-1">修改时间</p>
            <p className="text-sm">{formatDate(file.uploadedAt)}</p>
          </div>
          <div>
            <p className="text-xs text-gray-500 mb-1">文件大小</p>
            <p className="text-sm">{formatFileSize(file.size)}</p>
          </div>
          <div>
            <p className="text-xs text-gray-500 mb-1">创建者</p>
            <p className="text-sm">系统</p>
          </div>
        </div>
      </div>

      {/* Processing Status */}
      <div className="border-t border-gray-100 pt-4 mb-6">
        <h4 className="text-sm font-medium mb-3">处理状态</h4>
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-600">OCR识别</span>
            <span className={cn('px-2 py-1 text-xs rounded-full', getStatusColor(file.statuses.ocr))}>
              {getStatusText(file.statuses.ocr)}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-600">向量化</span>
            <span className={cn('px-2 py-1 text-xs rounded-full', getStatusColor(file.statuses.vectorization))}>
              {getStatusText(file.statuses.vectorization)}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-600">问答对</span>
            <span className={cn('px-2 py-1 text-xs rounded-full', getStatusColor(file.statuses.qaPairs))}>
              {getStatusText(file.statuses.qaPairs)}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-600">知识图谱</span>
            <span className={cn('px-2 py-1 text-xs rounded-full', getStatusColor(file.statuses.knowledgeGraph))}>
              {getStatusText(file.statuses.knowledgeGraph)}
            </span>
          </div>
        </div>
      </div>

      {/* Tags */}
      <div className="border-t border-gray-100 pt-4 mb-6">
        <h4 className="text-sm font-medium mb-3">文件标签</h4>
        <div className="flex flex-wrap gap-2">
          {tags.map((tag) => (
            <Badge key={tag} variant="secondary" className="text-xs">
              {tag}
            </Badge>
          ))}
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex flex-col space-y-2 mb-4">
        {onViewDetails && (
          <Button
            onClick={() => onViewDetails(file.id)}
            className="bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center"
          >
            <Eye className="mr-1.5 h-4 w-4" />
            查看详情
          </Button>
        )}
        
        {onDownload && (
          <Button
            onClick={() => onDownload(file.id)}
            variant="outline"
            className="flex items-center justify-center"
          >
            <Download className="mr-1.5 h-4 w-4" />
            下载文件
          </Button>
        )}
        
        {onShare && (
          <Button
            onClick={() => onShare(file.id)}
            variant="outline"
            className="flex items-center justify-center"
          >
            <Share2 className="mr-1.5 h-4 w-4 text-blue-600" />
            分享文件
          </Button>
        )}
      </div>

      {/* Process Actions */}
      {onStartProcess && (
        <div className="border-t border-gray-100 pt-4 mb-4">
          <h4 className="text-sm font-medium mb-3">处理操作</h4>
          <div className="flex flex-col space-y-2">
            <Button
              onClick={() => onStartProcess(file.id, 'ocr')}
              disabled={file.statuses.ocr === 'processing' || file.statuses.ocr === 'completed'}
              variant="outline"
              size="sm"
            >
              <Play className="mr-1.5 h-3 w-3" />
              开始 OCR
            </Button>
            <Button
              onClick={() => onStartProcess(file.id, 'vectorization')}
              disabled={file.statuses.vectorization === 'processing' || file.statuses.vectorization === 'completed'}
              variant="outline"
              size="sm"
            >
              <Play className="mr-1.5 h-3 w-3" />
              开始向量化
            </Button>
            <Button
              onClick={() => onStartProcess(file.id, 'qa-pairs')}
              disabled={file.statuses.qaPairs === 'processing' || file.statuses.qaPairs === 'completed'}
              variant="outline"
              size="sm"
            >
              <Play className="mr-1.5 h-3 w-3" />
              生成问答对
            </Button>
            <Button
              onClick={() => onStartProcess(file.id, 'knowledge-graph')}
              disabled={file.statuses.knowledgeGraph === 'processing' || file.statuses.knowledgeGraph === 'completed'}
              variant="outline"
              size="sm"
            >
              <Play className="mr-1.5 h-3 w-3" />
              生成知识图谱
            </Button>
          </div>
        </div>
      )}

      {/* Delete Button */}
      {onDelete && (
        <Button
          onClick={() => onDelete(file.id)}
          variant="outline"
          className="w-full text-red-600 border-red-200 hover:bg-red-50"
        >
          <Trash2 className="mr-1.5 h-4 w-4" />
          删除文件
        </Button>
      )}

      {/* Warning */}
      <div className="mt-6 p-3 bg-yellow-50 rounded-lg border border-yellow-100">
        <div className="flex items-start">
          <AlertCircle className="h-5 w-5 text-yellow-500 mr-2 mt-0.5" />
          <div>
            <p className="text-sm text-yellow-800 font-medium mb-1">提示信息</p>
            <p className="text-xs text-yellow-700">
              文件处理完成后可以在对应的标签页查看结果。
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
