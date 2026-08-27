import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { ShellComponent } from './features/shell/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'employees' },
      {
        path: 'employees',
        loadComponent: () =>
          import('./features/employee-directory/employee-directory.component').then(
            (m) => m.EmployeeDirectoryComponent,
          ),
      },
      {
        path: 'employees/:id',
        loadComponent: () =>
          import('./features/employee-detail/employee-detail.component').then(
            (m) => m.EmployeeDetailComponent,
          ),
      },
      {
        path: 'analytics',
        loadComponent: () =>
          import('./features/analytics-dashboard/analytics-dashboard.component').then(
            (m) => m.AnalyticsDashboardComponent,
          ),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
