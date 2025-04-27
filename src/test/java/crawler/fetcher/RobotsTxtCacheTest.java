package crawler.fetcher;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class RobotsTxtCacheTest {

    @Test
    void testGetHandlerFirstTimeCreatesNew() throws Exception {
        RobotsTxtCache cache = new RobotsTxtCache("SimpleBot");
        URI uri = new URI("https://www.example.com/page");

        RobotsTxtHandler handler = cache.getHandler(uri);

        assertNotNull(handler);
    }

    @Test
    void testGetHandlerCachesSameDomain() throws Exception {
        RobotsTxtCache cache = new RobotsTxtCache("SimpleBot");
        URI uri1 = new URI("https://www.example.com/page1");
        URI uri2 = new URI("https://www.example.com/page2");

        RobotsTxtHandler handler1 = cache.getHandler(uri1);
        RobotsTxtHandler handler2 = cache.getHandler(uri2);

        assertSame(handler1, handler2, "Handler should be reused for same domain");
    }

    @Test
    void testGetHandlerDifferentDomainsCreatesDifferentHandlers() throws Exception {
        RobotsTxtCache cache = new RobotsTxtCache("SimpleBot");
        URI uri1 = new URI("https://www.example.com");
        URI uri2 = new URI("https://www.anotherdomain.com");

        RobotsTxtHandler handler1 = cache.getHandler(uri1);
        RobotsTxtHandler handler2 = cache.getHandler(uri2);

        assertNotSame(handler1, handler2, "Handlers should be different for different domains");
    }
}

