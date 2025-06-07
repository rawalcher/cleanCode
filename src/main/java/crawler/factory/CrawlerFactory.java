package crawler.factory;

import crawler.app.WebCrawler;
import crawler.fetcher.PageFetcher;
import crawler.fetcher.RobotsTxtCache;
import crawler.parser.HtmlParser;
import crawler.reporter.MarkdownReporter;
import crawler.util.LinkFilter;

/**
 * Factory for creating web crawlers with configurable concurrency.
 */
public class CrawlerFactory {

    private CrawlerFactory() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a sequential web crawler (1 thread).
     */
    public static WebCrawler createSequentialCrawler(String userAgent) {
        return createCrawler(userAgent, 1, 30);
    }

    /**
     * Creates a concurrent web crawler.
     */
    public static WebCrawler createConcurrentCrawler(String userAgent, int threadCount, long timeoutSeconds) {
        return createCrawler(userAgent, threadCount, timeoutSeconds);
    }

    /**
     * Creates a web crawler with specified parameters.
     */
    public static WebCrawler createCrawler(String userAgent, int threadCount, long timeoutSeconds) {
        PageFetcher fetcher = new PageFetcher();
        HtmlParser parser = new HtmlParser();
        RobotsTxtCache robotsCache = new RobotsTxtCache(userAgent);
        LinkFilter linkFilter = new LinkFilter();
        MarkdownReporter reporter = new MarkdownReporter();

        return new WebCrawler(fetcher, parser, robotsCache, linkFilter, reporter,
                threadCount, timeoutSeconds);
    }

    public static WebCrawler getWebCrawler(String userAgent) {
        return createSequentialCrawler(userAgent);
    }
}