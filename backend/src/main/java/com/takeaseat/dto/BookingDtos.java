package com.takeaseat.dto;

import com.takeaseat.entity.Booking;
import com.takeaseat.entity.BookingSeat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record ConfirmBookingRequest(
            @NotNull(message = "Show ID is required")
            Long showId,

            @NotEmpty(message = "At least one seat is required")
            List<String> seatNumbers
    ) {}

    public record BookingResponse(
            Long id,
            String bookingReference,
            Long userId,
            String userName,
            String userEmail,
            Long showId,
            String movieTitle,
            String theatreName,
            String theatreCity,
            String theatreAddress,
            LocalDateTime showTime,
            BigDecimal ticketPrice,
            List<String> seats,
            BigDecimal totalAmount,
            String status,
            String ticketUrl,
            LocalDateTime createdAt,
            LocalDateTime cancelledAt
    ) {
        public static BookingResponse from(Booking booking) {
            List<String> seatList = booking.getBookedSeats().stream()
                    .map(BookingSeat::getSeatNumber)
                    .sorted(Comparator.naturalOrder())
                    .toList();

            return new BookingResponse(
                    booking.getId(),
                    booking.getBookingReference(),
                    booking.getUser().getId(),
                    booking.getUser().getName(),
                    booking.getUser().getEmail(),
                    booking.getShow().getId(),
                    booking.getShow().getMovie().getTitle(),
                    booking.getShow().getTheatre().getName(),
                    booking.getShow().getTheatre().getCity(),
                    booking.getShow().getTheatre().getAddress(),
                    booking.getShow().getShowTime(),
                    booking.getShow().getTicketPrice(),
                    seatList,
                    booking.getTotalAmount(),
                    booking.getStatus().name(),
                    booking.getTicketUrl(),
                    booking.getCreatedAt(),
                    booking.getCancelledAt()
            );
        }
    }

    public record TicketUploadResponse(
            Long bookingId,
            String bookingReference,
            String storageType,
            String ticketUrl
    ) {}
}
