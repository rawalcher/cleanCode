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

import static org.junit.jupiter.api.Assertions.*;
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

    private WebCrawler crawler;
    private URI rootUrl;
    private CrawlerConfig config;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        crawler = new WebCrawler(
                mockFetcher,
                mockParser,
                mockRobotsCache,
                mockLinkFilter,
                mockReporter
        );

        rootUrl = new URI("https://example.com");
        config = new CrawlerConfig(rootUrl, 2, "example.com");

        when(mockRobotsCache.getHandler(any(URI.class))).thenReturn(mockRobotsHandler);
    }

    private void configureMockForSuccessfulCrawl(URI url, int depth, PageResult mockResult) {
        when(mockLinkFilter.isAllowedDomain(url, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.isVisited(url)).thenReturn(false);
        when(mockRobotsHandler.isAllowed(url)).thenReturn(true);

        try {
            when(mockFetcher.fetch(url)).thenReturn(mockDocument);
        } catch (PageFetcher.FetchException e) {
            fail("Mock setup failed: " + e.getMessage());
        }

        when(mockParser.parse(eq(url), eq(depth), any(Document.class))).thenReturn(mockResult);
    }

    private void configureMockForBlockedByRobots(URI url) {
        when(mockLinkFilter.isAllowedDomain(url, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.isVisited(url)).thenReturn(false);
        when(mockRobotsHandler.isAllowed(url)).thenReturn(false);
    }

    private void configureMockForDisallowedDomain(URI url) {
        when(mockLinkFilter.isAllowedDomain(url, config.getAllowedDomains())).thenReturn(false);
    }

    private void configureMockForVisitedUrl(URI url) {
        when(mockLinkFilter.isAllowedDomain(url, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.isVisited(url)).thenReturn(true);
    }

    private void configureMockForFetchFailure(URI url) throws PageFetcher.FetchException {
        when(mockLinkFilter.isAllowedDomain(url, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.isVisited(url)).thenReturn(false);
        when(mockRobotsHandler.isAllowed(url)).thenReturn(true);
        when(mockFetcher.fetch(url)).thenThrow(new PageFetcher.FetchException("Test error", new Exception()));
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

    private PageResult createBrokenPageResult(URI url, int depth) {
        return new PageResult(url, depth, true, List.of(), Set.of());
    }

    @Test
    void testCrawlWithNullConfig() {
        crawler.crawl(null);

        verifyNoInteractions(mockFetcher, mockParser, mockReporter);
    }

    @Test
    void testCrawlWithValidConfig() {
        PageResult mockResult = createMockPageResult(rootUrl, 0, List.of());
        configureMockForSuccessfulCrawl(rootUrl, 0, mockResult);

        crawler.crawl(config);

        verify(mockReporter).writeReport(any(PageResult.class), eq(config));
    }

    @Test
    void testCrawlPageExceedsMaxDepth() {
        PageResult expectedResult = createBrokenPageResult(rootUrl, 3);
        int exceedingDepth = config.getMaxDepth() + 1;

        PageResult result = crawler.crawlPage(rootUrl, exceedingDepth, config);

        assertEquals(expectedResult, result);
        verifyNoInteractions(mockFetcher);
    }

    @Test
    void testCrawlPageAlreadyVisited() {
        configureMockForVisitedUrl(rootUrl);
        PageResult expectedResult = createBrokenPageResult(rootUrl, 0);

        PageResult result = crawler.crawlPage(rootUrl, 0, config);

        assertEquals(expectedResult, result);
        verifyNoInteractions(mockFetcher);
    }

    @Test
    void testCrawlPageDisallowedDomain() {
        configureMockForDisallowedDomain(rootUrl);
        PageResult expectedResult = createBrokenPageResult(rootUrl, 0);

        PageResult result = crawler.crawlPage(rootUrl, 0, config);

        assertEquals(expectedResult, result);
        verifyNoInteractions(mockFetcher);
    }

    @Test
    void testCrawlPageBlockedByRobotsTxt() {
        configureMockForBlockedByRobots(rootUrl);

        PageResult result = crawler.crawlPage(rootUrl, 0, config);

        assertNotNull(result);
        assertTrue(result.broken());
        assertEquals(rootUrl, result.url());
        assertEquals(0, result.depth());
        verifyNoInteractions(mockFetcher);
    }

    @Test
    void testCrawlPageFetchFailure() throws Exception {
        configureMockForFetchFailure(rootUrl);

        PageResult result = crawler.crawlPage(rootUrl, 0, config);

        assertNotNull(result);
        assertTrue(result.broken());
        assertEquals(rootUrl, result.url());
        assertEquals(0, result.depth());
        verifyNoInteractions(mockParser);
    }

    @Test
    void testCrawlPageSuccessfulFetch() throws Exception {
        PageResult mockResult = createMockPageResult(rootUrl, 0, List.of());
        configureMockForSuccessfulCrawl(rootUrl, 0, mockResult);

        PageResult result = crawler.crawlPage(rootUrl, 0, config);

        assertNotNull(result);
        assertFalse(result.broken());
        verify(mockFetcher).fetch(rootUrl);
        verify(mockParser).parse(rootUrl, 0, mockDocument);
    }

    @Test
    void testCrawlPageWithChildLinks() throws Exception {
        URI childUrl = new URI("https://example.com/child");

        PageResult rootResult = createMockPageResult(rootUrl, 0, List.of(childUrl));
        PageResult childResult = createMockPageResult(childUrl, 1, List.of());

        configureMockForSuccessfulCrawl(rootUrl, 0, rootResult);

        configureMockForSuccessfulCrawl(childUrl, 1, childResult);

        crawler.crawlPage(rootUrl, 0, config);

        verify(mockFetcher).fetch(childUrl);
        verify(mockParser).parse(childUrl, 1, mockDocument);
    }

    @Test
    void testCrawlPageWithSkippedChildLinks() throws Exception {
        URI visitedLink = new URI("https://example.com/visited");
        URI disallowedLink = new URI("https://other.com/disallowed");

        PageResult rootResult = createMockPageResult(rootUrl, 0, List.of(visitedLink, disallowedLink));

        configureMockForSuccessfulCrawl(rootUrl, 0, rootResult);

        configureMockForVisitedUrl(visitedLink);
        configureMockForDisallowedDomain(disallowedLink);

        crawler.crawlPage(rootUrl, 0, config);

        verify(mockFetcher, never()).fetch(visitedLink);
        verify(mockFetcher, never()).fetch(disallowedLink);
    }
}