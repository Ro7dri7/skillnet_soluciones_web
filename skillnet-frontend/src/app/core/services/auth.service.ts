import { HttpClient } from '@angular/common/http';
import { Injectable, WritableSignal, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { Observable, tap } from 'rxjs';
import { dashboardPathForRole } from '../../shared/utils/user-role.util';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  GoogleLoginRequest,
  LoginRequest,
  RegisterRequest,
  User,
} from '../../shared/models/auth.model';
import { resolveUserRole, isAdminAccount } from '../../shared/utils/user-role.util';
import { hasValidSessionToken } from '../../shared/utils/return-url.util';

const TOKEN_STORAGE_KEY = 'skillnet_token';
const USER_STORAGE_KEY = 'skillnet_user';

export type AppRole = 'student' | 'infoproductor' | 'admin';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  private readonly currentUserSignal: WritableSignal<User | null> = signal(this.loadStoredUser());

  /** Señal reactiva del usuario en sesión. */
  readonly currentUser = this.currentUserSignal.asReadonly();

  /** Observable derivado de la señal (compatible con `async` pipe). */
  readonly currentUser$ = toObservable(this.currentUser);

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/login`, credentials)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  loginWithGoogle(token: string): Observable<AuthResponse> {
    const body: GoogleLoginRequest = { token };
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/google`, body)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/register`, data)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  setToken(token: string): void {
    localStorage.setItem(TOKEN_STORAGE_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    this.currentUserSignal.set(null);
  }

  isLoggedIn(): boolean {
    return hasValidSessionToken(this.getToken());
  }

  getCurrentUser(): User | null {
    return this.currentUserSignal();
  }

  /**
   * Cambia el rol activo: persiste en BD, emite un JWT nuevo y actualiza la sesión local.
   */
  switchRole(newRole: AppRole): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/switch-role`, { role: newRole })
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  /** Ruta del panel según el rol activo tras login o cambio de rol. */
  dashboardPathForCurrentUser(): string {
    const user = this.currentUserSignal();
    return dashboardPathForRole(user?.role ?? user?.activeRole);
  }

  /** @deprecated Usar `switchRole` (retorna Observable). */
  switchActiveRole(role: 'student' | 'infoproductor'): Observable<AuthResponse> {
    return this.switchRole(role);
  }

  private handleAuthSuccess(response: AuthResponse): void {
    this.setToken(response.token);
    const user = this.normalizeUser(response.user, response.token);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
    this.currentUserSignal.set(user);
  }

  private loadStoredUser(): User | null {
    const stored = localStorage.getItem(USER_STORAGE_KEY);
    if (!stored) {
      return null;
    }
    try {
      const parsed = JSON.parse(stored) as User;
      return this.normalizeUser(parsed);
    } catch {
      localStorage.removeItem(USER_STORAGE_KEY);
      return null;
    }
  }

    private normalizeUser(user: User, token?: string): User {
    const jwt = token ?? this.getToken();
    const role = resolveUserRole(user, jwt) ?? user.activeRole ?? user.role;
    if (!role) {
      return user;
    }
    const isAdmin = role === 'admin' || isAdminAccount(user, jwt);
    return {
      ...user,
      role,
      activeRole: role,
      superUser: user.superUser,
      staff: user.staff,
      student: isAdmin ? user.student === true : user.student !== false,
      infoproductor: isAdmin ? user.infoproductor === true : user.infoproductor !== false,
    };
  }
}
