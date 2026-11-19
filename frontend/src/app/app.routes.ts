import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/role.guard';
import { LoginComponent } from './features/auth/login/login';
import { RegisterComponent } from './features/auth/register/register';
import { MovieListComponent } from './features/user/movie-list/movie-list';
import { MovieDetailComponent } from './features/user/movie-detail/movie-detail';
import { SeatSelectionComponent } from './features/user/seat-selection/seat-selection';
import { MyBookingsComponent } from './features/user/my-bookings/my-bookings';
import { OwnerDashboardComponent } from './features/owner/owner-dashboard/owner-dashboard';
import { AdminDashboardComponent } from './features/admin/admin-dashboard/admin-dashboard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  // User Panel
  { path: 'user/movies', component: MovieListComponent, canActivate: [authGuard] },
  { path: 'user/movies/:id', component: MovieDetailComponent, canActivate: [authGuard] },
  { path: 'user/shows/:id/seats', component: SeatSelectionComponent, canActivate: [authGuard] },
  { path: 'user/bookings', component: MyBookingsComponent, canActivate: [authGuard] },

  // Theatre Owner Panel
  { path: 'owner', component: OwnerDashboardComponent, canActivate: [roleGuard(['ROLE_OWNER'])] },

  // Admin Panel
  { path: 'admin', component: AdminDashboardComponent, canActivate: [roleGuard(['ROLE_ADMIN'])] },

  { path: '**', redirectTo: 'login' },
];
