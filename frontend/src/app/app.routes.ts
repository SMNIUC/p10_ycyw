import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'connexion',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
    title: 'Connexion — Assistance Your Car Your Way',
  },
  {
    path: 'mes-conversations',
    loadComponent: () =>
      import('./features/client/client-home.component').then((m) => m.ClientHomeComponent),
    title: 'Mes conversations — Assistance Your Car Your Way',
  },
  {
    path: 'file-attente',
    loadComponent: () =>
      import('./features/agent/agent-queue.component').then((m) => m.AgentQueueComponent),
    title: "File d'attente — Assistance Your Car Your Way",
  },
  {
    path: 'conversations/:id',
    loadComponent: () =>
      import('./features/chat/conversation.component').then((m) => m.ConversationComponent),
    title: 'Conversation — Assistance Your Car Your Way',
  },
  { path: '', pathMatch: 'full', redirectTo: 'connexion' },
  { path: '**', redirectTo: 'connexion' },
];
