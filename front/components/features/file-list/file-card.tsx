'use client';

import { FileText, FileSpreadsheet, Presentation, Image as ImageIcon, File, Folder } from 'lucide-react';
import { cn } from '@/lib/utils/cn';

interface FileCardProps {
  id: string;
  name: string;
  size: number;
  type: string;
  updatedAt: string;
  isFolder?: boolean;
  onClick?: () => void;
  isSelected?: boolean;
}

const getFileIcon = (type: string, isFolder?: boolean) => {
  if (isFolder) {
    return { Icon: Folder, bgColor: 'bg-blue-50', iconColor: 'text-blue-500' };
  }

  const extension = type.toLowerCase();
  
  if (extension.includes('doc') || extension.includes('docx')) {
    return { Icon: FileText, bgColor: 'bg-blue-50', iconColor: 'text-blue-500' };
  }
  
  if (extension.includes('xls') || extension.includes('xlsx') || extension.includes('csv')) {
    return { Icon: FileSpreadsheet, bgColor: 'bg-green-50', iconColor: 'text-green-500' };
  }
  
  if (extension.includes('ppt') || extension.includes('pptx')) {
    return { Icon: Presentation, bgColor: 'bg-orange-50', iconColor: 'text-orange-500' };
  }
  
  if (extension.includes('png') || extension.includes('jpg') || extension.includes('jpeg') || extension.includes('gif') || extension.includes('svg')) {
    return { Icon: ImageIcon, bgColor: 'bg-purple-50', iconColor: 'text-purple-500' };
  }
  
  if (extension.includes('pdf')) {
    return { Icon: FileText, bgColor: 'bg-red-50', iconColor: 'text-red-500' };
  }
  
  return { Icon: File, bgColor: 'bg-gray-50', iconColor: 'text-gray-500' };
};

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
};

const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return '刚刚';
  if (diffMins < 60) return `${diffMins}分钟前`;
  if (diffHours < 24) return `${diffHours}小时前`;
  if (diffDays === 1) return '昨天';
  if (diffDays === 2) return '2天前';
  if (diffDays < 7) return `${diffDays}天前`;
  
  return date.toLocaleDateString('zh-CN');
};

export function FileCard({ 
  id, 
  name, 
  size, 
  type, 
  updatedAt, 
  isFolder, 
  onClick,
  isSelected 
}: FileCardProps) {
  const { Icon, bgColor, iconColor } = getFileIcon(type, isFolder);

  return (
    <div
      onClick={onClick}
      className={cn(
        'file-card rounded-xl bg-white overflow-hidden shadow-sm cursor-pointer transition-all duration-300',
        'hover:transform hover:-translate-y-1 hover:scale-105 hover:shadow-md',
        isSelected && 'ring-2 ring-blue-500'
      )}
    >
      <div className={cn('h-28 flex items-center justify-center border-b', bgColor)}>
        <Icon className={cn('h-12 w-12', iconColor)} />
      </div>
      <div className="p-3">
        <h3 className="font-bold text-gray-800 mb-1 truncate" title={name}>
          {name}
        </h3>
        <div className="flex justify-between text-xs text-gray-500">
          <span>{isFolder ? `${size} 个文件` : formatFileSize(size)}</span>
          <span>{formatDate(updatedAt)}</span>
        </div>
      </div>
    </div>
  );
}
