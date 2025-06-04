package crawler.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default error handling strategy that provides sensible error handling behavior.
 * Network and HTTP errors are handled gracefully, while critical errors may abort crawling.
 */
public class DefaultErrorHandlingStrategy implements ErrorHandlingStrategy {
    private static final Logger logger = LoggerFactory.getLogger(DefaultErrorHandlingStrategy.class);

    @Override
    public ErrorAction handleError(CrawlError error) {
        logger.warn("Crawl error for {} at depth {}: {} - {}",
                error.url(), error.depth(), error.type(), error.message());

        return switch (error.type()) {
            case NETWORK_ERROR, HTTP_ERROR, TIMEOUT, PARSING_ERROR, ROBOTS_BLOCKED -> {
                logger.debug("Continuing crawl despite {} for {}", error.type(), error.url());
                yield ErrorAction.CONTINUE;
            }
            case INVALID_URL -> {
                logger.debug("Skipping invalid URL: {}", error.url());
                yield ErrorAction.SKIP;
            }
            case THREAD_INTERRUPTED -> {
                logger.warn("Thread interrupted, aborting crawl");
                yield ErrorAction.ABORT;
            }
            case UNKNOWN -> {
                logger.error("Unknown error type, continuing with caution: {}", error.details());
                yield ErrorAction.CONTINUE;
            }
        };
    }
}
