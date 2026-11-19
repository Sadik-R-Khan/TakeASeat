import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { Movie } from '../../../core/models/api.models';

@Component({
  selector: 'app-movie-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="container">
      <div class="page-header">
        <div>
          <h1>Now Showing</h1>
          <p>Select a movie to view theatres and available showtimes.</p>
        </div>
        <div class="search-box">
          <input
            type="text"
            placeholder="Filter by title, genre, or language..."
            [value]="query()"
            (input)="query.set($any($event.target).value)"
          />
        </div>
      </div>

      @if (error()) {
        <div class="alert alert-error">{{ error() }}</div>
      }

      @if (loading()) {
        <div class="card empty-state">Loading movies...</div>
      } @else if (filteredMovies().length === 0) {
        <div class="card empty-state">No movies found.</div>
      } @else {
        <div class="movie-grid">
          @for (movie of filteredMovies(); track movie.id) {
            <div class="card movie-card">
              @if (movie.posterUrl) {
                <a [routerLink]="['/user/movies', movie.id]" class="poster-wrap">
                  <img [src]="movie.posterUrl" [alt]="movie.title" loading="lazy" />
                </a>
              }

              <div class="movie-body">
                <div class="movie-head">
                  <h2>{{ movie.title }}</h2>
                  @if (movie.rating) {
                    <span class="badge badge-neutral">{{ movie.rating }}</span>
                  }
                </div>

                <div class="movie-meta muted">
                  <span>{{ movie.genre }}</span>
                  <span>•</span>
                  <span>{{ movie.language }}</span>
                  <span>•</span>
                  <span>{{ movie.durationMinutes }} min</span>
                </div>

                <p class="movie-desc">{{ movie.description || 'No description provided.' }}</p>

                <div class="movie-actions">
                  <a [routerLink]="['/user/movies', movie.id]" class="btn btn-primary btn-sm">
                    View Showtimes
                  </a>
                </div>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .search-box {
      width: 280px;
      max-width: 100%;
    }
    .movie-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(310px, 1fr));
      gap: 16px;
    }
    .movie-card {
      display: flex;
      flex-direction: column;
      margin-bottom: 0;
      padding: 0;
      overflow: hidden;
    }
    .poster-wrap {
      display: block;
      width: 100%;
      aspect-ratio: 5 / 3;
      background: var(--surface-subtle);
      border-bottom: 1px solid var(--border);
      overflow: hidden;
    }
    .poster-wrap img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .movie-body {
      padding: 16px 18px 18px;
      display: flex;
      flex-direction: column;
      flex: 1;
    }
    .movie-head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 8px;
      margin-bottom: 6px;
    }
    .movie-head h2 {
      margin: 0;
      font-size: 16px;
    }
    .movie-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      font-size: 12px;
      margin-bottom: 10px;
    }
    .movie-desc {
      margin: 0 0 16px;
      color: var(--text-muted);
      flex: 1;
    }
    .movie-actions {
      display: flex;
      justify-content: flex-end;
    }
  `],
})
export class MovieListComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly movies = signal<Movie[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly query = signal('');

  readonly filteredMovies = computed(() => {
    const q = this.query().trim().toLowerCase();
    if (!q) return this.movies();
    return this.movies().filter(
      (m) =>
        m.title.toLowerCase().includes(q) ||
        m.genre.toLowerCase().includes(q) ||
        m.language.toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.api.getMovies().subscribe({
      next: (data) => {
        this.movies.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.error || 'Could not load movies.');
        this.loading.set(false);
      },
    });
  }
}
