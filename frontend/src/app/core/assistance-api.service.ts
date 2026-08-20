import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { InstanceService } from './instance.service';
import { Conversation, DemandeEnAttente, Message, ResumeConversation } from './models';

/**
 * Appels REST du contexte Assistance.
 *
 * <p>Le canal REST et le canal temps réel se répartissent les rôles : REST porte ce qui doit être
 * fiable et rejouable — ouverture d'une demande, historique, file d'attente, prise en charge —, le
 * temps réel porte la livraison anticipée des messages. C'est pourquoi la reprise après coupure
 * passe par `historique()` : la base fait autorité, pas ce que le navigateur avait en mémoire.
 */
@Injectable({ providedIn: 'root' })
export class AssistanceApiService {
  private readonly http = inject(HttpClient);
  private readonly instances = inject(InstanceService);

  ouvrirConversation(sujet: string): Observable<Conversation> {
    return this.http.post<Conversation>(this.instances.api('/conversations'), { subject: sujet });
  }

  mesConversations(): Observable<ResumeConversation[]> {
    return this.http.get<ResumeConversation[]>(this.instances.api('/conversations'));
  }

  historique(conversationId: string): Observable<Message[]> {
    return this.http.get<Message[]>(
      this.instances.api(`/conversations/${conversationId}/messages`),
    );
  }

  marquerLu(conversationId: string): Observable<void> {
    return this.http.post<void>(this.instances.api(`/conversations/${conversationId}/read`), {});
  }

  cloturer(conversationId: string): Observable<Conversation> {
    return this.http.post<Conversation>(
      this.instances.api(`/conversations/${conversationId}/close`),
      {},
    );
  }

  fileDAttente(): Observable<DemandeEnAttente[]> {
    return this.http.get<DemandeEnAttente[]>(this.instances.api('/agent/queue'));
  }

  prendreEnCharge(conversationId: string): Observable<Conversation> {
    return this.http.post<Conversation>(
      this.instances.api(`/agent/conversations/${conversationId}/take`),
      {},
    );
  }
}
