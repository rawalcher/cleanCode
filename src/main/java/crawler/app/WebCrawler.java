package crawler.app;

/**
 * Calls all the necessary other methods / classes
 * Checks the depth constraint
 */

import crawler.model.CrawlerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    public WebCrawler() {
        // implement smth
    }

    public void crawl(CrawlerConfig config) {
        logger.info("Crawling started.");
    }
}

