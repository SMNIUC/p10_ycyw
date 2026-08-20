import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { ChatService } from '../../core/chat.service';
import { INSTANCES, Instance, InstanceService } from '../../core/instance.service';
import { SessionService } from '../../core/session.service';

/**
 * Connexion.
 *
 * <p>Le choix de l'instance ne figure ici que pour la démonstration : connecter le client à
 * l'instance 1 et l'agent à l'instance 2, puis constater qu'ils dialoguent, est la manière la plus
 * directe de vérifier que la diffusion passe bien par le broker externe.
 */
@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private readonly session = inject(SessionService);
  private readonly instanceService = inject(InstanceService);
  private readonly chat = inject(ChatService);
  private readonly router = inject(Router);

  protected readonly instances = INSTANCES;
  protected instanceChoisie: Instance = this.instanceService.courante();
  protected email = '';
  protected motDePasse = '';
  protected readonly erreur = signal<string | null>(null);
  protected readonly enCours = signal(false);

  protected seConnecter(): void {
    this.erreur.set(null);
    this.enCours.set(true);
    this.instanceService.choisir(this.instanceChoisie);

    // Le jeton anti-rejeu doit provenir de l'instance choisie : on la réinterroge avant d'écrire.
    this.session.amorcer().subscribe({
      next: () => this.envoyerLesIdentifiants(),
      error: () => this.envoyerLesIdentifiants(),
    });
  }

  private envoyerLesIdentifiants(): void {
    this.session.connexion(this.email, this.motDePasse).subscribe({
      next: (session) => {
        this.enCours.set(false);
        this.chat.connecter();
        void this.router.navigate([session.role === 'AGENT' ? '/file-attente' : '/mes-conversations']);
      },
      error: () => {
        this.enCours.set(false);
        this.erreur.set('Adresse électronique ou mot de passe incorrect.');
      },
    });
  }
}
