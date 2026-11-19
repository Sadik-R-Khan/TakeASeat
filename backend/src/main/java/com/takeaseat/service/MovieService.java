package com.takeaseat.service;

import com.takeaseat.dto.MovieDtos.CreateMovieRequest;
import com.takeaseat.dto.MovieDtos.MovieResponse;
import com.takeaseat.entity.Movie;
import com.takeaseat.entity.Show;
import com.takeaseat.repository.BookingRepository;
import com.takeaseat.repository.BookingSeatRepository;
import com.takeaseat.repository.MovieRepository;
import com.takeaseat.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatLockService seatLockService;

    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(MovieResponse::from)
                .toList();
    }

    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));
        return MovieResponse.from(movie);
    }

    @Transactional
    public MovieResponse createMovie(CreateMovieRequest request) {
        if (request.posterUrl() == null || request.posterUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movie poster image is required");
        }

        Movie movie = Movie.builder()
                .title(request.title().trim())
                .description(request.description() != null ? request.description().trim() : "")
                .genre(request.genre().trim())
                .language(request.language().trim())
                .durationMinutes(request.durationMinutes())
                .rating(request.rating() != null ? request.rating().trim() : "U/A")
                .releaseDate(request.releaseDate())
                .posterUrl(request.posterUrl().trim())
                .build();

        return MovieResponse.from(movieRepository.save(movie));
    }

    @Transactional
    public void deleteMovie(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));

        List<Show> shows = showRepository.findByMovieId(movieId);
        if (!shows.isEmpty()) {
            List<Long> showIds = shows.stream().map(Show::getId).toList();
            bookingSeatRepository.deleteByShowIdIn(showIds);
            bookingRepository.deleteByShowIdIn(showIds);
            showRepository.deleteAll(shows);
            showIds.forEach(seatLockService::clearShowRedisState);
        }

        movieRepository.delete(movie);
    }
}
