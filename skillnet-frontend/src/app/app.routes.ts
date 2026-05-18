import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/pages/register/register.component').then(
        (m) => m.RegisterComponent,
      ),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./core/layout/main-layout.component').then((m) => m.MainLayoutComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },

      {
        path: 'marketplace',
        loadComponent: () =>
          import('./features/marketplace/pages/marketplace/marketplace.component').then(
            (m) => m.MarketplaceComponent,
          ),
      },
      {
        path: 'marketplace/course/:slug',
        loadComponent: () =>
          import('./features/marketplace/pages/course-landing/course-landing.component').then(
            (m) => m.CourseLandingComponent,
          ),
      },
      {
        path: 'catalog',
        loadComponent: () =>
          import('./features/marketplace/pages/catalog/catalog.component').then(
            (m) => m.CatalogComponent,
          ),
      },

      {
        path: 'courses',
        loadComponent: () =>
          import('./features/courses/pages/course-list/course-list.component').then(
            (m) => m.CourseListComponent,
          ),
      },
      {
        path: 'courses/new',
        loadComponent: () =>
          import('./features/courses/pages/course-form/course-form.component').then(
            (m) => m.CourseFormComponent,
          ),
      },
      {
        path: 'courses/:id/edit',
        loadComponent: () =>
          import('./features/courses/pages/course-form/course-form.component').then(
            (m) => m.CourseFormComponent,
          ),
      },

      { path: 'home', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },

  { path: '**', redirectTo: 'login' },
];
