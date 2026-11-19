package com.takeaseat.repository;

import com.takeaseat.entity.TheatreSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TheatreSeatRepository extends JpaRepository<TheatreSeat, Long> {
    List<TheatreSeat> findByTheatreIdOrderByRowNumberAscColLetterAsc(Long theatreId);
    void deleteByTheatreId(Long theatreId);
}
