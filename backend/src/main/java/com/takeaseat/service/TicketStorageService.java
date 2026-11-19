package com.takeaseat.service;

import com.takeaseat.dto.BookingDtos.TicketUploadResponse;
import com.takeaseat.entity.Booking;
import com.takeaseat.entity.Role;
import com.takeaseat.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class TicketStorageService {

    private final BookingRepository bookingRepository;
    private final S3Client s3Client;
    private final boolean s3Enabled;
    private final String s3Bucket;
    private final String s3Region;
    private final Path localStorageDir;

    public TicketStorageService(
            BookingRepository bookingRepository,
            @Autowired(required = false) S3Client s3Client,
            @Value("${aws.s3.enabled:false}") boolean s3Enabled,
            @Value("${aws.s3.bucket:takeaseat-tickets}") String s3Bucket,
            @Value("${aws.s3.region:us-east-1}") String s3Region,
            @Value("${app.storage.local-dir:./uploads/tickets}") String localDir
    ) {
        this.bookingRepository = bookingRepository;
        this.s3Client = s3Client;
        this.s3Enabled = s3Enabled;
        this.s3Bucket = s3Bucket;
        this.s3Region = s3Region;
        this.localStorageDir = Paths.get(localDir).toAbsolutePath().normalize();
    }

    @Transactional
    public TicketUploadResponse uploadTicketPdf(
            Long bookingId,
            MultipartFile file,
            Long requesterId,
            Role requesterRole
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF file is required");
        }

        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (requesterRole != Role.ROLE_ADMIN && !booking.getUser().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot upload a ticket for another user's booking");
        }

        String filename = booking.getBookingReference() + ".pdf";
        String ticketUrl;
        String storageType;

        try {
            byte[] pdfBytes = file.getBytes();

            if (s3Enabled && s3Client != null) {
                String key = "tickets/" + filename;
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(s3Bucket)
                                .key(key)
                                .contentType("application/pdf")
                                .build(),
                        RequestBody.fromBytes(pdfBytes)
                );
                ticketUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", s3Bucket, s3Region, key);
                storageType = "S3";
                log.info("Uploaded ticket PDF to S3: bucket={}, key={}", s3Bucket, key);
            } else {
                Files.createDirectories(localStorageDir);
                Path destination = localStorageDir.resolve(filename).normalize();
                if (!destination.startsWith(localStorageDir)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
                }
                Files.write(destination, pdfBytes);
                ticketUrl = "/api/tickets/" + bookingId + "/download";
                storageType = "LOCAL";
                log.info("Saved ticket PDF to local disk: {}", destination);
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store ticket PDF", ex);
        }

        booking.setTicketUrl(ticketUrl);
        bookingRepository.save(booking);

        return new TicketUploadResponse(booking.getId(), booking.getBookingReference(), storageType, ticketUrl);
    }

    public byte[] downloadTicketPdf(Long bookingId, Long requesterId, Role requesterRole) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (requesterRole != Role.ROLE_ADMIN && !booking.getUser().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this ticket");
        }

        String filename = booking.getBookingReference() + ".pdf";

        if (s3Enabled && s3Client != null) {
            String key = "tickets/" + filename;
            ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(s3Bucket).key(key).build()
            );
            return bytes.asByteArray();
        }

        Path filePath = localStorageDir.resolve(filename).normalize();
        if (!filePath.startsWith(localStorageDir) || !Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored ticket PDF not found");
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read ticket PDF", ex);
        }
    }
}
