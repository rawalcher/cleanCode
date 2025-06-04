package crawler.concurrent;

import crawler.adapters.HtmlDocumentSource;
import crawler.error.CrawlError;
import crawler.error.ErrorCollector;
import crawler.fetcher.RobotsTxtCache;
import crawler.model.PageResult;
import crawler.parser.HtmlParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.Callable;

/**
 * Represents a single crawl task that can be executed by a thread pool.
 * Each task is responsible for crawling one URL and returning the result.
 * Use the original HtmlParser to ensure identical parsing behavior.
 */
public class CrawlTask implements Callable<PageResult> {
    private static final Logger logger = LoggerFactory.getLogger(CrawlTask.class);

    private final URI url;
    private final int depth;
    private final RobotsTxtCache robotsCache;
    private final ErrorCollector errorCollector;
    private final HtmlParser htmlParser;

    public CrawlTask(URI url, int depth, RobotsTxtCache robotsCache,
                     ErrorCollector errorCollector) {
        this.url = url;
        this.depth = depth;
        this.robotsCache = robotsCache;
        this.errorCollector = errorCollector;
        this.htmlParser = new HtmlParser();
    }

    @Override
    public PageResult call() {
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
        // Check robots.txt permission
        if (!robotsCache.getHandler(url).isAllowed(url)) {
            logger.debug("Blocked by robots.txt: {}", url);
            CrawlError error = CrawlError.create(url, depth,
                    CrawlError.ErrorType.ROBOTS_BLOCKED, "Blocked by robots.txt");
            errorCollector.addError(error);
            return PageResult.brokenLink(url, depth);
        }

        try {
            String html = fetchHtmlContent();
            Document document = Jsoup.parse(html, url.toString());

            return htmlParser.parse(url, depth, document);

        } catch (Exception e) {
            handleFetchError(e);
            return PageResult.brokenLink(url, depth);
        }
    }

    private String fetchHtmlContent() throws Exception {
        return Jsoup.connect(url.toString())
                .timeout(5000)
                .userAgent("SimpleWebCrawlerBot/1.0")
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .followRedirects(true)
                .get()
                .html();

    }

    private void handleFetchError(Exception e) {
        CrawlError.ErrorType errorType = CrawlError.ErrorType.NETWORK_ERROR;

        if (e.getCause() instanceof HtmlDocumentSource.DocumentRetrievalException dre) {
            errorType = switch (dre.getErrorType()) {
                case NETWORK_ERROR -> CrawlError.ErrorType.NETWORK_ERROR;
                case HTTP_ERROR -> CrawlError.ErrorType.HTTP_ERROR;
                case TIMEOUT -> CrawlError.ErrorType.TIMEOUT;
                case INVALID_URL -> CrawlError.ErrorType.INVALID_URL;
                case PARSING_ERROR -> CrawlError.ErrorType.PARSING_ERROR;
            };
        }

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