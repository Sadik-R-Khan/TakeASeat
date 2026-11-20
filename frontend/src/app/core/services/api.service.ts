import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  Booking,
  CreateMovieRequest,
  CreateShowRequest,
  CreateTheatreRequest,
  HoldSeatsResponse,
  Movie,
  Show,
  ShowSeatMapResponse,
  Theatre,
  TicketUploadResponse,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  private normalizePosterUrl<T extends { posterUrl?: string | null }>(item: T): T {
    if (item && item.posterUrl && typeof item.posterUrl === 'string') {
      item.posterUrl = item.posterUrl.replace(/^https?:\/\/[^/]+(\/api\/movies\/posters\/)/i, '$1');
    }
    return item;
  }

  // Movies
  getMovies(): Observable<Movie[]> {
    return this.http.get<Movie[]>(`${API_BASE_URL}/movies`).pipe(
      map((movies) => movies.map((m) => this.normalizePosterUrl(m)))
    );
  }

  getMovie(id: number): Observable<Movie> {
    return this.http.get<Movie>(`${API_BASE_URL}/movies/${id}`).pipe(
      map((m) => this.normalizePosterUrl(m))
    );
  }

  getShowsForMovie(movieId: number): Observable<Show[]> {
    return this.http.get<Show[]>(`${API_BASE_URL}/movies/${movieId}/shows`);
  }

  createMovie(payload: CreateMovieRequest): Observable<Movie> {
    if (payload.posterUrl) {
      payload.posterUrl = payload.posterUrl.replace(/^https?:\/\/[^/]+(\/api\/movies\/posters\/)/i, '$1');
    }
    return this.http.post<Movie>(`${API_BASE_URL}/movies`, payload).pipe(
      map((m) => this.normalizePosterUrl(m))
    );
  }

  uploadMoviePoster(file: File): Observable<{ posterUrl: string }> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<{ posterUrl: string }>(`${API_BASE_URL}/movies/posters`, formData).pipe(
      map((res) => ({
        posterUrl: res.posterUrl.replace(/^https?:\/\/[^/]+(\/api\/movies\/posters\/)/i, '$1')
      }))
    );
  }

  deleteMovie(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/movies/${id}`);
  }

  // Theatres
  getAllTheatres(): Observable<Theatre[]> {
    return this.http.get<Theatre[]>(`${API_BASE_URL}/theatres`);
  }

  getMyTheatres(): Observable<Theatre[]> {
    return this.http.get<Theatre[]>(`${API_BASE_URL}/theatres/mine`);
  }

  createTheatre(payload: CreateTheatreRequest): Observable<Theatre> {
    return this.http.post<Theatre>(`${API_BASE_URL}/theatres`, payload);
  }

  deleteTheatre(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/theatres/${id}`);
  }

  // Shows & Seat Locking
  getOwnerShows(): Observable<Show[]> {
    return this.http.get<Show[]>(`${API_BASE_URL}/shows/owner`);
  }

  createShow(payload: CreateShowRequest): Observable<Show> {
    return this.http.post<Show>(`${API_BASE_URL}/shows`, payload);
  }

  cancelShow(showId: number): Observable<Show> {
    return this.http.patch<Show>(`${API_BASE_URL}/shows/${showId}/cancel`, {});
  }

  getShowSeatMap(showId: number): Observable<ShowSeatMapResponse> {
    return this.http.get<ShowSeatMapResponse>(`${API_BASE_URL}/shows/${showId}/seats`);
  }

  holdSeats(showId: number, seatNumbers: string[]): Observable<HoldSeatsResponse> {
    return this.http.post<HoldSeatsResponse>(`${API_BASE_URL}/shows/${showId}/hold`, { seatNumbers });
  }

  releaseHold(showId: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/shows/${showId}/hold`);
  }

  // Bookings
  confirmBooking(showId: number, seatNumbers: string[]): Observable<Booking> {
    return this.http.post<Booking>(`${API_BASE_URL}/bookings/confirm`, { showId, seatNumbers });
  }

  getMyBookings(): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${API_BASE_URL}/bookings/mine`);
  }

  getAllBookings(): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${API_BASE_URL}/bookings`);
  }

  cancelBooking(bookingId: number): Observable<Booking> {
    return this.http.patch<Booking>(`${API_BASE_URL}/bookings/${bookingId}/cancel`, {});
  }

  // Tickets
  uploadTicketPdf(bookingId: number, pdfBlob: Blob, filename: string): Observable<TicketUploadResponse> {
    const formData = new FormData();
    formData.append('file', pdfBlob, filename);
    return this.http.post<TicketUploadResponse>(`${API_BASE_URL}/tickets/${bookingId}/upload`, formData);
  }
}
