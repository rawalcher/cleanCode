package crawler;

import crawler.app.WebCrawler;
import crawler.concurrent.ConcurrentWebCrawler;
import crawler.fetcher.RobotsTxtCache;
import crawler.model.CrawlerConfig;
import crawler.reporter.MarkdownReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static crawler.constants.CrawlerConstants.USER_AGENT;
import static crawler.factory.CrawlerFactory.getWebCrawler;

/**
 * Enhanced Main class that supports both sequential and concurrent crawling modes.
 * <p>
 * Usage:
 * - Sequential: java -jar webcrawler.jar <URL> <depth> <domains>
 * - Concurrent: java -jar webcrawler.jar --concurrent <URL> <depth> <domains> [threads]
 * <p>
 * The concurrent mode uses a thread pool to crawl multiple URLs in parallel,
 * providing better performance for large crawling tasks while maintaining
 * the same output format and structure.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 3) {
            printUsage();
            System.exit(1);
        }

        try {
            CrawlerConfig config;
            boolean useConcurrent = false;
            int threadCount = Runtime.getRuntime().availableProcessors() * 2;

            if (args[0].equals("--concurrent")) {
                if (args.length < 4) {
                    printUsage();
                    System.exit(1);
                }
                useConcurrent = true;
                config = parseCommandLineArgs(args, 1); // Skip the --concurrent flag

                if (args.length > 4) {
                    try {
                        threadCount = Integer.parseInt(args[4]);
                        if (threadCount <= 0) {
                            logger.warn("Invalid thread count, using default: {}", threadCount);
                            threadCount = Runtime.getRuntime().availableProcessors() * 2;
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid thread count format, using default: {}", threadCount);
                    }
                }
            } else {
                config = parseCommandLineArgs(args, 0);
            }

            if (useConcurrent) {
                executeConcurrentCrawl(config, threadCount);
            } else {
                executeSequentialCrawl(config);
            }

            logger.info("Crawling completed successfully.");
        } catch (Exception e) {
            logger.error("Error during crawl execution", e);
            System.exit(2);
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

    private static CrawlerConfig parseCommandLineArgs(String[] args, int offset) {
        URI url = URI.create(args[offset]);
        int depth = Integer.parseInt(args[offset + 1]);
        String[] domains = args[offset + 2].split(",");

        String domainLog = args[offset + 2].isEmpty() ? "none" : args[offset + 2];
        logger.info("Starting crawl: URL={}, depth={}, domains={}", url, depth, domainLog);

        return new CrawlerConfig(url, depth, domains);
    }

    private static void executeSequentialCrawl(CrawlerConfig config) {
        logger.info("Using sequential crawler");
        WebCrawler crawler = createSequentialWebCrawler();
        crawler.crawl(config);
    }

    private static void executeConcurrentCrawl(CrawlerConfig config, int threadCount) {
        logger.info("Using concurrent crawler with {} threads", threadCount);
        ConcurrentWebCrawler crawler = createConcurrentWebCrawler(threadCount);
        crawler.crawl(config);
    }

    private static WebCrawler createSequentialWebCrawler() {
        return getWebCrawler(USER_AGENT);
    }

    private static ConcurrentWebCrawler createConcurrentWebCrawler(int threadCount) {
        RobotsTxtCache robotsCache = new RobotsTxtCache(USER_AGENT);
        MarkdownReporter reporter = new MarkdownReporter();

        return new ConcurrentWebCrawler(
                robotsCache,
                reporter,
                threadCount,
                300 // 5-minute timeout
        );
    }
}