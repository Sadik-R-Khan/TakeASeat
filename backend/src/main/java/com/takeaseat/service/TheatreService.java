package com.takeaseat.service;

import com.takeaseat.dto.TheatreDtos.CreateTheatreRequest;
import com.takeaseat.dto.TheatreDtos.TheatreResponse;
import com.takeaseat.entity.Role;
import com.takeaseat.entity.Show;
import com.takeaseat.entity.Theatre;
import com.takeaseat.entity.TheatreSeat;
import com.takeaseat.entity.User;
import com.takeaseat.repository.*;
import com.takeaseat.util.SeatLayoutUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TheatreService {

    private final TheatreRepository theatreRepository;
    private final TheatreSeatRepository theatreSeatRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatLockService seatLockService;

    public List<TheatreResponse> getAllTheatres() {
        return theatreRepository.findAllWithOwner()
                .stream()
                .map(TheatreResponse::from)
                .toList();
    }

    public List<TheatreResponse> getOwnerTheatres(Long ownerId) {
        return theatreRepository.findByOwnerIdWithOwner(ownerId)
                .stream()
                .map(TheatreResponse::from)
                .toList();
    }

    /**
     * Creates a theatre and automatically generates all 50 fixed seats (1A..5J).
     */
    @Transactional
    public TheatreResponse createTheatre(CreateTheatreRequest request, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found"));

        Theatre theatre = Theatre.builder()
                .name(request.name().trim())
                .city(request.city().trim())
                .address(request.address().trim())
                .owner(owner)
                .totalRows(SeatLayoutUtil.TOTAL_ROWS)
                .totalCols(SeatLayoutUtil.TOTAL_COLS)
                .totalSeats(SeatLayoutUtil.TOTAL_SEATS)
                .build();

        Theatre saved = theatreRepository.save(theatre);
        generateFixedSeatsForTheatre(saved);

        return TheatreResponse.from(saved);
    }

    public void generateFixedSeatsForTheatre(Theatre theatre) {
        List<TheatreSeat> seats = new ArrayList<>(SeatLayoutUtil.TOTAL_SEATS);
        for (int row = 1; row <= SeatLayoutUtil.TOTAL_ROWS; row++) {
            for (char col : SeatLayoutUtil.COLUMNS) {
                seats.add(TheatreSeat.builder()
                        .theatre(theatre)
                        .seatNumber(row + String.valueOf(col))
                        .rowNumber(row)
                        .colLetter(String.valueOf(col))
                        .build());
            }
        }
        theatreSeatRepository.saveAll(seats);
    }

    @Transactional
    public void deleteTheatre(Long theatreId, Long requesterId, Role requesterRole) {
        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Theatre not found"));

        if (requesterRole != Role.ROLE_ADMIN && !theatre.getOwner().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only remove your own theatres");
        }

        List<Show> shows = showRepository.findByTheatreId(theatreId);
        if (!shows.isEmpty()) {
            List<Long> showIds = shows.stream().map(Show::getId).toList();
            bookingSeatRepository.deleteByShowIdIn(showIds);
            bookingRepository.deleteByShowIdIn(showIds);
            showRepository.deleteAll(shows);
            showIds.forEach(seatLockService::clearShowRedisState);
        }

        theatreSeatRepository.deleteByTheatreId(theatreId);
        theatreRepository.delete(theatre);
    }
}
