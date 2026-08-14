import { DatePipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { AssistanceApiService } from '../../core/assistance-api.service';
import { ChatService, LIBELLE_ETAT_CONNEXION } from '../../core/chat.service';
import { SessionService } from '../../core/session.service';
import {
  EtatConversation,
  EvenementTchat,
  LIBELLE_ETAT_CONVERSATION,
  LIBELLE_ETAT_MESSAGE,
  Message,
} from '../../core/models';

/**
 * Le composant de messagerie — le plus exigeant du point de vue de l'accessibilite, et le canal
 * d'assistance principal des personnes sourdes ou malentendantes.
 *
 * <p>Quatre choix repondent point par point aux criteres d'US-24 :
 *
 * <ol>
 *   <li>la conversation est une <b>region de journal</b> en annonce non intrusive : un message qui
 *       arrive est lu par le lecteur d'ecran <b>sans que le focus soit deplace</b>. Deplacer le
 *       focus interromprait la saisie en cours — c'est l'erreur classique de ces composants ;
 *   <li>l'etat de chaque message — envoye, remis, lu — est <b>ecrit en toutes lettres</b>, jamais
 *       porte par une icone ou une couleur seule ;
 *   <li>tout est atteignable au clavier : saisie, envoi par la touche Entree, parcours de
 *       l'historique par tabulation ;
 *   <li>a chaque reconnexion, l'historique <b>persiste</b> est recharge : la conversation reprend
 *       sans perte, meme si des messages ont ete emis pendant la coupure.
 * </ol>
 */
@Component({
  selector: 'app-conversation',
  imports: [FormsModule, DatePipe],
  templateUrl: './conversation.component.html',
})
export class ConversationComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(AssistanceApiService);
  private readonly chat = inject(ChatService);
  private readonly session = inject(SessionService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly conversationId = this.route.snapshot.paramMap.get('id') ?? '';

  protected readonly messages = signal<Message[]>([]);
  protected readonly statut = signal<EtatConversation>('WAITING');
  protected readonly erreur = signal<string | null>(null);
  protected brouillon = '';

  constructor() {
    this.chat.connecter();
    this.chargerHistorique();

    this.chat
      .ecouterConversation(this.conversationId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((evenement) => this.appliquer(evenement));

    // A chaque passage a l'etat connecte — premiere connexion comme reconnexion —, l'historique
    // persiste est recharge. C'est ce qui rend vrai « la conversation reprend sans perte ».
    effect(() => {
      if (this.chat.etat() === 'connecte') {
        this.chargerHistorique();
        this.chat.marquerLu(this.conversationId);
      }
    });

    effect(() => {
      const erreurCanal = this.chat.derniereErreur();
      if (erreurCanal) {
        this.erreur.set(erreurCanal);
      }
    });
  }

  protected libelleConnexion(): string {
    return LIBELLE_ETAT_CONNEXION[this.chat.etat()];
  }

  protected libelleStatut(): string {
    return LIBELLE_ETAT_CONVERSATION[this.statut()];
  }

  protected libelleEtat(message: Message): string {
    return LIBELLE_ETAT_MESSAGE[message.state];
  }

  protected estDeMoi(message: Message): boolean {
    return message.authorId === this.session.identifiant();
  }

  protected estAgent(): boolean {
    return this.session.estAgent();
  }

  protected conversationCloturee(): boolean {
    return this.statut() === 'CLOSED';
  }

  protected auteur(message: Message): string {
    if (this.estDeMoi(message)) {
      return 'Vous';
    }
    return message.authorRole === 'AGENT' ? 'Agent du service client' : 'Client';
  }

  protected envoyer(): void {
    const contenu = this.brouillon.trim();
    if (!contenu || this.conversationCloturee()) {
      return;
    }
    this.erreur.set(null);
    this.chat.envoyer(this.conversationId, contenu);
    this.brouillon = '';
  }

  /** Entree envoie, Maj + Entree va a la ligne : la saisie reste entierement au clavier. */
  protected envoyerAuClavier(evenement: Event): void {
    const clavier = evenement as KeyboardEvent;
    if (clavier.shiftKey) {
      return;
    }
    clavier.preventDefault();
    this.envoyer();
  }

  protected cloturer(): void {
    this.api.cloturer(this.conversationId).subscribe({
      next: (conversation) => this.statut.set(conversation.status),
      error: () => this.erreur.set('La clôture a échoué.'),
    });
  }

  private chargerHistorique(): void {
    this.api.historique(this.conversationId).subscribe({
      next: (messages) => {
        this.messages.set(messages);
        this.accuserReceptionDesMessagesRecus(messages);
      },
      error: () => this.erreur.set('L’historique n’a pas pu être chargé.'),
    });
    this.api.mesConversations().subscribe({
      next: (liste) => {
        const courante = liste.find((conversation) => conversation.id === this.conversationId);
        if (courante) {
          this.statut.set(courante.status);
        }
      },
      error: () => undefined,
    });
  }

  private appliquer(evenement: EvenementTchat): void {
    switch (evenement.type) {
      case 'MESSAGE_POSTED': {
        const message = evenement.message;
        if (!message || this.messages().some((connu) => connu.id === message.id)) {
          return;
        }
        this.messages.update((liste) => [...liste, message]);
        if (!this.estDeMoi(message)) {
          // La conversation est ouverte a l'ecran : le message est recu, puis lu.
          this.chat.accuserReception(this.conversationId, message.id);
          this.chat.marquerLu(this.conversationId);
        }
        break;
      }
      case 'MESSAGE_DELIVERED':
        this.majEtats([evenement.messageId ?? ''], 'DELIVERED');
        break;
      case 'CONVERSATION_READ':
        this.majEtats(evenement.messageIds ?? [], 'READ');
        break;
      case 'CONVERSATION_TAKEN':
      case 'CONVERSATION_CLOSED':
        if (evenement.status) {
          this.statut.set(evenement.status);
        }
        break;
      case 'CONVERSATION_OPENED':
        break;
    }
  }

  /**
   * L'etat d'un message ne redescend jamais : un accuse de reception arrive apres un accuse de
   * lecture — l'ordre reseau ne garantit rien — ne doit pas faire regresser l'affichage. La regle
   * est la meme cote serveur, dans le domaine.
   */
  private majEtats(identifiants: string[], etat: 'DELIVERED' | 'READ'): void {
    if (identifiants.length === 0) {
      return;
    }
    const rang = { SENT: 0, DELIVERED: 1, READ: 2 } as const;
    this.messages.update((liste) =>
      liste.map((message) =>
        identifiants.includes(message.id) && rang[message.state] < rang[etat]
          ? { ...message, state: etat }
          : message,
      ),
    );
  }

  private accuserReceptionDesMessagesRecus(messages: Message[]): void {
    messages
      .filter((message) => !this.estDeMoi(message) && message.state === 'SENT')
      .forEach((message) => this.chat.accuserReception(this.conversationId, message.id));
  }
}
