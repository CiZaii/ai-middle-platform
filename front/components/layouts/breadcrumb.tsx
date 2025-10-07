'use client';

import { useEffect, useMemo, useState } from 'react';
import { usePathname } from 'next/navigation';
import Link from 'next/link';
import { ChevronRight, Home } from 'lucide-react';
import { api } from '@/lib/api/client';

const routeNameMap: Record<string, string> = {
  dashboard: '仪表盘',
  knowledge: '知识库',
  'model-config': '模型配置',
  endpoints: '端点配置',
  'api-keys': 'API Key 管理',
  business: '业务配置',
  users: '用户管理',
  settings: '系统设置',
  list: '列表',
  files: '文件',
};

export function Breadcrumb() {
  const pathname = usePathname();
  const segments = useMemo(() => pathname.split('/').filter(Boolean), [pathname]);
  const [dynamicNames, setDynamicNames] = useState<Record<string, string>>({});

  // Remove (dashboard) from segments
  const cleanSegments = useMemo(
    () => segments.filter((seg) => !seg.startsWith('(')),
    [segments]
  );

  const breadcrumbKey = useMemo(
    () => cleanSegments.join('/'),
    [cleanSegments]
  );

  useEffect(() => {
    if (!breadcrumbKey) return;

    let cancelled = false;

    const fetchNames = async () => {
      const requests: Array<Promise<{ key: string; name: string | null }>> = [];

      const knowledgeIndex = cleanSegments.indexOf('knowledge');
      if (knowledgeIndex !== -1 && cleanSegments.length > knowledgeIndex + 1) {
        const kbSegment = cleanSegments[knowledgeIndex + 1];

        if (kbSegment && !routeNameMap[kbSegment]) {
          requests.push(
            api.knowledgeBases
              .get(kbSegment)
              .then((kb) => ({ key: kbSegment, name: kb?.name ?? null }))
              .catch(() => ({ key: kbSegment, name: null }))
          );
        }

        const filesIndex = cleanSegments.indexOf('files', knowledgeIndex + 1);
        if (filesIndex !== -1 && cleanSegments.length > filesIndex + 1) {
          const fileSegment = cleanSegments[filesIndex + 1];

          if (fileSegment && !routeNameMap[fileSegment]) {
            requests.push(
              api.files
                .get(fileSegment)
                .then((file) => ({ key: fileSegment, name: file?.name ?? null }))
                .catch(() => ({ key: fileSegment, name: null }))
            );
          }
        }
      }

      if (requests.length === 0) return;

      const results = await Promise.all(requests);
      if (cancelled) return;

      setDynamicNames((prev) => {
        const next = { ...prev };
        for (const { key, name } of results) {
          if (name && !next[key]) {
            next[key] = name;
          }
        }
        return next;
      });
    };

    fetchNames();

    return () => {
      cancelled = true;
    };
  }, [breadcrumbKey, cleanSegments]);

  // If only dashboard or root
  if (cleanSegments.length === 0 || cleanSegments[0] === 'dashboard') {
    return null;
  }

  return (
    <nav className="flex items-center gap-1 text-sm text-gray-600">
      <span>我的空间</span>

      {cleanSegments.map((segment, index) => {
        // Skip dynamic segments like [id]
        if (segment.startsWith('[') && segment.endsWith(']')) {
          return null;
        }

        const isLast = index === cleanSegments.length - 1;
        const href = `/${cleanSegments.slice(0, index + 1).join('/')}`;
        const name = dynamicNames[segment] || routeNameMap[segment] || segment;

        return (
          <div key={segment} className="flex items-center gap-1">
            <ChevronRight className="h-4 w-4 mx-1" />
            {isLast ? (
              <span className="text-blue-600 font-medium">{name}</span>
            ) : (
              <Link
                href={href}
                className="hover:text-gray-900 transition-colors"
              >
                {name}
              </Link>
            )}
          </div>
        );
      })}
    </nav>
  );
}
