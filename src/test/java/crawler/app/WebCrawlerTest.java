package crawler.app;

import crawler.fetcher.PageFetcher;
import crawler.fetcher.RobotsTxtCache;
import crawler.fetcher.RobotsTxtHandler;
import crawler.model.CrawlerConfig;
import crawler.model.PageResult;
import crawler.parser.HtmlParser;
import crawler.reporter.MarkdownReporter;
import crawler.util.LinkFilter;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WebCrawlerTest {

    @Mock private PageFetcher mockFetcher;
    @Mock private HtmlParser mockParser;
    @Mock private RobotsTxtCache mockRobotsCache;
    @Mock private RobotsTxtHandler mockRobotsHandler;
    @Mock private LinkFilter mockLinkFilter;
    @Mock private MarkdownReporter mockReporter;
    @Mock private Document mockDocument;

    private WebCrawler sequentialCrawler;
    private WebCrawler concurrentCrawler;
    private URI rootUrl;
    private CrawlerConfig config;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        sequentialCrawler = new WebCrawler(mockFetcher, mockParser, mockRobotsCache,
                mockLinkFilter, mockReporter, 1, 30);
        concurrentCrawler = new WebCrawler(mockFetcher, mockParser, mockRobotsCache,
                mockLinkFilter, mockReporter, 4, 60);

        rootUrl = new URI("https://example.com");
        config = new CrawlerConfig(rootUrl, 2, "example.com");

        when(mockRobotsCache.getHandler(any(URI.class))).thenReturn(mockRobotsHandler);
    }

    @Test
    void testSequentialCrawlBasicFlow() throws Exception {
        PageResult mockResult = createMockPageResult(rootUrl, 0, List.of());
        setupSuccessfulCrawl(rootUrl, 0, mockResult);

        sequentialCrawler.crawl(config);

        verify(mockFetcher).fetch(rootUrl);
        verify(mockParser).parse(eq(rootUrl), eq(0), any(Document.class));
        verify(mockReporter).writeReport(any(PageResult.class), eq(config));
    }

    @Test
    void testCrawlWithChildren() throws Exception {
        URI childUrl = new URI("https://example.com/child");

        PageResult rootResult = createMockPageResult(rootUrl, 0, List.of(childUrl));
        PageResult childResult = createMockPageResult(childUrl, 1, List.of());

        setupSuccessfulCrawl(rootUrl, 0, rootResult);
        setupSuccessfulCrawl(childUrl, 1, childResult);

        sequentialCrawler.crawl(config);

        verify(mockFetcher).fetch(rootUrl);
        verify(mockFetcher).fetch(childUrl);
    }

    @Test
    void testRobotsBlocking() throws Exception {
        when(mockLinkFilter.isAllowedDomain(rootUrl, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.markVisited(rootUrl)).thenReturn(true);
        when(mockRobotsHandler.isAllowed(rootUrl)).thenReturn(false);

        sequentialCrawler.crawl(config);

        verify(mockFetcher, never()).fetch(rootUrl);
        verify(mockReporter).writeReport(any(PageResult.class), eq(config));
    }

    @Test
    void testDomainFiltering() throws Exception {
        URI externalUrl = new URI("https://external.com/page");

        PageResult rootResult = createMockPageResult(rootUrl, 0, List.of(externalUrl));
        setupSuccessfulCrawl(rootUrl, 0, rootResult);

        when(mockLinkFilter.isAllowedDomain(externalUrl, config.getAllowedDomains())).thenReturn(false);

        sequentialCrawler.crawl(config);

        verify(mockFetcher).fetch(rootUrl);
        verify(mockFetcher, never()).fetch(externalUrl);
    }

    @Test
    void testDepthLimit() throws Exception {
        // Create a chain: root -> child -> grandchild
        URI childUrl = new URI("https://example.com/child");
        URI grandchildUrl = new URI("https://example.com/grandchild");

        PageResult rootResult = createMockPageResult(rootUrl, 0, List.of(childUrl));
        PageResult childResult = createMockPageResult(childUrl, 1, List.of(grandchildUrl));

        setupSuccessfulCrawl(rootUrl, 0, rootResult);
        setupSuccessfulCrawl(childUrl, 1, childResult);

        // Grandchild would be at depth 2, which equals maxDepth (2), so it should be crawled
        // But if we set maxDepth to 1, it shouldn't be crawled
        CrawlerConfig limitedConfig = new CrawlerConfig(rootUrl, 1, "example.com");

        sequentialCrawler.crawl(limitedConfig);

        verify(mockFetcher).fetch(rootUrl);
        verify(mockFetcher).fetch(childUrl);
        verify(mockFetcher, never()).fetch(grandchildUrl);
    }

    @Test
    void testVisitedUrlsNotRecrawled() throws Exception {
        URI childUrl = new URI("https://example.com/child");

        PageResult rootResult = createMockPageResult(rootUrl, 0, List.of(childUrl));
        setupSuccessfulCrawl(rootUrl, 0, rootResult);

        when(mockLinkFilter.isAllowedDomain(childUrl, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.markVisited(childUrl)).thenReturn(false);
        when(mockRobotsHandler.isAllowed(any())).thenReturn(true);

        sequentialCrawler.crawl(config);

        verify(mockFetcher).fetch(rootUrl);
        verify(mockFetcher, never()).fetch(childUrl);
    }

    @Test
    void testCrawlerConfiguration() {
        assertEquals(1, sequentialCrawler.getThreadCount());
        assertEquals(4, concurrentCrawler.getThreadCount());
        assertEquals(30, sequentialCrawler.getTimeoutSeconds());
        assertEquals(60, concurrentCrawler.getTimeoutSeconds());
    }

    private void setupSuccessfulCrawl(URI url, int depth, PageResult mockResult) throws Exception {
        when(mockLinkFilter.isAllowedDomain(url, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.markVisited(url)).thenReturn(true);
        when(mockRobotsHandler.isAllowed(url)).thenReturn(true);
        when(mockFetcher.fetch(url)).thenReturn(mockDocument);
        when(mockParser.parse(eq(url), eq(depth), any(Document.class))).thenReturn(mockResult);
    }

    private PageResult createMockPageResult(URI url, int depth, List<URI> links) {
        PageResult mockResult = mock(PageResult.class);
        when(mockResult.url()).thenReturn(url);
        when(mockResult.depth()).thenReturn(depth);
        when(mockResult.broken()).thenReturn(false);
        when(mockResult.sections()).thenReturn(List.of());
        when(mockResult.children()).thenReturn(Set.of());
        when(mockResult.getAllLinks()).thenReturn(links);
        when(mockResult.withChildren(any())).thenReturn(mockResult);
        return mockResult;
    }
}