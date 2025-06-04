package crawler.reporter;

import crawler.constants.CrawlerConstants;
import crawler.error.CrawlError;
import crawler.error.ErrorCollector;
import crawler.model.CrawlerConfig;
import crawler.model.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Enhanced Markdown reporter that includes error reporting capabilities.
 * Extends the original MarkdownReporter with comprehensive error summaries and statistics.
 */
public class EnhancedMarkdownReporter extends MarkdownReporter {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedMarkdownReporter.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Writes the crawl results with optional error information.
     *
     * @param root   The root page of the crawl result
     * @param config The crawler configuration
     * @param errorCollector Optional error collector for error reporting
     */
    public void writeReportWithErrors(PageResult root, CrawlerConfig config, ErrorCollector errorCollector) {
        if (root == null || config == null) {
            logger.error("Cannot write report: root or config is null.");
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(CrawlerConstants.REPORT_FILENAME))) {
            writeReportHeader(config, writer);
            writeExecutionSummary(root, errorCollector, writer);
            writePage(root, writer);

            if (errorCollector != null && errorCollector.hasErrors()) {
                writeErrorSummary(errorCollector, writer);
            }

            logger.info("Successfully wrote enhanced report to '{}'", CrawlerConstants.REPORT_FILENAME);
        } catch (IOException e) {
            logger.error("Failed to write enhanced report: {}", e.getMessage());
        }
    }

    private void writeExecutionSummary(PageResult root, ErrorCollector errorCollector, PrintWriter writer) {
        writer.println("## Execution Summary\n");

        int totalPages = countTotalPages(root);
        int successfulPages = countSuccessfulPages(root);
        int brokenPages = totalPages - successfulPages;

        writer.printf("**Total Pages Processed:** %d  %n", totalPages);
        writer.printf("**Successful Pages:** %d  %n", successfulPages);
        writer.printf("**Broken/Failed Pages:** %d  %n", brokenPages);

        if (errorCollector != null) {
            writer.printf("**Total Errors:** %d  %n", errorCollector.getTotalErrors());
        }

        writer.println("\n---\n");
    }

    private int countTotalPages(PageResult page) {
        if (page == null) return 0;
        return 1 + page.children().stream().mapToInt(this::countTotalPages).sum();
    }

    private int countSuccessfulPages(PageResult page) {
        if (page == null) return 0;
        int count = page.broken() ? 0 : 1;
        return count + page.children().stream().mapToInt(this::countSuccessfulPages).sum();
    }

    private void writeErrorSummary(ErrorCollector errorCollector, PrintWriter writer) {
        writer.println("---\n");
        writer.println("## Error Summary\n");

        writeErrorStatistics(errorCollector, writer);
        writeErrorDetails(errorCollector, writer);
    }

    private void writeErrorStatistics(ErrorCollector errorCollector, PrintWriter writer) {
        writer.println("### Error Statistics\n");

        Map<CrawlError.ErrorType, Long> stats = errorCollector.getErrorStatistics();

        if (stats.isEmpty()) {
            writer.println("No errors occurred during crawling.\n");
            return;
        }

        for (Map.Entry<CrawlError.ErrorType, Long> entry : stats.entrySet()) {
            writer.printf("**%s:** %d  %n",
                    formatErrorType(entry.getKey()),
                    entry.getValue());
        }
        writer.println();
    }

    private void writeErrorDetails(ErrorCollector errorCollector, PrintWriter writer) {
        writer.println("### Error Details\n");

        var errorsByType = errorCollector.getAllErrors().stream()
                .collect(java.util.stream.Collectors.groupingBy(CrawlError::type));

        for (Map.Entry<CrawlError.ErrorType, java.util.List<CrawlError>> entry : errorsByType.entrySet()) {
            writeErrorTypeSection(entry.getKey(), entry.getValue(), writer);
        }
    }

    private void writeErrorTypeSection(CrawlError.ErrorType errorType,
                                       java.util.List<CrawlError> errors,
                                       PrintWriter writer) {
        writer.printf("#### %s\n\n", formatErrorType(errorType));

        for (CrawlError error : errors) {
            writer.printf("- **URL:** %s  %n", error.url());
            writer.printf("  **Depth:** %d  %n", error.depth());
            writer.printf("  **Time:** %s  %n", TIME_FORMATTER.format(error.timestamp()));
            writer.printf("  **Message:** %s  %n", error.message());

            if (!error.details().isEmpty()) {
                writer.printf("  **Details:** %s  %n", error.details());
            }

            writer.println();
        }
    }

    private String formatErrorType(CrawlError.ErrorType errorType) {
        return switch (errorType) {
            case NETWORK_ERROR -> "Network Errors";
            case HTTP_ERROR -> "HTTP Errors";
            case TIMEOUT -> "Timeout Errors";
            case INVALID_URL -> "Invalid URL Errors";
            case PARSING_ERROR -> "Parsing Errors";
            case ROBOTS_BLOCKED -> "Robot.txt Blocked";
            case THREAD_INTERRUPTED -> "Thread Interruptions";
            case UNKNOWN -> "Unknown Errors";
        };
    }

    @Override
    void writePageHeader(PageResult page, PrintWriter writer) {
        writer.printf("## Page: %s%n", page.url());
        writer.printf("**Depth:** %d  %n", page.depth());

        String status = page.broken() ? "Broken" : "OK";
        writer.printf("**Status:** %s%n%n", status);

        if (page.broken()) {
            writer.println();
        }
    }

}