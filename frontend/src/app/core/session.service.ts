import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { InstanceService } from './instance.service';
import { Session } from './models';

/**
 * Session de l'utilisateur.
 *
 * <p>Le jeton n'apparaît jamais ici : il vit dans un cookie inaccessible au script. L'application
 * cliente ne connaît que l'identité affichable — nom et rôle — et n'a aucun moyen de divulguer le
 * jeton, même si une injection de script parvenait à s'executer.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly http = inject(HttpClient);
  private readonly instances = inject(InstanceService);

  readonly session = signal<Session | null>(null);

  /**
   * Premier appel de l'application : il renseigne la session et provoque le dépôt du cookie
   * portant le jeton anti-rejeu, sans lequel aucune écriture ne serait acceptée ensuite.
   */
  amorcer(): Observable<Session> {
    return this.http
      .get<Session>(this.instances.api('/auth/session'))
      .pipe(tap((session) => this.session.set(session)));
  }

  connexion(email: string, motDePasse: string): Observable<Session> {
    return this.http
      .post<Session>(this.instances.api('/auth/login'), { email, password: motDePasse })
      .pipe(tap((session) => this.session.set(session)));
  }

  deconnexion(): Observable<void> {
    return this.http
      .post<void>(this.instances.api('/auth/logout'), {})
      .pipe(tap(() => this.session.set(null)));
  }

  estAgent(): boolean {
    return this.session()?.role === 'AGENT';
  }

  identifiant(): string | null {
    return this.session()?.userId ?? null;
  }
}
