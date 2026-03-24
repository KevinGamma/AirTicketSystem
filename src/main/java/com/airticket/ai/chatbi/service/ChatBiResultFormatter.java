package com.airticket.ai.chatbi.service;

import org.springframework.stereotype.Component;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChatBiResultFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ZoneId zoneId = ZoneId.systemDefault();

    public List<Map<String, Object>> formatRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        return rows.stream()
            .map(this::formatRow)
            .toList();
    }

    private Map<String, Object> formatRow(Map<String, Object> row) {
        Map<String, Object> formattedRow = new LinkedHashMap<>();
        row.forEach((key, value) -> formattedRow.put(key, formatValue(value)));
        return formattedRow;
    }

    private Object formatValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(DATETIME_FORMATTER);
        }

        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().format(DATE_FORMATTER);
        }

        if (value instanceof Time sqlTime) {
            return sqlTime.toLocalTime().format(TIME_FORMATTER);
        }

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DATETIME_FORMATTER);
        }

        if (value instanceof LocalDate localDate) {
            return localDate.format(DATE_FORMATTER);
        }

        if (value instanceof LocalTime localTime) {
            return localTime.format(TIME_FORMATTER);
        }

        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDateTime().format(DATETIME_FORMATTER);
        }

        if (value instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toLocalDateTime().format(DATETIME_FORMATTER);
        }

        if (value instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, zoneId).format(DATETIME_FORMATTER);
        }

        if (value instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), zoneId).format(DATETIME_FORMATTER);
        }

        return value;
    }
}
