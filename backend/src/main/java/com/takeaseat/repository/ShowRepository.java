package com.takeaseat.repository;

import com.takeaseat.entity.Show;
import com.takeaseat.entity.ShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("SELECT s FROM Show s JOIN FETCH s.movie JOIN FETCH s.theatre t JOIN FETCH t.owner WHERE s.id = :id")
    Optional<Show> findByIdWithDetails(Long id);

    @Query("SELECT s FROM Show s JOIN FETCH s.movie JOIN FETCH s.theatre WHERE s.movie.id = :movieId AND s.status = :status ORDER BY s.showTime ASC")
    List<Show> findByMovieIdAndStatusWithDetails(Long movieId, ShowStatus status);

    @Query("SELECT s FROM Show s JOIN FETCH s.movie JOIN FETCH s.theatre t WHERE t.owner.id = :ownerId ORDER BY s.showTime DESC")
    List<Show> findByOwnerIdWithDetails(Long ownerId);

    List<Show> findByTheatreId(Long theatreId);

    List<Show> findByMovieId(Long movieId);
}
