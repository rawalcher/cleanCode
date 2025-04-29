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
    void testIsAllowedDomainDoesNotMatch() throws Exception {
        URI url = new URI("https://www.other.com/page");
        List<String> allowedDomains = List.of("example.com");

        assertFalse(linkFilter.isAllowedDomain(url, allowedDomains));
    }

    @Test
    void testIsAllowedDomainNullHost() throws Exception {
        URI url = new URI("mailto:someone@example.com"); // no host part
        List<String> allowedDomains = List.of("example.com");

        assertFalse(linkFilter.isAllowedDomain(url, allowedDomains));
    }

    @Test
    void testIsVisitedFirstTime() throws Exception {
        URI url = new URI("https://www.example.com/page");

        assertFalse(linkFilter.isVisited(url));
    }

    @Test
    void testIsVisitedSecondTime() throws Exception {
        URI url = new URI("https://www.example.com/page");

        linkFilter.isVisited(url);
        assertTrue(linkFilter.isVisited(url));
    }
}

