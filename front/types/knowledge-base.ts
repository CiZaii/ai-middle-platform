import { User } from './user';

export interface Member {
  user: User;
  role: 'owner' | 'editor' | 'viewer';
  joinedAt: string;
}

export interface KnowledgeBase {
  id: string;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
  fileCount: number;
  owner: User;
  members: Member[];
}
