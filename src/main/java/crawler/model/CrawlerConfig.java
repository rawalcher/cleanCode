package crawler.model;

import java.net.URI;
import java.util.List;

public class CrawlerConfig {
    private final URI rootUrl;
    private final int maxDepth;
    private final List<String> allowedDomains;

    public CrawlerConfig(URI url, int maxDepth, String... domains) {
        this.rootUrl = url;
        if (maxDepth < 0) {
            throw new IllegalArgumentException("Depth must be non-negative.");
        }
        this.maxDepth = maxDepth;
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