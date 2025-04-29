package crawler.model;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CrawlerConfigTest {

    @Test
    void testValidConstructor() {
        URI url = URI.create("http://example.com");
        CrawlerConfig config = new CrawlerConfig(url, 2, "example.com");

        // sonar-ignore: S125
        assertEquals(url, config.getRootUrl());
        assertEquals(2, config.getMaxDepth());
        assertTrue(config.getAllowedDomains().contains("example.com"));
    }

    @Test
    void testConstructorWithNegativeMaxDepth() {
        URI url = URI.create("http://example.com");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new CrawlerConfig(url, -1, "example.com"));

        assertEquals("Depth must be non-negative.", exception.getMessage());
    }

    @Test
    void testConstructorWithMultipleDomains() {
        URI url = URI.create("http://example.com");
        CrawlerConfig config = new CrawlerConfig(url, 2, "example.com", "another.com");

        List<String> allowedDomains = config.getAllowedDomains();
        assertTrue(allowedDomains.contains("example.com"));
        assertTrue(allowedDomains.contains("another.com"));
    }
}
