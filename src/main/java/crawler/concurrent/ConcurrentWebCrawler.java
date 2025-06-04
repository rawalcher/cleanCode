package crawler.concurrent;

import crawler.adapters.HtmlDocumentSource;
import crawler.error.DefaultErrorHandlingStrategy;
import crawler.error.ErrorCollector;
import crawler.error.ErrorHandlingStrategy;
import crawler.fetcher.RobotsTxtCache;
import crawler.model.CrawlerConfig;
import crawler.model.PageResult;
import crawler.reporter.MarkdownReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Concurrent implementation of web crawler using ThreadPoolExecutor.
 * Manages parallel crawling of web pages while maintaining the original structure.
 *
 * Key features:
 * - Thread-safe crawling with configurable thread pool
 * - Error collection and handling
 * - Result consolidation maintaining hierarchical structure
 * - Clean shutdown and resource management
 */
public class ConcurrentWebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentWebCrawler.class);

    private final HtmlDocumentSource documentSource;
    private final RobotsTxtCache robotsCache;
    private final MarkdownReporter reporter;
    private final ErrorHandlingStrategy errorStrategy;
    private final int maxThreads;
    private final long timeoutSeconds;

    public ConcurrentWebCrawler(HtmlDocumentSource documentSource,
                                RobotsTxtCache robotsCache,
                                MarkdownReporter reporter) {
        this(documentSource, robotsCache, reporter,
                Runtime.getRuntime().availableProcessors() * 2,
                300,
                new DefaultErrorHandlingStrategy());
    }

    public ConcurrentWebCrawler(HtmlDocumentSource documentSource,
                                RobotsTxtCache robotsCache,
                                MarkdownReporter reporter,
                                int maxThreads,
                                long timeoutSeconds,
                                ErrorHandlingStrategy errorStrategy) {
        this.documentSource = documentSource;
        this.robotsCache = robotsCache;
        this.reporter = reporter;
        this.maxThreads = maxThreads;
        this.timeoutSeconds = timeoutSeconds;
        this.errorStrategy = errorStrategy;
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
            PageResult rootResult = performConcurrentCrawl(config, executor, linkFilter, errorCollector);

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
                Math.min(maxThreads, 4), // Core pool size
                maxThreads,              // Maximum pool size
                60L,                     // Keep alive time
                TimeUnit.SECONDS,        // Time unit
                new LinkedBlockingQueue<>(), // Work queue
                r -> {
                    Thread t = new Thread(r, "CrawlerThread");
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    private PageResult performConcurrentCrawl(CrawlerConfig config,
                                              ThreadPoolExecutor executor,
                                              ThreadSafeLinkFilter linkFilter,
                                              ErrorCollector errorCollector) throws InterruptedException {

        Map<URI, Future<PageResult>> futures = new ConcurrentHashMap<>();
        Queue<CrawlLevel> levelsToProcess = new LinkedList<>();

        levelsToProcess.offer(new CrawlLevel(0, List.of(config.getRootUrl())));

        PageResult rootResult = null;
        Map<URI, PageResult> allResults = new ConcurrentHashMap<>();

        while (!levelsToProcess.isEmpty() && !executor.isShutdown()) {
            CrawlLevel currentLevel = levelsToProcess.poll();

            if (currentLevel.depth() > config.getMaxDepth()) {
                break;
            }

            logger.debug("Processing depth level {} with {} URLs",
                    currentLevel.depth(), currentLevel.urls().size());

            for (URI url : currentLevel.urls()) {
                CrawlTask task = new CrawlTask(url, currentLevel.depth(), config,
                        documentSource, robotsCache, linkFilter, errorCollector);
                Future<PageResult> future = executor.submit(task);
                futures.put(url, future);
            }

            List<URI> nextLevelUrls = new ArrayList<>();

            for (Map.Entry<URI, Future<PageResult>> entry : futures.entrySet()) {
                URI url = entry.getKey();
                Future<PageResult> future = entry.getValue();

                try {
                    PageResult result = future.get(timeoutSeconds, TimeUnit.SECONDS);
                    allResults.put(url, result);

                    if (url.equals(config.getRootUrl())) {
                        rootResult = result;
                    }

                    if (currentLevel.depth() < config.getMaxDepth() && !result.broken()) {
                        nextLevelUrls.addAll(result.getAllLinks());
                    }

                } catch (TimeoutException e) {
                    logger.warn("Timeout waiting for result from {}", url);
                    future.cancel(true);
                    errorCollector.addError(
                            crawler.error.CrawlError.create(url, currentLevel.depth(),
                                    crawler.error.CrawlError.ErrorType.TIMEOUT, "Task execution timeout"));
                } catch (ExecutionException e) {
                    logger.warn("Error executing crawl task for {}: {}", url, e.getCause().getMessage());
                }
            }

            futures.clear();

            if (!nextLevelUrls.isEmpty() && currentLevel.depth() < config.getMaxDepth()) {
                List<URI> filteredUrls = nextLevelUrls.stream()
                        .filter(uri -> !linkFilter.markVisited(uri))
                        .filter(uri -> linkFilter.isAllowedDomain(uri, config.getAllowedDomains()))
                        .collect(Collectors.toList());

                if (!filteredUrls.isEmpty()) {
                    levelsToProcess.offer(new CrawlLevel(currentLevel.depth() + 1, filteredUrls));
                }
            }
        }

        return buildHierarchicalResult(rootResult, allResults);
    }

    private PageResult buildHierarchicalResult(PageResult root, Map<URI, PageResult> allResults) {
        if (root == null) {
            logger.warn("No root result found");
            return null;
        }

        return buildResultTree(root, allResults, new HashSet<>());
    }

    private PageResult buildResultTree(PageResult current, Map<URI, PageResult> allResults, Set<URI> visited) {
        if (visited.contains(current.url())) {
            return current;
        }
        visited.add(current.url());

        Set<PageResult> children = current.getAllLinks().stream()
                .map(allResults::get)
                .filter(Objects::nonNull)
                .filter(child -> !visited.contains(child.url()))
                .map(child -> buildResultTree(child, allResults, visited))
                .collect(Collectors.toSet());

        return current.withChildren(children);
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

    private record CrawlLevel(int depth, List<URI> urls) {}

    public int getMaxThreads() { return maxThreads; }
    public long getTimeoutSeconds() { return timeoutSeconds; }
}