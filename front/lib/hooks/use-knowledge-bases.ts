import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api/client';
import { KnowledgeBase } from '@/types/knowledge-base';

export function useKnowledgeBases() {
  return useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: () => api.knowledgeBases.list(),
  });
}

export function useKnowledgeBase(id: string) {
  return useQuery({
    queryKey: ['knowledge-base', id],
    queryFn: () => api.knowledgeBases.get(id),
    enabled: !!id,
  });
}

export function useCreateKnowledgeBase() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { name: string; description?: string }) =>
      api.knowledgeBases.create(data),
    onSuccess: (knowledgeBase) => {
      if (knowledgeBase?.id) {
        queryClient.setQueryData(['knowledge-base', knowledgeBase.id], knowledgeBase);
      }
      queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
    },
  });
}

export function useDeleteKnowledgeBase() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => api.knowledgeBases.delete(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: ['knowledge-bases'] });
      const previousData = queryClient.getQueryData(['knowledge-bases']);

      queryClient.setQueryData(
        ['knowledge-bases'],
        (old: KnowledgeBase[] = []) => old.filter(kb => kb.id !== id)
      );

      return { previousData };
    },
    onError: (err, id, context) => {
      queryClient.setQueryData(['knowledge-bases'], context?.previousData);
    },
    onSettled: (_data, _error, id) => {
      queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      if (id) {
        queryClient.removeQueries({ queryKey: ['knowledge-base', id] });
      }
    },
  });
}
