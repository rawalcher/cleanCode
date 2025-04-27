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

    /**
     * Checks if a URL's domain is allowed based on configuration.
     *
     * @param url The URL to check
     * @param allowedDomains List of allowed domain strings
     * @return true if domain is allowed, false otherwise
     */
    public boolean isAllowedDomain(URI url, List<String> allowedDomains) {
        String host = url.getHost();
        if (host == null) {
            return false;
        }
        return allowedDomains.stream().anyMatch(host::endsWith);
    }

    /**
     * Checks if a URL was already visited, and marks it as visited if not.
     *
     * @param url The URL to check
     * @return true if already visited, false if marking now
     */
    public boolean isVisited(URI url) {
        return !visited.add(url);
    }
}
