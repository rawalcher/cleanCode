package crawler;

import crawler.app.WebCrawler;
import crawler.model.CrawlerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static crawler.constants.CrawlerConstants.USER_AGENT;
import static crawler.factory.CrawlerFactory.createCrawler;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int DEFAULT_CONCURRENT_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    private static final long SEQUENTIAL_TIMEOUT = 30;
    private static final long CONCURRENT_TIMEOUT = 300;

    public static void main(String[] args) {
        if (!hasValidArguments(args)) {
            printUsage();
            System.exit(1);
        }

        try {
            CrawlConfiguration crawlConfig = parseArguments(args);
            executeCrawl(crawlConfig);
            logger.info("Crawling completed successfully.");
        } catch (Exception e) {
            logger.error("Error during crawl execution", e);
            System.exit(2);
        }
    }

    private static boolean hasValidArguments(String[] args) {
        if (args.length < 3) return false;
        return !args[0].equals("--concurrent") || args.length >= 4;
    }

    private static CrawlConfiguration parseArguments(String[] args) {
        if (args[0].equals("--concurrent")) {
            return parseConcurrentArguments(args);
        } else {
            return parseSequentialArguments(args);
        }
    }

    private static CrawlConfiguration parseSequentialArguments(String[] args) {
        CrawlerConfig config = createCrawlerConfig(args, 0);
        return new CrawlConfiguration(config, 1, SEQUENTIAL_TIMEOUT);
    }

    private static CrawlConfiguration parseConcurrentArguments(String[] args) {
        CrawlerConfig config = createCrawlerConfig(args, 1); // Skip --concurrent flag
        int threadCount = extractThreadCount(args);
        return new CrawlConfiguration(config, threadCount, CONCURRENT_TIMEOUT);
    }

    private static int extractThreadCount(String[] args) {
        if (args.length <= 4) {
            return DEFAULT_CONCURRENT_THREADS;
        }

        try {
            int threadCount = Integer.parseInt(args[4]);
            return threadCount > 0 ? threadCount : DEFAULT_CONCURRENT_THREADS;
        } catch (NumberFormatException e) {
            logger.warn("Invalid thread count format, using default: {}", DEFAULT_CONCURRENT_THREADS);
            return DEFAULT_CONCURRENT_THREADS;
        }
    }

    private static CrawlerConfig createCrawlerConfig(String[] args, int offset) {
        URI url = URI.create(args[offset]);
        int depth = Integer.parseInt(args[offset + 1]);
        String[] domains = args[offset + 2].split(",");

        logCrawlStart(url, depth, args[offset + 2]);
        return new CrawlerConfig(url, depth, domains);
    }

    private static void logCrawlStart(URI url, int depth, String domainString) {
        String domainLog = domainString.isEmpty() ? "none" : domainString;
        logger.info("Starting crawl: URL={}, depth={}, domains={}", url, depth, domainLog);
    }

    private static void executeCrawl(CrawlConfiguration crawlConfig) {
        WebCrawler crawler = createCrawler(USER_AGENT, crawlConfig.threadCount(), crawlConfig.timeoutSeconds());

        logCrawlerMode(crawlConfig.threadCount());
        crawler.crawl(crawlConfig.config());
    }

    private static void logCrawlerMode(int threadCount) {
        if (threadCount == 1) {
            logger.info("Using sequential crawler");
        } else {
            logger.info("Using concurrent crawler with {} threads", threadCount);
        }
    }

    private static void printUsage() {
        logger.error("Usage:");
        logger.error("  Sequential: <URL> <depth> <domains (comma-separated)>");
        logger.error("  Concurrent: --concurrent <URL> <depth> <domains (comma-separated)> [thread-count]");
        logger.error("");
        logger.error("Examples:");
        logger.error("  java -jar webcrawler.jar https://example.com 2 example.com");
        logger.error("  java -jar webcrawler.jar --concurrent https://example.com 2 example.com,example.org 8");
    }

    private record CrawlConfiguration(
            CrawlerConfig config,
            int threadCount,
            long timeoutSeconds
    ) {}
}