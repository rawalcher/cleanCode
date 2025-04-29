package crawler.fetcher;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches RobotsTxtHandler per domain to avoid re-downloading robots.txt repeatedly.
 */
public class RobotsTxtCache {

    private final Map<String, RobotsTxtHandler> robotsCache = new HashMap<>();
    private final String userAgent;

    public RobotsTxtCache(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Get the RobotsTxtHandler for a domain, loading if necessary.
     *
     * @param uri the URI to check
     * @return RobotsTxtHandler for the domain
     */
    public RobotsTxtHandler getHandler(URI uri) {
        String domain = uri.getHost();
        robotsCache.computeIfAbsent(domain, d -> new RobotsTxtHandler(userAgent, uri));
        return robotsCache.get(domain);
    }
}
