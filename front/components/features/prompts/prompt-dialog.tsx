'use client';

import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Switch } from '@/components/ui/switch';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Prompt, PromptRequest, BusinessCode, BUSINESS_NAME_MAP } from '@/types/prompt';
import { useCreatePrompt, useUpdatePrompt } from '@/lib/hooks/use-prompts';

interface PromptDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  prompt?: Prompt | null;
}

interface FormValues extends PromptRequest {}

export function PromptDialog({ open, onOpenChange, prompt }: PromptDialogProps) {
  const isEditing = !!prompt;
  const createMutation = useCreatePrompt();
  const updateMutation = useUpdatePrompt(prompt?.id ?? 0);
  const [extractedVariables, setExtractedVariables] = useState<string[]>([]);

  const form = useForm<FormValues>({
    defaultValues: {
      businessCode: '',
      promptName: '',
      promptContent: '',
      description: '',
      variables: '[]',
      isActive: true,
    },
  });

  useEffect(() => {
    if (prompt) {
      form.reset({
        businessCode: prompt.businessCode,
        promptName: prompt.promptName,
        promptContent: prompt.promptContent,
        description: prompt.description || '',
        variables: JSON.stringify(prompt.variables),
        isActive: prompt.isActive,
      });
      setExtractedVariables(prompt.variables);
    } else {
      form.reset({
        businessCode: '',
        promptName: '',
        promptContent: '',
        description: '',
        variables: '[]',
        isActive: true,
      });
      setExtractedVariables([]);
    }
  }, [prompt, form]);

  // 监听 promptContent 变化，自动提取变量
  useEffect(() => {
    const subscription = form.watch((value, { name }) => {
      if (name === 'promptContent' && value.promptContent) {
        const content = value.promptContent;
        const variableRegex = /\{(\w+)\}/g;
        const matches = Array.from(content.matchAll(variableRegex));
        const uniqueVariables = Array.from(new Set(matches.map(m => m[1])));
        setExtractedVariables(uniqueVariables);
        form.setValue('variables', JSON.stringify(uniqueVariables));
      }
    });
    return () => subscription.unsubscribe();
  }, [form]);

  const onSubmit = async (values: FormValues) => {
    try {
      if (isEditing) {
        await updateMutation.mutateAsync(values);
      } else {
        await createMutation.mutateAsync(values);
      }
      onOpenChange(false);
      form.reset();
    } catch (error) {
      console.error('保存 Prompt 失败', error);
    }
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEditing ? '编辑 Prompt' : '新增 Prompt'}</DialogTitle>
          <DialogDescription>
            {isEditing
              ? '修改 Prompt 模板内容，版本号将自动递增'
              : '创建新的 Prompt 模板，使用 {变量名} 语法定义变量'}
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="businessCode"
                rules={{ required: '请选择业务代码' }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>业务代码</FormLabel>
                    <Select
                      onValueChange={field.onChange}
                      value={field.value}
                      disabled={isEditing}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="选择业务代码" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {Object.entries(BUSINESS_NAME_MAP).map(([code, name]) => (
                          <SelectItem key={code} value={code}>
                            {name} ({code})
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormDescription>
                      {isEditing && '业务代码创建后不可修改'}
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="promptName"
                rules={{ required: 'Prompt 名称不能为空' }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Prompt 名称</FormLabel>
                    <FormControl>
                      <Input placeholder="例如：OCR文字识别" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>描述（可选）</FormLabel>
                  <FormControl>
                    <Input placeholder="简要描述此 Prompt 的用途" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="promptContent"
              rules={{ required: 'Prompt 内容不能为空' }}
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Prompt 内容</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="输入 Prompt 模板内容，使用 {变量名} 定义变量"
                      className="min-h-[300px] font-mono text-sm"
                      {...field}
                    />
                  </FormControl>
                  <FormDescription>
                    支持 {'{变量名}'} 语法，例如 {'{questionCount}'}, {'{content}'}
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            {extractedVariables.length > 0 && (
              <div className="bg-muted p-4 rounded-lg">
                <p className="text-sm font-medium mb-2">检测到的变量：</p>
                <div className="flex flex-wrap gap-2">
                  {extractedVariables.map((variable) => (
                    <Badge key={variable} variant="secondary">
                      {'{' + variable + '}'}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            <FormField
              control={form.control}
              name="isActive"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                  <div className="space-y-0.5">
                    <FormLabel className="text-base">激活状态</FormLabel>
                    <FormDescription>
                      激活后，此 Prompt 将被 AI 业务使用
                    </FormDescription>
                  </div>
                  <FormControl>
                    <Switch
                      checked={field.value}
                      onCheckedChange={field.onChange}
                    />
                  </FormControl>
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isSaving}
              >
                取消
              </Button>
              <Button type="submit" disabled={isSaving}>
                {isSaving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {isEditing ? '保存修改' : '创建 Prompt'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
