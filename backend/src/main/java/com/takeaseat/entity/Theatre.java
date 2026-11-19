package com.takeaseat.entity;

import com.takeaseat.util.SeatLayoutUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "theatres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Theatre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 300)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Builder.Default
    @Column(name = "total_rows", nullable = false)
    private Integer totalRows = SeatLayoutUtil.TOTAL_ROWS;

    @Builder.Default
    @Column(name = "total_cols", nullable = false)
    private Integer totalCols = SeatLayoutUtil.TOTAL_COLS;

    @Builder.Default
    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats = SeatLayoutUtil.TOTAL_SEATS;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        totalRows = SeatLayoutUtil.TOTAL_ROWS;
        totalCols = SeatLayoutUtil.TOTAL_COLS;
        totalSeats = SeatLayoutUtil.TOTAL_SEATS;
    }
}
