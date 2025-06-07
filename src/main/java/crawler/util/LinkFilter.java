package crawler.util;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LinkFilter {
    private final ConcurrentMap<URI, Boolean> visited = new ConcurrentHashMap<>();

    public boolean isAllowedDomain(URI url, List<String> allowedDomains) {
        if (url == null || allowedDomains == null || allowedDomains.isEmpty()) {
            return false;
        }

        String host = url.getHost();
        if (host == null) return false;

        return allowedDomains.stream().anyMatch(host::endsWith);
    }

    public boolean isVisited(URI url) {
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
        if (url == null) return false;

        URI normalizedUrl = normalizeUri(url);
        // putIfAbsent returns null if the key was not present (first visit)
        // returns the existing value if the key was already present (already visited)
        return visited.putIfAbsent(normalizedUrl, Boolean.TRUE) == null;
    }

    public int getVisitedCount() {
        return visited.size();
    }

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