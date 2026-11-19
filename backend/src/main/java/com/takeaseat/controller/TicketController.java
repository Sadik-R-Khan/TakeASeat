package com.takeaseat.controller;

import com.takeaseat.dto.BookingDtos.TicketUploadResponse;
import com.takeaseat.security.UserPrincipal;
import com.takeaseat.service.TicketStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketStorageService ticketStorageService;

    @PostMapping(value = "/{bookingId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public TicketUploadResponse uploadTicket(
            @PathVariable Long bookingId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ticketStorageService.uploadTicketPdf(bookingId, file, principal.getId(), principal.getRole());
    }

    @GetMapping(value = "/{bookingId}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadTicket(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        byte[] bytes = ticketStorageService.downloadTicketPdf(bookingId, principal.getId(), principal.getRole());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("ticket-" + bookingId + ".pdf").build()
        );
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
