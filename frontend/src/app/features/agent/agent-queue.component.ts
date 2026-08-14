import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';

import { AssistanceApiService } from '../../core/assistance-api.service';
import { ChatService } from '../../core/chat.service';
import { DemandeEnAttente, ResumeConversation } from '../../core/models';

/**
 * Poste de l'agent : file d'attente et conversations en cours (US-26).
 *
 * <p>La file se met a jour <b>par le canal temps reel</b> : une demande ouverte sur une autre
 * instance y apparait, et une demande prise par un collegue en disparait sans qu'aucun agent n'ait
 * a recharger sa page. C'est la meme diffusion que celle des messages, sur une autre destination.
 */
@Component({
  selector: 'app-agent-queue',
  imports: [RouterLink, DatePipe],
  templateUrl: './agent-queue.component.html',
})
export class AgentQueueComponent implements OnInit {
  private readonly api = inject(AssistanceApiService);
  private readonly chat = inject(ChatService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly file = signal<DemandeEnAttente[]>([]);
  protected readonly mesConversations = signal<ResumeConversation[]>([]);
  protected readonly erreur = signal<string | null>(null);

  ngOnInit(): void {
    this.charger();
    this.chat.connecter();

    this.chat
      .ecouterFileDAttente()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.charger());
  }

  protected prendreEnCharge(demande: DemandeEnAttente): void {
    this.api.prendreEnCharge(demande.id).subscribe({
      next: (conversation) => void this.router.navigate(['/conversations', conversation.id]),
      error: (reponse: { status?: number }) => {
        // 409 : un autre agent a ete plus rapide. Le refus vient du domaine, pas de l'interface.
        this.erreur.set(
          reponse.status === 409
            ? 'Cette demande vient d’être prise en charge par un autre agent.'
            : 'La prise en charge a échoué.',
        );
        this.charger();
      },
    });
  }

  protected attente(demande: DemandeEnAttente): string {
    const minutes = Math.floor(demande.waitingSeconds / 60);
    const secondes = demande.waitingSeconds % 60;
    return minutes > 0 ? `${minutes} min ${secondes} s` : `${secondes} s`;
  }

  private charger(): void {
    this.api.fileDAttente().subscribe({
      next: (file) => this.file.set(file),
      error: () => this.erreur.set('La file d’attente n’a pas pu être chargée.'),
    });
    this.api.mesConversations().subscribe({
      next: (liste) => this.mesConversations.set(liste),
      error: () => undefined,
    });
  }
}
