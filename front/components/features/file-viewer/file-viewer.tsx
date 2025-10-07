'use client';

import { useState, useEffect, useCallback } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { 
  Download, 
  ExternalLink, 
  Loader2, 
  ChevronLeft, 
  ChevronRight,
  ZoomIn,
  ZoomOut,
  Maximize2,
  RotateCw,
  Minimize2
} from 'lucide-react';
import dynamic from 'next/dynamic';
import { useAuthStore } from '@/lib/stores/auth-store';
import { Input } from '@/components/ui/input';

// 动态导入 react-pdf，避免服务端渲染问题
const Document = dynamic(
  () => import('react-pdf').then((mod) => mod.Document),
  { ssr: false }
);
const Page = dynamic(
  () => import('react-pdf').then((mod) => mod.Page),
  { ssr: false }
);

// 在客户端配置 PDF.js worker 和导入样式
if (typeof window !== 'undefined') {
  import('react-pdf').then((pdfjs) => {
    pdfjs.pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.pdfjs.version}/build/pdf.worker.min.js`;
  });
  // 导入样式
  import('react-pdf/dist/Page/AnnotationLayer.css');
  import('react-pdf/dist/Page/TextLayer.css');
}

interface FileViewerProps {
  fileUrl: string;
  fileName: string;
  mimeType: string;
  fileType: 'pdf' | 'image' | 'word';
}

export function FileViewer({ fileUrl, fileName, mimeType, fileType }: FileViewerProps) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [numPages, setNumPages] = useState<number>(0);
  const [pageNumber, setPageNumber] = useState<number>(1);
  const [scale, setScale] = useState<number>(1.0);
  const [pageInput, setPageInput] = useState<string>('1');
  const [containerRef, setContainerRef] = useState<HTMLDivElement | null>(null);
  
  // 获取token（方案2：通过httpHeaders传递）
  const token = useAuthStore((state) => state.token);

  // 使用后端代理URL（通过x-file-storage）
  const proxyUrl = `/api/proxy/file?url=${encodeURIComponent(fileUrl)}`;
  
  // 携带token到后端
  const httpHeaders = token ? { Authorization: `Bearer ${token}` } : undefined;

  // 调试信息
  useEffect(() => {
    console.log('FileViewer加载:', {
      fileUrl,
      proxyUrl,
      hasToken: !!token,
      fileType,
      mimeType
    });
  }, [fileUrl, proxyUrl, token, fileType, mimeType]);

  const handleDownload = () => {
    window.open(fileUrl, '_blank');
  };

  const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
    console.log('PDF加载成功，页数:', numPages);
    setNumPages(numPages);
    setLoading(false);
    setError(null);
  };

  const onDocumentLoadError = (error: Error) => {
    console.error('PDF加载失败:', error);
    setError(`PDF加载失败: ${error.message}`);
    setLoading(false);
  };

  // 缩放控制
  const zoomIn = () => {
    setScale((prev) => Math.min(prev + 0.2, 3.0));
  };

  const zoomOut = () => {
    setScale((prev) => Math.max(prev - 0.2, 0.5));
  };

  const resetZoom = () => {
    setScale(1.0);
  };

  const fitWidth = () => {
    setScale(1.5);
  };

  // 页面跳转
  const goToPage = (page: number) => {
    const targetPage = Math.max(1, Math.min(page, numPages));
    setPageNumber(targetPage);
    setPageInput(targetPage.toString());
    
    // 滚动到指定页面
    const pageElement = document.getElementById(`pdf-page-${targetPage}`);
    if (pageElement) {
      pageElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  const handlePageInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPageInput(e.target.value);
  };

  const handlePageInputSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const page = parseInt(pageInput, 10);
    if (!isNaN(page)) {
      goToPage(page);
    }
  };

  const goToPrevPage = () => {
    goToPage(pageNumber - 1);
  };

  const goToNextPage = () => {
    goToPage(pageNumber + 1);
  };

  // 滚动监听，自动更新当前页码
  useEffect(() => {
    if (!containerRef || numPages === 0) return;

    const handleScroll = () => {
      // 获取所有页面元素
      const pages = Array.from({ length: numPages }, (_, i) => 
        document.getElementById(`pdf-page-${i + 1}`)
      ).filter(Boolean);

      // 找到当前视口中最靠上的页面
      const containerTop = containerRef.getBoundingClientRect().top;
      let currentPage = 1;

      for (let i = 0; i < pages.length; i++) {
        const page = pages[i];
        if (page) {
          const pageRect = page.getBoundingClientRect();
          const pageMiddle = pageRect.top + pageRect.height / 2;
          
          // 如果页面中心在视口中
          if (pageMiddle > containerTop && pageMiddle < window.innerHeight) {
            currentPage = i + 1;
            break;
          }
        }
      }

      setPageNumber(currentPage);
      setPageInput(currentPage.toString());
    };

    containerRef.addEventListener('scroll', handleScroll);
    return () => containerRef.removeEventListener('scroll', handleScroll);
  }, [containerRef, numPages]); // 移除pageNumber依赖，避免无限循环

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
            src={proxyUrl}
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

    // PDF文件 - 使用react-pdf多页滚动模式
    if (fileType === 'pdf' || mimeType === 'application/pdf') {
      return (
        <div className="w-full h-full flex flex-col rounded-lg overflow-hidden border bg-gray-100">
          {/* 工具栏 */}
          {numPages > 0 && (
            <div className="flex items-center justify-between gap-2 p-2 bg-white border-b shrink-0">
              {/* 左侧：缩放控制 */}
              <div className="flex items-center gap-1">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={zoomOut}
                  disabled={scale <= 0.5}
                  title="缩小"
                >
                  <ZoomOut className="h-4 w-4" />
                </Button>
                <span className="text-xs text-muted-foreground min-w-[50px] text-center">
                  {Math.round(scale * 100)}%
                </span>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={zoomIn}
                  disabled={scale >= 3.0}
                  title="放大"
                >
                  <ZoomIn className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={resetZoom}
                  title="实际大小"
                >
                  <Minimize2 className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={fitWidth}
                  title="适应宽度"
                >
                  <Maximize2 className="h-4 w-4" />
                </Button>
              </div>

              {/* 中间：页面导航 */}
              <div className="flex items-center gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={goToPrevPage}
                  disabled={pageNumber <= 1}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                
                <form onSubmit={handlePageInputSubmit} className="flex items-center gap-1">
                  <Input
                    type="text"
                    value={pageInput}
                    onChange={handlePageInputChange}
                    className="w-12 h-7 text-center text-xs p-1"
                  />
                  <span className="text-xs text-muted-foreground whitespace-nowrap">
                    / {numPages}
                  </span>
                </form>
                
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={goToNextPage}
                  disabled={pageNumber >= numPages}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>

              {/* 右侧：下载按钮 */}
              <Button
                variant="ghost"
                size="sm"
                onClick={handleDownload}
                title="下载PDF"
              >
                <Download className="h-4 w-4" />
              </Button>
            </div>
          )}

          {/* PDF内容区域 - 多页滚动模式 */}
          <div 
            ref={setContainerRef}
            className="flex-1 overflow-auto bg-gray-200"
          >
            {error ? (
              <div className="h-full flex items-center justify-center">
                <div className="text-center">
                  <p className="text-red-500 mb-4">{error}</p>
                  <Button onClick={handleDownload}>
                    <Download className="mr-2 h-4 w-4" />
                    下载PDF
                  </Button>
                </div>
              </div>
            ) : (
              <div className="flex flex-col items-center py-4 gap-4">
                <Document
                  file={proxyUrl}
                  onLoadSuccess={onDocumentLoadSuccess}
                  onLoadError={onDocumentLoadError}
                  options={{
                    httpHeaders: httpHeaders,
                  }}
                  loading={
                    <div className="flex items-center justify-center h-full min-h-[400px]">
                      <Loader2 className="h-8 w-8 animate-spin text-primary" />
                    </div>
                  }
                >
                  {/* 渲染所有页面 */}
                  {Array.from(new Array(numPages), (el, index) => (
                    <div
                      key={`page_${index + 1}`}
                      id={`pdf-page-${index + 1}`}
                      className="mb-4 shadow-lg bg-white"
                    >
                      <Page
                        pageNumber={index + 1}
                        renderTextLayer={true}
                        renderAnnotationLayer={true}
                        scale={scale}
                        className="mx-auto"
                      />
                    </div>
                  ))}
                </Document>
              </div>
            )}
          </div>
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
