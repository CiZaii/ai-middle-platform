import { create } from 'zustand';
import { KnowledgeBase } from '@/types/knowledge-base';

interface KnowledgeBaseStore {
  selectedKnowledgeBase: KnowledgeBase | null;
  setSelectedKnowledgeBase: (kb: KnowledgeBase | null) => void;
}

export const useKnowledgeBaseStore = create<KnowledgeBaseStore>()((set) => ({
  selectedKnowledgeBase: null,
  setSelectedKnowledgeBase: (kb) => set({ selectedKnowledgeBase: kb }),
}));
