import { Badge } from '@/components/ui/badge';
import { ProcessingStatus } from '@/types/file';
import { CheckCircle2, Clock, Loader2, XCircle } from 'lucide-react';

interface StatusBadgeProps {
  status: ProcessingStatus;
  label?: string;
}

const statusConfig = {
  [ProcessingStatus.PENDING]: {
    label: '待处理',
    variant: 'secondary' as const,
    icon: Clock,
  },
  [ProcessingStatus.PROCESSING]: {
    label: '处理中',
    variant: 'processing' as const,
    icon: Loader2,
    animate: true,
  },
  [ProcessingStatus.COMPLETED]: {
    label: '已完成',
    variant: 'success' as const,
    icon: CheckCircle2,
  },
  [ProcessingStatus.FAILED]: {
    label: '失败',
    variant: 'destructive' as const,
    icon: XCircle,
  },
};

export function StatusBadge({ status, label }: StatusBadgeProps) {
  const config = statusConfig[status];
  const Icon = config.icon;
  const animate = 'animate' in config ? config.animate : false;

  return (
    <Badge variant={config.variant} className="gap-1">
      <Icon className={`h-3 w-3 ${animate ? 'animate-spin' : ''}`} />
      {label || config.label}
    </Badge>
  );
}
