import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-auth-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './auth-navbar.component.html',
})
export class AuthNavbarComponent {}
