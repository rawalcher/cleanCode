package crawler.fetcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple robots.txt handler: downloads and parses robots.txt,
 * checks if a given URI is allowed to crawl.
 */
public class RobotsTxtHandler {

    private static final Logger logger = LoggerFactory.getLogger(RobotsTxtHandler.class);

    protected final Set<URI> disallowedPaths = new HashSet<>();
    protected final Set<URI> allowedPaths = new HashSet<>();
    protected int delay = 0;
    private final String userAgent;

    public RobotsTxtHandler(String userAgent, URI baseUri) {
        this.userAgent = userAgent;
        fetchAndParseRobotsTxt(baseUri);
    }

    private void fetchAndParseRobotsTxt(URI baseUri) {
        try (BufferedReader reader = downloadRobotsTxt(baseUri)) {
            parseRobotsTxt(reader);
        } catch (IOException e) {
            logger.warn("No robots.txt found or failed to load for {}: {}", baseUri.getHost(), e.getMessage());
        }
    }

    private BufferedReader downloadRobotsTxt(URI baseUri) throws IOException {
        try {
            URI robotsUri = new URI(
                    baseUri.getScheme(),
                    baseUri.getHost(),
                    "/robots.txt",
                    null
            );
            URL robotsUrl = robotsUri.toURL();
            logger.info("Fetching robots.txt from {}", robotsUrl);
            return new BufferedReader(new InputStreamReader(robotsUrl.openStream()));
        } catch (Exception e) {
            throw new IOException("Failed to construct robots.txt URL for " + baseUri, e);
        }
    }

    private void parseRobotsTxt(BufferedReader reader) throws IOException {
        boolean appliesToUs = false;
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (isIgnorableLine(line)) continue;

            String[] parts = line.split(":", 2);

            String key = parts[0].trim().toLowerCase();
            String value = parts[1].trim();

            appliesToUs = handleRobotsDirective(key, value, appliesToUs);
        }

        logger.info("Finished parsing robots.txt: {} allowed, {} disallowed paths, delay {}",
                allowedPaths.size(), disallowedPaths.size(), delay);
    }

    private boolean isIgnorableLine(String line) {
        return line.startsWith("#") || !line.contains(":");
    }

    private boolean handleRobotsDirective(String key, String value, boolean appliesToUs) {
        switch (key) {
            case "user-agent":
                return checkUserAgent(value);
            case "disallow":
                if (appliesToUs) addDisallowedPath(value);
                break;
            case "allow":
                if (appliesToUs) addAllowedPath(value);
                break;
            case "crawl-delay":
                if (appliesToUs) parseCrawlDelay(value);
                break;
            default:
                // do nothing as its maybe malformed
        }
        return appliesToUs;
    }

    private void parseCrawlDelay(String value) {
        try {
            this.delay = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid crawl-delay value in robots.txt: {}", value);
        }
    }

    private boolean checkUserAgent(String line) {
        if (line.length() <= 11) {
            return false;
        }

        String agent = line.substring(11).trim();
        return agent.equals("*") || agent.equalsIgnoreCase(userAgent);
    }

    private void addDisallowedPath(String line) {
        String path = line.substring(9).trim();
        if (!path.isEmpty()) {
            disallowedPaths.add(normalizePath(path));
        }
    }

    private void addAllowedPath(String line) {
        String path = line.substring(6).trim();
        if (!path.isEmpty()) {
            allowedPaths.add(normalizePath(path));
        }
    }

    URI normalizePath(String path) {
        try {
            URI rawUri = new URI(path);
            URI normalized = rawUri.normalize();
            if (normalized.getPath() == null || normalized.getPath().isEmpty()) {
                return URI.create("/");
            }
            return normalized;
        } catch (Exception e) {
            logger.warn("Failed to normalize path '{}', defaulting to root '/'", path);
            return URI.create("/");
        }
    }

    /**
     * Check if a URI is allowed to crawl.
     *
     * @param uri the URI to check
     * @return true if allowed, false if disallowed
     */
    public boolean isAllowed(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        URI normalizedPath = normalizePath(path);

        for (URI allow : allowedPaths) {
            if (normalizedPath.getPath().startsWith(allow.getPath())) {
                return true;
            }
        }
        for (URI disallow : disallowedPaths) {
            if (normalizedPath.getPath().startsWith(disallow.getPath())) {
                return false;
            }
        }

        return true;
    }
}
