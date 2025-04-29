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
import static org.mockito.ArgumentMatchers.*;
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

    @Test
    void testCrawlWithNullConfig() {

        crawler.crawl(null);

        verifyNoInteractions(mockFetcher, mockParser, mockReporter);
    }

    @Test
    void testCrawlWithValidConfig() throws Exception {

        PageResult mockResult = mock(PageResult.class);
        when(mockResult.url()).thenReturn(rootUrl);
        when(mockResult.depth()).thenReturn(0);
        when(mockResult.broken()).thenReturn(false);
        when(mockResult.sections()).thenReturn(List.of());
        when(mockResult.children()).thenReturn(Set.of());
        when(mockResult.getAllLinks()).thenReturn(List.of());
        when(mockResult.withChildren(any())).thenReturn(mockResult);

        when(mockLinkFilter.isAllowedDomain(any(URI.class), anyList())).thenReturn(true);
        when(mockLinkFilter.isVisited(rootUrl)).thenReturn(false);
        when(mockRobotsHandler.isAllowed(any(URI.class))).thenReturn(true);

        when(mockFetcher.fetch(rootUrl)).thenReturn(mockDocument);
        when(mockParser.parse(eq(rootUrl), eq(0), any(Document.class))).thenReturn(mockResult);

        crawler.crawl(config);

        verify(mockReporter).writeReport(any(PageResult.class), eq(config));
    }

    @Test
    void testCrawlPageExceedsMaxDepth() {
        PageResult expectedResult = new PageResult(URI.create("https://example.com"), 3, true, List.of(), Set.of());
        int exceedingDepth = config.getMaxDepth() + 1;

        PageResult result = crawler.crawlPage(rootUrl, exceedingDepth, config);

        assertEquals(expectedResult, result);
        verifyNoInteractions(mockFetcher);
    }

    @Test
    void testCrawlPageAlreadyVisited() {
        when(mockLinkFilter.isAllowedDomain(rootUrl, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.isVisited(rootUrl)).thenReturn(true);

        PageResult expectedResult = new PageResult(URI.create("https://example.com"), 0, true, List.of(), Set.of());

        PageResult result = crawler.crawlPage(rootUrl, 0, config);

        assertEquals(expectedResult, result);
        verifyNoInteractions(mockFetcher);
    }

    @Test
    void testCrawlPageDisallowedDomain() {
        when(mockLinkFilter.isAllowedDomain(rootUrl, config.getAllowedDomains())).thenReturn(false);
        PageResult expectedResult = new PageResult(URI.create("https://example.com"), 0, true, List.of(), Set.of());

        PageResult result = crawler.crawlPage(rootUrl, 0, config);

        assertEquals(expectedResult, result);
        verifyNoInteractions(mockFetcher);
    }

    @Test
    void testCrawlPageBlockedByRobotsTxt() {
        when(mockLinkFilter.isAllowedDomain(rootUrl, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.isVisited(rootUrl)).thenReturn(false);
        when(mockRobotsHandler.isAllowed(rootUrl)).thenReturn(false);

        PageResult result = crawler.crawlPage(rootUrl, 0, config);

        assertNotNull(result);
        assertTrue(result.broken());
        assertEquals(rootUrl, result.url());
        assertEquals(0, result.depth());
        verifyNoInteractions(mockFetcher);
    }

    @Test
    void testCrawlPageFetchFailure() throws Exception {
        when(mockLinkFilter.isAllowedDomain(rootUrl, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.isVisited(rootUrl)).thenReturn(false);
        when(mockRobotsHandler.isAllowed(rootUrl)).thenReturn(true);

        when(mockFetcher.fetch(rootUrl)).thenThrow(new PageFetcher.FetchException("Test error", new Exception()));

        PageResult result = crawler.crawlPage(rootUrl, 0, config);

        assertNotNull(result);
        assertTrue(result.broken());
        assertEquals(rootUrl, result.url());
        assertEquals(0, result.depth());
        verifyNoInteractions(mockParser);
    }

    @Test
    void testCrawlPageSuccessfulFetch() throws Exception {
        PageResult mockResult = mock(PageResult.class);
        when(mockResult.url()).thenReturn(rootUrl);
        when(mockResult.depth()).thenReturn(0);
        when(mockResult.broken()).thenReturn(false);
        when(mockResult.sections()).thenReturn(List.of());
        when(mockResult.children()).thenReturn(Set.of());
        when(mockResult.getAllLinks()).thenReturn(List.of());
        when(mockResult.withChildren(any())).thenReturn(mockResult);

        when(mockLinkFilter.isAllowedDomain(rootUrl, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.isVisited(rootUrl)).thenReturn(false);
        when(mockRobotsHandler.isAllowed(rootUrl)).thenReturn(true);
        when(mockFetcher.fetch(rootUrl)).thenReturn(mockDocument);
        when(mockParser.parse(eq(rootUrl), eq(0), any(Document.class))).thenReturn(mockResult);

        PageResult result = crawler.crawlPage(rootUrl, 0, config);

        assertNotNull(result);
        assertFalse(result.broken());
        verify(mockFetcher).fetch(rootUrl);
        verify(mockParser).parse(rootUrl, 0, mockDocument);
    }

    @Test
    void testCrawlPageWithChildLinks() throws Exception {
        URI childUrl = new URI("https://example.com/child");

        PageResult rootResult = mock(PageResult.class);
        when(rootResult.url()).thenReturn(rootUrl);
        when(rootResult.depth()).thenReturn(0);
        when(rootResult.broken()).thenReturn(false);
        when(rootResult.sections()).thenReturn(List.of());
        when(rootResult.children()).thenReturn(Set.of());
        when(rootResult.getAllLinks()).thenReturn(List.of(childUrl));
        when(rootResult.withChildren(any())).thenReturn(rootResult);

        PageResult childResult = mock(PageResult.class);
        when(childResult.url()).thenReturn(childUrl);
        when(childResult.depth()).thenReturn(1);
        when(childResult.broken()).thenReturn(false);
        when(childResult.sections()).thenReturn(List.of());
        when(childResult.children()).thenReturn(Set.of());
        when(childResult.getAllLinks()).thenReturn(List.of());

        when(mockLinkFilter.isAllowedDomain(rootUrl, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.isVisited(rootUrl)).thenReturn(false);
        when(mockRobotsHandler.isAllowed(rootUrl)).thenReturn(true);
        when(mockFetcher.fetch(rootUrl)).thenReturn(mockDocument);
        when(mockParser.parse(eq(rootUrl), eq(0), any(Document.class))).thenReturn(rootResult);

        when(mockLinkFilter.isAllowedDomain(childUrl, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.isVisited(childUrl)).thenReturn(false);
        when(mockRobotsHandler.isAllowed(childUrl)).thenReturn(true);
        when(mockFetcher.fetch(childUrl)).thenReturn(mockDocument);
        when(mockParser.parse(eq(childUrl), eq(1), any(Document.class))).thenReturn(childResult);

        crawler.crawlPage(rootUrl, 0, config);

        verify(mockFetcher).fetch(childUrl);
        verify(mockParser).parse(childUrl, 1, mockDocument);
    }

    @Test
    void testCrawlPageWithSkippedChildLinks() throws Exception {
        URI visitedLink = new URI("https://example.com/visited");
        URI disallowedLink = new URI("https://other.com/disallowed");

        PageResult rootResult = mock(PageResult.class);
        when(rootResult.url()).thenReturn(rootUrl);
        when(rootResult.depth()).thenReturn(0);
        when(rootResult.broken()).thenReturn(false);
        when(rootResult.sections()).thenReturn(List.of());
        when(rootResult.children()).thenReturn(Set.of());
        when(rootResult.getAllLinks()).thenReturn(List.of(visitedLink, disallowedLink));
        when(rootResult.withChildren(any())).thenReturn(rootResult);

        when(mockLinkFilter.isAllowedDomain(rootUrl, config.getAllowedDomains())).thenReturn(true);
        when(mockLinkFilter.isVisited(rootUrl)).thenReturn(false);
        when(mockRobotsHandler.isAllowed(rootUrl)).thenReturn(true);
        when(mockFetcher.fetch(rootUrl)).thenReturn(mockDocument);
        when(mockParser.parse(eq(rootUrl), eq(0), any(Document.class))).thenReturn(rootResult);

        when(mockLinkFilter.isVisited(visitedLink)).thenReturn(true);
        when(mockLinkFilter.isAllowedDomain(disallowedLink, config.getAllowedDomains())).thenReturn(false);

        crawler.crawlPage(rootUrl, 0, config);

        verify(mockFetcher, never()).fetch(visitedLink);
        verify(mockFetcher, never()).fetch(disallowedLink);
    }
}