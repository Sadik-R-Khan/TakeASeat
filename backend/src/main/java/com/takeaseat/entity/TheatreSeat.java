package com.takeaseat.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "theatre_seats",
    uniqueConstraints = @UniqueConstraint(name = "uk_theatre_seat", columnNames = {"theatre_id", "seat_number"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheatreSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "theatre_id", nullable = false)
    private Theatre theatre;

    @Column(name = "seat_number", nullable = false, length = 5)
    private String seatNumber;

    @Column(name = "seat_row", nullable = false)
    private Integer rowNumber;

    @Column(name = "col_letter", nullable = false, length = 1)
    private String colLetter;
}
