package org.example.config;

import org.slf4j.LoggerFactory;

public class Logger {
    private static final Logger logger = LoggerFactory.getLogger(Logger.class);

    public static void main(String[] args) {
        logger.debug("Debug log message");
        logger.info("Info log message");
        logger.error("Error log message");
    }
}
