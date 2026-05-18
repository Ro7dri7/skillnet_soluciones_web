import { Component, computed, input } from '@angular/core';

export type AuthBrandVariant = 'login' | 'register';

@Component({
  selector: 'app-auth-brand-panel',
  standalone: true,
  templateUrl: './auth-brand-panel.component.html',
})
export class AuthBrandPanelComponent {
  readonly variant = input<AuthBrandVariant>('login');

  readonly backgroundSrc = computed(() =>
    this.variant() === 'login'
      ? 'assets/images/fondo_login_skillnet.png'
      : 'assets/images/img_registrar_skillnet.png',
  );
}
