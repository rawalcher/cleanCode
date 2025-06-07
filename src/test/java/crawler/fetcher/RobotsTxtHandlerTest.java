package crawler.fetcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class RobotsTxtHandlerTest {

    private RobotsTxtHandler handler;
    private static final URI BASE_URI = URI.create("https://example.com");

    @BeforeEach
    void setUp() {
        handler = new RobotsTxtHandler("TestBot", BASE_URI);
    }

    @Test
    void testPathAllowanceLogic() {
        handler.disallowedPaths.add(URI.create("/private"));
        handler.allowedPaths.add(URI.create("/private/public"));

        URI disallowedUri = BASE_URI.resolve("/private/secret");
        assertFalse(handler.isAllowed(disallowedUri));

        URI allowedUri = BASE_URI.resolve("/private/public/page");
        assertTrue(handler.isAllowed(allowedUri));

        URI normalUri = BASE_URI.resolve("/normal/page");
        assertTrue(handler.isAllowed(normalUri));
    }

    @Test
    void testPathNormalization() {
        URI normalized = handler.normalizePath("/path/../another");
        assertEquals("/another", normalized.getPath());

        URI normalized2 = handler.normalizePath("/path/./to/page");
        assertEquals("/path/to/page", normalized2.getPath());

        URI normalized3 = handler.normalizePath("invalid:path");
        assertEquals("/", normalized3.getPath());
    }
}