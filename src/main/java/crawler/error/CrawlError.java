package crawler.error;

import java.net.URI;
import java.time.Instant;

/**
 * Represents an error that occurred during crawling.
 * Immutable record that captures all relevant error information.
 */
public record CrawlError(
        URI url,
        int depth,
        ErrorType type,
        String message,
        String details,
        Instant timestamp
) {

    public enum ErrorType {
        NETWORK_ERROR("Network connectivity issue"),
        HTTP_ERROR("HTTP response error"),
        TIMEOUT("Request timeout"),
        INVALID_URL("Malformed or invalid URL"),
        PARSING_ERROR("HTML parsing failure"),
        ROBOTS_BLOCKED("Blocked by robots.txt"),
        THREAD_INTERRUPTED("Thread execution interrupted"),
        UNKNOWN("Unexpected error");

        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public CrawlError {
        if (url == null) throw new IllegalArgumentException("URL cannot be null");
        if (type == null) throw new IllegalArgumentException("Error type cannot be null");
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }
        if (timestamp == null) timestamp = Instant.now();
        if (details == null) details = "";
    }

    /**
     * Creates a CrawlError with current timestamp.
     */
    public static CrawlError create(URI url, int depth, ErrorType type, String message) {
        return new CrawlError(url, depth, type, message, "", Instant.now());
    }

    /**
     * Creates a CrawlError with exception details.
     */
    public static CrawlError create(URI url, int depth, ErrorType type, String message, Throwable cause) {
        String details = cause != null ? cause.getClass().getSimpleName() + ": " + cause.getMessage() : "";
        return new CrawlError(url, depth, type, message, details, Instant.now());
    }
}