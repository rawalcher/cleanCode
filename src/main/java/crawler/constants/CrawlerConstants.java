package crawler.constants;

public class CrawlerConstants {

    private CrawlerConstants() {}

    public static final int CONNECTION_TIMEOUT_MS = 2000;
    public static final String USER_AGENT = "SimpleWebCrawlerBot/1.0";
    public static final String REPORT_FILENAME = "report.md";
    public static final int MAX_HEADING_LEVEL = 6;

    public static final int MIN_THREADS = 4;
    public static final long THREAD_KEEP_ALIVE_TIME = 60L;

}
