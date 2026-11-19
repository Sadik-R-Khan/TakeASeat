package com.takeaseat.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Fixed 5 rows x 10 columns seat layout (50 seats total): 1A..1J through 5A..5J.
 */
public final class SeatLayoutUtil {

    public static final int TOTAL_ROWS = 5;
    public static final int TOTAL_COLS = 10;
    public static final int TOTAL_SEATS = TOTAL_ROWS * TOTAL_COLS;
    public static final char[] COLUMNS = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'};

    public static final List<String> ALL_SEATS;
    private static final Set<String> VALID_SEAT_SET;

    static {
        List<String> seats = new ArrayList<>(TOTAL_SEATS);
        for (int row = 1; row <= TOTAL_ROWS; row++) {
            for (char col : COLUMNS) {
                seats.add(row + String.valueOf(col));
            }
        }
        ALL_SEATS = Collections.unmodifiableList(seats);
        VALID_SEAT_SET = Collections.unmodifiableSet(new LinkedHashSet<>(seats));
    }

    private SeatLayoutUtil() {
    }

    public static boolean isValidSeat(String seatNumber) {
        return seatNumber != null && VALID_SEAT_SET.contains(seatNumber);
    }

    public static List<String> normalizeAndValidate(List<String> seatNumbers) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : seatNumbers) {
            if (raw == null) {
                throw new IllegalArgumentException("Seat number cannot be null");
            }
            String seat = raw.trim().toUpperCase();
            if (!VALID_SEAT_SET.contains(seat)) {
                throw new IllegalArgumentException("Invalid seat number: " + raw + " (valid range: 1A-5J)");
            }
            normalized.add(seat);
        }
        return new ArrayList<>(normalized);
    }
}
