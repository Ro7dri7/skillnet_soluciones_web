import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { dashboardRoleGuard } from './core/guards/dashboard-role.guard';

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
    path: 'infoproductor/courses/new/audience',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/course-builder/pages/builder-manage-redirect/builder-manage-redirect.component').then(
        (m) => m.BuilderManageRedirectComponent,
      ),
    data: { manageStep: 'audience' },
  },
  {
    path: 'infoproductor/courses/new/curriculum',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/course-builder/pages/builder-manage-redirect/builder-manage-redirect.component').then(
        (m) => m.BuilderManageRedirectComponent,
      ),
    data: { manageStep: 'curriculum' },
  },
  {
    path: 'instructor/courses/:id/manage',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/course-manage/layout/course-manage-layout.component').then(
        (m) => m.CourseManageLayoutComponent,
      ),
    children: [
      { path: '', redirectTo: 'curriculum', pathMatch: 'full' },
      {
        path: 'curriculum',
        loadComponent: () =>
          import('./features/course-manage/pages/manage-curriculum/manage-curriculum.component').then(
            (m) => m.ManageCurriculumComponent,
          ),
      },
      {
        path: 'audience',
        loadComponent: () =>
          import('./features/course-manage/pages/manage-audience/manage-audience.component').then(
            (m) => m.ManageAudienceComponent,
          ),
      },
      {
        path: 'basics',
        loadComponent: () =>
          import('./features/course-manage/pages/manage-basics/manage-basics.component').then(
            (m) => m.ManageBasicsComponent,
          ),
      },
      {
        path: 'pricing',
        loadComponent: () =>
          import('./features/course-manage/pages/manage-pricing/manage-pricing.component').then(
            (m) => m.ManagePricingComponent,
          ),
      },
      {
        path: 'promotions',
        loadComponent: () =>
          import('./features/course-manage/pages/manage-promotions/manage-promotions.component').then(
            (m) => m.ManagePromotionsComponent,
          ),
      },
      {
        path: 'messages',
        loadComponent: () =>
          import('./features/course-manage/pages/manage-messages/manage-messages.component').then(
            (m) => m.ManageMessagesComponent,
          ),
      },
    ],
  },
  {
    path: 'build/type',
    redirectTo: 'infoproductor/courses/new/type',
    pathMatch: 'full',
  },
  {
    path: 'build/title',
    redirectTo: 'infoproductor/courses/new/type',
    pathMatch: 'full',
  },
  {
    path: 'build/category',
    redirectTo: 'infoproductor/courses/new/category',
    pathMatch: 'full',
  },
  {
    path: 'build/subcategory',
    redirectTo: 'infoproductor/courses/new/subcategory',
    pathMatch: 'full',
  },
  {
    path: 'build/audience',
    redirectTo: 'infoproductor/courses/new/audience',
    pathMatch: 'full',
  },
  {
    path: 'build/curriculum',
    redirectTo: 'infoproductor/courses/new/curriculum',
    pathMatch: 'full',
  },
  {
    path: 'build',
    redirectTo: 'infoproductor/courses/new/type',
    pathMatch: 'full',
  },
  {
    path: 'courses/new/type',
    redirectTo: 'infoproductor/courses/new/type',
    pathMatch: 'full',
  },
  {
    path: 'courses/new/title',
    redirectTo: 'infoproductor/courses/new/type',
    pathMatch: 'full',
  },
  {
    path: 'courses/new/category',
    redirectTo: 'infoproductor/courses/new/category',
    pathMatch: 'full',
  },
  {
    path: 'courses/new/subcategory',
    redirectTo: 'infoproductor/courses/new/subcategory',
    pathMatch: 'full',
  },
  {
    path: 'courses/new/audience',
    redirectTo: 'infoproductor/courses/new/audience',
    pathMatch: 'full',
  },
  {
    path: 'courses/new/curriculum',
    redirectTo: 'infoproductor/courses/new/curriculum',
    pathMatch: 'full',
  },
  {
    path: 'courses/new',
    redirectTo: 'infoproductor/courses/new/type',
    pathMatch: 'full',
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
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/dashboard/pages/dashboard-redirect/dashboard-redirect.component').then(
                (m) => m.DashboardRedirectComponent,
              ),
          },
          {
            path: 'estudiante',
            canActivate: [dashboardRoleGuard],
            data: { requiredRole: 'student' },
            loadComponent: () =>
              import(
                './features/dashboard/pages/student-dashboard-page/student-dashboard-page.component'
              ).then((m) => m.StudentDashboardPageComponent),
          },
          {
            path: 'infoproductor',
            canActivate: [dashboardRoleGuard],
            data: { requiredRole: 'infoproductor' },
            loadComponent: () =>
              import(
                './features/dashboard/pages/producer-dashboard-page/producer-dashboard-page.component'
              ).then((m) => m.ProducerDashboardPageComponent),
          },
        ],
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
        path: 'checkout',
        loadComponent: () =>
          import('./features/checkout/pages/checkout/checkout.component').then(
            (m) => m.CheckoutComponent,
          ),
      },
      {
        path: 'mis-cursos',
        canActivate: [dashboardRoleGuard],
        data: { requiredRole: 'student' },
        loadComponent: () =>
          import('./features/student/pages/my-products/my-products.component').then(
            (m) => m.MyProductsComponent,
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
        path: 'courses/:id/edit',
        loadComponent: () =>
          import('./features/courses/pages/course-form/course-form.component').then(
            (m) => m.CourseFormComponent,
          ),
      },

      {
        path: 'infoproductor/courses/new',
        loadComponent: () =>
          import('./features/course-builder/layout/wizard-layout.component').then(
            (m) => m.WizardLayoutComponent,
          ),
        children: [
          { path: '', redirectTo: 'type', pathMatch: 'full' },
          {
            path: 'type',
            loadComponent: () =>
              import('./features/course-builder/pages/builder-type-step/builder-type-step.component').then(
                (m) => m.BuilderTypeStepComponent,
              ),
          },
          {
            path: 'title',
            loadComponent: () =>
              import('./features/course-builder/pages/builder-title-step/builder-title-step.component').then(
                (m) => m.BuilderTitleStepComponent,
              ),
          },
          {
            path: 'category',
            loadComponent: () =>
              import('./features/course-builder/pages/builder-category-step/builder-category-step.component').then(
                (m) => m.BuilderCategoryStepComponent,
              ),
          },
          {
            path: 'subcategory',
            loadComponent: () =>
              import('./features/course-builder/pages/builder-subcategory-step/builder-subcategory-step.component').then(
                (m) => m.BuilderSubcategoryStepComponent,
              ),
          },
        ],
      },

      { path: 'home', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },

  { path: '**', redirectTo: 'login' },
];
