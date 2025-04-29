package crawler.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinkFilterTest {

    private LinkFilter linkFilter;

    @BeforeEach
    void setUp() {
        linkFilter = new LinkFilter();
    }

    @Test
    void testIsAllowedDomainMatchesExactly() throws Exception {
        URI url = new URI("https://www.example.com/page");
        List<String> allowedDomains = List.of("www.example.com");

        assertTrue(linkFilter.isAllowedDomain(url, allowedDomains));
    }

    @Test
    void testIsAllowedDomainMatchesSubdomain() throws Exception {
        URI url = new URI("https://subdomain.example.com/page");
        List<String> allowedDomains = List.of("example.com");

        assertTrue(linkFilter.isAllowedDomain(url, allowedDomains));
    }

    @Test
    void testIsVisitedFirstTime() throws Exception {
        URI url = new URI("https://www.example.com/page");

        assertFalse(linkFilter.isVisited(url), "First visit should return false");
        assertTrue(linkFilter.isVisited(url), "Second visit should return true");
    }

    @Test
    void testIsVisitedWithFragment() throws Exception {
        URI urlWithoutFragment = new URI("https://www.example.com/page");
        URI urlWithFragment = new URI("https://www.example.com/page#section");

        assertFalse(linkFilter.isVisited(urlWithoutFragment), "First visit should return false");
        assertTrue(linkFilter.isVisited(urlWithFragment), "Same URL with different fragment should be considered visited");
    }

    @Test
    void testIsVisitedWithQueryParams() throws Exception {
        URI urlWithoutParams = new URI("https://www.example.com/page");
        URI urlWithParams = new URI("https://www.example.com/page?param=value");
        URI urlWithDifferentParams = new URI("https://www.example.com/page?param=different");

        assertFalse(linkFilter.isVisited(urlWithoutParams), "First visit should return false");
        assertFalse(linkFilter.isVisited(urlWithParams), "Different query params should be considered unvisited");
        assertFalse(linkFilter.isVisited(urlWithDifferentParams), "Different query params should be considered unvisited");
    }

    @Test
    void testClearVisited() throws Exception {
        URI url = new URI("https://www.example.com/page");

        assertFalse(linkFilter.isVisited(url), "First visit should return false");

        linkFilter.clearVisited();

        assertFalse(linkFilter.isVisited(url), "After clearing, should be unvisited again");
    }
}