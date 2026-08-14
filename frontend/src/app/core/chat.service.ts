import { Injectable, inject, signal } from '@angular/core';
import { Client, StompSubscription } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';

import { InstanceService } from './instance.service';
import { EvenementTchat } from './models';

export type EtatConnexion = 'deconnecte' | 'connexion' | 'connecte' | 'reconnexion';

/** Libelles textuels de l'etat de connexion — jamais une pastille de couleur seule (US-24). */
export const LIBELLE_ETAT_CONNEXION: Record<EtatConnexion, string> = {
  deconnecte: 'Déconnecté',
  connexion: 'Connexion en cours…',
  connecte: 'Connecté',
  reconnexion: 'Connexion perdue, reconnexion en cours…',
};

interface Abonnement {
  readonly sujet: Subject<EvenementTchat>;
  souscription?: StompSubscription;
}

/**
 * Canal temps reel : une connexion WebSocket, protocole STOMP.
 *
 * <p>Trois comportements repondent directement aux criteres d'US-24 :
 *
 * <ul>
 *   <li><b>reconnexion automatique</b> avec etat expose sous forme textuelle ;
 *   <li><b>reabonnement</b> aux memes destinations apres chaque reconnexion — sans quoi la
 *       connexion reviendrait muette ;
 *   <li><b>l'historique fait autorite</b> : ce service ne conserve aucun message. C'est le
 *       composant qui recharge l'historique persiste a chaque reconnexion, et la conversation
 *       reprend donc sans perte.
 * </ul>
 */
@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly instances = inject(InstanceService);

  readonly etat = signal<EtatConnexion>('deconnecte');
  readonly derniereErreur = signal<string | null>(null);

  private client?: Client;
  private readonly abonnements = new Map<string, Abonnement>();

  connecter(): void {
    if (this.client) {
      return;
    }
    this.etat.set('connexion');

    const client = new Client({
      brokerURL: this.instances.websocket(),
      // Le cookie de session accompagne la poignee de main : aucun jeton ne transite par le script.
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.etat.set('connecte');
        this.derniereErreur.set(null);
        this.abonnements.forEach((abonnement, destination) =>
          this.souscrire(destination, abonnement),
        );
        this.ecouterErreursPrivees();
      },
      onWebSocketClose: () => {
        this.abonnements.forEach((abonnement) => (abonnement.souscription = undefined));
        if (this.etat() !== 'deconnecte') {
          this.etat.set('reconnexion');
        }
      },
      onStompError: (trame) => {
        this.derniereErreur.set(
          trame.headers['message'] ?? 'Le canal temps réel a refusé la demande.',
        );
      },
    });

    client.activate();
    this.client = client;
  }

  deconnecter(): void {
    this.etat.set('deconnecte');
    this.abonnements.forEach((abonnement) => abonnement.sujet.complete());
    this.abonnements.clear();
    void this.client?.deactivate();
    this.client = undefined;
  }

  /**
   * Le separateur de la destination est un point : le broker traite ce qui suit `/topic/` comme
   * une cle de routage unique et refuserait un second niveau de chemin.
   */
  ecouterConversation(conversationId: string): Observable<EvenementTchat> {
    return this.ecouter(`/topic/conversations.${conversationId}`);
  }

  /** File d'attente des agents : les demandes y apparaissent et en disparaissent (US-26). */
  ecouterFileDAttente(): Observable<EvenementTchat> {
    return this.ecouter('/topic/agent-queue');
  }

  envoyer(conversationId: string, contenu: string): void {
    this.publier(`/app/conversations/${conversationId}/messages`, { body: contenu });
  }

  accuserReception(conversationId: string, messageId: string): void {
    this.publier(`/app/conversations/${conversationId}/delivered`, { messageId });
  }

  marquerLu(conversationId: string): void {
    this.publier(`/app/conversations/${conversationId}/read`, {});
  }

  private ecouter(destination: string): Observable<EvenementTchat> {
    let abonnement = this.abonnements.get(destination);
    if (!abonnement) {
      abonnement = { sujet: new Subject<EvenementTchat>() };
      this.abonnements.set(destination, abonnement);
    }
    if (this.client?.connected && !abonnement.souscription) {
      this.souscrire(destination, abonnement);
    }
    return abonnement.sujet.asObservable();
  }

  private souscrire(destination: string, abonnement: Abonnement): void {
    abonnement.souscription = this.client?.subscribe(destination, (trame) => {
      abonnement.sujet.next(JSON.parse(trame.body) as EvenementTchat);
    });
  }

  private ecouterErreursPrivees(): void {
    this.client?.subscribe('/user/queue/errors', (trame) => {
      const erreur = JSON.parse(trame.body) as { code: string; message: string };
      this.derniereErreur.set(erreur.message);
    });
  }

  private publier(destination: string, corps: unknown): void {
    if (!this.client?.connected) {
      this.derniereErreur.set('Message non envoyé : la connexion est interrompue.');
      return;
    }
    this.client.publish({ destination, body: JSON.stringify(corps) });
  }
}
