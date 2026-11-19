package com.takeaseat.controller;

import com.takeaseat.dto.BookingDtos.BookingResponse;
import com.takeaseat.dto.BookingDtos.ConfirmBookingRequest;
import com.takeaseat.security.UserPrincipal;
import com.takeaseat.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public BookingResponse confirmBooking(
            @Valid @RequestBody ConfirmBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return bookingService.confirmBooking(request, principal.getId());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal UserPrincipal principal) {
        return bookingService.getUserBookings(principal.getId());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingResponse> allBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public BookingResponse getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return bookingService.getBookingById(id, principal.getId(), principal.getRole());
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public BookingResponse cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return bookingService.cancelBooking(id, principal.getId(), principal.getRole());
    }
}
