import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { TicketPdfService } from '../../../core/services/ticket-pdf.service';
import { Booking } from '../../../core/models/api.models';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [RouterLink, DatePipe, DecimalPipe],
  template: `
    <div class="container">
      <div class="page-header">
        <div>
          <h1>My Bookings</h1>
          <p>Download PDF tickets or cancel confirmed reservations.</p>
        </div>
        <a routerLink="/user/movies" class="btn btn-primary btn-sm">Browse Movies</a>
      </div>

      @if (error()) {
        <div class="alert alert-error">{{ error() }}</div>
      }
      @if (message()) {
        <div class="alert alert-success">{{ message() }}</div>
      }

      <div class="card">
        @if (loading()) {
          <div class="empty-state">Loading your bookings...</div>
        } @else if (bookings().length === 0) {
          <div class="empty-state">You have no bookings yet.</div>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Reference</th>
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
                    <td><strong>{{ b.movieTitle }}</strong></td>
                    <td>{{ b.theatreName }}, {{ b.theatreCity }}</td>
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
                      <div class="row-actions">
                        <button type="button" class="btn btn-sm" (click)="downloadTicket(b)">
                          PDF Ticket
                        </button>
                        @if (b.status === 'CONFIRMED') {
                          <button
                            type="button"
                            class="btn btn-sm btn-danger"
                            [disabled]="cancellingId() === b.id"
                            (click)="cancel(b)"
                          >
                            {{ cancellingId() === b.id ? 'Cancelling...' : 'Cancel' }}
                          </button>
                        }
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .row-actions {
      display: flex;
      gap: 6px;
      flex-wrap: nowrap;
    }
  `],
})
export class MyBookingsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly pdfService = inject(TicketPdfService);

  readonly bookings = signal<Booking[]>([]);
  readonly loading = signal(true);
  readonly cancellingId = signal<number | null>(null);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);

  ngOnInit(): void {
    this.loadBookings();
  }

  loadBookings(): void {
    this.loading.set(true);
    this.api.getMyBookings().subscribe({
      next: (data) => {
        this.bookings.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.error || 'Could not load bookings.');
        this.loading.set(false);
      },
    });
  }

  downloadTicket(booking: Booking): void {
    this.pdfService.downloadTicket(booking);
  }

  cancel(booking: Booking): void {
    if (!confirm(`Cancel booking ${booking.bookingReference} and release seats ${booking.seats.join(', ')}?`)) {
      return;
    }
    this.cancellingId.set(booking.id);
    this.error.set(null);
    this.message.set(null);

    this.api.cancelBooking(booking.id).subscribe({
      next: (updated) => {
        this.cancellingId.set(null);
        this.bookings.update((list) => list.map((item) => (item.id === updated.id ? updated : item)));
        this.message.set(`Booking ${updated.bookingReference} cancelled and seats released.`);
      },
      error: (err) => {
        this.cancellingId.set(null);
        this.error.set(err?.error?.error || 'Could not cancel booking.');
      },
    });
  }
}
