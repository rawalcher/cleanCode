package crawler.app;

import crawler.error.ErrorCollector;
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
import java.util.concurrent.*;

/**
 * Unified web crawler that supports both sequential and concurrent crawling.
 * When threadCount = 1, it behaves like a sequential crawler.
 * When threadCount > 1, it crawls pages concurrently.
 */
public class WebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    private final PageFetcher fetcher;
    private final HtmlParser parser;
    private final RobotsTxtCache robotsCache;
    private final LinkFilter linkFilter;
    private final MarkdownReporter reporter;
    private final int threadCount;
    private final long timeoutSeconds;

    /**
     * Creates a WebCrawler with configurable concurrency.
     *
     * @param fetcher Component for fetching web pages
     * @param parser Component for parsing HTML content
     * @param robotsCache Component for checking robots.txt rules
     * @param linkFilter Component for filtering links
     * @param reporter Component for generating reports
     * @param threadCount Number of threads to use (1 = sequential, >1 = concurrent)
     * @param timeoutSeconds Timeout for individual page fetches
     */
    public WebCrawler(PageFetcher fetcher, HtmlParser parser, RobotsTxtCache robotsCache,
                      LinkFilter linkFilter, MarkdownReporter reporter,
                      int threadCount, long timeoutSeconds) {
        this.fetcher = fetcher;
        this.parser = parser;
        this.robotsCache = robotsCache;
        this.linkFilter = linkFilter;
        this.reporter = reporter;
        this.threadCount = Math.max(1, threadCount);
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Convenience constructor for sequential crawling (1 thread).
     */
    public WebCrawler(PageFetcher fetcher, HtmlParser parser, RobotsTxtCache robotsCache,
                      LinkFilter linkFilter, MarkdownReporter reporter) {
        this(fetcher, parser, robotsCache, linkFilter, reporter, 1, 30);
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

        long startTime = System.currentTimeMillis();

        if (threadCount == 1) {
            logger.info("Starting sequential crawl, max depth {}", config.getMaxDepth());
            crawlSequential(config, startTime);
        } else {
            logger.info("Starting concurrent crawl with {} threads, max depth {}",
                    threadCount, config.getMaxDepth());
            crawlConcurrent(config, startTime);
        }
    }

    /**
     * Sequential crawling implementation (original logic).
     */
    private void crawlSequential(CrawlerConfig config, long startTime) {
        PageResult rootResult = crawlPageSequential(config.getRootUrl(), 0, config);

        long endTime = System.currentTimeMillis();
        logger.info("Sequential crawl completed in {} ms", endTime - startTime);

        if (rootResult != null) {
            reporter.writeReport(rootResult, config);
        } else {
            logger.warn("No crawl results were generated");
        }
    }

    private PageResult crawlPageSequential(URI url, int depth, CrawlerConfig config) {
        logger.debug("Crawling {} at depth {} (sequential)", url, depth);

        if (!isAllowedByRobots(url)) {
            return PageResult.brokenLink(url, depth);
        }

        try {
            return fetchAndParse(url, depth, config);
        } catch (PageFetcher.FetchException e) {
            logger.warn("Failed to fetch {}: {}", url, e.getMessage());
            return PageResult.brokenLink(url, depth);
        }
    }

    private PageResult fetchAndParse(URI url, int depth, CrawlerConfig config)
            throws PageFetcher.FetchException {

        Document document = fetcher.fetch(url);
        linkFilter.markVisited(url);
        PageResult page = parser.parse(url, depth, document);
        Set<PageResult> children = processChildLinksSequential(page.getAllLinks(), depth, config);

        return page.withChildren(children);
    }

    private Set<PageResult> processChildLinksSequential(List<URI> links, int depth, CrawlerConfig config) {
        Set<PageResult> children = new HashSet<>();

        if (links == null || links.isEmpty()) {
            return children;
        }

        for (URI link : links) {
            if (isLinkEligibleForCrawling(link, depth, config)) {
                PageResult child = crawlPageSequential(link, depth + 1, config);
                if (child != null) {
                    children.add(child);
                }
            }
        }

        return children;
    }

    /**
     * Concurrent crawling implementation.
     */
    private void crawlConcurrent(CrawlerConfig config, long startTime) {
        ErrorCollector errorCollector = new ErrorCollector();

        try (ThreadPoolExecutor executor = createThreadPool()) {
            PageResult rootResult = crawlPageConcurrent(config.getRootUrl(), 0, config,
                    executor, errorCollector);

            long endTime = System.currentTimeMillis();
            logger.info("Concurrent crawl completed in {} ms. Visited {} URLs, {} errors",
                    endTime - startTime, linkFilter.getVisitedCount(), errorCollector.getTotalErrors());

            generateReport(rootResult, config, errorCollector);

        } catch (Exception e) {
            logger.error("Fatal error during concurrent crawl", e);
        }
    }

    private PageResult crawlPageConcurrent(URI url, int depth, CrawlerConfig config,
                                           ThreadPoolExecutor executor, ErrorCollector errorCollector) {
        logger.debug("Crawling {} at depth {} (concurrent)", url, depth);

        if (!robotsCache.getHandler(url).isAllowed(url)) {
            logger.debug("Blocked by robots.txt: {}", url);
            return PageResult.brokenLink(url, depth);
        }

        PageResult pageResult;
        try {
            Document document = fetcher.fetch(url);
            pageResult = parser.parse(url, depth, document);
        } catch (Exception e) {
            logger.warn("Failed to crawl {}: {}", url, e.getMessage());
            return PageResult.brokenLink(url, depth);
        }

        if (pageResult.broken()) {
            return pageResult;
        }

        List<URI> links = pageResult.getAllLinks();
        Set<PageResult> children = crawlChildrenConcurrent(links, depth + 1, config,
                executor, errorCollector);

        return pageResult.withChildren(children);
    }

    private Set<PageResult> crawlChildrenConcurrent(List<URI> links, int childDepth,
                                                    CrawlerConfig config, ThreadPoolExecutor executor,
                                                    ErrorCollector errorCollector) {
        if (links == null || links.isEmpty()) {
            return Set.of();
        }

        Set<PageResult> children = new HashSet<>();

        for (URI link : links) {
            if (isLinkEligibleForCrawling(link, childDepth - 1, config)) {
                Future<PageResult> future = executor.submit(() ->
                        crawlPageConcurrent(link, childDepth, config, executor, errorCollector)
                );

                try {
                    PageResult child = future.get(timeoutSeconds, TimeUnit.SECONDS);
                    if (child != null) {
                        children.add(child);
                    }
                } catch (TimeoutException e) {
                    logger.warn("Timeout waiting for child result at depth {}", childDepth);
                    future.cancel(true);
                } catch (InterruptedException | ExecutionException e) {
                    logger.warn("Error getting child result at depth {}: {}", childDepth, e.getMessage());
                }
            }
        }

        return children;
    }

    private boolean isAllowedByRobots(URI url) {
        boolean allowed = robotsCache.getHandler(url).isAllowed(url);
        if (!allowed) {
            logger.warn("Blocked by robots.txt: {}", url);
        }
        return allowed;
    }

    private boolean isLinkEligibleForCrawling(URI link, int depth, CrawlerConfig config) {
        return !linkFilter.isVisited(link) &&
                linkFilter.isAllowedDomain(link, config.getAllowedDomains()) &&
                depth + 1 <= config.getMaxDepth();
    }

    private ThreadPoolExecutor createThreadPool() {
        return new ThreadPoolExecutor(
                Math.min(threadCount, 4),
                threadCount,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, "CrawlerThread");
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    private void generateReport(PageResult rootResult, CrawlerConfig config, ErrorCollector errorCollector) {
        if (rootResult != null) {
            reporter.writeReport(rootResult, config);

            if (errorCollector.hasErrors()) {
                logger.info("Crawl completed with {} errors. Error breakdown: {}",
                        errorCollector.getTotalErrors(),
                        errorCollector.getErrorStatistics());
            }
        } else {
            logger.warn("No crawl results generated - check configuration and connectivity");
        }
    }

    // Getters for testing
    public int getThreadCount() { return threadCount; }
    public long getTimeoutSeconds() { return timeoutSeconds; }
}