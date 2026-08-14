import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';

import { ChatService, LIBELLE_ETAT_CONNEXION } from './core/chat.service';
import { SessionService } from './core/session.service';

/**
 * Cadre de l'application.
 *
 * <p>Il porte trois elements d'accessibilite systematiques : un lien d'evitement vers le contenu,
 * une hierarchie de titres unique, et l'etat de connexion annonce en <b>region d'etat</b> — donc
 * sans deplacer le focus de l'utilisateur, qui peut etre en train de saisir un message.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.component.html',
})
export class AppComponent implements OnInit {
  protected readonly session = inject(SessionService);
  private readonly chat = inject(ChatService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    // Premier appel : il renseigne la session et amorce le jeton anti-rejeu.
    this.session.amorcer().subscribe({
      next: (session) => {
        if (session.authenticated) {
          this.chat.connecter();
        }
      },
      error: () => undefined,
    });
  }

  protected libelleConnexion(): string {
    return LIBELLE_ETAT_CONNEXION[this.chat.etat()];
  }

  protected seDeconnecter(): void {
    this.chat.deconnecter();
    this.session.deconnexion().subscribe({
      next: () => void this.router.navigate(['/connexion']),
      error: () => void this.router.navigate(['/connexion']),
    });
  }
}
