package crawler.app;

import crawler.fetcher.PageFetcher;
import crawler.fetcher.RobotsTxtCache;
import crawler.model.CrawlerConfig;
import crawler.model.PageResult;
import crawler.parser.HtmlParser;
import crawler.reporter.MarkdownReporter;
import crawler.util.LinkFilter;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Main web crawler implementation that coordinates the crawling process.
 * Handles page fetching, parsing, filtering, and reporting.
 */
public class WebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    private final PageFetcher fetcher;
    private final HtmlParser parser;
    private final RobotsTxtCache robotsCache;
    private final LinkFilter linkFilter;
    private final MarkdownReporter reporter;

    /**
     * Creates a new WebCrawler with injected dependencies.
     *
     * @param fetcher     Component for fetching web pages
     * @param parser      Component for parsing HTML content
     * @param robotsCache Component for checking robots.txt rules
     * @param linkFilter  Component for filtering links
     * @param reporter    Component for generating reports
     */
    public WebCrawler(PageFetcher fetcher, HtmlParser parser, RobotsTxtCache robotsCache,
                      LinkFilter linkFilter, MarkdownReporter reporter) {
        this.fetcher = fetcher;
        this.parser = parser;
        this.robotsCache = robotsCache;
        this.linkFilter = linkFilter;
        this.reporter = reporter;
    }

    /**
     * Executes the crawling process according to the provided configuration.
     *
     * @param config The configuration for this crawl
     */
    public void crawl(CrawlerConfig config) {
        if (config == null) {
            logger.error("Crawl config cannot be null");
            return;
        }

        long start = System.currentTimeMillis();

        // Perform the crawl starting from the root URL
        PageResult rootResult = crawlPage(config.getRootUrl(), 0, config);

        long end = System.currentTimeMillis();
        logger.info("Crawl time: {} ms", end - start);

        // Generate report from crawl results
        if (rootResult != null) {
            reporter.writeReport(rootResult, config);
        } else {
            logger.warn("No crawl results were generated");
        }
    }

    /**
     * Recursively crawls a page and its links up to the configured depth.
     *
     * @param url    The URL to crawl
     * @param depth  The current crawl depth
     * @param config The crawl configuration
     * @return A PageResult containing the crawl results, or null if the page should be skipped
     */
    PageResult crawlPage(URI url, int depth, CrawlerConfig config) {
        logger.info("Crawling {} at depth {}", url, depth);

        if (!isAllowedByRobots(url)) {
            // we treat non allowed like broken links
            return PageResult.brokenLink(url, depth);
        }

        try {
            return fetchAndParse(url, depth, config);
        } catch (PageFetcher.FetchException e) {
            logger.warn("Failed to fetch {}: {}", url, e.getMessage());
            return PageResult.brokenLink(url, depth);
        }
    }

    /**
     * Checks if a URL is allowed according to the site's robots.txt rules.
     *
     * @param url The URL to check
     * @return true if the URL is allowed, false otherwise
     */
    private boolean isAllowedByRobots(URI url) {
        boolean allowed = robotsCache.getHandler(url).isAllowed(url);
        if (!allowed) {
            logger.warn("Blocked by robots.txt: {}", url);
        }
        return allowed;
    }

    /**
     * Fetches and parses a URL, then recursively processes its links.
     *
     * @param url    The URL to fetch and parse
     * @param depth  The current depth
     * @param config The crawl configuration
     * @return A PageResult containing the page content and child pages
     * @throws PageFetcher.FetchException If the page cannot be fetched
     */
    private PageResult fetchAndParse(URI url, int depth, CrawlerConfig config)
            throws PageFetcher.FetchException {

        Document document = fetcher.fetch(url);
        PageResult page = parser.parse(url, depth, document);
        Set<PageResult> children = processChildLinks(page.getAllLinks(), depth, config);

        return page.withChildren(children);
    }

    /**
     * Processes a list of links by crawling each one at an incremented depth.
     *
     * @param links  The links to process
     * @param depth  The parent depth (children will be at depth+1)
     * @param config The crawl configuration
     * @return A set of PageResults for the successfully crawled links
     */
    private Set<PageResult> processChildLinks(List<URI> links, int depth, CrawlerConfig config) {
        Set<PageResult> children = new HashSet<>();

        if (links == null || links.isEmpty()) {
            return children;
        }

        for (URI link : links) {
            if (isLinkEligibleForCrawling(link, depth, config)) {
                PageResult child = crawlPage(link, depth + 1, config);
                if (child != null) {
                    children.add(child);
                }
            }
        }

        return children;
    }

    private boolean isLinkEligibleForCrawling(URI link, int depth, CrawlerConfig config) {
        return !linkFilter.isVisited(link) &&
                linkFilter.isAllowedDomain(link, config.getAllowedDomains()) &&
                depth + 1 <= config.getMaxDepth();
    }
}