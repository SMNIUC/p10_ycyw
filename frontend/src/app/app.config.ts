import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { provideHttpClient, withXsrfConfiguration } from '@angular/common/http';
import { ApplicationConfig, LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';

registerLocaleData(localeFr);

/**
 * Configuration de l'application cliente.
 *
 * Le jeton anti-rejeu est lu dans le cookie depose par le serveur et renvoye en en-tete a chaque
 * ecriture. C'est la contrepartie necessaire de l'authentification par cookie : le jeton de session
 * etant inaccessible au script, il part automatiquement avec chaque requete — y compris celles
 * qu'un site tiers declencherait. Le jeton anti-rejeu, lui, ne peut etre pose que par du code de
 * cette application.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
    ),
    { provide: LOCALE_ID, useValue: 'fr-FR' },
  ],
};
