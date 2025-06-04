package crawler.factory;

import crawler.adapters.JsoupDocumentSource;
import crawler.app.WebCrawler;
import crawler.concurrent.ConcurrentCrawlerConfig;
import crawler.concurrent.ConcurrentWebCrawler;
import crawler.error.DefaultErrorHandlingStrategy;
import crawler.fetcher.PageFetcher;
import crawler.fetcher.RobotsTxtCache;
import crawler.parser.HtmlParser;
import crawler.reporter.EnhancedMarkdownReporter;
import crawler.reporter.MarkdownReporter;
import crawler.util.LinkFilter;

import static crawler.constants.CrawlerConstants.USER_AGENT;

/**
 * Factory class for creating different types of web crawlers.
 * Encapsulates the creation logic and dependency injection for crawler components.
 *
 * This factory supports the Dependency Inversion Principle by allowing
 * different implementations to be injected while maintaining clean interfaces.
 */
public class CrawlerFactory {

    private CrawlerFactory() {
        // Utility class - prevent instantiation
    }

    public static WebCrawler createSequentialCrawler() {
        return createSequentialCrawler(USER_AGENT);
    }

    public static WebCrawler createSequentialCrawler(String userAgent) {
        return getWebCrawler(userAgent);
    }

    public static WebCrawler getWebCrawler(String userAgent) {
        PageFetcher fetcher = new PageFetcher();
        HtmlParser parser = new HtmlParser();
        RobotsTxtCache robotsCache = new RobotsTxtCache(userAgent);
        LinkFilter linkFilter = new LinkFilter();
        MarkdownReporter reporter = new MarkdownReporter();

        return new WebCrawler(fetcher, parser, robotsCache, linkFilter, reporter);
    }

    public static ConcurrentWebCrawler createConcurrentCrawler() {
        return createConcurrentCrawler(ConcurrentCrawlerConfig.defaultConfig());
    }

    public static ConcurrentWebCrawler createConcurrentCrawler(int threadCount) {
        ConcurrentCrawlerConfig config = ConcurrentCrawlerConfig.builder()
                .maxThreads(threadCount)
                .build();

        return createConcurrentCrawler(config);
    }

    public static ConcurrentWebCrawler createConcurrentCrawler(ConcurrentCrawlerConfig config) {
        return createConcurrentCrawler(config, USER_AGENT);
    }

    public static ConcurrentWebCrawler createConcurrentCrawler(ConcurrentCrawlerConfig config, String userAgent) {
        JsoupDocumentSource documentSource = new JsoupDocumentSource();
        RobotsTxtCache robotsCache = new RobotsTxtCache(userAgent);
        MarkdownReporter reporter = createReporter(config.isDetailedErrorReportingEnabled());

        return new ConcurrentWebCrawler(
                documentSource,
                robotsCache,
                reporter,
                config.getMaxThreads(),
                config.getTimeoutSeconds(),
                config.getErrorStrategy()
        );
    }

    public static Object createWebCrawler(boolean useConcurrent, int threadCount) {
        if (useConcurrent) {
            return createConcurrentCrawler(threadCount);
        } else {
            return createSequentialCrawler();
        }
    }

    /**
     * Creates the appropriate reporter based on requirements.
     *
     * @param enhancedReporting Whether to enable enhanced error reporting
     * @return A configured reporter instance
     */
    private static MarkdownReporter createReporter(boolean enhancedReporting) {
        if (enhancedReporting) {
            return new EnhancedMarkdownReporter();
        } else {
            return new MarkdownReporter();
        }
    }

    public static class CrawlerBuilder {
        private boolean concurrent = false;
        private int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        private String userAgent = USER_AGENT;
        private boolean enhancedReporting = true;
        private long timeoutSeconds = 300;

        public CrawlerBuilder concurrent(boolean concurrent) {
            this.concurrent = concurrent;
            return this;
        }

        public CrawlerBuilder threadCount(int threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        public CrawlerBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public CrawlerBuilder enhancedReporting(boolean enhanced) {
            this.enhancedReporting = enhanced;
            return this;
        }

        public CrawlerBuilder timeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Object build() {
            if (concurrent) {
                ConcurrentCrawlerConfig config = ConcurrentCrawlerConfig.builder()
                        .maxThreads(threadCount)
                        .timeoutSeconds(timeoutSeconds)
                        .enableDetailedErrorReporting(enhancedReporting)
                        .errorStrategy(new DefaultErrorHandlingStrategy())
                        .build();

                return createConcurrentCrawler(config, userAgent);
            } else {
                return createSequentialCrawler(userAgent);
            }
        }
    }

    public static CrawlerBuilder builder() {
        return new CrawlerBuilder();
    }
}