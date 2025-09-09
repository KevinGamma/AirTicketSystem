package com.airticket.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class TimeZoneUtil {
    
    public static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DISPLAY_FORMATTER_WITH_TZ = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    public static Instant convertToUtc(LocalDateTime localTime, ZoneId timeZone) {
        if (localTime == null || timeZone == null) {
            return null;
        }
        return localTime.atZone(timeZone).toInstant();
    }
    
    public static LocalDateTime convertFromUtc(Instant utcTime, ZoneId timeZone) {
        if (utcTime == null || timeZone == null) {
            return null;
        }
        return LocalDateTime.ofInstant(utcTime, timeZone);
    }
    
    public static String formatWithTimeZone(Instant utcTime, ZoneId timeZone) {
        if (utcTime == null || timeZone == null) {
            return null;
        }
        return utcTime.atZone(timeZone).format(DISPLAY_FORMATTER_WITH_TZ);
    }
    
    public static String formatInTimeZone(Instant utcTime, ZoneId timeZone) {
        if (utcTime == null || timeZone == null) {
            return null;
        }
        return LocalDateTime.ofInstant(utcTime, timeZone).format(DISPLAY_FORMATTER);
    }
    
    public static String formatInTimeZoneISO(Instant utcTime, ZoneId timeZone) {
        if (utcTime == null || timeZone == null) {
            return null;
        }
        return LocalDateTime.ofInstant(utcTime, timeZone).format(ISO_FORMATTER);
    }
    
    public static Duration calculateFlightDuration(Instant departureUtc, Instant arrivalUtc) {
        if (departureUtc == null || arrivalUtc == null) {
            return Duration.ZERO;
        }
        return Duration.between(departureUtc, arrivalUtc);
    }
    
    public static boolean isValidTimeZone(String timeZoneId) {
        try {
            ZoneId.of(timeZoneId);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
    
    public static ZoneId parseTimeZone(String timeZoneId) {
        if (timeZoneId == null || timeZoneId.trim().isEmpty()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timeZoneId);
        } catch (DateTimeException e) {
            return ZoneOffset.UTC;
        }
    }
    
    public static boolean isFlightTimeValid(Instant departureUtc, Instant arrivalUtc) {
        if (departureUtc == null || arrivalUtc == null) {
            return false;
        }
        return arrivalUtc.isAfter(departureUtc);
    }
    
    public static boolean isDepartureInFuture(Instant departureUtc) {
        if (departureUtc == null) {
            return false;
        }
        return departureUtc.isAfter(Instant.now());
    }
}