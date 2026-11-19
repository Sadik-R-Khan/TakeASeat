package com.takeaseat.dto;

import com.takeaseat.entity.Show;
import com.takeaseat.util.SeatLayoutUtil;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ShowDtos {

    private ShowDtos() {
    }

    public record CreateShowRequest(
            @NotNull(message = "Movie ID is required")
            Long movieId,

            @NotNull(message = "Theatre ID is required")
            Long theatreId,

            @NotNull(message = "Show time is required")
            LocalDateTime showTime,

            @NotNull(message = "Ticket price is required")
            @DecimalMin(value = "0.01", message = "Ticket price must be positive")
            BigDecimal ticketPrice
    ) {}

    public record ShowResponse(
            Long id,
            Long movieId,
            String movieTitle,
            Long theatreId,
            String theatreName,
            String theatreCity,
            String theatreAddress,
            LocalDateTime showTime,
            BigDecimal ticketPrice,
            String status,
            int totalSeats,
            long bookedSeats
    ) {
        public static ShowResponse from(Show show, long bookedSeats) {
            return new ShowResponse(
                    show.getId(),
                    show.getMovie().getId(),
                    show.getMovie().getTitle(),
                    show.getTheatre().getId(),
                    show.getTheatre().getName(),
                    show.getTheatre().getCity(),
                    show.getTheatre().getAddress(),
                    show.getShowTime(),
                    show.getTicketPrice(),
                    show.getStatus().name(),
                    SeatLayoutUtil.TOTAL_SEATS,
                    bookedSeats
            );
        }
    }

    public record SeatStatusItem(
            String seatNumber,
            int rowNumber,
            String colLetter,
            String status // AVAILABLE, HELD, HELD_BY_ME, BOOKED
    ) {}

    public record ShowSeatMapResponse(
            Long showId,
            Long movieId,
            String movieTitle,
            String theatreName,
            String theatreCity,
            LocalDateTime showTime,
            BigDecimal ticketPrice,
            String showStatus,
            int totalRows,
            int totalCols,
            List<SeatStatusItem> seats,
            List<String> heldByMeSeats,
            long myHoldRemainingSeconds
    ) {}

    public record HoldSeatsRequest(
            @NotEmpty(message = "Select at least one seat to hold")
            List<String> seatNumbers
    ) {}

    public record HoldSeatsResponse(
            Long showId,
            List<String> seatNumbers,
            long ttlSeconds,
            BigDecimal totalAmount
    ) {}
}
