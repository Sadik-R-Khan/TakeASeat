export type UserRole = 'ROLE_USER' | 'ROLE_OWNER' | 'ROLE_ADMIN';

export interface AuthResponse {
  token: string | null;
  userId: number;
  name: string;
  email: string;
  role: UserRole;
}

export interface Movie {
  id: number;
  title: string;
  description: string;
  genre: string;
  language: string;
  durationMinutes: number;
  rating: string;
  releaseDate: string | null;
  posterUrl: string | null;
}

export interface CreateMovieRequest {
  title: string;
  description: string;
  genre: string;
  language: string;
  durationMinutes: number;
  rating: string;
  releaseDate: string | null;
  posterUrl: string;
}

export interface Theatre {
  id: number;
  name: string;
  city: string;
  address: string;
  ownerId: number;
  ownerName: string;
  ownerEmail: string;
  totalRows: number;
  totalCols: number;
  totalSeats: number;
}

export interface CreateTheatreRequest {
  name: string;
  city: string;
  address: string;
}

export interface Show {
  id: number;
  movieId: number;
  movieTitle: string;
  theatreId: number;
  theatreName: string;
  theatreCity: string;
  theatreAddress: string;
  showTime: string;
  ticketPrice: number;
  status: 'SCHEDULED' | 'CANCELLED';
  totalSeats: number;
  bookedSeats: number;
}

export interface CreateShowRequest {
  movieId: number;
  theatreId: number;
  showTime: string;
  ticketPrice: number;
}

export type SeatStatus = 'AVAILABLE' | 'HELD' | 'HELD_BY_ME' | 'BOOKED';

export interface SeatStatusItem {
  seatNumber: string;
  rowNumber: number;
  colLetter: string;
  status: SeatStatus;
}

export interface ShowSeatMapResponse {
  showId: number;
  movieId: number;
  movieTitle: string;
  theatreName: string;
  theatreCity: string;
  showTime: string;
  ticketPrice: number;
  showStatus: 'SCHEDULED' | 'CANCELLED';
  totalRows: number;
  totalCols: number;
  seats: SeatStatusItem[];
  heldByMeSeats: string[];
  myHoldRemainingSeconds: number;
}

export interface HoldSeatsResponse {
  showId: number;
  seatNumbers: string[];
  ttlSeconds: number;
  totalAmount: number;
}

export interface Booking {
  id: number;
  bookingReference: string;
  userId: number;
  userName: string;
  userEmail: string;
  showId: number;
  movieTitle: string;
  theatreName: string;
  theatreCity: string;
  theatreAddress: string;
  showTime: string;
  ticketPrice: number;
  seats: string[];
  totalAmount: number;
  status: 'CONFIRMED' | 'CANCELLED';
  ticketUrl: string | null;
  createdAt: string;
  cancelledAt: string | null;
}

export interface TicketUploadResponse {
  bookingId: number;
  bookingReference: string;
  storageType: string;
  ticketUrl: string;
}
