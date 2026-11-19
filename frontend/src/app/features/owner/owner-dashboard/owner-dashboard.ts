import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { Movie, Show, Theatre } from '../../../core/models/api.models';

@Component({
  selector: 'app-owner-dashboard',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe],
  template: `
    <div class="container">
      <div class="page-header">
        <div>
          <h1>Theatre Owner Panel</h1>
          <p>Manage your theatres (fixed 5×10 seat layout: 1A–5J) and schedule movie showtimes.</p>
        </div>
      </div>

      @if (error()) {
        <div class="alert alert-error">{{ error() }}</div>
      }
      @if (message()) {
        <div class="alert alert-success">{{ message() }}</div>
      }

      <div class="tabs">
        <button
          type="button"
          class="tab-btn"
          [class.active]="activeTab() === 'theatres'"
          (click)="activeTab.set('theatres')"
        >
          My Theatres ({{ theatres().length }})
        </button>
        <button
          type="button"
          class="tab-btn"
          [class.active]="activeTab() === 'shows'"
          (click)="activeTab.set('shows')"
        >
          Scheduled Shows ({{ shows().length }})
        </button>
      </div>

      @if (activeTab() === 'theatres') {
        <div class="card">
          <h2>Add New Theatre</h2>
          <p class="muted note">Every theatre is automatically provisioned with 50 seats (Rows 1–5, Columns A–J).</p>

          <form [formGroup]="theatreForm" (ngSubmit)="addTheatre()">
            <div class="form-grid">
              <div class="form-field">
                <label for="t-name">Theatre Name</label>
                <input id="t-name" type="text" formControlName="name" placeholder="Grand Cinema Screen 2" />
              </div>
              <div class="form-field">
                <label for="t-city">City</label>
                <input id="t-city" type="text" formControlName="city" placeholder="Bengaluru" />
              </div>
              <div class="form-field">
                <label for="t-address">Address</label>
                <input id="t-address" type="text" formControlName="address" placeholder="100 MG Road" />
              </div>
            </div>
            <button type="submit" class="btn btn-primary" [disabled]="busy() || theatreForm.invalid">
              Add Theatre (5×10 Layout)
            </button>
          </form>
        </div>

        <div class="card">
          <h2>Your Theatres</h2>
          @if (theatres().length === 0) {
            <div class="empty-state">You have not added any theatres yet.</div>
          } @else {
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>City</th>
                    <th>Address</th>
                    <th>Seat Layout</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  @for (t of theatres(); track t.id) {
                    <tr>
                      <td class="mono">#{{ t.id }}</td>
                      <td><strong>{{ t.name }}</strong></td>
                      <td>{{ t.city }}</td>
                      <td>{{ t.address }}</td>
                      <td class="mono">{{ t.totalRows }}×{{ t.totalCols }} ({{ t.totalSeats }} seats)</td>
                      <td>
                        <button
                          type="button"
                          class="btn btn-sm btn-danger"
                          [disabled]="busy()"
                          (click)="removeTheatre(t)"
                        >
                          Remove
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      }

      @if (activeTab() === 'shows') {
        <div class="card">
          <h2>Schedule a Movie Show</h2>
          @if (theatres().length === 0) {
            <div class="alert alert-warning">Add at least one theatre before scheduling shows.</div>
          } @else {
            <form [formGroup]="showForm" (ngSubmit)="scheduleShow()">
              <div class="form-grid">
                <div class="form-field">
                  <label for="s-theatre">Theatre</label>
                  <select id="s-theatre" formControlName="theatreId">
                    <option value="">Select theatre...</option>
                    @for (t of theatres(); track t.id) {
                      <option [value]="t.id">{{ t.name }} ({{ t.city }})</option>
                    }
                  </select>
                </div>

                <div class="form-field">
                  <label for="s-movie">Movie</label>
                  <select id="s-movie" formControlName="movieId">
                    <option value="">Select movie...</option>
                    @for (m of movies(); track m.id) {
                      <option [value]="m.id">{{ m.title }} ({{ m.language }})</option>
                    }
                  </select>
                </div>

                <div class="form-field">
                  <label for="s-time">Show Date & Time</label>
                  <input id="s-time" type="datetime-local" formControlName="showTime" />
                </div>

                <div class="form-field">
                  <label for="s-price">Ticket Price (₹)</label>
                  <input id="s-price" type="number" min="1" step="0.01" formControlName="ticketPrice" placeholder="250.00" />
                </div>
              </div>

              <button type="submit" class="btn btn-primary" [disabled]="busy() || showForm.invalid">
                Schedule Show
              </button>
            </form>
          }
        </div>

        <div class="card">
          <h2>Scheduled Shows & Booking Counts</h2>
          @if (shows().length === 0) {
            <div class="empty-state">No shows scheduled yet.</div>
          } @else {
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Movie</th>
                    <th>Theatre</th>
                    <th>Showtime</th>
                    <th>Price</th>
                    <th>Booked Seats</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  @for (s of shows(); track s.id) {
                    <tr>
                      <td class="mono">#{{ s.id }}</td>
                      <td><strong>{{ s.movieTitle }}</strong></td>
                      <td>{{ s.theatreName }} ({{ s.theatreCity }})</td>
                      <td>{{ s.showTime | date:'MMM d, y, h:mm a' }}</td>
                      <td>₹{{ s.ticketPrice | number:'1.2-2' }}</td>
                      <td class="mono">{{ s.bookedSeats }} / {{ s.totalSeats }}</td>
                      <td>
                        <span
                          class="badge"
                          [class.badge-scheduled]="s.status === 'SCHEDULED'"
                          [class.badge-cancelled]="s.status === 'CANCELLED'"
                        >
                          {{ s.status }}
                        </span>
                      </td>
                      <td>
                        @if (s.status === 'SCHEDULED') {
                          <button
                            type="button"
                            class="btn btn-sm btn-danger"
                            [disabled]="busy()"
                            (click)="cancelShow(s)"
                          >
                            Cancel Show
                          </button>
                        } @else {
                          <span class="muted">—</span>
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .note { margin: -6px 0 14px; font-size: 13px; }
  `],
})
export class OwnerDashboardComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly activeTab = signal<'theatres' | 'shows'>('theatres');
  readonly theatres = signal<Theatre[]>([]);
  readonly movies = signal<Movie[]>([]);
  readonly shows = signal<Show[]>([]);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);

  readonly theatreForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    city: ['', Validators.required],
    address: ['', Validators.required],
  });

  readonly showForm = this.fb.nonNullable.group({
    theatreId: ['', Validators.required],
    movieId: ['', Validators.required],
    showTime: ['', Validators.required],
    ticketPrice: [250, [Validators.required, Validators.min(1)]],
  });

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.api.getMyTheatres().subscribe({
      next: (data) => this.theatres.set(data),
      error: (err) => this.error.set(err?.error?.error || 'Could not load theatres.'),
    });
    this.api.getMovies().subscribe({
      next: (data) => this.movies.set(data),
    });
    this.api.getOwnerShows().subscribe({
      next: (data) => this.shows.set(data),
    });
  }

  addTheatre(): void {
    if (this.theatreForm.invalid) return;
    this.busy.set(true);
    this.error.set(null);
    this.message.set(null);

    this.api.createTheatre(this.theatreForm.getRawValue()).subscribe({
      next: (created) => {
        this.busy.set(false);
        this.theatres.update((list) => [created, ...list]);
        this.theatreForm.reset();
        this.message.set(`Theatre "${created.name}" created with 50 seats (1A–5J).`);
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(err?.error?.error || 'Could not create theatre.');
      },
    });
  }

  removeTheatre(theatre: Theatre): void {
    if (!confirm(`Remove theatre "${theatre.name}" and all its scheduled shows?`)) return;
    this.busy.set(true);
    this.error.set(null);
    this.message.set(null);

    this.api.deleteTheatre(theatre.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.theatres.update((list) => list.filter((t) => t.id !== theatre.id));
        this.shows.update((list) => list.filter((s) => s.theatreId !== theatre.id));
        this.message.set(`Theatre "${theatre.name}" removed.`);
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(err?.error?.error || 'Could not remove theatre.');
      },
    });
  }

  scheduleShow(): void {
    if (this.showForm.invalid) return;
    this.busy.set(true);
    this.error.set(null);
    this.message.set(null);

    const raw = this.showForm.getRawValue();
    this.api
      .createShow({
        theatreId: Number(raw.theatreId),
        movieId: Number(raw.movieId),
        showTime: raw.showTime,
        ticketPrice: Number(raw.ticketPrice),
      })
      .subscribe({
        next: (created) => {
          this.busy.set(false);
          this.shows.update((list) => [created, ...list]);
          this.showForm.patchValue({ showTime: '' });
          this.message.set(`Scheduled "${created.movieTitle}" at ${created.theatreName}.`);
        },
        error: (err) => {
          this.busy.set(false);
          this.error.set(err?.error?.error || 'Could not schedule show.');
        },
      });
  }

  cancelShow(show: Show): void {
    if (!confirm(`Cancel "${show.movieTitle}" at ${show.theatreName}? Existing bookings for this show will also be cancelled.`)) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.message.set(null);

    this.api.cancelShow(show.id).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.shows.update((list) => list.map((s) => (s.id === updated.id ? updated : s)));
        this.message.set(`Show #${updated.id} cancelled.`);
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(err?.error?.error || 'Could not cancel show.');
      },
    });
  }
}
