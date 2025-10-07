import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api/client';
import { Prompt, PromptRequest } from '@/types/prompt';

const QUERY_KEY = 'prompts';

/**
 * 获取所有 Prompt 列表
 */
export function usePrompts() {
  return useQuery({
    queryKey: [QUERY_KEY],
    queryFn: () => api.prompts.list(),
  });
}

/**
 * 获取单个 Prompt
 */
export function usePrompt(id: number) {
  return useQuery({
    queryKey: [QUERY_KEY, id],
    queryFn: () => api.prompts.get(id),
    enabled: !!id,
  });
}

/**
 * 根据业务代码获取 Prompt
 */
export function usePromptByBusinessCode(businessCode: string) {
  return useQuery({
    queryKey: [QUERY_KEY, 'business', businessCode],
    queryFn: () => api.prompts.getByBusinessCode(businessCode),
    enabled: !!businessCode,
  });
}

/**
 * 创建 Prompt
 */
export function useCreatePrompt() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (data: PromptRequest) => api.prompts.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] });
    },
  });
}

/**
 * 更新 Prompt
 */
export function useUpdatePrompt(id: number) {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (data: PromptRequest) => api.prompts.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY, id] });
    },
  });
}

/**
 * 删除 Prompt
 */
export function useDeletePrompt() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (id: number) => api.prompts.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] });
    },
  });
}

/**
 * 激活/停用 Prompt
 */
export function useTogglePrompt() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, isActive }: { id: number; isActive: boolean }) =>
      api.prompts.toggleActive(id, isActive),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] });
    },
  });
}
