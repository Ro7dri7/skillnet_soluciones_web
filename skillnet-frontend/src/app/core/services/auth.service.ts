import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  GoogleLoginRequest,
  LoginRequest,
  RegisterRequest,
  User,
} from '../../shared/models/auth.model';
import { resolveUserRole } from '../../shared/utils/user-role.util';

const TOKEN_STORAGE_KEY = 'skillnet_token';
const USER_STORAGE_KEY = 'skillnet_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  private readonly currentUserSubject = new BehaviorSubject<User | null>(this.loadStoredUser());
  readonly currentUser$ = this.currentUserSubject.asObservable();

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
    this.currentUserSubject.next(null);
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  private handleAuthSuccess(response: AuthResponse): void {
    this.setToken(response.token);
    const user = this.normalizeUser(response.user, response.token);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
    this.currentUserSubject.next(user);
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

  /** Asegura `role` desde API, flags legacy o claim JWT (sesiones antiguas). */
  private normalizeUser(user: User, token?: string): User {
    const role = resolveUserRole(user, token ?? this.getToken());
    return role ? { ...user, role } : user;
  }
}
