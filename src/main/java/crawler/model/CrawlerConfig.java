package crawler.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public class CrawlerConfig {
    private final URL rootUrl;
    private final int maxDepth;
    private final List<String> allowedDomains;

    public CrawlerConfig(String url, int maxDepth, String[] domains) throws MalformedURLException {
        this.rootUrl = new URL(url);
        if (maxDepth < 0) {
            throw new IllegalArgumentException("Depth must be non-negative.");
        }
        this.maxDepth = maxDepth;
        this.allowedDomains = List.of(domains);
    }

    public URL getRootUrl() {
        return rootUrl;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public List<String> getAllowedDomains() {
        return allowedDomains;
    }

    // do we really need this? see if we can safely remove
    // its common practise to override, sonar even complains but lets see

    @Override
    public String toString() {
        return "CrawlerConfig{" +
                "rootUrl=" + rootUrl +
                ", maxDepth=" + maxDepth +
                ", allowedDomains=" + allowedDomains +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrawlerConfig)) return false;
        CrawlerConfig that = (CrawlerConfig) o;
        return maxDepth == that.maxDepth &&
                Objects.equals(rootUrl, that.rootUrl) &&
                Objects.equals(allowedDomains, that.allowedDomains);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rootUrl, maxDepth, allowedDomains);
    }
}

