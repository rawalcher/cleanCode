package crawler.error;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe collector for crawl errors.
 * Provides statistics and categorization of errors encountered during crawling.
 */
public class ErrorCollector {
    private final ConcurrentLinkedQueue<CrawlError> errors = new ConcurrentLinkedQueue<>();
    private final AtomicInteger totalErrors = new AtomicInteger(0);

    public void addError(CrawlError error) {
        if (error != null) {
            errors.offer(error);
            totalErrors.incrementAndGet();
        }
    }

    public List<CrawlError> getAllErrors() {
        return List.copyOf(errors);
    }

    public int getTotalErrors() {
        return totalErrors.get();
    }

    public List<CrawlError> getErrorsByType(CrawlError.ErrorType type) {
        return errors.stream()
                .filter(error -> error.type() == type)
                .toList();
    }

    public java.util.Map<CrawlError.ErrorType, Long> getErrorStatistics() {
        return errors.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        CrawlError::type,
                        java.util.stream.Collectors.counting()
                ));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void clear() {
        errors.clear();
        totalErrors.set(0);
    }
}