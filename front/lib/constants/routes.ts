export const ROUTES = {
  HOME: '/',
  KNOWLEDGE_BASES: '/knowledge-bases',
  KB_DETAIL: (id: string) => `/kb/${id}`,
  FILE_DETAIL: (id: string) => `/files/${id}`,
  LOGIN: '/login',
} as const;
