import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { Movie, Show } from '../../../core/models/api.models';

interface TheatreShowGroup {
  theatreId: number;
  theatreName: string;
  theatreCity: string;
  theatreAddress: string;
  shows: Show[];
}

@Component({
  selector: 'app-movie-detail',
  standalone: true,
  imports: [RouterLink, DatePipe, DecimalPipe],
  template: `
    <div class="container">
      <div class="back-link">
        <a routerLink="/user/movies">&larr; Back to movies</a>
      </div>

      @if (error()) {
        <div class="alert alert-error">{{ error() }}</div>
      }

      @if (loading()) {
        <div class="card empty-state">Loading movie details...</div>
      } @else if (movie(); as m) {
        <div class="card movie-banner-card">
          @if (m.posterUrl) {
            <img class="detail-poster" [src]="m.posterUrl" [alt]="m.title" />
          }
          <div class="detail-body">
            <div class="movie-title-row">
              <h1>{{ m.title }}</h1>
              @if (m.rating) {
                <span class="badge badge-neutral">{{ m.rating }}</span>
              }
            </div>
            <div class="meta-bar muted">
              <span>{{ m.genre }}</span>
              <span>•</span>
              <span>{{ m.language }}</span>
              <span>•</span>
              <span>{{ m.durationMinutes }} minutes</span>
              @if (m.releaseDate) {
                <span>•</span>
                <span>Released {{ m.releaseDate }}</span>
              }
            </div>
            <p class="desc">{{ m.description || 'No synopsis available.' }}</p>
          </div>
        </div>

        <h2 class="section-title">Scheduled Showtimes</h2>

        @if (groupedShows().length === 0) {
          <div class="card empty-state">No upcoming shows are scheduled for this movie yet.</div>
        } @else {
          @for (group of groupedShows(); track group.theatreId) {
            <div class="card theatre-group">
              <div class="theatre-info">
                <h3>{{ group.theatreName }}</h3>
                <p class="muted">{{ group.theatreAddress }}, {{ group.theatreCity }}</p>
              </div>

              <div class="showtime-list">
                @for (show of group.shows; track show.id) {
                  <a [routerLink]="['/user/shows', show.id, 'seats']" class="showtime-chip">
                    <div class="show-date">{{ show.showTime | date:'EEE, MMM d' }}</div>
                    <div class="show-hour">{{ show.showTime | date:'shortTime' }}</div>
                    <div class="show-sub">
                      <span>₹{{ show.ticketPrice | number:'1.2-2' }}</span>
                      <span>{{ show.totalSeats - show.bookedSeats }}/{{ show.totalSeats }} left</span>
                    </div>
                  </a>
                }
              </div>
            </div>
          }
        }
      }
    </div>
  `,
  styles: [`
    .back-link { margin-bottom: 14px; }
    .movie-banner-card {
      display: flex;
      gap: 20px;
      align-items: flex-start;
    }
    .detail-poster {
      width: 240px;
      aspect-ratio: 5 / 3;
      object-fit: cover;
      border-radius: 5px;
      border: 1px solid var(--border);
      flex-shrink: 0;
      background: var(--surface-subtle);
    }
    .detail-body {
      flex: 1;
      min-width: 0;
    }
    @media (max-width: 680px) {
      .movie-banner-card { flex-direction: column; }
      .detail-poster { width: 100%; }
    }
    .movie-title-row {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 6px;
    }
    .movie-title-row h1 {
      margin: 0;
      font-size: 22px;
    }
    .meta-bar {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      font-size: 13px;
      margin-bottom: 12px;
    }
    .desc { margin: 0; }
    .section-title {
      font-size: 16px;
      margin: 20px 0 12px;
    }
    .theatre-info h3 { margin: 0 0 2px; }
    .theatre-info p { margin: 0 0 14px; font-size: 13px; }
    .showtime-list {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
    }
    .showtime-chip {
      display: block;
      min-width: 155px;
      padding: 10px 12px;
      border: 1px solid var(--border);
      border-radius: 5px;
      background: #ffffff;
      color: var(--text);
      text-decoration: none;
    }
    .showtime-chip:hover {
      border-color: var(--primary);
      background: #eff6ff;
      text-decoration: none;
    }
    .show-date {
      font-size: 11px;
      color: var(--text-muted);
      text-transform: uppercase;
    }
    .show-hour {
      font-size: 15px;
      font-weight: 600;
      margin: 2px 0 4px;
    }
    .show-sub {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: var(--text-muted);
    }
  `],
})
export class MovieDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ApiService);

  readonly movie = signal<Movie | null>(null);
  readonly shows = signal<Show[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly groupedShows = computed<TheatreShowGroup[]>(() => {
    const map = new Map<number, TheatreShowGroup>();
    for (const s of this.shows()) {
      let group = map.get(s.theatreId);
      if (!group) {
        group = {
          theatreId: s.theatreId,
          theatreName: s.theatreName,
          theatreCity: s.theatreCity,
          theatreAddress: s.theatreAddress,
          shows: [],
        };
        map.set(s.theatreId, group);
      }
      group.shows.push(s);
    }
    return Array.from(map.values());
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('Invalid movie ID.');
      this.loading.set(false);
      return;
    }

    this.api.getMovie(id).subscribe({
      next: (movie) => {
        this.movie.set(movie);
        this.api.getShowsForMovie(id).subscribe({
          next: (shows) => {
            this.shows.set(shows);
            this.loading.set(false);
          },
          error: (err) => {
            this.error.set(err?.error?.error || 'Could not load showtimes.');
            this.loading.set(false);
          },
        });
      },
      error: (err) => {
        this.error.set(err?.error?.error || 'Movie not found.');
        this.loading.set(false);
      },
    });
  }
}
