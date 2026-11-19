package com.takeaseat.config;

import com.takeaseat.entity.*;
import com.takeaseat.repository.*;
import com.takeaseat.service.TheatreService;
import com.takeaseat.util.SeatLayoutUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ShowRepository showRepository;
    private final TheatreService theatreService;
    private final PasswordEncoder passwordEncoder;

    @Value("${server.port:8080}")
    private int serverPort;

    @Override
    @Transactional
    public void run(String... args) {
        String posterBase = "http://localhost:" + serverPort + "/api/movies/posters/";

        if (userRepository.count() > 0) {
            backfillSeededMoviePosters(posterBase);
            return;
        }

        log.info("Seeding initial TakeASeat users, movies, theatres (5x10 seats), and shows...");

        userRepository.save(User.builder()
                .name("Platform Admin")
                .email("admin@takeaseat.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(Role.ROLE_ADMIN)
                .build());

        User owner = userRepository.save(User.builder()
                .name("Grand Cinemas Owner")
                .email("owner@takeaseat.com")
                .passwordHash(passwordEncoder.encode("owner123"))
                .role(Role.ROLE_OWNER)
                .build());

        userRepository.save(User.builder()
                .name("Alex Viewer")
                .email("user@takeaseat.com")
                .passwordHash(passwordEncoder.encode("user123"))
                .role(Role.ROLE_USER)
                .build());

        Movie movie1 = movieRepository.save(Movie.builder()
                .title("Interstellar")
                .description("A team of explorers travels through a wormhole in space in an attempt to ensure humanity's survival.")
                .genre("Sci-Fi / Drama")
                .language("English")
                .durationMinutes(169)
                .rating("PG-13")
                .releaseDate(LocalDate.of(2014, 11, 7))
                .posterUrl(posterBase + "interstellar.svg")
                .build());

        Movie movie2 = movieRepository.save(Movie.builder()
                .title("The Grand Budapest Hotel")
                .description("A writer encounters the owner of an aging high-class hotel who tells him of his early years serving as a lobby boy.")
                .genre("Comedy / Drama")
                .language("English")
                .durationMinutes(99)
                .rating("R")
                .releaseDate(LocalDate.of(2014, 3, 28))
                .posterUrl(posterBase + "grand-budapest.svg")
                .build());

        Movie movie3 = movieRepository.save(Movie.builder()
                .title("Dune: Part Two")
                .description("Paul Atreides unites with the Fremen while on a path of revenge against the conspirators who destroyed his family.")
                .genre("Sci-Fi / Adventure")
                .language("English")
                .durationMinutes(166)
                .rating("PG-13")
                .releaseDate(LocalDate.of(2024, 3, 1))
                .posterUrl(posterBase + "dune-part-two.svg")
                .build());

        Theatre theatre1 = theatreRepository.save(Theatre.builder()
                .name("Grand Cinema Screen 1")
                .city("Bengaluru")
                .address("100 MG Road, Indiranagar")
                .owner(owner)
                .totalRows(SeatLayoutUtil.TOTAL_ROWS)
                .totalCols(SeatLayoutUtil.TOTAL_COLS)
                .totalSeats(SeatLayoutUtil.TOTAL_SEATS)
                .build());
        theatreService.generateFixedSeatsForTheatre(theatre1);

        Theatre theatre2 = theatreRepository.save(Theatre.builder()
                .name("Metroplex Downtown")
                .city("Mumbai")
                .address("42 Linking Road, Bandra West")
                .owner(owner)
                .totalRows(SeatLayoutUtil.TOTAL_ROWS)
                .totalCols(SeatLayoutUtil.TOTAL_COLS)
                .totalSeats(SeatLayoutUtil.TOTAL_SEATS)
                .build());
        theatreService.generateFixedSeatsForTheatre(theatre2);

        LocalDateTime tomorrow = LocalDate.now().plusDays(1).atTime(18, 0);

        showRepository.save(Show.builder()
                .movie(movie1)
                .theatre(theatre1)
                .showTime(tomorrow)
                .ticketPrice(new BigDecimal("250.00"))
                .status(ShowStatus.SCHEDULED)
                .build());

        showRepository.save(Show.builder()
                .movie(movie1)
                .theatre(theatre2)
                .showTime(tomorrow.plusHours(3))
                .ticketPrice(new BigDecimal("300.00"))
                .status(ShowStatus.SCHEDULED)
                .build());

        showRepository.save(Show.builder()
                .movie(movie2)
                .theatre(theatre1)
                .showTime(tomorrow.plusDays(1).withHour(15).withMinute(30))
                .ticketPrice(new BigDecimal("200.00"))
                .status(ShowStatus.SCHEDULED)
                .build());

        showRepository.save(Show.builder()
                .movie(movie3)
                .theatre(theatre2)
                .showTime(tomorrow.plusDays(1).withHour(20).withMinute(0))
                .ticketPrice(new BigDecimal("350.00"))
                .status(ShowStatus.SCHEDULED)
                .build());

        log.info("TakeASeat seed complete.");
    }

    private void backfillSeededMoviePosters(String posterBase) {
        Map<String, String> defaults = Map.of(
                "Interstellar", posterBase + "interstellar.svg",
                "The Grand Budapest Hotel", posterBase + "grand-budapest.svg",
                "Dune: Part Two", posterBase + "dune-part-two.svg"
        );
        for (Movie movie : movieRepository.findAll()) {
            if (movie.getPosterUrl() == null || movie.getPosterUrl().isBlank()) {
                String fallback = defaults.get(movie.getTitle());
                if (fallback != null) {
                    movie.setPosterUrl(fallback);
                    movieRepository.save(movie);
                }
            }
        }
    }
}
