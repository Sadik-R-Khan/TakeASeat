import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, UserRole } from '../models/api.models';
import { API_BASE_URL } from '../config/api.config';

const STORAGE_KEY = 'takeaseat_auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly authState = signal<AuthResponse | null>(this.loadStoredAuth());

  readonly currentUser = computed(() => this.authState());
  readonly isLoggedIn = computed(() => !!this.authState()?.token);
  readonly role = computed<UserRole | null>(() => this.authState()?.role ?? null);

  get token(): string | null {
    return this.authState()?.token ?? null;
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE_URL}/auth/login`, { email, password })
      .pipe(tap((res) => this.persistAuth(res)));
  }

  register(name: string, email: string, password: string, role: 'ROLE_USER' | 'ROLE_OWNER'): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE_URL}/auth/register`, { name, email, password, role })
      .pipe(tap((res) => this.persistAuth(res)));
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.authState.set(null);
    this.router.navigate(['/login']);
  }

  defaultRouteForRole(role: UserRole | null = this.role()): string {
    switch (role) {
      case 'ROLE_ADMIN':
        return '/admin';
      case 'ROLE_OWNER':
        return '/owner';
      default:
        return '/user/movies';
    }
  }

  private persistAuth(res: AuthResponse): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(res));
    this.authState.set(res);
  }

  private loadStoredAuth(): AuthResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const parsed = JSON.parse(raw) as AuthResponse;
      return parsed?.token ? parsed : null;
    } catch {
      return null;
    }
  }
}
