package crawler.concurrent;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe implementation of link filtering and visited URL tracking.
 * Uses concurrent data structures to ensure thread safety without blocking.
 */
public class ThreadSafeLinkFilter {
    private final ConcurrentMap<URI, Boolean> visited = new ConcurrentHashMap<>();

    /**
     * Checks if a URL has been visited and marks it as visited atomically.
     *
     * @param url the URL to check and mark
     * @return true if the URL was already visited, false if this is the first visit
     */
    public boolean markVisited(URI url) {
        if (url == null) return true;

        URI normalizedUrl = normalizeUri(url);
        return visited.putIfAbsent(normalizedUrl, Boolean.TRUE) != null;
    }

    /**
     * Checks if a URL's domain is allowed.
     *
     * @param url the URL to check
     * @param allowedDomains list of allowed domains
     * @return true if the domain is allowed, false otherwise
     */
    public boolean isAllowedDomain(URI url, List<String> allowedDomains) {
        if (url == null || allowedDomains == null || allowedDomains.isEmpty()) {
            return false;
        }

        String host = url.getHost();
        if (host == null) return false;

        return allowedDomains.stream().anyMatch(host::endsWith);
    }

    public int getVisitedCount() {
        return visited.size();
    }

    public void clear() {
        visited.clear();
    }

    private URI normalizeUri(URI uri) {
        try {
            return new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    uri.getQuery(),
                    null
            );
        } catch (Exception e) {
            return uri;
        }
    }
}