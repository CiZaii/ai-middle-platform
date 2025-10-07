import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api/client';
import { useAuthStore } from '@/lib/stores/auth-store';
import { User } from '@/types/user';

type LoginVariables = {
  email: string;
  password: string;
};

type RegisterVariables = {
  username: string;
  email: string;
  password: string;
  name: string;
};

export function useCurrentUser(options: { enabled?: boolean } = {}) {
  const { enabled = true } = options;
  const token = useAuthStore((state) => state.token);
  const setToken = useAuthStore((state) => state.setToken);
  const setUser = useAuthStore((state) => state.setUser);
  const logoutStore = useAuthStore((state) => state.logout);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    if (!token) {
      const storedToken = localStorage.getItem('auth_token');
      if (storedToken) {
        setToken(storedToken);
      }
    }
  }, [token, setToken]);

  const query = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: () => api.auth.getCurrentUser(),
    enabled: enabled && !!token,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });

  // Handle side effects with useEffect instead of callbacks
  useEffect(() => {
    if (query.isSuccess && query.data) {
      setUser(query.data);
    }
  }, [query.isSuccess, query.data, setUser]);

  useEffect(() => {
    if (query.isError && token) {
      logoutStore();
      api.auth.logout().catch(() => undefined);
    }
  }, [query.isError, token, logoutStore]);

  return query;
}

export function useLogin() {
  const setUser = useAuthStore((state) => state.setUser);
  const setToken = useAuthStore((state) => state.setToken);
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ email, password }: LoginVariables) =>
      api.auth.login(email, password),
    onSuccess: ({ user, token }) => {
      setUser(user);
      setToken(token);
      queryClient.invalidateQueries({ queryKey: ['auth', 'me'] });
    },
  });
}

export function useRegister() {
  const setUser = useAuthStore((state) => state.setUser);
  const setToken = useAuthStore((state) => state.setToken);
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: RegisterVariables) => api.auth.register(payload),
    onSuccess: ({ user, token }) => {
      setUser(user);
      setToken(token);
      queryClient.invalidateQueries({ queryKey: ['auth', 'me'] });
    },
  });
}

export function useLogout() {
  const logoutStore = useAuthStore((state) => state.logout);
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => api.auth.logout(),
    onSuccess: () => {
      logoutStore();
      queryClient.removeQueries({ queryKey: ['auth'] });
      queryClient.invalidateQueries();
    },
  });
}

export function useAuthUser() {
  return useAuthStore((state) => state.user);
}

export function useAuthToken() {
  return useAuthStore((state) => state.token);
}
