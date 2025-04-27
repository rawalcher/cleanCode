package crawler.app;

import crawler.model.CrawlerConfig;
import crawler.model.PageResult;
import crawler.fetcher.PageFetcher;
import crawler.fetcher.RobotsTxtCache;
import crawler.fetcher.RobotsTxtHandler;
import crawler.parser.HtmlParser;
import crawler.reporter.MarkdownReporter;
import crawler.util.LinkFilter;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

class WebCrawlerTest {

    private WebCrawler crawler;

    @BeforeEach
    void setUp() {
        crawler = new WebCrawler();
    }

    @Test
    void testCrawlSinglePageSuccess() throws Exception {
        URI rootUrl = new URI("https://example.com");
        CrawlerConfig config = new CrawlerConfig(rootUrl, 1, "example.com");

        try (
                MockedConstruction<PageFetcher> mockedFetcher = mockConstruction(PageFetcher.class, (mock, context) -> {
                    when(mock.fetch(any(URI.class))).thenReturn(mock(Document.class));
                });
                MockedConstruction<HtmlParser> mockedParser = mockConstruction(HtmlParser.class, (mock, context) -> {
                    when(mock.parse(any(URI.class), anyInt(), any(Document.class)))
                            .thenReturn(new PageResult(rootUrl, 0, false, List.of(), List.of(), Set.of()));
                });
                MockedConstruction<RobotsTxtCache> mockedRobotsCache = mockConstruction(RobotsTxtCache.class, (mock, context) -> {
                    RobotsTxtHandler robotsMock = mock(RobotsTxtHandler.class);
                    when(robotsMock.isAllowed(any(URI.class))).thenReturn(true);
                    when(mock.getHandler(any(URI.class))).thenReturn(robotsMock);
                });
                MockedConstruction<LinkFilter> mockedLinkFilter = mockConstruction(LinkFilter.class, (mock, context) -> {
                    when(mock.isAllowedDomain(any(URI.class), any())).thenReturn(true);
                    when(mock.isVisited(any(URI.class))).thenReturn(false);
                });
                MockedConstruction<MarkdownReporter> mockedReporter = mockConstruction(MarkdownReporter.class)
        ) {
            crawler.crawl(config);

            MarkdownReporter reporterInstance = mockedReporter.constructed().getFirst();
            verify(reporterInstance, times(1)).writeReport(any(PageResult.class), eq(config));
        }
    }

    @Test
    void testCrawlBlockedByRobots() throws Exception {
        URI rootUrl = new URI("https://example.com");
        CrawlerConfig config = new CrawlerConfig(rootUrl, 1, "example.com");

        try (
                MockedConstruction<PageFetcher> mockedFetcher = mockConstruction(PageFetcher.class);
                MockedConstruction<HtmlParser> mockedParser = mockConstruction(HtmlParser.class);
                MockedConstruction<RobotsTxtCache> mockedRobotsCache = mockConstruction(RobotsTxtCache.class, (mock, context) -> {
                    RobotsTxtHandler robotsMock = mock(RobotsTxtHandler.class);
                    when(robotsMock.isAllowed(any(URI.class))).thenReturn(false);
                    when(mock.getHandler(any(URI.class))).thenReturn(robotsMock);
                });
                MockedConstruction<LinkFilter> mockedLinkFilter = mockConstruction(LinkFilter.class, (mock, context) -> {
                    when(mock.isAllowedDomain(any(URI.class), any())).thenReturn(true);
                    when(mock.isVisited(any(URI.class))).thenReturn(false);
                });
                MockedConstruction<MarkdownReporter> mockedReporter = mockConstruction(MarkdownReporter.class)
        ) {
            crawler.crawl(config);

            MarkdownReporter reporterInstance = mockedReporter.constructed().getFirst();
            verify(reporterInstance, times(1)).writeReport(any(PageResult.class), eq(config));
        }
    }

    @Test
    void testCrawlMaxDepthReached() throws Exception {
        URI rootUrl = new URI("https://example.com");
        CrawlerConfig config = new CrawlerConfig(rootUrl, 0, "example.com"); // Max depth 0

        try (
                MockedConstruction<PageFetcher> mockedFetcher = mockConstruction(PageFetcher.class);
                MockedConstruction<HtmlParser> mockedParser = mockConstruction(HtmlParser.class);
                MockedConstruction<RobotsTxtCache> mockedRobotsCache = mockConstruction(RobotsTxtCache.class);
                MockedConstruction<LinkFilter> mockedLinkFilter = mockConstruction(LinkFilter.class, (mock, context) -> {
                    when(mock.isAllowedDomain(any(URI.class), any())).thenReturn(true);
                    when(mock.isVisited(any(URI.class))).thenReturn(false);
                });
                MockedConstruction<MarkdownReporter> mockedReporter = mockConstruction(MarkdownReporter.class)
        ) {
            crawler.crawl(config);

            MarkdownReporter reporterInstance = mockedReporter.constructed().getFirst();
            verify(reporterInstance, times(1)).writeReport(any(PageResult.class), eq(config));
        }
    }
}
