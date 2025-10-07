'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Loader2, Edit, Save, X } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';
import rehypeHighlight from 'rehype-highlight';
import 'katex/dist/katex.min.css';
import 'highlight.js/styles/github-dark.css';

interface OcrEditorProps {
  content: string;
  onSave: (newContent: string) => Promise<void>;
  readonly?: boolean;
}

export function OcrEditor({ content, onSave, readonly = false }: OcrEditorProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editedContent, setEditedContent] = useState(content);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setEditedContent(content);
  }, [content]);

  const handleSave = async () => {
    if (editedContent === content) {
      setIsEditing(false);
      return;
    }

    try {
      setSaving(true);
      await onSave(editedContent);
      setIsEditing(false);
    } catch (error) {
      console.error('保存失败', error);
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setEditedContent(content);
    setIsEditing(false);
  };

  if (isEditing) {
    return (
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h4 className="text-sm font-medium">编辑OCR内容</h4>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handleCancel}
              disabled={saving}
            >
              <X className="mr-2 h-4 w-4" />
              取消
            </Button>
            <Button
              size="sm"
              onClick={handleSave}
              disabled={saving}
            >
              {saving ? (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              ) : (
                <Save className="mr-2 h-4 w-4" />
              )}
              保存
            </Button>
          </div>
        </div>
        <Textarea
          value={editedContent}
          onChange={(e) => setEditedContent(e.target.value)}
          className="min-h-[500px] font-mono text-sm"
          placeholder="请输入OCR内容（支持Markdown格式）"
        />
        <div className="text-xs text-muted-foreground">
          支持Markdown格式，包括标题、列表、代码块、数学公式等
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {!readonly && (
        <div className="flex items-center justify-between">
          <h4 className="text-sm font-medium">OCR识别内容</h4>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setIsEditing(true)}
          >
            <Edit className="mr-2 h-4 w-4" />
            编辑
          </Button>
        </div>
      )}
      <div className="prose prose-slate dark:prose-invert max-w-none">
        <ReactMarkdown
          remarkPlugins={[remarkGfm, remarkMath]}
          rehypePlugins={[rehypeKatex, rehypeHighlight]}
        >
          {content || '暂无OCR内容'}
        </ReactMarkdown>
      </div>
    </div>
  );
}
