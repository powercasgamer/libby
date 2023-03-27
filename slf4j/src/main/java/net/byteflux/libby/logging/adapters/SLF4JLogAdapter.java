package net.byteflux.libby.logging.adapters;

import net.byteflux.libby.logging.LogLevel;
import org.slf4j.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Logging adapter that logs to a SLF4J logger.
 */
public class SLF4JLogAdapter implements LogAdapter {
    /**
     * SLF4J logger
     */
    private final Logger logger;

    /**
     * Creates a new SLF4J log adapter that logs to a {@link Logger}.
     *
     * @param logger the SLF4J logger to wrap
     */
    public SLF4JLogAdapter(final Logger logger) {
        this.logger = requireNonNull(logger, "logger");
    }

    /**
     * Logs a message with the provided level to the SLF4J logger.
     *
     * @param level   message severity level
     * @param message the message to log
     */
    @Override
    public void log(final LogLevel level, final String message) {
        switch (requireNonNull(level, "level")) {
            case DEBUG:
                this.logger.debug(message);
                break;
            case INFO:
                this.logger.info(message);
                break;
            case WARN:
                this.logger.warn(message);
                break;
            case ERROR:
                this.logger.error(message);
                break;
        }
    }

    /**
     * Logs a message and stack trace with the provided level to the SLF4J
     * logger.
     *
     * @param level     message severity level
     * @param message   the message to log
     * @param throwable the throwable to print
     */
    @Override
    public void log(final LogLevel level, final String message, final Throwable throwable) {
        switch (requireNonNull(level, "level")) {
            case DEBUG:
                this.logger.debug(message, throwable);
                break;
            case INFO:
                this.logger.info(message, throwable);
                break;
            case WARN:
                this.logger.warn(message, throwable);
                break;
            case ERROR:
                this.logger.error(message, throwable);
                break;
        }
    }
}
