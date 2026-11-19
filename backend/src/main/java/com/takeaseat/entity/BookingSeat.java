package com.takeaseat.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "booking_seats",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_show_seat_active",
        columnNames = {"show_id", "seat_number", "active_flag"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(name = "seat_number", nullable = false, length = 5)
    private String seatNumber;

    /**
     * 1 while the booking is CONFIRMED; NULL when CANCELLED so the seat can be rebooked
     * while still enforcing DB-level uniqueness for active seats.
     */
    @Builder.Default
    @Column(name = "active_flag")
    private Integer activeFlag = 1;
}
