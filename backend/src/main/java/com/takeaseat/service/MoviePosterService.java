package com.takeaseat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class MoviePosterService {

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif",
            "image/svg+xml", ".svg"
    );

    public record PosterData(byte[] bytes, MediaType mediaType) {}

    private final S3Client s3Client;
    private final boolean s3Enabled;
    private final String s3Bucket;
    private final String s3Region;
    private final Path localPostersDir;

    public MoviePosterService(
            @Autowired(required = false) S3Client s3Client,
            @Value("${aws.s3.enabled:false}") boolean s3Enabled,
            @Value("${aws.s3.bucket:takeaseat-tickets}") String s3Bucket,
            @Value("${aws.s3.region:us-east-1}") String s3Region,
            @Value("${app.storage.posters-dir:./uploads/posters}") String postersDir
    ) {
        this.s3Client = s3Client;
        this.s3Enabled = s3Enabled;
        this.s3Bucket = s3Bucket;
        this.s3Region = s3Region;
        this.localPostersDir = Paths.get(postersDir).toAbsolutePath().normalize();
    }

    public String uploadPoster(MultipartFile file, String baseUrl) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poster image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.containsKey(contentType.toLowerCase())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported image format. Please upload JPEG, PNG, WebP, or GIF."
            );
        }

        String ext = ALLOWED_TYPES.get(contentType.toLowerCase());
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        try {
            byte[] bytes = file.getBytes();

            if (s3Enabled && s3Client != null) {
                String key = "posters/" + filename;
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(s3Bucket)
                                .key(key)
                                .contentType(contentType)
                                .build(),
                        RequestBody.fromBytes(bytes)
                );
                String url = String.format("https://%s.s3.%s.amazonaws.com/%s", s3Bucket, s3Region, key);
                log.info("Uploaded movie poster to S3: {}", url);
                return url;
            }

            Files.createDirectories(localPostersDir);
            Path destination = localPostersDir.resolve(filename).normalize();
            if (!destination.startsWith(localPostersDir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid poster filename");
            }
            Files.write(destination, bytes);
            String url = baseUrl + "/api/movies/posters/" + filename;
            log.info("Saved movie poster locally: {}", destination);
            return url;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store movie poster", ex);
        }
    }

    public PosterData loadPoster(String filename) {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid poster filename");
        }

        MediaType mediaType = resolveMediaType(filename);

        // 1. Check uploaded posters on local disk
        Path diskFile = localPostersDir.resolve(filename).normalize();
        if (diskFile.startsWith(localPostersDir) && Files.exists(diskFile)) {
            try {
                return new PosterData(Files.readAllBytes(diskFile), mediaType);
            } catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read poster", ex);
            }
        }

        // 2. Fallback to bundled default posters for seeded movies
        ClassPathResource defaultPoster = new ClassPathResource("default-posters/" + filename);
        if (defaultPoster.exists()) {
            try {
                return new PosterData(defaultPoster.getContentAsByteArray(), mediaType);
            } catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read default poster", ex);
            }
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Poster not found");
    }

    private MediaType resolveMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".svg")) return MediaType.parseMediaType("image/svg+xml");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
