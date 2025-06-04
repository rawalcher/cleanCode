package crawler.concurrent;

import crawler.adapters.HtmlDocument;
import crawler.adapters.HtmlDocumentSource;
import crawler.error.CrawlError;
import crawler.error.ErrorCollector;
import crawler.fetcher.RobotsTxtCache;
import crawler.model.CrawlerConfig;
import crawler.model.PageResult;
import crawler.model.PageResult.Heading;
import crawler.model.PageResult.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Represents a single crawl task that can be executed by a thread pool.
 * Each task is responsible for crawling one URL and returning the result.
 * Implements Callable to support Future-based result retrieval.
 */
public class CrawlTask implements Callable<PageResult> {
    private static final Logger logger = LoggerFactory.getLogger(CrawlTask.class);

    private final URI url;
    private final int depth;
    private final CrawlerConfig config;
    private final HtmlDocumentSource documentSource;
    private final RobotsTxtCache robotsCache;
    private final ThreadSafeLinkFilter linkFilter;
    private final ErrorCollector errorCollector;

    public CrawlTask(URI url, int depth, CrawlerConfig config,
                     HtmlDocumentSource documentSource, RobotsTxtCache robotsCache,
                     ThreadSafeLinkFilter linkFilter, ErrorCollector errorCollector) {
        this.url = url;
        this.depth = depth;
        this.config = config;
        this.documentSource = documentSource;
        this.robotsCache = robotsCache;
        this.linkFilter = linkFilter;
        this.errorCollector = errorCollector;
    }

    @Override
    public PageResult call() throws Exception {
        logger.debug("Starting crawl task for {} at depth {}", url, depth);

        try {
            return performCrawl();
        } catch (Exception e) {
            CrawlError error = CrawlError.create(url, depth,
                    CrawlError.ErrorType.UNKNOWN, "Unexpected error during crawl", e);
            errorCollector.addError(error);
            return PageResult.brokenLink(url, depth);
        }
    }

    private PageResult performCrawl() {
        if (linkFilter.markVisited(url)) {
            logger.debug("URL already visited: {}", url);
            return PageResult.brokenLink(url, depth);
        }

        if (!linkFilter.isAllowedDomain(url, config.getAllowedDomains())) {
            logger.debug("Domain not allowed: {}", url);
            CrawlError error = CrawlError.create(url, depth,
                    CrawlError.ErrorType.INVALID_URL, "Domain not in allowed list");
            errorCollector.addError(error);
            return PageResult.brokenLink(url, depth);
        }

        if (!robotsCache.getHandler(url).isAllowed(url)) {
            logger.debug("Blocked by robots.txt: {}", url);
            CrawlError error = CrawlError.create(url, depth,
                    CrawlError.ErrorType.ROBOTS_BLOCKED, "Blocked by robots.txt");
            errorCollector.addError(error);
            return PageResult.brokenLink(url, depth);
        }

        try {
            HtmlDocument document = documentSource.fetchDocument(url);
            return parseDocument(document);
        } catch (HtmlDocumentSource.DocumentRetrievalException e) {
            handleDocumentRetrievalError(e);
            return PageResult.brokenLink(url, depth);
        }
    }

    private PageResult parseDocument(HtmlDocument document) {
        List<HtmlDocument.DocumentHeading> headings = document.getHeadings();
        List<URI> links = document.getLinks(url);

        List<Section> sections = convertToSections(headings, links);

        logger.debug("Parsed {} headings and {} links from {}",
                headings.size(), links.size(), url);

        return new PageResult(url, depth, false, sections, Set.of());
    }

    private List<Section> convertToSections(List<HtmlDocument.DocumentHeading> headings, List<URI> links) {

        if (headings.isEmpty()) {
            if (links.isEmpty()) {
                return List.of();
            }

            Heading rootHeading = new Heading(0, "Page Root");
            LinkedHashSet<URI> linkSet = new LinkedHashSet<>(links);
            return List.of(new Section(rootHeading, linkSet));
        }

        return headings.stream()
                .map(h -> new Heading(h.level(), h.text()))
                .map(h -> new Section(h, new LinkedHashSet<>()))
                .collect(Collectors.toList());
    }

    private void handleDocumentRetrievalError(HtmlDocumentSource.DocumentRetrievalException e) {
        CrawlError.ErrorType errorType = switch (e.getErrorType()) {
            case NETWORK_ERROR -> CrawlError.ErrorType.NETWORK_ERROR;
            case HTTP_ERROR -> CrawlError.ErrorType.HTTP_ERROR;
            case TIMEOUT -> CrawlError.ErrorType.TIMEOUT;
            case INVALID_URL -> CrawlError.ErrorType.INVALID_URL;
            case PARSING_ERROR -> CrawlError.ErrorType.PARSING_ERROR;
        };

        CrawlError error = CrawlError.create(url, depth, errorType, e.getMessage(), e);
        errorCollector.addError(error);

        logger.warn("Failed to retrieve document from {}: {}", url, e.getMessage());
    }

    public URI getUrl() {
        return url;
    }

    public int getDepth() {
        return depth;
    }
}