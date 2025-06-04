package crawler.concurrent;

import crawler.error.ErrorCollector;
import crawler.fetcher.RobotsTxtCache;
import crawler.model.CrawlerConfig;
import crawler.model.PageResult;
import crawler.reporter.MarkdownReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Concurrent implementation of web crawler that maintains the same hierarchical structure
 * as the sequential crawler. Each page's links become direct children of that page,
 * preserving the depth-first tree structure while using parallel processing.
 */
public class ConcurrentWebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentWebCrawler.class);

    private final RobotsTxtCache robotsCache;
    private final MarkdownReporter reporter;
    private final int maxThreads;
    private final long timeoutSeconds;

    public ConcurrentWebCrawler(RobotsTxtCache robotsCache,
                                MarkdownReporter reporter,
                                int maxThreads,
                                long timeoutSeconds) {
        this.robotsCache = robotsCache;
        this.reporter = reporter;
        this.maxThreads = maxThreads;
        this.timeoutSeconds = timeoutSeconds;
    }

    public void crawl(CrawlerConfig config) {
        if (config == null) {
            logger.error("Crawl config cannot be null");
            return;
        }

        long startTime = System.currentTimeMillis();
        logger.info("Starting concurrent crawl with {} threads, max depth {}",
                maxThreads, config.getMaxDepth());

        ThreadSafeLinkFilter linkFilter = new ThreadSafeLinkFilter();
        ErrorCollector errorCollector = new ErrorCollector();

        try (ThreadPoolExecutor executor = createThreadPool()) {
            PageResult rootResult = crawlPageConcurrently(config.getRootUrl(), 0, config,
                    executor, linkFilter, errorCollector);

            long endTime = System.currentTimeMillis();
            logger.info("Crawl completed in {} ms. Visited {} URLs, {} errors",
                    endTime - startTime, linkFilter.getVisitedCount(), errorCollector.getTotalErrors());

            generateReport(rootResult, config, errorCollector);

        } catch (Exception e) {
            logger.error("Fatal error during concurrent crawl", e);
        }
    }

    private ThreadPoolExecutor createThreadPool() {
        return new ThreadPoolExecutor(
                Math.min(maxThreads, 4),
                maxThreads,
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

    /**
     * Entry point - crawls the root URL exactly like the sequential crawler.
     */
    private PageResult crawlPageConcurrently(URI url, int depth, CrawlerConfig config,
                                             ThreadPoolExecutor executor,
                                             ThreadSafeLinkFilter linkFilter,
                                             ErrorCollector errorCollector) {

        logger.debug("Crawling {} at depth {}", url, depth);

        // Check robots.txt first (like sequential crawler)
        if (!robotsCache.getHandler(url).isAllowed(url)) {
            logger.debug("Blocked by robots.txt: {}", url);
            return PageResult.brokenLink(url, depth);
        }

        CrawlTask task = new CrawlTask(url, depth, robotsCache, errorCollector);

        PageResult pageResult;
        try {
            pageResult = task.call();
        } catch (Exception e) {
            logger.warn("Failed to crawl {}: {}", url, e.getMessage());
            return PageResult.brokenLink(url, depth);
        }

        if (pageResult.broken()) {
            return pageResult;
        }

        List<URI> links = pageResult.getAllLinks();
        Set<PageResult> children = crawlChildrenConcurrently(links, depth + 1, config,
                executor, linkFilter, errorCollector);

        return pageResult.withChildren(children);
    }

    /**
     * Crawls child links concurrently while maintaining the hierarchical structure.
     * Uses the same logic as the sequential crawler's processChildLinks method.
     */
    private Set<PageResult> crawlChildrenConcurrently(List<URI> links, int childDepth,
                                                      CrawlerConfig config,
                                                      ThreadPoolExecutor executor,
                                                      ThreadSafeLinkFilter linkFilter,
                                                      ErrorCollector errorCollector) {

        if (links == null || links.isEmpty()) {
            return Set.of();
        }

        Set<PageResult> children = new HashSet<>();

        for (URI link : links) {
            if (isLinkEligibleForCrawling(link, childDepth - 1, config, linkFilter)) {
                Future<PageResult> future = executor.submit(() ->
                        crawlPageConcurrently(link, childDepth, config, executor, linkFilter, errorCollector)
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

    /**
     * Exactly mirrors the sequential crawler's isLinkEligibleForCrawling logic.
     */
    private boolean isLinkEligibleForCrawling(URI link, int depth, CrawlerConfig config, ThreadSafeLinkFilter linkFilter) {
        return !linkFilter.markVisited(link) &&
                linkFilter.isAllowedDomain(link, config.getAllowedDomains()) &&
                depth + 1 <= config.getMaxDepth();
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
}