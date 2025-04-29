package crawler.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CrawlerConfigTest {

    private static final URI VALID_URI = URI.create("https://example.com");
    private static final String VALID_DOMAIN = "example.com";

    @Test
    void testBasicConstructorAndGetters() {
        CrawlerConfig config = new CrawlerConfig(VALID_URI, 2, VALID_DOMAIN);

        assertEquals(VALID_URI, config.getRootUrl());
        assertEquals(2, config.getMaxDepth());
        assertEquals(1, config.getAllowedDomains().size());
        assertTrue(config.getAllowedDomains().contains(VALID_DOMAIN));
    }

    @Test
    void testNullUrlThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> new CrawlerConfig(null, 1, VALID_DOMAIN));

        assertEquals("URL cannot be null.", exception.getMessage());
    }

    @Test
    void testNullDomainsThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> new CrawlerConfig(VALID_URI, 1, (String[])null));

        assertEquals("Domains cannot be null.", exception.getMessage());
    }

    @Test
    void testEmptyDomainsIsValid() {
        CrawlerConfig config = new CrawlerConfig(VALID_URI, 1);

        assertTrue(config.getAllowedDomains().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100})
    void testNegativeMaxDepthThrowsException(int negativeDepth) {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> new CrawlerConfig(VALID_URI, negativeDepth, VALID_DOMAIN));

        assertEquals("Depth must be non-negative.", exception.getMessage());
    }

    @Test
    void testZeroMaxDepthIsValid() {
        CrawlerConfig config = new CrawlerConfig(VALID_URI, 0, VALID_DOMAIN);

        assertEquals(0, config.getMaxDepth());
    }

    @Test
    void testLargeMaxDepthIsValid() {
        int veryLargeDepth = Integer.MAX_VALUE;
        CrawlerConfig config = new CrawlerConfig(VALID_URI, veryLargeDepth, VALID_DOMAIN);

        assertEquals(veryLargeDepth, config.getMaxDepth());
    }

    @Test
    void testMultipleDomainsAreHandledCorrectly() {
        String[] domains = {"example.com", "example.org", "another-domain.net"};
        CrawlerConfig config = new CrawlerConfig(VALID_URI, 1, domains);

        List<String> allowedDomains = config.getAllowedDomains();
        assertEquals(3, allowedDomains.size());
        assertTrue(allowedDomains.contains("example.com"));
        assertTrue(allowedDomains.contains("example.org"));
        assertTrue(allowedDomains.contains("another-domain.net"));
    }

    @Test
    void testDomainsShouldBeDefensivelyEncapsulated() {
        String[] domains = {"example.com"};
        CrawlerConfig config = new CrawlerConfig(VALID_URI, 1, domains);

        domains[0] = "modified.com";

        List<String> allowedDomains = config.getAllowedDomains();
        assertTrue(allowedDomains.contains("example.com"));
        assertFalse(allowedDomains.contains("modified.com"));
    }

    @Test
    void testAllowedDomainsListShouldGiveCorrectValues() {
        CrawlerConfig config = new CrawlerConfig(VALID_URI, 1, "example.com", "test.org");

        List<String> allowedDomains = config.getAllowedDomains();

        assertEquals(2, allowedDomains.size());
        assertTrue(allowedDomains.contains("example.com"));
        assertTrue(allowedDomains.contains("test.org"));
    }
}