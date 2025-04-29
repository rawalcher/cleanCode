package crawler.fetcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class RobotsTxtHandlerTest {

    private RobotsTxtHandler handler;
    private static final URI BASE_URI = URI.create("https://example.com");

    @BeforeEach
    void setUp() {
        handler = new RobotsTxtHandler("TestBot", BASE_URI);
    }

    @ParameterizedTest
    @CsvSource({
            "/path/to/page,  true",
            "/private,       false",
            "/private/public,true"
    })
    void testPathAllowance(String path, boolean expectedAllowed) {
        handler.disallowedPaths.add(URI.create("/private"));
        handler.allowedPaths.add(URI.create("/private/public"));

        URI testUri = BASE_URI.resolve(path);
        assertEquals(expectedAllowed, handler.isAllowed(testUri),
                "Path allowance should match expected result");
    }

    @ParameterizedTest
    @CsvSource({
            "/path/../another,        /another",
            "/path/./to/page,         /path/to/page",
            "/../outside,              /",
            "/nested/../path,          /path",
            "/very/deep/../nested,     /very/nested",
            "../outside,                /",
            "/../../,                   /",
            "/../../../../../,          /",
            "invalid:path,              /"
    })
    void testPathNormalization(String originalPath, String expectedNormalizedPath) {
        URI normalized = handler.normalizePath(originalPath);

        assertNotNull(normalized, "Normalized path should not be null");
        assertEquals(expectedNormalizedPath, normalized.getPath(),
                "Path should be correctly normalized");
    }

    @Test
    void testInvalidPathNormalization() {
        URI normalized = handler.normalizePath("invalid:path");

        assertEquals("/", normalized.getPath(),
                "Invalid path should resolve to root");
    }

    @Test
    void testNormalizationOfRootPath() {
        URI normalized = handler.normalizePath("");
        assertEquals("/", normalized.getPath(),
                "Empty path should normalize to root path");
    }

    @Test
    void testDefaultPathAllowance() {
        URI testUri = URI.create("https://example.com/any/page");
        assertTrue(handler.isAllowed(testUri),
                "By default, all paths should be allowed");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/index.html",
            "/page",
            "/deep/nested/path"
    })
    void testValidPathAllowance(String path) {
        URI testUri = BASE_URI.resolve(path);
        assertTrue(handler.isAllowed(testUri),
                "Standard paths should be allowed by default");
    }

    @Test
    void testNullPathHandling() throws URISyntaxException {
        URI testUri = new URI("https://example.com");
        assertTrue(handler.isAllowed(testUri),
                "URI with null path should be allowed");
    }

    @Test
    void testComplexPathPrecedence() {
        handler.disallowedPaths.add(URI.create("/private"));
        handler.allowedPaths.add(URI.create("/private/public"));

        URI disallowedUri = BASE_URI.resolve("/private/secret");
        URI allowedUri = BASE_URI.resolve("/private/public/page");

        assertFalse(handler.isAllowed(disallowedUri),
                "More specific disallowed path should take precedence");
        assertTrue(handler.isAllowed(allowedUri),
                "Allowed path should override parent disallowed path");
    }
}