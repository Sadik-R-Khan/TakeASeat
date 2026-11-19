package com.takeaseat.service;

import com.takeaseat.dto.ShowDtos.*;
import com.takeaseat.entity.*;
import com.takeaseat.repository.*;
import com.takeaseat.util.SeatLayoutUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final TheatreSeatRepository theatreSeatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatLockService seatLockService;

    public List<ShowResponse> getScheduledShowsByMovie(Long movieId) {
        List<Show> shows = showRepository.findByMovieIdAndStatusWithDetails(movieId, ShowStatus.SCHEDULED);
        return mapWithBookedCounts(shows);
    }

    public List<ShowResponse> getOwnerShows(Long ownerId) {
        List<Show> shows = showRepository.findByOwnerIdWithDetails(ownerId);
        return mapWithBookedCounts(shows);
    }

    public ShowResponse getShowById(Long showId) {
        Show show = showRepository.findByIdWithDetails(showId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));
        long bookedCount = bookingSeatRepository.countActiveSeatsByShowId(showId);
        return ShowResponse.from(show, bookedCount);
    }

    @Transactional
    public ShowResponse createShow(CreateShowRequest request, Long ownerId) {
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));

        Theatre theatre = theatreRepository.findById(request.theatreId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Theatre not found"));

        if (!theatre.getOwner().getId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only schedule shows in your own theatres");
        }

        // Ensure fixed 5x10 seats exist for this theatre
        if (theatreSeatRepository.findByTheatreIdOrderByRowNumberAscColLetterAsc(theatre.getId()).isEmpty()) {
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

        Show show = Show.builder()
                .movie(movie)
                .theatre(theatre)
                .showTime(request.showTime())
                .ticketPrice(request.ticketPrice())
                .status(ShowStatus.SCHEDULED)
                .build();

        Show saved = showRepository.save(show);
        return ShowResponse.from(saved, 0L);
    }

    @Transactional
    public ShowResponse cancelShow(Long showId, Long requesterId, Role requesterRole) {
        Show show = showRepository.findByIdWithDetails(showId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));

        if (requesterRole != Role.ROLE_ADMIN && !show.getTheatre().getOwner().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel shows for your own theatres");
        }

        if (show.getStatus() == ShowStatus.CANCELLED) {
            return ShowResponse.from(show, 0L);
        }

        show.setStatus(ShowStatus.CANCELLED);
        showRepository.save(show);

        // Cancel any confirmed bookings for this show and free seats
        List<Booking> showBookings = bookingRepository.findByShowId(showId);
        LocalDateTime now = LocalDateTime.now();
        for (Booking booking : showBookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelledAt(now);
                for (BookingSeat bs : booking.getBookedSeats()) {
                    bs.setActiveFlag(null);
                }
            }
        }
        bookingRepository.saveAll(showBookings);
        seatLockService.clearShowRedisState(showId);

        return ShowResponse.from(show, 0L);
    }

    public ShowSeatMapResponse getSeatMap(Long showId, Long currentUserId) {
        Show show = showRepository.findByIdWithDetails(showId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));

        Set<String> bookedSeats = new HashSet<>(bookingSeatRepository.findActiveSeatNumbersByShowId(showId));
        Map<String, String> lockHolders = seatLockService.getSeatLockHolders(showId);

        String currentUserIdStr = currentUserId != null ? String.valueOf(currentUserId) : null;
        List<SeatStatusItem> items = new ArrayList<>(SeatLayoutUtil.TOTAL_SEATS);
        List<String> heldByMe = new ArrayList<>();

        for (int row = 1; row <= SeatLayoutUtil.TOTAL_ROWS; row++) {
            for (char col : SeatLayoutUtil.COLUMNS) {
                String seatNum = row + String.valueOf(col);
                String status;
                if (bookedSeats.contains(seatNum)) {
                    status = "BOOKED";
                } else if (lockHolders.containsKey(seatNum)) {
                    String holder = lockHolders.get(seatNum);
                    if (currentUserIdStr != null && currentUserIdStr.equals(holder)) {
                        status = "HELD_BY_ME";
                        heldByMe.add(seatNum);
                    } else {
                        status = "HELD";
                    }
                } else {
                    status = "AVAILABLE";
                }

                items.add(new SeatStatusItem(seatNum, row, String.valueOf(col), status));
            }
        }

        long remainingTtl = seatLockService.getRemainingTtlSeconds(showId, heldByMe);

        return new ShowSeatMapResponse(
                show.getId(),
                show.getMovie().getId(),
                show.getMovie().getTitle(),
                show.getTheatre().getName(),
                show.getTheatre().getCity(),
                show.getShowTime(),
                show.getTicketPrice(),
                show.getStatus().name(),
                SeatLayoutUtil.TOTAL_ROWS,
                SeatLayoutUtil.TOTAL_COLS,
                items,
                heldByMe,
                remainingTtl
        );
    }

    public HoldSeatsResponse holdSeats(Long showId, List<String> seatNumbers, Long userId) {
        Show show = showRepository.findByIdWithDetails(showId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));

        if (show.getStatus() != ShowStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot hold seats for a cancelled show");
        }

        List<String> validSeats = SeatLayoutUtil.normalizeAndValidate(seatNumbers);
        seatLockService.acquireLocks(showId, validSeats, userId);

        BigDecimal totalAmount = show.getTicketPrice().multiply(BigDecimal.valueOf(validSeats.size()));
        return new HoldSeatsResponse(showId, validSeats, seatLockService.getLockTtlSeconds(), totalAmount);
    }

    public void releaseHold(Long showId, Long userId) {
        seatLockService.releaseAllUserLocksForShow(showId, userId);
    }

    private List<ShowResponse> mapWithBookedCounts(List<Show> shows) {
        if (shows.isEmpty()) {
            return List.of();
        }
        List<Long> showIds = shows.stream().map(Show::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : bookingSeatRepository.countActiveSeatsGroupedByShowIds(showIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return shows.stream()
                .map(s -> ShowResponse.from(s, counts.getOrDefault(s.getId(), 0L)))
                .toList();
    }
}
