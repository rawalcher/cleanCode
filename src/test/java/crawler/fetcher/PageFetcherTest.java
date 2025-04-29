package crawler.fetcher;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PageFetcherTest {

    private final PageFetcher fetcher = new PageFetcher();

    @Test
    void testFetchSuccessMock() throws Exception {
        URI url = new URI("https://example.com");

        Document fakeDoc = Jsoup.parse("<html><head><title>Fake Page</title></head></html>");

        try (MockedStatic<Jsoup> mockedJsoup = mockStatic(Jsoup.class)) {
            Connection mockConnection = mock(Connection.class);

            mockedJsoup.when(() -> Jsoup.connect(url.toString())).thenReturn(mockConnection);
            when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            when(mockConnection.ignoreHttpErrors(true)).thenReturn(mockConnection);
            when(mockConnection.ignoreContentType(true)).thenReturn(mockConnection);
            when(mockConnection.followRedirects(true)).thenReturn(mockConnection);
            when(mockConnection.get()).thenReturn(fakeDoc);

            Document result = fetcher.fetch(url);

            assertNotNull(result);
            assertEquals("Fake Page", result.title());
        }
    }

    @Test
    void testFetchFailureMock() throws Exception {
        URI url = new URI("https://bad.example.com");

        try (MockedStatic<Jsoup> mockedJsoup = mockStatic(Jsoup.class)) {
            Connection mockConnection = mock(Connection.class);

            mockedJsoup.when(() -> Jsoup.connect(url.toString())).thenReturn(mockConnection);
            when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            when(mockConnection.ignoreHttpErrors(true)).thenReturn(mockConnection);
            when(mockConnection.ignoreContentType(true)).thenReturn(mockConnection);
            when(mockConnection.followRedirects(true)).thenReturn(mockConnection);
            when(mockConnection.get()).thenThrow(new IOException("Failed to connect"));

            assertThrows(PageFetcher.FetchException.class, () -> fetcher.fetch(url));
        }
    }
}
