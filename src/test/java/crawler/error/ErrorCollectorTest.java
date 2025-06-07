package crawler.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErrorCollectorTest {

    private ErrorCollector errorCollector;
    private URI testUrl;

    @BeforeEach
    void setUp() throws Exception {
        errorCollector = new ErrorCollector();
        testUrl = new URI("https://example.com/test");
    }

    @Test
    void testBasicErrorCollection() {
        CrawlError error = CrawlError.create(testUrl, 1,
                CrawlError.ErrorType.NETWORK_ERROR, "Connection failed");

        errorCollector.addError(error);

        assertEquals(1, errorCollector.getTotalErrors());
        assertTrue(errorCollector.hasErrors());
        assertEquals(error, errorCollector.getAllErrors().get(0));
    }

    @Test
    void testErrorsByType() {
        CrawlError networkError = CrawlError.create(testUrl, 1,
                CrawlError.ErrorType.NETWORK_ERROR, "Network failed");
        CrawlError httpError = CrawlError.create(testUrl, 1,
                CrawlError.ErrorType.HTTP_ERROR, "HTTP 404");
        CrawlError anotherNetworkError = CrawlError.create(testUrl, 2,
                CrawlError.ErrorType.NETWORK_ERROR, "Another network error");

        errorCollector.addError(networkError);
        errorCollector.addError(httpError);
        errorCollector.addError(anotherNetworkError);

        List<CrawlError> networkErrors = errorCollector.getErrorsByType(CrawlError.ErrorType.NETWORK_ERROR);
        assertEquals(2, networkErrors.size());

        List<CrawlError> httpErrors = errorCollector.getErrorsByType(CrawlError.ErrorType.HTTP_ERROR);
        assertEquals(1, httpErrors.size());

        List<CrawlError> timeoutErrors = errorCollector.getErrorsByType(CrawlError.ErrorType.TIMEOUT);
        assertEquals(0, timeoutErrors.size());
    }

    @Test
    void testErrorStatistics() {
        errorCollector.addError(CrawlError.create(testUrl, 1, CrawlError.ErrorType.NETWORK_ERROR, "Error 1"));
        errorCollector.addError(CrawlError.create(testUrl, 1, CrawlError.ErrorType.NETWORK_ERROR, "Error 2"));
        errorCollector.addError(CrawlError.create(testUrl, 1, CrawlError.ErrorType.HTTP_ERROR, "Error 3"));

        Map<CrawlError.ErrorType, Long> stats = errorCollector.getErrorStatistics();

        assertEquals(2L, stats.get(CrawlError.ErrorType.NETWORK_ERROR));
        assertEquals(1L, stats.get(CrawlError.ErrorType.HTTP_ERROR));
        assertNull(stats.get(CrawlError.ErrorType.TIMEOUT));
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        // This is the most important test for ErrorCollector
        // since it's used in concurrent crawling

        int numThreads = 10;
        int errorsPerThread = 100;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < errorsPerThread; j++) {
                    CrawlError error = CrawlError.create(testUrl, threadId,
                            CrawlError.ErrorType.NETWORK_ERROR, "Thread " + threadId + " Error " + j);
                    errorCollector.addError(error);
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all errors were collected correctly
        assertEquals(numThreads * errorsPerThread, errorCollector.getTotalErrors());
        assertEquals(numThreads * errorsPerThread, errorCollector.getAllErrors().size());

        // Verify statistics are correct
        Map<CrawlError.ErrorType, Long> stats = errorCollector.getErrorStatistics();
        assertEquals((long) numThreads * errorsPerThread, stats.get(CrawlError.ErrorType.NETWORK_ERROR));
    }

    @Test
    void testClear() {
        errorCollector.addError(CrawlError.create(testUrl, 1,
                CrawlError.ErrorType.NETWORK_ERROR, "Test error"));

        assertTrue(errorCollector.hasErrors());
        assertEquals(1, errorCollector.getTotalErrors());

        errorCollector.clear();

        assertFalse(errorCollector.hasErrors());
        assertEquals(0, errorCollector.getTotalErrors());
        assertTrue(errorCollector.getAllErrors().isEmpty());
    }

    @Test
    void testNullErrorHandling() {
        errorCollector.addError(null);

        assertEquals(0, errorCollector.getTotalErrors());
        assertFalse(errorCollector.hasErrors());
        assertTrue(errorCollector.getAllErrors().isEmpty());
    }
}

class CrawlErrorTest {

    @Test
    void testBasicCreation() throws Exception {
        URI url = new URI("https://example.com");

        CrawlError error = CrawlError.create(url, 1,
                CrawlError.ErrorType.NETWORK_ERROR, "Connection failed");

        assertEquals(url, error.url());
        assertEquals(1, error.depth());
        assertEquals(CrawlError.ErrorType.NETWORK_ERROR, error.type());
        assertEquals("Connection failed", error.message());
        assertNotNull(error.timestamp());
    }

    @Test
    void testCreateWithException() throws Exception {
        URI url = new URI("https://example.com");
        Exception cause = new RuntimeException("Test exception");

        CrawlError error = CrawlError.create(url, 1,
                CrawlError.ErrorType.PARSING_ERROR, "Parsing failed", cause);

        assertEquals("RuntimeException: Test exception", error.details());
    }

    @Test
    void testValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                CrawlError.create(null, 1, CrawlError.ErrorType.NETWORK_ERROR, "Test"));

        assertThrows(IllegalArgumentException.class, () ->
                new CrawlError(URI.create("https://example.com"), 1, null, "Test", "", null));

        assertThrows(IllegalArgumentException.class, () ->
                CrawlError.create(URI.create("https://example.com"), 1, CrawlError.ErrorType.NETWORK_ERROR, null));

        assertThrows(IllegalArgumentException.class, () ->
                CrawlError.create(URI.create("https://example.com"), 1, CrawlError.ErrorType.NETWORK_ERROR, "   "));
    }
}