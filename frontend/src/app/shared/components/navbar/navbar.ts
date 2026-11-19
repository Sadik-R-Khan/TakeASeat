import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="navbar">
      <div class="navbar-inner">
        <a class="brand" [routerLink]="auth.isLoggedIn() ? auth.defaultRouteForRole() : '/login'">
          TakeASeat
        </a>

        @if (auth.currentUser(); as user) {
          <nav class="nav-links">
            @if (user.role === 'ROLE_USER') {
              <a routerLink="/user/movies" routerLinkActive="active">Movies</a>
              <a routerLink="/user/bookings" routerLinkActive="active">My Bookings</a>
            }
            @if (user.role === 'ROLE_OWNER') {
              <a routerLink="/owner" routerLinkActive="active">Owner Panel</a>
              <a routerLink="/user/movies" routerLinkActive="active">Browse Movies</a>
            }
            @if (user.role === 'ROLE_ADMIN') {
              <a routerLink="/admin" routerLinkActive="active">Admin Panel</a>
              <a routerLink="/user/movies" routerLinkActive="active">Browse Movies</a>
            }
          </nav>

          <div class="nav-user">
            <span class="user-name">{{ user.name }}</span>
            <span class="badge badge-neutral">{{ formatRole(user.role) }}</span>
            <button type="button" class="btn btn-sm" (click)="auth.logout()">Sign out</button>
          </div>
        } @else {
          <div class="nav-user">
            <a routerLink="/login" routerLinkActive="active" class="btn btn-sm">Sign in</a>
            <a routerLink="/register" routerLinkActive="active" class="btn btn-sm btn-primary">Register</a>
          </div>
        }
      </div>
    </header>
  `,
  styles: [`
    .navbar {
      background: #ffffff;
      border-bottom: 1px solid var(--border);
      position: sticky;
      top: 0;
      z-index: 20;
    }
    .navbar-inner {
      max-width: 1120px;
      margin: 0 auto;
      padding: 0 20px;
      height: 54px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
    }
    .brand {
      font-size: 17px;
      font-weight: 700;
      color: var(--text);
      letter-spacing: -0.02em;
      text-decoration: none;
    }
    .nav-links {
      display: flex;
      gap: 18px;
      align-items: center;
    }
    .nav-links a {
      color: var(--text-muted);
      font-weight: 500;
      padding: 6px 2px;
      border-bottom: 2px solid transparent;
      text-decoration: none;
    }
    .nav-links a.active, .nav-links a:hover {
      color: var(--text);
    }
    .nav-links a.active {
      border-bottom-color: var(--primary);
    }
    .nav-user {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .user-name {
      font-weight: 500;
      color: var(--text);
    }
    @media (max-width: 680px) {
      .user-name { display: none; }
    }
  `],
})
export class NavbarComponent {
  readonly auth = inject(AuthService);

  formatRole(role: string): string {
    return role.replace('ROLE_', '');
  }
}
