import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { TicketPdfService } from '../../../core/services/ticket-pdf.service';
import { Booking, SeatStatusItem, ShowSeatMapResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-seat-selection',
  standalone: true,
  imports: [RouterLink, DatePipe, DecimalPipe],
  template: `
    <div class="container">
      <div class="top-row">
        <a [routerLink]="seatMap() ? ['/user/movies', seatMap()!.movieId] : ['/user/movies']">
          &larr; Back to showtimes
        </a>
        <button type="button" class="btn btn-sm" (click)="loadSeatMap(true)" [disabled]="refreshing()">
          {{ refreshing() ? 'Refreshing...' : 'Refresh Seats' }}
        </button>
      </div>

      @if (error()) {
        <div class="alert alert-error">{{ error() }}</div>
      }
      @if (notice()) {
        <div class="alert alert-warning">{{ notice() }}</div>
      }

      @if (confirmedBooking(); as booking) {
        <div class="card confirmation-card">
          <div class="alert alert-success">
            Booking confirmed! Reference: <strong class="mono">{{ booking.bookingReference }}</strong>
            @if (uploadStatus()) {
              — {{ uploadStatus() }}
            }
          </div>

          <h2>{{ booking.movieTitle }}</h2>
          <p class="muted">
            {{ booking.theatreName }}, {{ booking.theatreCity }} • {{ booking.showTime | date:'medium' }}
          </p>
          <p>
            <strong>Seats:</strong> <span class="mono">{{ booking.seats.join(', ') }}</span> &nbsp;|&nbsp;
            <strong>Total Paid:</strong> ₹{{ booking.totalAmount | number:'1.2-2' }}
          </p>

          <div class="action-row">
            <button type="button" class="btn btn-primary" (click)="downloadTicket(booking)">
              Download PDF Ticket
            </button>
            <a routerLink="/user/bookings" class="btn">View My Bookings</a>
          </div>
        </div>
      }

      @if (loading()) {
        <div class="card empty-state">Loading 5×10 seat map...</div>
      } @else if (seatMap(); as map) {
        <div class="layout-grid">
          <div class="card">
            <div class="show-header">
              <div>
                <h1>{{ map.movieTitle }}</h1>
                <p class="muted">
                  {{ map.theatreName }} ({{ map.theatreCity }}) • {{ map.showTime | date:'EEE, MMM d, h:mm a' }}
                </p>
              </div>
              <div class="price-tag">
                ₹{{ map.ticketPrice | number:'1.2-2' }} <span class="muted">/ seat</span>
              </div>
            </div>

            @if (map.showStatus === 'CANCELLED') {
              <div class="alert alert-error">This show has been cancelled.</div>
            }

            <div class="screen-bar">SCREEN THIS WAY</div>

            <!-- Column Headers A-J -->
            <div class="seat-grid-wrapper">
              <div class="col-headers">
                <div class="row-label"></div>
                @for (col of columns; track col) {
                  <div class="col-label mono">{{ col }}</div>
                }
              </div>

              <!-- 5 Rows x 10 Columns -->
              @for (row of rows; track row) {
                <div class="seat-row">
                  <div class="row-label mono">R{{ row }}</div>
                  @for (seat of seatsForRow(row); track seat.seatNumber) {
                    <button
                      type="button"
                      class="seat-btn mono"
                      [class.available]="seat.status === 'AVAILABLE' && !isSelected(seat.seatNumber)"
                      [class.selected]="seat.status === 'AVAILABLE' && isSelected(seat.seatNumber)"
                      [class.held-me]="seat.status === 'HELD_BY_ME'"
                      [class.held-other]="seat.status === 'HELD'"
                      [class.booked]="seat.status === 'BOOKED'"
                      [disabled]="seat.status !== 'AVAILABLE' || hasActiveHold() || map.showStatus === 'CANCELLED'"
                      [title]="seatTitle(seat)"
                      (click)="toggleSeat(seat.seatNumber)"
                    >
                      {{ seat.seatNumber }}
                    </button>
                  }
                </div>
              }
            </div>

            <div class="legend">
              <span><i class="swatch available"></i> Available</span>
              <span><i class="swatch selected"></i> Selected</span>
              <span><i class="swatch held-me"></i> Held by You</span>
              <span><i class="swatch held-other"></i> Held (Locked)</span>
              <span><i class="swatch booked"></i> Booked</span>
            </div>
          </div>

          <!-- Checkout / Hold Sidebar -->
          <div class="card summary-card">
            <h2>Booking Summary</h2>

            @if (hasActiveHold()) {
              <div class="timer-box">
                <div class="timer-label">Seats Reserved — Complete Payment In</div>
                <div class="timer-value mono">{{ formattedCountdown() }}</div>
              </div>

              <div class="summary-row">
                <span class="muted">Held Seats</span>
                <strong class="mono">{{ heldSeats().join(', ') }}</strong>
              </div>
              <div class="summary-row">
                <span class="muted">Quantity</span>
                <strong>{{ heldSeats().length }}</strong>
              </div>
              <div class="summary-row total-row">
                <span>Total Amount</span>
                <strong>₹{{ heldTotal() | number:'1.2-2' }}</strong>
              </div>

              <button
                type="button"
                class="btn btn-primary full-width"
                [disabled]="busy()"
                (click)="confirmAndPay()"
              >
                {{ busy() ? 'Processing Booking...' : 'Confirm & Pay (Simulated)' }}
              </button>

              <button
                type="button"
                class="btn btn-danger full-width release-btn"
                [disabled]="busy()"
                (click)="releaseHold()"
              >
                Release Hold
              </button>
            } @else {
              <p class="muted help-text">
                Select one or more available seats from the 5×10 grid, then hold them for 10 minutes to check out.
              </p>

              <div class="summary-row">
                <span class="muted">Selected Seats</span>
                <strong class="mono">
                  {{ selectedSeats().length > 0 ? selectedSeats().join(', ') : 'None' }}
                </strong>
              </div>
              <div class="summary-row">
                <span class="muted">Seat Price</span>
                <span>₹{{ map.ticketPrice | number:'1.2-2' }}</span>
              </div>
              <div class="summary-row total-row">
                <span>Estimated Total</span>
                <strong>₹{{ selectedTotal() | number:'1.2-2' }}</strong>
              </div>

              <button
                type="button"
                class="btn btn-primary full-width"
                [disabled]="selectedSeats().length === 0 || busy() || map.showStatus === 'CANCELLED'"
                (click)="holdSelectedSeats()"
              >
                {{ busy() ? 'Locking Seats...' : 'Hold Selected Seats (10 min)' }}
              </button>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .top-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 14px;
    }
    .layout-grid {
      display: grid;
      grid-template-columns: 1fr 310px;
      gap: 18px;
      align-items: start;
    }
    @media (max-width: 900px) {
      .layout-grid { grid-template-columns: 1fr; }
    }
    .show-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 12px;
      margin-bottom: 16px;
    }
    .show-header h1 {
      margin: 0 0 4px;
      font-size: 20px;
    }
    .show-header p { margin: 0; }
    .price-tag {
      font-size: 16px;
      font-weight: 600;
      white-space: nowrap;
    }
    .screen-bar {
      text-align: center;
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.14em;
      color: var(--text-muted);
      background: var(--surface-subtle);
      border: 1px solid var(--border);
      border-radius: 4px;
      padding: 6px;
      margin-bottom: 18px;
    }
    .seat-grid-wrapper {
      overflow-x: auto;
      padding-bottom: 4px;
    }
    .col-headers, .seat-row {
      display: grid;
      grid-template-columns: 32px repeat(10, minmax(38px, 1fr));
      gap: 6px;
      align-items: center;
      margin-bottom: 6px;
    }
    .col-label, .row-label {
      text-align: center;
      font-size: 11px;
      font-weight: 600;
      color: var(--text-muted);
    }
    .seat-btn {
      height: 36px;
      border-radius: 4px;
      border: 1px solid var(--border-strong);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: background-color 0.1s ease, border-color 0.1s ease;
    }
    .seat-btn.available {
      background: #ffffff;
      color: var(--text);
    }
    .seat-btn.available:hover:not(:disabled) {
      background: #eff6ff;
      border-color: var(--primary);
      color: var(--primary);
    }
    .seat-btn.selected {
      background: var(--primary);
      border-color: var(--primary);
      color: #ffffff;
    }
    .seat-btn.held-me {
      background: #fef3c7;
      border: 2px solid #d97706;
      color: #92400e;
      cursor: default;
    }
    .seat-btn.held-other {
      background: #e2e8f0;
      border: 1px dashed #64748b;
      color: #64748b;
      cursor: not-allowed;
    }
    .seat-btn.booked {
      background: #cbd5e1;
      border-color: #94a3b8;
      color: #64748b;
      text-decoration: line-through;
      cursor: not-allowed;
    }
    .legend {
      display: flex;
      flex-wrap: wrap;
      gap: 14px;
      margin-top: 16px;
      padding-top: 12px;
      border-top: 1px solid var(--border);
      font-size: 12px;
      color: var(--text-muted);
    }
    .legend span {
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
    .swatch {
      display: inline-block;
      width: 14px;
      height: 14px;
      border-radius: 3px;
      border: 1px solid var(--border-strong);
    }
    .swatch.available { background: #ffffff; }
    .swatch.selected { background: var(--primary); border-color: var(--primary); }
    .swatch.held-me { background: #fef3c7; border: 2px solid #d97706; }
    .swatch.held-other { background: #e2e8f0; border: 1px dashed #64748b; }
    .swatch.booked { background: #cbd5e1; border-color: #94a3b8; }

    .timer-box {
      background: #fef3c7;
      border: 1px solid #fcd34d;
      border-radius: 5px;
      padding: 12px;
      text-align: center;
      margin-bottom: 16px;
    }
    .timer-label {
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
      color: #92400e;
      margin-bottom: 4px;
    }
    .timer-value {
      font-size: 26px;
      font-weight: 700;
      color: #92400e;
    }
    .summary-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 7px 0;
      border-bottom: 1px solid var(--border);
    }
    .total-row {
      border-bottom: none;
      padding-top: 10px;
      margin-bottom: 14px;
      font-size: 15px;
    }
    .help-text { margin: 0 0 14px; font-size: 13px; }
    .full-width { width: 100%; }
    .release-btn { margin-top: 8px; }
    .confirmation-card h2 { margin-bottom: 4px; }
    .action-row {
      display: flex;
      gap: 10px;
      margin-top: 14px;
      flex-wrap: wrap;
    }
  `],
})
export class SeatSelectionComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ApiService);
  private readonly pdfService = inject(TicketPdfService);

  readonly rows = [1, 2, 3, 4, 5];
  readonly columns = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'];

  readonly showId = signal<number>(0);
  readonly seatMap = signal<ShowSeatMapResponse | null>(null);
  readonly selectedSeats = signal<string[]>([]);
  readonly remainingSeconds = signal<number>(0);
  readonly loading = signal(true);
  readonly refreshing = signal(false);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  readonly confirmedBooking = signal<Booking | null>(null);
  readonly uploadStatus = signal<string | null>(null);

  readonly heldSeats = computed(() => this.seatMap()?.heldByMeSeats ?? []);
  readonly hasActiveHold = computed(() => this.heldSeats().length > 0 && this.remainingSeconds() > 0);

  readonly selectedTotal = computed(() => {
    const price = this.seatMap()?.ticketPrice ?? 0;
    return price * this.selectedSeats().length;
  });

  readonly heldTotal = computed(() => {
    const price = this.seatMap()?.ticketPrice ?? 0;
    return price * this.heldSeats().length;
  });

  readonly formattedCountdown = computed(() => {
    const total = Math.max(0, this.remainingSeconds());
    const mins = Math.floor(total / 60);
    const secs = total % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  });

  private pollHandle: ReturnType<typeof setInterval> | null = null;
  private tickHandle: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('Invalid show ID.');
      this.loading.set(false);
      return;
    }
    this.showId.set(id);
    this.loadSeatMap(false);

    // Poll every 5s to reflect real-time seat locks & bookings from other users
    this.pollHandle = setInterval(() => this.loadSeatMap(true), 5000);

    // 1-second local countdown timer
    this.tickHandle = setInterval(() => {
      const current = this.remainingSeconds();
      if (current > 1) {
        this.remainingSeconds.set(current - 1);
      } else if (current === 1) {
        this.remainingSeconds.set(0);
        this.notice.set('Your 10-minute seat hold expired and the seats were automatically released.');
        this.loadSeatMap(true);
      }
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.pollHandle) clearInterval(this.pollHandle);
    if (this.tickHandle) clearInterval(this.tickHandle);
  }

  loadSeatMap(silent: boolean): void {
    if (silent) {
      this.refreshing.set(true);
    } else {
      this.loading.set(true);
    }

    this.api.getShowSeatMap(this.showId()).subscribe({
      next: (map) => {
        this.seatMap.set(map);
        this.remainingSeconds.set(map.myHoldRemainingSeconds);

        // Drop any pre-selected seats that became held or booked
        const availableSet = new Set(
          map.seats.filter((s) => s.status === 'AVAILABLE').map((s) => s.seatNumber)
        );
        this.selectedSeats.update((list) => list.filter((s) => availableSet.has(s)));

        this.loading.set(false);
        this.refreshing.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.error || 'Could not load seat map.');
        this.loading.set(false);
        this.refreshing.set(false);
      },
    });
  }

  seatsForRow(row: number): SeatStatusItem[] {
    return (this.seatMap()?.seats ?? []).filter((s) => s.rowNumber === row);
  }

  isSelected(seatNumber: string): boolean {
    return this.selectedSeats().includes(seatNumber);
  }

  seatTitle(seat: SeatStatusItem): string {
    switch (seat.status) {
      case 'BOOKED':
        return `${seat.seatNumber} — Booked`;
      case 'HELD':
        return `${seat.seatNumber} — Held by another user`;
      case 'HELD_BY_ME':
        return `${seat.seatNumber} — Held by you`;
      default:
        return `${seat.seatNumber} — Available`;
    }
  }

  toggleSeat(seatNumber: string): void {
    if (this.hasActiveHold()) return;
    this.error.set(null);
    this.notice.set(null);

    this.selectedSeats.update((current) => {
      if (current.includes(seatNumber)) {
        return current.filter((s) => s !== seatNumber);
      }
      return [...current, seatNumber].sort();
    });
  }

  holdSelectedSeats(): void {
    const seats = this.selectedSeats();
    if (seats.length === 0) return;

    this.busy.set(true);
    this.error.set(null);
    this.notice.set(null);

    this.api.holdSeats(this.showId(), seats).subscribe({
      next: (res) => {
        this.busy.set(false);
        this.selectedSeats.set([]);
        this.remainingSeconds.set(res.ttlSeconds);
        this.loadSeatMap(true);
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(err?.error?.error || 'Could not hold selected seats. Some may have just been taken.');
        this.loadSeatMap(true);
      },
    });
  }

  releaseHold(): void {
    this.busy.set(true);
    this.error.set(null);

    this.api.releaseHold(this.showId()).subscribe({
      next: () => {
        this.busy.set(false);
        this.remainingSeconds.set(0);
        this.notice.set('Seat hold released.');
        this.loadSeatMap(true);
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(err?.error?.error || 'Could not release hold.');
      },
    });
  }

  confirmAndPay(): void {
    const seats = this.heldSeats();
    if (seats.length === 0) return;

    this.busy.set(true);
    this.error.set(null);
    this.notice.set(null);
    this.uploadStatus.set(null);

    this.api.confirmBooking(this.showId(), seats).subscribe({
      next: (booking) => {
        this.busy.set(false);
        this.confirmedBooking.set(booking);
        this.remainingSeconds.set(0);
        this.loadSeatMap(true);

        // Generate client-side PDF ticket via jspdf and upload to backend (S3 / local disk)
        const blob = this.pdfService.createTicketBlob(booking);
        const filename = `${booking.bookingReference}.pdf`;
        this.api.uploadTicketPdf(booking.id, blob, filename).subscribe({
          next: (res) => {
            this.uploadStatus.set(`Ticket archived (${res.storageType})`);
          },
          error: () => {
            this.uploadStatus.set('Ticket ready for client download');
          },
        });
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(err?.error?.error || 'Booking confirmation failed.');
        this.loadSeatMap(true);
      },
    });
  }

  downloadTicket(booking: Booking): void {
    this.pdfService.downloadTicket(booking);
  }
}
