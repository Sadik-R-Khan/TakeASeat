package com.takeaseat.dto;

import com.takeaseat.entity.Theatre;
import jakarta.validation.constraints.NotBlank;

public final class TheatreDtos {

    private TheatreDtos() {
    }

    public record CreateTheatreRequest(
            @NotBlank(message = "Theatre name is required")
            String name,

            @NotBlank(message = "City is required")
            String city,

            @NotBlank(message = "Address is required")
            String address
    ) {}

    public record TheatreResponse(
            Long id,
            String name,
            String city,
            String address,
            Long ownerId,
            String ownerName,
            String ownerEmail,
            Integer totalRows,
            Integer totalCols,
            Integer totalSeats
    ) {
        public static TheatreResponse from(Theatre theatre) {
            return new TheatreResponse(
                    theatre.getId(),
                    theatre.getName(),
                    theatre.getCity(),
                    theatre.getAddress(),
                    theatre.getOwner().getId(),
                    theatre.getOwner().getName(),
                    theatre.getOwner().getEmail(),
                    theatre.getTotalRows(),
                    theatre.getTotalCols(),
                    theatre.getTotalSeats()
            );
        }
    }
}
