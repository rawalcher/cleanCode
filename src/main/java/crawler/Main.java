package main.java.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 3) {
            logger.error("Usage: java -jar crawler.jar <URL> <depth> <domains (comma-separated)>");
            System.exit(1);
        }

        try {
            String url = args[0];
            int depth = Integer.parseInt(args[1]);
            String[] domains = args[2].split(",");

            logger.info("Starting crawl: URL={}, depth={}, domains={}", url, depth, String.join(",", domains));

            crawler.model.CrawlerConfig config = new crawler.model.CrawlerConfig(url, depth, domains);
            //WebCrawler crawler = new WebCrawler();
            //crawler.crawl(config);

            logger.info("Crawling completed.");
        } catch (Exception e) {
            logger.error("Error during crawl execution", e);
            System.exit(2);
        }
    }
}

