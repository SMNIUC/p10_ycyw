import { Injectable, signal } from '@angular/core';

export interface Instance {
  readonly id: string;
  readonly libelle: string;
  /** Préfixe de chemin achemine vers cette instance par le serveur frontal. */
  readonly base: string;
}

export const INSTANCES: readonly Instance[] = [
  { id: 'instance-1', libelle: 'Instance 1', base: '' },
  { id: 'instance-2', libelle: 'Instance 2', base: '/instance-2' },
];

const CLE_STOCKAGE = 'ycyw-instance';

/**
 * Choix de l'instance servant la session.
 *
 * <p>Ce sélecteur n'existe que pour la démonstration : en production, un répartiteur de charge
 * distribue les connexions et personne ne choisit son instance. Il rend visible ce que
 * l'architecture affirme — un client connecté à l'instance 1 et un agent connecté à l'instance 2
 * dialoguent, parce que la diffusion passe par le broker externe et non par la mémoire d'une
 * instance.
 *
 * <p>Le préfixe de chemin sert aussi à cloisonner les cookies de session : celui de l'instance 2
 * est porté par le chemin `/instance-2`, ce qui permet d'ouvrir deux sessions distinctes dans le
 * même navigateur.
 */
@Injectable({ providedIn: 'root' })
export class InstanceService {
  readonly courante = signal<Instance>(this.chargerChoix());

  choisir(instance: Instance): void {
    localStorage.setItem(CLE_STOCKAGE, instance.id);
    this.courante.set(instance);
  }

  /** URL d'un point d'entrée REST sur l'instance courante. */
  api(chemin: string): string {
    return `${this.courante().base}/api${chemin}`;
  }

  /** URL de la poignée de main WebSocket sur l'instance courante. */
  websocket(): string {
    const protocole = window.location.protocol === 'https:' ? 'wss' : 'ws';
    return `${protocole}://${window.location.host}${this.courante().base}/ws`;
  }

  private chargerChoix(): Instance {
    const memorise = localStorage.getItem(CLE_STOCKAGE);
    return INSTANCES.find((instance) => instance.id === memorise) ?? INSTANCES[0];
  }
}
