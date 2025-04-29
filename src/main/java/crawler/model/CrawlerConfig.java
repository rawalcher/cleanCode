package crawler.model;

import java.net.URI;
import java.util.List;

public class CrawlerConfig {
    private final URI rootUrl;
    private final int maxDepth;
    private final List<String> allowedDomains;

    public CrawlerConfig(URI url, int maxDepth, String... domains) {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null.");
        }
        this.rootUrl = url;

        if (maxDepth < 0) {
            throw new IllegalArgumentException("Depth must be non-negative.");
        }
        this.maxDepth = maxDepth;

        if (domains == null) {
            throw new IllegalArgumentException("Domains cannot be null.");
        }
        this.allowedDomains = List.of(domains);
    }

    public URI getRootUrl() {
        return rootUrl;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public List<String> getAllowedDomains() {
        return allowedDomains;
    }
}