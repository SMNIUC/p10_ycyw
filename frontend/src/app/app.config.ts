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
 * Le jeton anti-rejeu est lu dans le cookie déposé par le serveur et renvoyé en en-tête à chaque
 * écriture. C'est la contrepartie nécessaire de l'authentification par cookie : le jeton de session
 * étant inaccessible au script, il part automatiquement avec chaque requête — y compris celles
 * qu'un site tiers déclencherait. Le jeton anti-rejeu, lui, ne peut être pose que par du code de
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
