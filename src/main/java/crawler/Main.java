package crawler;

import crawler.app.WebCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 3) {
            logger.error("Usage: <URL> <depth> <domains (comma-separated)>");
            System.exit(1);
        }

        try {
            URI url = URI.create(args[0]);
            int depth = Integer.parseInt(args[1]);
            String[] domains = args[2].split(",");

            // SonarCube compliant non conditional calling
            String domainLog = (domains.length > 0) ? String.join(",", domains) : "none";

            logger.info("Starting crawl: URL= {}, depth= {}, domains= {}", url, depth, domainLog);

            crawler.model.CrawlerConfig config = new crawler.model.CrawlerConfig(url, depth, domains);
            WebCrawler crawler = new WebCrawler();
            crawler.crawl(config);

            logger.info("Crawling completed.");
        } catch (Exception e) {
            logger.error("Error during crawl execution", e);
            System.exit(2);
        }
    }
}

