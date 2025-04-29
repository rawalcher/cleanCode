package crawler.app;

import crawler.fetcher.PageFetcher;
import crawler.fetcher.RobotsTxtCache;
import crawler.fetcher.RobotsTxtHandler;
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
import java.util.Set;

public class WebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    private final PageFetcher fetcher;
    private final HtmlParser parser;
    private final RobotsTxtCache robotsCache;
    private final LinkFilter linkFilter;

    public WebCrawler() {
        this.fetcher = new PageFetcher();
        this.parser = new HtmlParser();
        this.robotsCache = new RobotsTxtCache("SimpleWebCrawlerBot/1.0");
        this.linkFilter = new LinkFilter();
    }

    public void crawl(CrawlerConfig config) {
        long start = System.currentTimeMillis();
        PageResult rootResult = crawlPage(config.getRootUrl(), 0, config);
        long end = System.currentTimeMillis();
        logger.info("Crawl time: {} ms", end-start);

        // Generate report
        MarkdownReporter reporter = new MarkdownReporter();
        reporter.writeReport(rootResult, config);
    }

    private PageResult crawlPage(URI url, int depth, CrawlerConfig config) {
        logger.info("Crawling {} at depth {}", url, depth);

        if (isOutOfDepth(depth, config)) return null;
        if (!isDomainAllowed(url, config)) return null;
        if (isAlreadyVisited(url)) return null;
        if (!isAllowedByRobots(url)) return PageResult.brokenLink(url, depth);

        try {
            return fetchAndParse(url, depth, config);
        } catch (PageFetcher.FetchException e) {
            logger.warn("Failed to fetch {}: {}", url, e.getMessage());
            return PageResult.brokenLink(url, depth);
        }
    }

    private boolean isOutOfDepth(int depth, CrawlerConfig config) {
        if (depth > config.getMaxDepth()) {
            logger.info("Max depth reached at {}", depth);
            return true;
        }
        return false;
    }

    private boolean isDomainAllowed(URI url, CrawlerConfig config) {
        if (!linkFilter.isAllowedDomain(url, config.getAllowedDomains())) {
            logger.info("Domain not allowed for {}", url);
            return false;
        }
        return true;
    }

    private boolean isAlreadyVisited(URI url) {
        if (linkFilter.isVisited(url)) {
            logger.info("Already visited {}", url);
            return true;
        }
        return false;
    }

    private boolean isAllowedByRobots(URI url) {
        RobotsTxtHandler robotsHandler = robotsCache.getHandler(url);
        if (!robotsHandler.isAllowed(url)) {
            logger.warn("Blocked by robots.txt: {}", url);
            return false;
        }
        return true;
    }

    private PageResult fetchAndParse(URI url, int depth, CrawlerConfig config) throws PageFetcher.FetchException {
        Document document = fetcher.fetch(url);
        PageResult page = parser.parse(url, depth, document);

        Set<PageResult> children = new HashSet<>();
        for (URI link : page.links()) {
            PageResult child = crawlPage(link, depth + 1, config);
            if (child != null) {
                children.add(child);
            }
        }

        return new PageResult(page.url(), page.depth(), page.broken(), page.headings(), page.links(), children);
    }


}


