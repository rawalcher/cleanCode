package crawler;

import crawler.app.WebCrawler;
import crawler.fetcher.PageFetcher;
import crawler.fetcher.RobotsTxtCache;
import crawler.model.CrawlerConfig;
import crawler.parser.HtmlParser;
import crawler.util.LinkFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static crawler.constants.CrawlerConstants.USER_AGENT;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 3) {
            logger.error("Usage: <URL> <depth> <domains (comma-separated)>");
            System.exit(1);
        }

        try {
            CrawlerConfig config = parseCommandLineArgs(args);
            WebCrawler crawler = createWebCrawler();
            crawler.crawl(config);

            logger.info("Crawling completed.");
        } catch (Exception e) {
            logger.error("Error during crawl execution", e);
            System.exit(2);
        }
    }

    private static CrawlerConfig parseCommandLineArgs(String[] args) {
        URI url = URI.create(args[0]);
        int depth = Integer.parseInt(args[1]);

        String domainLog = args[2].isEmpty() ? "none" : args[2];
        logger.info("Starting crawl: URL= {}, depth= {}, domains= {}", url, depth, domainLog);

        return new CrawlerConfig(url, depth, args[2].split(","));
    }

    private static WebCrawler createWebCrawler() {
        PageFetcher fetcher = new PageFetcher();
        HtmlParser parser = new HtmlParser();
        RobotsTxtCache robotsCache = new RobotsTxtCache(USER_AGENT);
        LinkFilter linkFilter = new LinkFilter();

        return new WebCrawler(fetcher, parser, robotsCache, linkFilter);
    }
}