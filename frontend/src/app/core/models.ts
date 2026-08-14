/** Types echanges avec le serveur. Ils reprennent tels quels les termes du domaine. */

export type RoleUtilisateur = 'CUSTOMER' | 'AGENT';
export type EtatMessage = 'SENT' | 'DELIVERED' | 'READ';
export type EtatConversation = 'WAITING' | 'TAKEN' | 'CLOSED';

export interface Session {
  authenticated: boolean;
  userId: string | null;
  displayName: string | null;
  role: RoleUtilisateur | null;
  /** Instance ayant servi la requete — sert uniquement a rendre la demonstration lisible. */
  instance: string;
}

export interface Message {
  id: string;
  conversationId: string;
  authorId: string;
  authorRole: RoleUtilisateur;
  body: string;
  sentAt: string;
  state: EtatMessage;
}

export interface Conversation {
  id: string;
  subject: string;
  status: EtatConversation;
  openedAt: string;
  agentId: string | null;
}

export interface ResumeConversation {
  id: string;
  subject: string;
  status: EtatConversation;
  openedAt: string;
  lastMessageAt: string | null;
  lastMessagePreview: string | null;
  unreadCount: number;
}

export interface DemandeEnAttente {
  id: string;
  subject: string;
  openedAt: string;
  waitingSeconds: number;
}

/** Trame diffusee sur le canal temps reel. Le champ `type` porte la discrimination. */
export interface EvenementTchat {
  type:
    | 'MESSAGE_POSTED'
    | 'MESSAGE_DELIVERED'
    | 'CONVERSATION_READ'
    | 'CONVERSATION_OPENED'
    | 'CONVERSATION_TAKEN'
    | 'CONVERSATION_CLOSED';
  conversationId: string;
  occurredAt: string;
  message?: Message;
  messageId?: string;
  messageIds?: string[];
  actorId?: string;
  actorRole?: RoleUtilisateur;
  status?: EtatConversation;
  subject?: string;
}

/** Libelles textuels des etats : jamais une icone seule (ENF-03, US-24). */
export const LIBELLE_ETAT_MESSAGE: Record<EtatMessage, string> = {
  SENT: 'envoyé',
  DELIVERED: 'remis',
  READ: 'lu',
};

export const LIBELLE_ETAT_CONVERSATION: Record<EtatConversation, string> = {
  WAITING: 'en attente d’un agent',
  TAKEN: 'prise en charge',
  CLOSED: 'clôturée',
};
