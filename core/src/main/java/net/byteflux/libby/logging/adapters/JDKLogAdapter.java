package net.byteflux.libby.logging.adapters;

import net.byteflux.libby.logging.LogLevel;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Logging adapter that logs to a JDK logger.
 */
public class JDKLogAdapter implements LogAdapter {
    /**
     * JDK logger
     */
    private final Logger logger;

    /**
     * Creates a new JDK log adapter that logs to a {@link Logger}.
     *
     * @param logger the JDK logger to wrap
     */
    public JDKLogAdapter(final Logger logger) {
        this.logger = requireNonNull(logger, "logger");
    }

    /**
     * Logs a message with the provided level to the JDK logger.
     *
     * @param level   message severity level
     * @param message the message to log
     */
    @Override
    public void log(final LogLevel level, final String message) {
        switch (requireNonNull(level, "level")) {
            case DEBUG -> this.logger.log(Level.FINE, message);
            case INFO -> this.logger.log(Level.INFO, message);
            case WARN -> this.logger.log(Level.WARNING, message);
            case ERROR -> this.logger.log(Level.SEVERE, message);
        }
    }

    /**
     * Logs a message and stack trace with the provided level to the JDK
     * logger.
     *
     * @param level     message severity level
     * @param message   the message to log
     * @param throwable the throwable to print
     */
    @Override
    public void log(final LogLevel level, final String message, final Throwable throwable) {
        switch (requireNonNull(level, "level")) {
            case DEBUG -> this.logger.log(Level.FINE, message, throwable);
            case INFO -> this.logger.log(Level.INFO, message, throwable);
            case WARN -> this.logger.log(Level.WARNING, message, throwable);
            case ERROR -> this.logger.log(Level.SEVERE, message, throwable);
        }
    }
}
