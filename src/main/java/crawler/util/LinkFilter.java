package crawler.util;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe link filter that validates domains and tracks visited URLs.
 * Uses concurrent data structures to ensure thread safety without blocking.
 */
public class LinkFilter {
    private final ConcurrentMap<URI, Boolean> visited = new ConcurrentHashMap<>();

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

    /**
     * Checks if the URL has been visited before.
     * If not visited, mark it as visited and return false.
     * This method is thread-safe and atomic.
     *
     * @param url URI to check
     * @return true if already visited, false if this is a new URL
     */
    public synchronized boolean isVisited(URI url) {
        if (url == null) return true;

        URI normalizedUrl = normalizeUri(url);
        return visited.containsKey(normalizedUrl);
    }

    /**
     * Marks a URL as visited atomically.
     *
     * @param url the URL to mark as visited
     * @return true if the URL was already visited, false if this is the first visit
     */
    public boolean markVisited(URI url) {
        if (url == null) return true;

        URI normalizedUrl = normalizeUri(url);
        return visited.putIfAbsent(normalizedUrl, Boolean.TRUE) != null;
    }

    /**
     * Gets the count of visited URLs.
     *
     * @return number of unique URLs visited
     */
    public int getVisitedCount() {
        return visited.size();
    }

    /**
     * Clears the visited set. Useful for testing or resetting crawl state.
     */
    public void clearVisited() {
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