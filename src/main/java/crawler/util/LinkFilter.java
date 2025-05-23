package crawler.util;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates domains, detects visited URLs, and filters links based on domain/duplication rules.
 */
public class LinkFilter {

    private final Set<URI> visited = new HashSet<>();

    public boolean isAllowedDomain(URI url, List<String> allowedDomains) {
        String host = url.getHost();
        if (host == null) {
            return false;
        }
        return allowedDomains.stream().anyMatch(host::endsWith);
    }

    /**
     * Checks if the URL has been visited before.
     * If not visited, marks it as visited and returns false.
     *
     * @param url URI to check
     * @return true if already visited, false if this is a new URL
     */
    public boolean isVisited(URI url) {
        URI normalizedUrl = normalizeUri(url);

        boolean alreadyVisited = visited.contains(normalizedUrl);

        if (!alreadyVisited) {
            visited.add(normalizedUrl);
        }

        return alreadyVisited;
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

    /**
     * Clears the visited set. Useful for testing or resetting crawl state.
     */
    public void clearVisited() {
        visited.clear();
    }
}