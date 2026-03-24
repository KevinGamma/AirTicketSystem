package com.airticket.ai.chatbi.service;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatBiResultFormatterTest {

    private final ChatBiResultFormatter formatter = new ChatBiResultFormatter();

    @Test
    void shouldFormatSqlTemporalValuesAsReadableStrings() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("order_date", java.sql.Date.valueOf(LocalDate.of(2026, 3, 15)));
        row.put("created_at", Timestamp.valueOf(LocalDateTime.of(2026, 3, 15, 9, 30, 45)));
        row.put("departure_time", java.sql.Time.valueOf(LocalTime.of(8, 5, 0)));
        row.put("order_count", 1);

        List<Map<String, Object>> formattedRows = formatter.formatRows(List.of(row));

        assertEquals("2026-03-15", formattedRows.getFirst().get("order_date"));
        assertEquals("2026-03-15 09:30:45", formattedRows.getFirst().get("created_at"));
        assertEquals("08:05:00", formattedRows.getFirst().get("departure_time"));
        assertEquals(1, formattedRows.getFirst().get("order_count"));
    }
}
