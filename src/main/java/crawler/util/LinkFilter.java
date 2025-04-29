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

    public boolean isVisited(URI url) {
        return !visited.add(url);
    }
}
