package com.takeaseat.service;

import com.takeaseat.dto.BookingDtos.BookingResponse;
import com.takeaseat.dto.BookingDtos.ConfirmBookingRequest;
import com.takeaseat.entity.*;
import com.takeaseat.repository.BookingRepository;
import com.takeaseat.repository.BookingSeatRepository;
import com.takeaseat.repository.ShowRepository;
import com.takeaseat.repository.UserRepository;
import com.takeaseat.util.SeatLayoutUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final SeatLockService seatLockService;

    /**
     * Verifies Redis seat lock ownership within the 10-minute window, persists the Booking
     * and BookingSeats atomically in MySQL, marks seats booked in Redis, and releases lock keys.
     */
    @Transactional
    public BookingResponse confirmBooking(ConfirmBookingRequest request, Long userId) {
        List<String> validSeats = SeatLayoutUtil.normalizeAndValidate(request.seatNumbers());

        Show show = showRepository.findByIdWithDetails(request.showId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));

        if (show.getStatus() != ShowStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This show has been cancelled");
        }

        // Verify user still owns active Redis locks for every requested seat
        if (!seatLockService.verifyUserOwnsLocks(show.getId(), validSeats, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Your 10-minute seat hold has expired or you do not hold all selected seats. Please select seats again."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        BigDecimal totalAmount = show.getTicketPrice().multiply(BigDecimal.valueOf(validSeats.size()));

        Booking booking = Booking.builder()
                .bookingReference(generateBookingReference())
                .user(user)
                .show(show)
                .totalAmount(totalAmount)
                .status(BookingStatus.CONFIRMED)
                .build();

        for (String seatNumber : validSeats) {
            booking.getBookedSeats().add(BookingSeat.builder()
                    .booking(booking)
                    .show(show)
                    .seatNumber(seatNumber)
                    .activeFlag(1)
                    .build());
        }

        // Flush immediately so MySQL unique constraint (show_id, seat_number, active_flag) triggers before Redis update
        Booking saved = bookingRepository.saveAndFlush(booking);

        // Update Redis booked set and delete temporary seat_lock:* keys
        seatLockService.markSeatsBookedAndReleaseLocks(show.getId(), validSeats, userId);

        log.info("Booking confirmed: ref={}, userId={}, showId={}, seats={}",
                saved.getBookingReference(), userId, show.getId(), validSeats);

        return BookingResponse.from(saved);
    }

    public List<BookingResponse> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdWithDetails(userId)
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAllWithDetails()
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    public BookingResponse getBookingById(Long bookingId, Long requesterId, Role requesterRole) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (requesterRole != Role.ROLE_ADMIN && !booking.getUser().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this booking");
        }
        return BookingResponse.from(booking);
    }

    /**
     * Cancels a booking (owner of booking or Admin), updates MySQL status to CANCELLED,
     * clears active_flag on BookingSeats, and frees seats in Redis.
     */
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long requesterId, Role requesterRole) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (requesterRole != Role.ROLE_ADMIN && !booking.getUser().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel your own bookings");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return BookingResponse.from(booking);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());

        List<String> freedSeats = booking.getBookedSeats().stream()
                .map(bs -> {
                    bs.setActiveFlag(null);
                    return bs.getSeatNumber();
                })
                .toList();

        Booking saved = bookingRepository.saveAndFlush(booking);
        seatLockService.unmarkSeatsBooked(booking.getShow().getId(), freedSeats);

        log.info("Booking cancelled: ref={}, byUserId={}, role={}, freedSeats={}",
                saved.getBookingReference(), requesterId, requesterRole, freedSeats);

        return BookingResponse.from(saved);
    }

    private String generateBookingReference() {
        String datePart = LocalDateTime.now().format(REF_DATE);
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "TAS-" + datePart + "-" + randomPart;
    }
}
