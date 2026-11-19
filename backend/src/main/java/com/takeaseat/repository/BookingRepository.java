package com.takeaseat.repository;

import com.takeaseat.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT DISTINCT b FROM Booking b " +
           "JOIN FETCH b.user " +
           "JOIN FETCH b.show s " +
           "JOIN FETCH s.movie " +
           "JOIN FETCH s.theatre " +
           "LEFT JOIN FETCH b.bookedSeats " +
           "WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(Long id);

    @Query("SELECT DISTINCT b FROM Booking b " +
           "JOIN FETCH b.user " +
           "JOIN FETCH b.show s " +
           "JOIN FETCH s.movie " +
           "JOIN FETCH s.theatre " +
           "LEFT JOIN FETCH b.bookedSeats " +
           "WHERE b.user.id = :userId " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdWithDetails(Long userId);

    @Query("SELECT DISTINCT b FROM Booking b " +
           "JOIN FETCH b.user " +
           "JOIN FETCH b.show s " +
           "JOIN FETCH s.movie " +
           "JOIN FETCH s.theatre " +
           "LEFT JOIN FETCH b.bookedSeats " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findAllWithDetails();

    List<Booking> findByShowId(Long showId);

    void deleteByShowIdIn(Collection<Long> showIds);
}
