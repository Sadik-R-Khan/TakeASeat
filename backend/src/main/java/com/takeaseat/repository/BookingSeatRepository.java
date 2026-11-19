package com.takeaseat.repository;

import com.takeaseat.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    @Query("SELECT bs.seatNumber FROM BookingSeat bs WHERE bs.show.id = :showId AND bs.activeFlag = 1")
    List<String> findActiveSeatNumbersByShowId(Long showId);

    @Query("SELECT COUNT(bs) FROM BookingSeat bs WHERE bs.show.id = :showId AND bs.activeFlag = 1")
    long countActiveSeatsByShowId(Long showId);

    @Query("SELECT bs.show.id, COUNT(bs) FROM BookingSeat bs WHERE bs.show.id IN :showIds AND bs.activeFlag = 1 GROUP BY bs.show.id")
    List<Object[]> countActiveSeatsGroupedByShowIds(Collection<Long> showIds);

    void deleteByShowIdIn(Collection<Long> showIds);
}
