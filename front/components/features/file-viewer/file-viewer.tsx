'use client';

import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Download, ExternalLink, Loader2 } from 'lucide-react';

interface FileViewerProps {
  fileUrl: string;
  fileName: string;
  mimeType: string;
  fileType: 'pdf' | 'image' | 'word';
}

export function FileViewer({ fileUrl, fileName, mimeType, fileType }: FileViewerProps) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const handleDownload = () => {
    window.open(fileUrl, '_blank');
  };

  const renderViewer = () => {
    // 图片文件
    if (fileType === 'image' || mimeType.startsWith('image/')) {
      return (
        <div className="relative w-full h-full flex items-center justify-center bg-muted/20 rounded-lg overflow-hidden">
          {loading && (
            <div className="absolute inset-0 flex items-center justify-center">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          )}
          <img
            src={fileUrl}
            alt={fileName}
            className="max-w-full max-h-full object-contain"
            onLoad={() => setLoading(false)}
            onError={() => {
              setLoading(false);
              setError('图片加载失败');
            }}
          />
        </div>
      );
    }

    // PDF文件
    if (fileType === 'pdf' || mimeType === 'application/pdf') {
      return (
        <div className="w-full h-full rounded-lg overflow-hidden border">
          <iframe
            src={`${fileUrl}#view=FitH`}
            className="w-full h-full"
            title={fileName}
            onLoad={() => setLoading(false)}
            onError={() => {
              setLoading(false);
              setError('PDF加载失败');
            }}
          />
          {loading && (
            <div className="absolute inset-0 flex items-center justify-center bg-background/80">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          )}
        </div>
      );
    }

    // Word文档或其他文件类型 - 使用Google Docs Viewer或Office Online
    if (fileType === 'word' || mimeType.includes('word') || mimeType.includes('document')) {
      return (
        <div className="w-full h-full rounded-lg overflow-hidden border">
          <iframe
            src={`https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(fileUrl)}`}
            className="w-full h-full"
            title={fileName}
            onLoad={() => setLoading(false)}
            onError={() => {
              setLoading(false);
              setError('文档预览失败');
            }}
          />
          {loading && (
            <div className="absolute inset-0 flex items-center justify-center bg-background/80">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          )}
        </div>
      );
    }

    // 不支持的文件类型
    return (
      <div className="w-full h-full flex items-center justify-center">
        <div className="text-center">
          <ExternalLink className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
          <p className="text-muted-foreground mb-4">该文件类型不支持在线预览</p>
          <Button onClick={handleDownload}>
            <Download className="mr-2 h-4 w-4" />
            下载文件
          </Button>
        </div>
      </div>
    );
  };

  return (
    <Card className="w-full h-full flex flex-col overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3 border-b">
        <div className="flex-1 min-w-0">
          <h3 className="text-sm font-medium truncate">{fileName}</h3>
          <p className="text-xs text-muted-foreground">{mimeType}</p>
        </div>
        <Button variant="ghost" size="sm" onClick={handleDownload}>
          <Download className="h-4 w-4" />
        </Button>
      </div>
      <div className="flex-1 relative overflow-hidden">
        {error ? (
          <div className="w-full h-full flex items-center justify-center">
            <div className="text-center">
              <p className="text-red-600 mb-4">{error}</p>
              <Button onClick={handleDownload}>
                <Download className="mr-2 h-4 w-4" />
                下载文件
              </Button>
            </div>
          </div>
        ) : (
          renderViewer()
        )}
      </div>
    </Card>
  );
}
