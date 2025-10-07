import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api/client';
import { ApiKey, Business, Endpoint } from '@/types/model-config';

type EndpointInput = Record<string, unknown>;
type ApiKeyInput = Record<string, unknown>;
type BusinessInput = Record<string, unknown>;

type QueryOptions = {
  enabled?: boolean;
};

export function useModelEndpoints() {
  return useQuery({
    queryKey: ['model-config', 'endpoints'],
    queryFn: () => api.modelConfig.endpoints.list(),
  });
}

export function useModelEndpoint(id: string | number, options: QueryOptions = {}) {
  const { enabled = true } = options;
  const hasValidId = id !== undefined && id !== null && id !== '';
  return useQuery({
    queryKey: ['model-config', 'endpoint', id],
    queryFn: () => api.modelConfig.endpoints.get(id),
    enabled: enabled && hasValidId,
  });
}

export function useCreateModelEndpoint() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: EndpointInput) => api.modelConfig.endpoints.create(data),
    onSuccess: (endpoint: Endpoint) => {
      queryClient.invalidateQueries({ queryKey: ['model-config', 'endpoints'] });
      queryClient.setQueryData(['model-config', 'endpoint', endpoint.id], endpoint);
    },
  });
}

export function useUpdateModelEndpoint(id: string | number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: EndpointInput) => api.modelConfig.endpoints.update(id, data),
    onSuccess: (endpoint: Endpoint) => {
      queryClient.invalidateQueries({ queryKey: ['model-config', 'endpoints'] });
      queryClient.setQueryData(['model-config', 'endpoint', endpoint.id], endpoint);
    },
  });
}

export function useDeleteModelEndpoint() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string | number) => api.modelConfig.endpoints.delete(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['model-config', 'endpoints'] });
      queryClient.removeQueries({ queryKey: ['model-config', 'endpoint', id] });
    },
  });
}

export function useApiKeys(endpointId?: string | number) {
  return useQuery({
    queryKey: ['model-config', 'api-keys', endpointId ?? 'all'],
    queryFn: () => api.modelConfig.apiKeys.list(endpointId),
  });
}

export function useApiKey(id: string | number, options: QueryOptions = {}) {
  const { enabled = true } = options;
  const hasValidId = id !== undefined && id !== null && id !== '';

  return useQuery({
    queryKey: ['model-config', 'api-key', id],
    queryFn: () => api.modelConfig.apiKeys.get(id),
    enabled: enabled && hasValidId,
  });
}

export function useCreateApiKey() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: ApiKeyInput) => api.modelConfig.apiKeys.create(data),
    onSuccess: (apiKey: ApiKey) => {
      queryClient.invalidateQueries({ queryKey: ['model-config', 'api-keys'] });
      queryClient.setQueryData(['model-config', 'api-key', apiKey.id], apiKey);
    },
  });
}

export function useUpdateApiKey(id: string | number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: ApiKeyInput) => api.modelConfig.apiKeys.update(id, data),
    onSuccess: (apiKey: ApiKey) => {
      queryClient.invalidateQueries({ queryKey: ['model-config', 'api-keys'] });
      queryClient.setQueryData(['model-config', 'api-key', apiKey.id], apiKey);
    },
  });
}

export function useDeleteApiKey() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string | number) => api.modelConfig.apiKeys.delete(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['model-config', 'api-keys'] });
      queryClient.removeQueries({ queryKey: ['model-config', 'api-key', id] });
    },
  });
}

export function useBusinesses() {
  return useQuery({
    queryKey: ['model-config', 'businesses'],
    queryFn: () => api.modelConfig.business.list(),
  });
}

export function useBusiness(id: string | number, options: QueryOptions = {}) {
  const { enabled = true } = options;
  const hasValidId = id !== undefined && id !== null && id !== '';

  return useQuery({
    queryKey: ['model-config', 'business', id],
    queryFn: () => api.modelConfig.business.get(id),
    enabled: enabled && hasValidId,
  });
}

export function useCreateBusiness() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: BusinessInput) => api.modelConfig.business.create(data),
    onSuccess: (business: Business) => {
      queryClient.invalidateQueries({ queryKey: ['model-config', 'businesses'] });
      queryClient.setQueryData(['model-config', 'business', business.id], business);
    },
  });
}

export function useUpdateBusiness(id: string | number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: BusinessInput) => api.modelConfig.business.update(id, data),
    onSuccess: (business: Business) => {
      queryClient.invalidateQueries({ queryKey: ['model-config', 'businesses'] });
      queryClient.setQueryData(['model-config', 'business', business.id], business);
    },
  });
}

export function useDeleteBusiness() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string | number) => api.modelConfig.business.delete(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['model-config', 'businesses'] });
      queryClient.removeQueries({ queryKey: ['model-config', 'business', id] });
    },
  });
}
