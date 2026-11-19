import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-wrap">
      <div class="card auth-card">
        <h1>Sign in to TakeASeat</h1>
        <p class="muted subtitle">Enter your credentials to access your dashboard.</p>

        @if (error()) {
          <div class="alert alert-error">{{ error() }}</div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="form-field">
            <label for="email">Email</label>
            <input id="email" type="email" formControlName="email" placeholder="you@example.com" />
          </div>

          <div class="form-field">
            <label for="password">Password</label>
            <input id="password" type="password" formControlName="password" placeholder="••••••" />
          </div>

          <button type="submit" class="btn btn-primary full-width" [disabled]="loading() || form.invalid">
            {{ loading() ? 'Signing in...' : 'Sign in' }}
          </button>
        </form>

        <p class="auth-footer">
          Need an account? <a routerLink="/register">Create one</a>
        </p>
      </div>
    </div>
  `,
  styles: [`
    .auth-wrap {
      max-width: 440px;
      margin: 40px auto;
      padding: 0 16px;
    }
    .auth-card h1 {
      margin: 0 0 4px;
      font-size: 20px;
    }
    .subtitle {
      margin: 0 0 18px;
    }
    .full-width {
      width: 100%;
      margin-top: 6px;
    }
    .auth-footer {
      margin: 16px 0 0;
      text-align: center;
      color: var(--text-muted);
    }
  `],
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);

    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.router.navigate([this.auth.defaultRouteForRole(res.role)]);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.error || 'Sign in failed. Check your credentials.');
      },
    });
  }
}
