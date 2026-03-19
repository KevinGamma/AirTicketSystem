package com.airticket.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppLog.class);

    private AppLog() {
    }

    public static void debug(String message) {
        // Intentionally silent to suppress legacy debug console noise.
    }

    public static void error(String message) {
        LOGGER.error(message);
    }

    public static void exception(Throwable throwable) {
        LOGGER.error("Unhandled exception", throwable);
    }
}
