package crawler.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkFilterTest {

    private LinkFilter linkFilter;

    @BeforeEach
    void setUp() {
        linkFilter = new LinkFilter();
    }

    @Test
    void testMarkVisitedReturnsCorrectValue() throws Exception {
        URI url = new URI("https://example.com/page");

        assertTrue(linkFilter.markVisited(url));
        assertFalse(linkFilter.markVisited(url));
    }

    @Test
    void testIsAllowedDomainWithSubdomain() throws Exception {
        URI url = new URI("https://subdomain.example.com/page");
        List<String> allowedDomains = List.of("example.com");

        assertTrue(linkFilter.isAllowedDomain(url, allowedDomains));
    }

    @Test
    void testIsAllowedDomainRejectsUnallowed() throws Exception {
        URI url = new URI("https://evil.com/page");
        List<String> allowedDomains = List.of("example.com");

        assertFalse(linkFilter.isAllowedDomain(url, allowedDomains));
    }

    @Test
    void testFragmentNormalization() throws Exception {
        URI urlWithoutFragment = new URI("https://example.com/page");
        URI urlWithFragment = new URI("https://example.com/page#section");

        assertTrue(linkFilter.markVisited(urlWithoutFragment));
        assertFalse(linkFilter.markVisited(urlWithFragment));
    }
}