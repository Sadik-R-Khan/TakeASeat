package com.takeaseat.controller;

import com.takeaseat.dto.ShowDtos.*;
import com.takeaseat.security.UserPrincipal;
import com.takeaseat.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping("/{id}")
    public ShowResponse getShow(@PathVariable Long id) {
        return showService.getShowById(id);
    }

    @GetMapping("/{id}/seats")
    public ShowSeatMapResponse getSeatMap(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return showService.getSeatMap(id, principal != null ? principal.getId() : null);
    }

    @GetMapping("/owner")
    @PreAuthorize("hasRole('OWNER')")
    public List<ShowResponse> listOwnerShows(@AuthenticationPrincipal UserPrincipal principal) {
        return showService.getOwnerShows(principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public ShowResponse createShow(
            @Valid @RequestBody CreateShowRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return showService.createShow(request, principal.getId());
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ShowResponse cancelShow(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return showService.cancelShow(id, principal.getId(), principal.getRole());
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public HoldSeatsResponse holdSeats(
            @PathVariable Long id,
            @Valid @RequestBody HoldSeatsRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return showService.holdSeats(id, request.seatNumbers(), principal.getId());
    }

    @DeleteMapping("/{id}/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public void releaseHold(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        showService.releaseHold(id, principal.getId());
    }
}
