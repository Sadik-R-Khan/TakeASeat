package com.takeaseat;

import com.takeaseat.util.SeatLayoutUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeatLayoutUtilTest {

    @Test
    void generatesExactly50FixedSeatsFrom1ATo5J() {
        assertEquals(50, SeatLayoutUtil.ALL_SEATS.size());
        assertEquals("1A", SeatLayoutUtil.ALL_SEATS.get(0));
        assertEquals("1J", SeatLayoutUtil.ALL_SEATS.get(9));
        assertEquals("2A", SeatLayoutUtil.ALL_SEATS.get(10));
        assertEquals("5I", SeatLayoutUtil.ALL_SEATS.get(48));
        assertEquals("5J", SeatLayoutUtil.ALL_SEATS.get(49));
    }

    @Test
    void validatesAndNormalizesSeats() {
        List<String> normalized = SeatLayoutUtil.normalizeAndValidate(List.of("1a", " 3C ", "1A", "5j"));
        assertEquals(List.of("1A", "3C", "5J"), normalized);
    }

    @Test
    void rejectsOutOfBoundsSeatLabels() {
        assertThrows(IllegalArgumentException.class, () -> SeatLayoutUtil.normalizeAndValidate(List.of("6A")));
        assertThrows(IllegalArgumentException.class, () -> SeatLayoutUtil.normalizeAndValidate(List.of("1K")));
        assertThrows(IllegalArgumentException.class, () -> SeatLayoutUtil.normalizeAndValidate(List.of("A1")));
        assertThrows(IllegalArgumentException.class, () -> SeatLayoutUtil.normalizeAndValidate(List.of()));
    }
}
