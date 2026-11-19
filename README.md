# TakeASeat

A full-stack, high-concurrency movie and show ticket booking platform built with **Spring Boot 3 (Java 17)**, **Angular (TypeScript)**, **MySQL**, **Redis**, and **AWS S3** (with local-disk fallback).

---

## Architecture & High-Concurrency Seat Locking

- **Fixed 5 × 10 Seat Layout (50 seats):** Every theatre automatically generates seats `1A`–`1J` through `5A`–`5J` upon creation.
- **Atomic Seat Holds (Redis Lua Script):** `POST /api/shows/{id}/hold` executes [`lock_seats.lua`](backend/src/main/resources/scripts/lock_seats.lua) via `DefaultRedisScript<Long>`, atomically verifying that every requested seat is neither booked nor locked by another user, and setting `seat_lock:{showId}:{seatNumber} -> userId` with a **600-second (10-minute) TTL**. If even one seat is unavailable, zero seats are locked.
- **Automatic Hold Release (Redis Keyspace Notifications):** `SeatLockExpirationListener` extends `KeyExpirationEventMessageListener` and listens for expired `seat_lock:*` keys so abandoned carts release automatically after 10 minutes.
- **Transactional Checkout & Double-Booking Guard:** `POST /api/bookings/confirm` verifies active Redis lock ownership, persists `bookings` and `booking_seats` inside `@Transactional`, adds confirmed seats to `show_booked:{showId}`, and removes the temporary lock keys. A MySQL unique constraint `uk_show_seat_active (show_id, seat_number, active_flag)` guarantees zero double bookings while allowing cancelled seats (`active_flag = NULL`) to be rebooked cleanly.
- **Client-Side PDF Tickets + Storage:** Tickets are generated on the client using `jspdf` and uploaded to `POST /api/tickets/{bookingId}/upload`. When `aws.s3.enabled=false` (default), PDFs are stored locally under `./uploads/tickets/`; when `aws.s3.enabled=true`, PDFs are uploaded to the configured AWS S3 bucket using AWS SDK v2.

---

## Prerequisites

- **Java 17+**
- **Node.js 20+** and **npm**
- **MySQL 8.x**
- **Redis 6+**

---

## 1. MySQL Setup

Start MySQL and create the database (or allow `createDatabaseIfNotExist=true` in `application.yml` to create it automatically):

```sql
CREATE DATABASE IF NOT EXISTS takeaseat
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Reference DDL is available at [`backend/src/main/resources/schema-reference.sql`](backend/src/main/resources/schema-reference.sql). Hibernate (`ddl-auto: update`) creates and updates all tables (`users`, `movies`, `theatres`, `theatre_seats`, `shows`, `bookings`, `booking_seats`) automatically on startup.

---

## 2. Redis Setup (Required for Keyspace Expiration Events)

Start Redis and enable expired-key notifications:

```bash
redis-cli CONFIG SET notify-keyspace-events Ex
```

Verify it is active:

```bash
redis-cli CONFIG GET notify-keyspace-events
# Expected output:
# 1) "notify-keyspace-events"
# 2) "xE" (or "Ex")
```

To persist this setting across Redis restarts, add the following line to your `redis.conf`:

```text
notify-keyspace-events Ex
```

---

## 3. Running the Backend (Spring Boot)

All connection settings live in [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml) and can be overridden via environment variables:

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/takeaseat?...` | MySQL JDBC URL |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `root` | MySQL password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | *(empty)* | Redis password |
| `JWT_SECRET` | *(dev default)* | Base64-encoded 256-bit+ HMAC secret |
| `AWS_S3_ENABLED` | `false` | `false` = local disk fallback (`./uploads/tickets`), `true` = AWS S3 |
| `AWS_S3_BUCKET` | `takeaseat-tickets` | Target S3 bucket |
| `AWS_S3_REGION` | `us-east-1` | AWS region |
| `AWS_ACCESS_KEY_ID` | *(empty)* | AWS access key (or use IAM role/default chain) |
| `AWS_SECRET_ACCESS_KEY` | *(empty)* | AWS secret key |

Start the backend from `backend/`:

```bash
# Windows (PowerShell / CMD)
cd backend
.\mvnw.cmd spring-boot:run

# macOS / Linux
cd backend
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

---

## 4. Running the Frontend (Angular)

```bash
cd frontend
npm install
npm start
```

Open `http://localhost:4200` in your browser. The dev server connects to `http://localhost:8080/api`.

---

## 5. Seed Credentials

On first startup against an empty database, `DataSeeder` creates sample movies, two theatres (with 50 auto-generated seats each), scheduled shows, and three accounts:

| Role | Email | Password | Dashboard Route |
|---|---|---|---|
| **Admin** (`ROLE_ADMIN`) | `admin@takeaseat.com` | `admin123` | `/admin` |
| **Theatre Owner** (`ROLE_OWNER`) | `owner@takeaseat.com` | `owner123` | `/owner` |
| **User** (`ROLE_USER`) | `user@takeaseat.com` | `user123` | `/user/movies` |

New User and Theatre Owner accounts can also be created on `/register`.

---

## 6. REST API Overview

| Group | Method & Path | Role(s) | Purpose |
|---|---|---|---|
| **Auth** | `POST /api/auth/register` | Public | Register `ROLE_USER` or `ROLE_OWNER` |
| | `POST /api/auth/login` | Public | Authenticate & obtain JWT |
| | `GET /api/auth/me` | Authenticated | Current user profile |
| **Movies** | `GET /api/movies` | Public | List ongoing movies |
| | `GET /api/movies/{id}` | Public | Movie details |
| | `GET /api/movies/{id}/shows` | Public | Scheduled shows for a movie |
| | `POST /api/movies` | `ADMIN` | Add a movie to the catalog |
| | `DELETE /api/movies/{id}` | `ADMIN` | Remove a movie |
| **Theatres** | `GET /api/theatres` | Authenticated | List all theatres |
| | `GET /api/theatres/mine` | `OWNER` | List owner's theatres |
| | `POST /api/theatres` | `OWNER` | Add theatre (auto-generates `1A`–`5J`) |
| | `DELETE /api/theatres/{id}` | `OWNER`, `ADMIN` | Remove own theatre (`OWNER`) or any (`ADMIN`) |
| **Shows** | `GET /api/shows/{id}` | Authenticated | Show details |
| | `GET /api/shows/{id}/seats` | Authenticated | Live 5×10 seat map + active hold TTL |
| | `GET /api/shows/owner` | `OWNER` | Owner's shows with booked counts |
| | `POST /api/shows` | `OWNER` | Schedule a movie showtime |
| | `PATCH /api/shows/{id}/cancel` | `OWNER`, `ADMIN` | Cancel a scheduled show |
| | `POST /api/shows/{id}/hold` | Authenticated | Atomically lock seats for 10 minutes (Lua) |
| | `DELETE /api/shows/{id}/hold` | Authenticated | Release current user's active seat locks |
| **Bookings** | `POST /api/bookings/confirm` | Authenticated | Verify Redis locks & confirm booking in MySQL |
| | `GET /api/bookings/mine` | Authenticated | Current user's bookings |
| | `GET /api/bookings` | `ADMIN` | All system bookings |
| | `PATCH /api/bookings/{id}/cancel` | Authenticated | Cancel own booking or any booking (`ADMIN`) |
| **Tickets** | `POST /api/tickets/{bookingId}/upload` | Authenticated | Upload client-generated PDF to S3 / local disk |
| | `GET /api/tickets/{bookingId}/download` | Authenticated | Download stored PDF ticket |
