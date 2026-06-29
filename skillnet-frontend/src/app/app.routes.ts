import { Routes } from '@angular/router';



import { authGuard } from './core/guards/auth.guard';

import { adminRoleGuard } from './core/guards/admin-role.guard';

import { dashboardRoleGuard } from './core/guards/dashboard-role.guard';



const producerRoleGuard = dashboardRoleGuard;



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

    path: 'verify-email',

    loadComponent: () =>

      import('./features/auth/pages/verify-email/verify-email.component').then(

        (m) => m.VerifyEmailComponent,

      ),

  },

  {

    path: 'login/2fa',

    loadComponent: () =>

      import('./features/auth/pages/login-2fa/login-2fa.component').then(

        (m) => m.Login2faComponent,

      ),

  },

  {

    path: 'password-reset',

    loadComponent: () =>

      import('./features/auth/pages/password-reset/password-reset.component').then(

        (m) => m.PasswordResetComponent,

      ),

  },

  {

    path: 'reset-password',

    loadComponent: () =>

      import('./features/auth/pages/reset-password/reset-password.component').then(

        (m) => m.ResetPasswordComponent,

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

    path: 'instructor/courses/:format/:slug/manage',

    canActivate: [authGuard],

    loadComponent: () =>

      import('./features/course-manage/layout/course-manage-layout.component').then(

        (m) => m.CourseManageLayoutComponent,

      ),

    children: [

      { path: '', redirectTo: 'curriculum', pathMatch: 'full' },

      {

        path: 'setup',

        loadComponent: () =>

          import('./features/course-manage/pages/manage-setup/manage-setup.component').then(

            (m) => m.ManageSetupComponent,

          ),

      },

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

    path: 'instructor/courses/:slug/manage',

    canActivate: [authGuard],

    loadComponent: () =>

      import('./features/course-manage/layout/course-manage-layout.component').then(

        (m) => m.CourseManageLayoutComponent,

      ),

    children: [

      { path: '', redirectTo: 'curriculum', pathMatch: 'full' },

      { path: 'setup', loadComponent: () => import('./features/course-manage/pages/manage-setup/manage-setup.component').then((m) => m.ManageSetupComponent) },

      { path: 'curriculum', loadComponent: () => import('./features/course-manage/pages/manage-curriculum/manage-curriculum.component').then((m) => m.ManageCurriculumComponent) },

      { path: 'audience', loadComponent: () => import('./features/course-manage/pages/manage-audience/manage-audience.component').then((m) => m.ManageAudienceComponent) },

      { path: 'basics', loadComponent: () => import('./features/course-manage/pages/manage-basics/manage-basics.component').then((m) => m.ManageBasicsComponent) },

      { path: 'pricing', loadComponent: () => import('./features/course-manage/pages/manage-pricing/manage-pricing.component').then((m) => m.ManagePricingComponent) },

      { path: 'promotions', loadComponent: () => import('./features/course-manage/pages/manage-promotions/manage-promotions.component').then((m) => m.ManagePromotionsComponent) },

      { path: 'messages', loadComponent: () => import('./features/course-manage/pages/manage-messages/manage-messages.component').then((m) => m.ManageMessagesComponent) },

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

    loadComponent: () =>

      import('./core/layout/main-layout.component').then((m) => m.MainLayoutComponent),

    children: [

      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },



      {

        path: 'marketplace',

        loadComponent: () =>

          import('./features/marketplace/pages/marketplace/marketplace.component').then(

            (m) => m.MarketplaceComponent,

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

        path: 'marketplace/course/:format/:slug',

        loadComponent: () =>

          import('./features/marketplace/pages/course-landing/course-landing.component').then(

            (m) => m.CourseLandingComponent,

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

        path: 'infoproductor/student-progress',

        canActivate: [authGuard, producerRoleGuard],

        data: { requiredRole: 'infoproductor' },

        loadComponent: () =>

          import('./features/producer/pages/producer-students/producer-students.component').then(

            (m) => m.ProducerStudentsComponent,

          ),

      },

      {

        path: 'infoproductor/students',

        redirectTo: 'infoproductor/student-progress',

        pathMatch: 'full',

      },

      {

        path: 'infoproductor/quiz-review',

        canActivate: [authGuard, producerRoleGuard],

        data: { requiredRole: 'infoproductor' },

        loadComponent: () =>

          import('./features/producer/pages/producer-students/producer-students.component').then(

            (m) => m.ProducerStudentsComponent,

          ),

      },

      {

        path: 'infoproductor/traffic',

        canActivate: [authGuard, producerRoleGuard],

        data: { requiredRole: 'infoproductor' },

        loadComponent: () =>

          import('./features/producer/pages/producer-traffic/producer-traffic.component').then(

            (m) => m.ProducerTrafficComponent,

          ),

      },

      {

        path: 'infoproductor/plans',

        canActivate: [authGuard, producerRoleGuard],

        data: { requiredRole: 'infoproductor' },

        loadComponent: () =>

          import('./features/producer/pages/producer-plans/producer-plans.component').then(

            (m) => m.ProducerPlansComponent,

          ),

      },

      {

        path: 'infoproductor/:username',

        loadComponent: () =>

          import('./features/producer/pages/public-infoproductor-profile/public-infoproductor-profile.component').then(

            (m) => m.PublicInfoproductorProfileComponent,

          ),

      },



      {

        path: 'admin',

        canActivate: [authGuard, adminRoleGuard],

        children: [

          {

            path: '',

            loadComponent: () =>

              import('./features/admin/pages/admin-dashboard/admin-dashboard.component').then(

                (m) => m.AdminDashboardComponent,

              ),

          },

          {

            path: 'users',

            loadComponent: () =>

              import('./features/admin/pages/admin-users/admin-users.component').then(

                (m) => m.AdminUsersComponent,

              ),

          },

          {

            path: 'courses',

            loadComponent: () =>

              import('./features/admin/pages/admin-courses/admin-courses.component').then(

                (m) => m.AdminCoursesComponent,

              ),

          },

          {

            path: 'audit-log',

            loadComponent: () =>

              import('./features/admin/pages/admin-audit-log/admin-audit-log.component').then(

                (m) => m.AdminAuditLogComponent,

              ),

          },

          {

            path: 'enrollments',

            loadComponent: () =>

              import('./features/admin/pages/admin-enrollments/admin-enrollments.component').then(

                (m) => m.AdminEnrollmentsComponent,

              ),

          },

          {

            path: 'service-offerings',

            loadComponent: () =>

              import('./features/admin/pages/admin-service-offerings/admin-service-offerings.component').then(

                (m) => m.AdminServiceOfferingsComponent,

              ),

          },

        ],

      },



      {

        path: 'dashboard',

        canActivate: [authGuard],

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

        path: 'marketplace/course/:format/:slug/learn',

        canActivate: [authGuard],

        loadComponent: () =>

          import('./features/learning/pages/course-learn/course-learn.component').then(

            (m) => m.CourseLearnComponent,

          ),

      },

      {

        path: 'marketplace/course/:slug/learn',

        canActivate: [authGuard],

        loadComponent: () =>

          import('./features/learning/pages/course-learn/course-learn.component').then(

            (m) => m.CourseLearnComponent,

          ),

      },

      {

        path: 'checkout',

        canActivate: [authGuard],

        loadComponent: () =>

          import('./features/checkout/pages/checkout/checkout.component').then(

            (m) => m.CheckoutComponent,

          ),

      },

      {

        path: 'payment/success',

        canActivate: [authGuard],

        loadComponent: () =>

          import('./features/payment/pages/payment-success/payment-success.component').then(

            (m) => m.PaymentSuccessComponent,

          ),

      },

      {

        path: 'profile',

        canActivate: [authGuard],

        loadComponent: () =>

          import('./features/profile/pages/profile/profile.component').then(

            (m) => m.ProfileComponent,

          ),

      },

      {

        path: 'certificates',

        canActivate: [authGuard],

        loadComponent: () =>

          import('./features/certificates/pages/certificates/certificates.component').then(

            (m) => m.CertificatesComponent,

          ),

      },

      {

        path: 'mis-cursos',

        canActivate: [authGuard, dashboardRoleGuard],

        data: { requiredRole: 'student' },

        loadComponent: () =>

          import('./features/student/pages/my-products/my-products.component').then(

            (m) => m.MyProductsComponent,

          ),

      },



      {

        path: 'courses',

        canActivate: [authGuard],

        loadComponent: () =>

          import('./features/courses/pages/course-list/course-list.component').then(

            (m) => m.CourseListComponent,

          ),

      },

      {

        path: 'courses/:id/edit',

        canActivate: [authGuard],

        loadComponent: () =>

          import('./features/courses/pages/course-form/course-form.component').then(

            (m) => m.CourseFormComponent,

          ),

      },



      {

        path: 'infoproductor/courses/new',

        canActivate: [authGuard],

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


