import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api/client';
import type { File as KnowledgeFile } from '@/types/file';

export function useFiles(knowledgeBaseId: string) {
  return useQuery({
    queryKey: ['files', knowledgeBaseId],
    queryFn: () => api.files.list(knowledgeBaseId),
    enabled: !!knowledgeBaseId,
  });
}

export function useFile(id: string) {
  return useQuery({
    queryKey: ['file', id],
    queryFn: () => api.files.get(id),
    enabled: !!id,
  });
}

type UploadVariables = {
  knowledgeBaseId: string;
  file: globalThis.File;
  onProgress?: (progress: number) => void;
};

type TriggerProcessVariables = {
  knowledgeBaseId: string;
  fileId: string;
  processType: 'ocr' | 'vectorization' | 'qa-pairs' | 'knowledge-graph';
};

export function useUploadFile() {
  const queryClient = useQueryClient();

  return useMutation<KnowledgeFile, Error, UploadVariables>({
    mutationFn: ({ knowledgeBaseId, file, onProgress }) =>
      api.files.upload(knowledgeBaseId, file, onProgress),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['files', variables.knowledgeBaseId],
      });
    },
  });
}

export function useTriggerFileProcess() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, TriggerProcessVariables>({
    mutationFn: ({ fileId, processType }) => api.files.triggerProcess(fileId, processType),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['files', variables.knowledgeBaseId],
      });
      queryClient.invalidateQueries({
        queryKey: ['file', variables.fileId],
      });
    },
  });
}
