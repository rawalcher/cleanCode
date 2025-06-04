package crawler.factory;

import crawler.app.WebCrawler;
import crawler.fetcher.PageFetcher;
import crawler.fetcher.RobotsTxtCache;
import crawler.parser.HtmlParser;
import crawler.reporter.MarkdownReporter;
import crawler.util.LinkFilter;

/**
 * Factory class for creating different types of web crawlers.
 * Encapsulates the creation logic and dependency injection for crawler components.
 * This factory supports the Dependency Inversion Principle by allowing
 * different implementations to be injected while maintaining clean interfaces.
 */
public class CrawlerFactory {

    private CrawlerFactory() {
        // Utility class - prevent instantiation
    }

    public static WebCrawler getWebCrawler(String userAgent) {
        PageFetcher fetcher = new PageFetcher();
        HtmlParser parser = new HtmlParser();
        RobotsTxtCache robotsCache = new RobotsTxtCache(userAgent);
        LinkFilter linkFilter = new LinkFilter();
        MarkdownReporter reporter = new MarkdownReporter();

        return new WebCrawler(fetcher, parser, robotsCache, linkFilter, reporter);
    }
}