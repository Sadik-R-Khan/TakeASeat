package com.takeaseat.controller;

import com.takeaseat.dto.MovieDtos.CreateMovieRequest;
import com.takeaseat.dto.MovieDtos.MovieResponse;
import com.takeaseat.dto.ShowDtos.ShowResponse;
import com.takeaseat.service.MoviePosterService;
import com.takeaseat.service.MovieService;
import com.takeaseat.service.ShowService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final ShowService showService;
    private final MoviePosterService moviePosterService;

    @GetMapping
    public List<MovieResponse> listMovies() {
        return movieService.getAllMovies();
    }

    @GetMapping("/{id}")
    public MovieResponse getMovie(@PathVariable Long id) {
        return movieService.getMovieById(id);
    }

    @GetMapping("/{id}/shows")
    public List<ShowResponse> listShowsForMovie(@PathVariable Long id) {
        return showService.getScheduledShowsByMovie(id);
    }

    @PostMapping(value = "/posters", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> uploadPoster(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();
        String posterUrl = moviePosterService.uploadPoster(file, baseUrl);
        return Map.of("posterUrl", posterUrl);
    }

    @GetMapping("/posters/{filename}")
    public ResponseEntity<byte[]> getPoster(@PathVariable String filename) {
        MoviePosterService.PosterData poster = moviePosterService.loadPoster(filename);
        return ResponseEntity.ok()
                .contentType(poster.mediaType())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                .body(poster.bytes());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MovieResponse createMovie(@Valid @RequestBody CreateMovieRequest request) {
        return movieService.createMovie(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
    }
}
