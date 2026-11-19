package com.takeaseat.dto;

import com.takeaseat.entity.Movie;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public final class MovieDtos {

    private MovieDtos() {
    }

    public record CreateMovieRequest(
            @NotBlank(message = "Title is required")
            String title,

            String description,

            @NotBlank(message = "Genre is required")
            String genre,

            @NotBlank(message = "Language is required")
            String language,

            @NotNull(message = "Duration is required")
            @Min(value = 1, message = "Duration must be at least 1 minute")
            Integer durationMinutes,

            String rating,

            LocalDate releaseDate,

            @NotBlank(message = "Movie poster image is required")
            String posterUrl
    ) {}

    public record MovieResponse(
            Long id,
            String title,
            String description,
            String genre,
            String language,
            Integer durationMinutes,
            String rating,
            LocalDate releaseDate,
            String posterUrl
    ) {
        public static MovieResponse from(Movie movie) {
            return new MovieResponse(
                    movie.getId(),
                    movie.getTitle(),
                    movie.getDescription(),
                    movie.getGenre(),
                    movie.getLanguage(),
                    movie.getDurationMinutes(),
                    movie.getRating(),
                    movie.getReleaseDate(),
                    movie.getPosterUrl()
            );
        }
    }
}
