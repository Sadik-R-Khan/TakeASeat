package com.takeaseat.controller;

import com.takeaseat.dto.TheatreDtos.CreateTheatreRequest;
import com.takeaseat.dto.TheatreDtos.TheatreResponse;
import com.takeaseat.security.UserPrincipal;
import com.takeaseat.service.TheatreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/theatres")
@RequiredArgsConstructor
public class TheatreController {

    private final TheatreService theatreService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'USER')")
    public List<TheatreResponse> listAllTheatres() {
        return theatreService.getAllTheatres();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('OWNER')")
    public List<TheatreResponse> listOwnerTheatres(@AuthenticationPrincipal UserPrincipal principal) {
        return theatreService.getOwnerTheatres(principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public TheatreResponse createTheatre(
            @Valid @RequestBody CreateTheatreRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return theatreService.createTheatre(request, principal.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public void deleteTheatre(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        theatreService.deleteTheatre(id, principal.getId(), principal.getRole());
    }
}
