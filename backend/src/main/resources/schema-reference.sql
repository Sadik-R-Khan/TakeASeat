-- TakeASeat MySQL Schema Reference
-- Hibernate (ddl-auto: update) creates/updates these tables automatically on startup.

CREATE DATABASE IF NOT EXISTS takeaseat
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE takeaseat;

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL, -- ROLE_USER, ROLE_OWNER, ROLE_ADMIN
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS movies (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    genre            VARCHAR(80)  NOT NULL,
    language         VARCHAR(60)  NOT NULL,
    duration_minutes INT          NOT NULL,
    rating           VARCHAR(20),
    release_date     DATE,
    poster_url       VARCHAR(500),
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS theatres (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    address     VARCHAR(300) NOT NULL,
    owner_id    BIGINT       NOT NULL,
    total_rows  INT          NOT NULL DEFAULT 5,
    total_cols  INT          NOT NULL DEFAULT 10,
    total_seats INT          NOT NULL DEFAULT 50,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_theatres_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Fixed 5x10 seat layout (1A..5J) auto-generated when a theatre is created
CREATE TABLE IF NOT EXISTS theatre_seats (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    theatre_id  BIGINT      NOT NULL,
    seat_number VARCHAR(5)  NOT NULL,
    seat_row    INT         NOT NULL,
    col_letter  VARCHAR(1)  NOT NULL,
    CONSTRAINT uk_theatre_seat UNIQUE (theatre_id, seat_number),
    CONSTRAINT fk_theatre_seats_theatre FOREIGN KEY (theatre_id) REFERENCES theatres(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `shows` (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id     BIGINT         NOT NULL,
    theatre_id   BIGINT         NOT NULL,
    show_time    DATETIME       NOT NULL,
    ticket_price DECIMAL(10, 2) NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, CANCELLED
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shows_movie   FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT fk_shows_theatre FOREIGN KEY (theatre_id) REFERENCES theatres(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bookings (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_reference VARCHAR(32)    NOT NULL UNIQUE,
    user_id           BIGINT         NOT NULL,
    show_id           BIGINT         NOT NULL,
    total_amount      DECIMAL(10, 2) NOT NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'CONFIRMED', -- CONFIRMED, CANCELLED
    ticket_url        VARCHAR(500),
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at      DATETIME       NULL,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_bookings_show FOREIGN KEY (show_id) REFERENCES `shows`(id) ON DELETE CASCADE
);

-- active_flag = 1 for active CONFIRMED bookings; NULL when CANCELLED so seats can be rebooked
-- while preserving full cancelled-booking seat history and DB-level uniqueness protection.
CREATE TABLE IF NOT EXISTS booking_seats (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id  BIGINT     NOT NULL,
    show_id     BIGINT     NOT NULL,
    seat_number VARCHAR(5) NOT NULL,
    active_flag INT        NULL,
    CONSTRAINT uk_show_seat_active UNIQUE (show_id, seat_number, active_flag),
    CONSTRAINT fk_booking_seats_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_seats_show    FOREIGN KEY (show_id) REFERENCES `shows`(id) ON DELETE CASCADE
);
