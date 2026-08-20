import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AssistanceApiService } from '../../core/assistance-api.service';
import { LIBELLE_ETAT_CONVERSATION, ResumeConversation } from '../../core/models';

/**
 * Espace client : ouvrir une demande (US-23) et retrouver ses échanges (US-25).
 *
 * <p>Le compteur de messages non lus répond au critère d'US-28 : « quand un agent répond alors que
 * j'ai quitte la page, alors j'en suis informé à mon retour ». Il est calculé côté serveur à partir
 * du marqueur de lecture, et non devine côté navigateur.
 */
@Component({
  selector: 'app-client-home',
  imports: [FormsModule, RouterLink, DatePipe],
  templateUrl: './client-home.component.html',
})
export class ClientHomeComponent implements OnInit {
  private readonly api = inject(AssistanceApiService);
  private readonly router = inject(Router);

  protected readonly conversations = signal<ResumeConversation[]>([]);
  protected readonly erreur = signal<string | null>(null);
  protected sujet = '';

  ngOnInit(): void {
    this.charger();
  }

  protected ouvrir(): void {
    const sujet = this.sujet.trim();
    if (!sujet) {
      return;
    }
    this.api.ouvrirConversation(sujet).subscribe({
      next: (conversation) => void this.router.navigate(['/conversations', conversation.id]),
      error: () => this.erreur.set("La demande n'a pas pu être ouverte. Réessayez."),
    });
  }

  protected libelleStatut(conversation: ResumeConversation): string {
    return LIBELLE_ETAT_CONVERSATION[conversation.status];
  }

  private charger(): void {
    this.api.mesConversations().subscribe({
      next: (liste) => this.conversations.set(liste),
      error: () => this.erreur.set('Les conversations n’ont pas pu être chargées.'),
    });
  }
}
