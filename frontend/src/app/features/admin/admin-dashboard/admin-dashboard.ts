import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { Booking, Movie, Theatre } from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe],
  template: `
    <div class="container">
      <div class="page-header">
        <div>
          <h1>Admin Panel</h1>
          <p>Manage platform movies, theatres, and system-wide bookings.</p>
        </div>
        <button type="button" class="btn btn-sm" (click)="loadAll()">Refresh All</button>
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
          [class.active]="activeTab() === 'movies'"
          (click)="activeTab.set('movies')"
        >
          Movies ({{ movies().length }})
        </button>
        <button
          type="button"
          class="tab-btn"
          [class.active]="activeTab() === 'theatres'"
          (click)="activeTab.set('theatres')"
        >
          All Theatres ({{ theatres().length }})
        </button>
        <button
          type="button"
          class="tab-btn"
          [class.active]="activeTab() === 'bookings'"
          (click)="activeTab.set('bookings')"
        >
          All Bookings ({{ bookings().length }})
        </button>
      </div>

      <!-- Tab 1: Movies -->
      @if (activeTab() === 'movies') {
        <div class="card">
          <h2>Add Movie to Catalog</h2>
          <form [formGroup]="movieForm" (ngSubmit)="addMovie()">
            <div class="form-grid">
              <div class="form-field">
                <label for="m-title">Title *</label>
                <input id="m-title" type="text" formControlName="title" placeholder="Arrival" />
              </div>
              <div class="form-field">
                <label for="m-genre">Genre *</label>
                <input id="m-genre" type="text" formControlName="genre" placeholder="Sci-Fi / Drama" />
              </div>
              <div class="form-field">
                <label for="m-lang">Language *</label>
                <input id="m-lang" type="text" formControlName="language" placeholder="English" />
              </div>
              <div class="form-field">
                <label for="m-dur">Duration (min) *</label>
                <input id="m-dur" type="number" min="1" formControlName="durationMinutes" />
              </div>
              <div class="form-field">
                <label for="m-rating">Rating</label>
                <input id="m-rating" type="text" formControlName="rating" placeholder="PG-13" />
              </div>
              <div class="form-field">
                <label for="m-date">Release Date</label>
                <input id="m-date" type="date" formControlName="releaseDate" />
              </div>
            </div>

            <div class="form-field">
              <label for="m-desc">Description</label>
              <textarea id="m-desc" rows="2" formControlName="description" placeholder="Short synopsis..."></textarea>
            </div>

            <!-- Mandatory Movie Poster Image -->
            <div class="poster-input-section">
              <div class="poster-header">
                <label class="section-label">Movie Poster Image * <span class="required-note">(Required)</span></label>
                <div class="mode-switch">
                  <button
                    type="button"
                    class="btn btn-sm"
                    [class.btn-primary]="imageMode() === 'file'"
                    (click)="setImageMode('file')"
                  >
                    Upload Image
                  </button>
                  <button
                    type="button"
                    class="btn btn-sm"
                    [class.btn-primary]="imageMode() === 'url'"
                    (click)="setImageMode('url')"
                  >
                    Image URL
                  </button>
                </div>
              </div>

              @if (imageMode() === 'file') {
                <div class="form-field">
                  <input
                    id="m-poster-file"
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/gif"
                    (change)="onFileSelected($event)"
                  />
                  <span class="muted small">Accepted formats: JPG, PNG, WebP, GIF (max 10 MB).</span>
                </div>
              } @else {
                <div class="form-field">
                  <input
                    id="m-poster-url"
                    type="url"
                    formControlName="posterUrl"
                    placeholder="https://example.com/poster.jpg"
                  />
                </div>
              }

              @if (posterPreviewUrl()) {
                <div class="poster-preview">
                  <img [src]="posterPreviewUrl()!" alt="Movie poster preview" />
                </div>
              }
            </div>

            <button
              type="submit"
              class="btn btn-primary"
              [disabled]="busy() || movieForm.invalid || !hasValidPoster()"
            >
              {{ busy() ? 'Saving Movie...' : 'Add Movie' }}
            </button>
          </form>
        </div>

        <div class="card">
          <h2>Platform Movie Catalog</h2>
          @if (movies().length === 0) {
            <div class="empty-state">No movies in the catalog.</div>
          } @else {
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Poster</th>
                    <th>Title</th>
                    <th>Genre</th>
                    <th>Language</th>
                    <th>Duration</th>
                    <th>Rating</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  @for (m of movies(); track m.id) {
                    <tr>
                      <td class="mono">#{{ m.id }}</td>
                      <td>
                        @if (m.posterUrl) {
                          <img class="table-thumb" [src]="m.posterUrl" [alt]="m.title" />
                        } @else {
                          <span class="muted small">None</span>
                        }
                      </td>
                      <td><strong>{{ m.title }}</strong></td>
                      <td>{{ m.genre }}</td>
                      <td>{{ m.language }}</td>
                      <td>{{ m.durationMinutes }} min</td>
                      <td>{{ m.rating || '—' }}</td>
                      <td>
                        <button
                          type="button"
                          class="btn btn-sm btn-danger"
                          [disabled]="busy()"
                          (click)="removeMovie(m)"
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

      <!-- Tab 2: All Theatres -->
      @if (activeTab() === 'theatres') {
        <div class="card">
          <h2>All Registered Theatres</h2>
          @if (theatres().length === 0) {
            <div class="empty-state">No theatres registered on the platform.</div>
          } @else {
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Theatre</th>
                    <th>City</th>
                    <th>Address</th>
                    <th>Owner</th>
                    <th>Layout</th>
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
                      <td>
                        {{ t.ownerName }}<br />
                        <span class="muted mono small">{{ t.ownerEmail }}</span>
                      </td>
                      <td class="mono">{{ t.totalRows }}×{{ t.totalCols }} ({{ t.totalSeats }})</td>
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

      <!-- Tab 3: All Bookings -->
      @if (activeTab() === 'bookings') {
        <div class="card">
          <h2>All Platform Bookings</h2>
          @if (bookings().length === 0) {
            <div class="empty-state">No bookings have been placed yet.</div>
          } @else {
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Reference</th>
                    <th>User</th>
                    <th>Movie</th>
                    <th>Theatre</th>
                    <th>Showtime</th>
                    <th>Seats</th>
                    <th>Total</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  @for (b of bookings(); track b.id) {
                    <tr>
                      <td class="mono">{{ b.bookingReference }}</td>
                      <td>
                        {{ b.userName }}<br />
                        <span class="muted mono small">{{ b.userEmail }}</span>
                      </td>
                      <td><strong>{{ b.movieTitle }}</strong></td>
                      <td>{{ b.theatreName }}</td>
                      <td>{{ b.showTime | date:'MMM d, y, h:mm a' }}</td>
                      <td class="mono">{{ b.seats.join(', ') }}</td>
                      <td>₹{{ b.totalAmount | number:'1.2-2' }}</td>
                      <td>
                        <span
                          class="badge"
                          [class.badge-confirmed]="b.status === 'CONFIRMED'"
                          [class.badge-cancelled]="b.status === 'CANCELLED'"
                        >
                          {{ b.status }}
                        </span>
                      </td>
                      <td>
                        @if (b.status === 'CONFIRMED') {
                          <button
                            type="button"
                            class="btn btn-sm btn-danger"
                            [disabled]="busy()"
                            (click)="cancelUserBooking(b)"
                          >
                            Cancel Ticket
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
    .small { font-size: 11px; }
    .poster-input-section {
      border: 1px solid var(--border);
      border-radius: 5px;
      padding: 12px 14px;
      background: var(--bg);
      margin-bottom: 14px;
    }
    .poster-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 10px;
    }
    .section-label {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }
    .required-note {
      color: var(--danger);
      text-transform: none;
      font-weight: 500;
    }
    .mode-switch {
      display: flex;
      gap: 6px;
    }
    .poster-preview {
      margin-top: 8px;
    }
    .poster-preview img {
      display: block;
      width: 220px;
      height: 132px;
      object-fit: cover;
      border-radius: 4px;
      border: 1px solid var(--border);
      background: #ffffff;
    }
    .table-thumb {
      display: block;
      width: 72px;
      height: 44px;
      object-fit: cover;
      border-radius: 3px;
      border: 1px solid var(--border);
      background: var(--surface-subtle);
    }
  `],
})
export class AdminDashboardComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly activeTab = signal<'movies' | 'theatres' | 'bookings'>('movies');
  readonly movies = signal<Movie[]>([]);
  readonly theatres = signal<Theatre[]>([]);
  readonly bookings = signal<Booking[]>([]);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);

  readonly imageMode = signal<'file' | 'url'>('file');
  readonly selectedFile = signal<File | null>(null);
  readonly filePreviewUrl = signal<string | null>(null);
  readonly urlValue = signal<string>('');

  readonly movieForm = this.fb.nonNullable.group({
    title: ['', Validators.required],
    genre: ['', Validators.required],
    language: ['English', Validators.required],
    durationMinutes: [120, [Validators.required, Validators.min(1)]],
    rating: ['U/A'],
    releaseDate: [''],
    description: [''],
    posterUrl: [''],
  });

  readonly hasValidPoster = computed(() => {
    if (this.imageMode() === 'file') {
      return this.selectedFile() !== null;
    }
    return this.urlValue().trim().length > 0;
  });

  readonly posterPreviewUrl = computed(() => {
    if (this.imageMode() === 'file') {
      return this.filePreviewUrl();
    }
    const url = this.urlValue().trim();
    return url.length > 0 ? url : null;
  });

  ngOnInit(): void {
    this.movieForm.controls.posterUrl.valueChanges.subscribe((val) => {
      this.urlValue.set(val ?? '');
    });
    this.loadAll();
  }

  setImageMode(mode: 'file' | 'url'): void {
    this.imageMode.set(mode);
    this.error.set(null);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (this.filePreviewUrl()) {
      URL.revokeObjectURL(this.filePreviewUrl()!);
      this.filePreviewUrl.set(null);
    }

    if (!file) {
      this.selectedFile.set(null);
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.error.set('Please select a valid image file (JPG, PNG, WebP, or GIF).');
      this.selectedFile.set(null);
      input.value = '';
      return;
    }

    this.error.set(null);
    this.selectedFile.set(file);
    this.filePreviewUrl.set(URL.createObjectURL(file));
  }

  loadAll(): void {
    this.api.getMovies().subscribe({
      next: (data) => this.movies.set(data),
      error: (err) => this.error.set(err?.error?.error || 'Could not load movies.'),
    });
    this.api.getAllTheatres().subscribe({
      next: (data) => this.theatres.set(data),
    });
    this.api.getAllBookings().subscribe({
      next: (data) => this.bookings.set(data),
    });
  }

  addMovie(): void {
    if (this.movieForm.invalid || !this.hasValidPoster()) {
      this.error.set('Please complete all required fields including a movie poster image.');
      return;
    }

    this.busy.set(true);
    this.error.set(null);
    this.message.set(null);

    if (this.imageMode() === 'file') {
      const file = this.selectedFile();
      if (!file) {
        this.busy.set(false);
        this.error.set('Movie poster image is required.');
        return;
      }

      this.api.uploadMoviePoster(file).subscribe({
        next: (uploadRes) => {
          this.submitMovieWithPosterUrl(uploadRes.posterUrl);
        },
        error: (err) => {
          this.busy.set(false);
          this.error.set(err?.error?.error || 'Could not upload movie poster image.');
        },
      });
    } else {
      const posterUrl = this.movieForm.getRawValue().posterUrl.trim();
      this.submitMovieWithPosterUrl(posterUrl);
    }
  }

  private submitMovieWithPosterUrl(posterUrl: string): void {
    const raw = this.movieForm.getRawValue();
    this.api
      .createMovie({
        title: raw.title,
        genre: raw.genre,
        language: raw.language,
        durationMinutes: Number(raw.durationMinutes),
        rating: raw.rating,
        releaseDate: raw.releaseDate || null,
        description: raw.description,
        posterUrl,
      })
      .subscribe({
        next: (created) => {
          this.busy.set(false);
          this.movies.update((list) => [created, ...list]);
          this.resetMovieForm();
          this.message.set(`Movie "${created.title}" added to catalog.`);
        },
        error: (err) => {
          this.busy.set(false);
          this.error.set(err?.error?.error || 'Could not add movie.');
        },
      });
  }

  private resetMovieForm(): void {
    this.movieForm.reset({ language: 'English', durationMinutes: 120, rating: 'U/A', posterUrl: '' });
    this.urlValue.set('');
    this.selectedFile.set(null);
    if (this.filePreviewUrl()) {
      URL.revokeObjectURL(this.filePreviewUrl()!);
      this.filePreviewUrl.set(null);
    }
    const fileInput = document.getElementById('m-poster-file') as HTMLInputElement | null;
    if (fileInput) fileInput.value = '';
  }

  removeMovie(movie: Movie): void {
    if (!confirm(`Remove "${movie.title}" from the platform? Associated shows will also be removed.`)) return;
    this.busy.set(true);
    this.error.set(null);
    this.message.set(null);

    this.api.deleteMovie(movie.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.movies.update((list) => list.filter((m) => m.id !== movie.id));
        this.message.set(`Movie "${movie.title}" removed.`);
        this.api.getAllBookings().subscribe((data) => this.bookings.set(data));
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(err?.error?.error || 'Could not remove movie.');
      },
    });
  }

  removeTheatre(theatre: Theatre): void {
    if (!confirm(`Remove theatre "${theatre.name}" (${theatre.city}) from the platform?`)) return;
    this.busy.set(true);
    this.error.set(null);
    this.message.set(null);

    this.api.deleteTheatre(theatre.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.theatres.update((list) => list.filter((t) => t.id !== theatre.id));
        this.message.set(`Theatre "${theatre.name}" removed.`);
        this.api.getAllBookings().subscribe((data) => this.bookings.set(data));
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(err?.error?.error || 'Could not remove theatre.');
      },
    });
  }

  cancelUserBooking(booking: Booking): void {
    if (!confirm(`Cancel ticket ${booking.bookingReference} for ${booking.userName} and free seats ${booking.seats.join(', ')}?`)) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.message.set(null);

    this.api.cancelBooking(booking.id).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.bookings.update((list) => list.map((b) => (b.id === updated.id ? updated : b)));
        this.message.set(`Booking ${updated.bookingReference} cancelled and seats ${updated.seats.join(', ')} freed.`);
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(err?.error?.error || 'Could not cancel booking.');
      },
    });
  }
}
