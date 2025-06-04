package crawler.error;

/**
 * Strategy interface for handling different types of crawl errors.
 * Allows for flexible error handling approaches based on error type and severity.
 */
public interface ErrorHandlingStrategy {

    /**
     * Determines how to handle a specific error.
     *
     * @param error the error to handle
     * @return the action to take
     */
    ErrorAction handleError(CrawlError error);

    enum ErrorAction {
        CONTINUE,    // Continue crawling despite the error
        RETRY,       // Retry the failed operation
        SKIP,        // Skip this URL and continue with others
        ABORT        // Abort the entire crawl
    }
}