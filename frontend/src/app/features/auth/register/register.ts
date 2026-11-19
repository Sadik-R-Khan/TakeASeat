import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-wrap">
      <div class="card auth-card">
        <h1>Create an account</h1>
        <p class="muted subtitle">Register as a moviegoer or a theatre owner.</p>

        @if (error()) {
          <div class="alert alert-error">{{ error() }}</div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="form-field">
            <label for="name">Full Name</label>
            <input id="name" type="text" formControlName="name" placeholder="Jane Doe" />
          </div>

          <div class="form-field">
            <label for="email">Email</label>
            <input id="email" type="email" formControlName="email" placeholder="jane@example.com" />
          </div>

          <div class="form-field">
            <label for="password">Password</label>
            <input id="password" type="password" formControlName="password" placeholder="At least 6 characters" />
          </div>

          <div class="form-field">
            <label for="role">Account Type</label>
            <select id="role" formControlName="role">
              <option value="ROLE_USER">User (Book Tickets)</option>
              <option value="ROLE_OWNER">Theatre Owner (Manage Theatres & Shows)</option>
            </select>
          </div>

          <button type="submit" class="btn btn-primary full-width" [disabled]="loading() || form.invalid">
            {{ loading() ? 'Creating account...' : 'Register' }}
          </button>
        </form>

        <p class="auth-footer">
          Already have an account? <a routerLink="/login">Sign in</a>
        </p>
      </div>
    </div>
  `,
  styles: [`
    .auth-wrap {
      max-width: 440px;
      margin: 36px auto;
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
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    role: ['ROLE_USER' as 'ROLE_USER' | 'ROLE_OWNER', [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);

    const { name, email, password, role } = this.form.getRawValue();
    this.auth.register(name, email, password, role).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.router.navigate([this.auth.defaultRouteForRole(res.role)]);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.error || 'Registration failed.');
      },
    });
  }
}
