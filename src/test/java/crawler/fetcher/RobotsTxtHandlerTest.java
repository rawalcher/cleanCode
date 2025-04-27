package crawler.fetcher;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class RobotsTxtHandlerTest {

    static class TestRobotsTxtHandler extends RobotsTxtHandler {
        TestRobotsTxtHandler() {
            super("TestBot", URI.create("https://example.com"));
        }

        // Allow test to directly modify paths
        void addAllow() {
            allowedPaths.add(URI.create("/private/public"));
        }

        void addDisallow() {
            disallowedPaths.add(URI.create("/private"));
        }
    }

    @Test
    void testIsAllowedWithNoRules() throws Exception {
        RobotsTxtHandler handler = new TestRobotsTxtHandler();

        URI testUri = new URI("https://example.com/page");

        assertTrue(handler.isAllowed(testUri));
    }

    @Test
    void testIsDisallowed() throws Exception {
        TestRobotsTxtHandler handler = new TestRobotsTxtHandler();
        handler.addDisallow();

        URI allowed = new URI("https://example.com/public/page");
        URI disallowed = new URI("https://example.com/private/page");

        assertTrue(handler.isAllowed(allowed));
        assertFalse(handler.isAllowed(disallowed));
    }

    @Test
    void testIsAllowedOverridesDisallow() throws Exception {
        TestRobotsTxtHandler handler = new TestRobotsTxtHandler();
        handler.addDisallow();
        handler.addAllow();

        URI allowed = new URI("https://example.com/private/public/page");
        URI disallowed = new URI("https://example.com/private/secret");

        assertTrue(handler.isAllowed(allowed));
        assertFalse(handler.isAllowed(disallowed));
    }

    @Test
    void testNormalizePath() {
        RobotsTxtHandler handler = new TestRobotsTxtHandler();

        URI normalized = handler.normalizePath("/path/../another");
        assertEquals("/another", normalized.getPath());
    }
}
