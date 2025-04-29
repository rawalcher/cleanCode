package crawler.parser;

import crawler.model.PageResult;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static crawler.constants.CrawlerConstants.MAX_HEADING_LEVEL;

/**
 * Parses PageFetcher output into PageResult.
 */
public class HtmlParser {
    /**
     * Parses a jsoup Document into a PageResult.
     *
     * @param url     The URL of the page
     * @param depth   The current crawl depth
     * @param document The jsoup Document to parse
     * @return Parsed PageResult
     */
    public PageResult parse(URI url, int depth, Document document) {
        List<PageResult.Heading> headings = extractHeadings(document);
        List<URI> links = extractLinks(document, url);

        return new PageResult(url, depth, false, headings, links, Set.of());
    }

    private List<PageResult.Heading> extractHeadings(Document document) {
        List<PageResult.Heading> headings = new ArrayList<>();
        if (document == null) return headings;

        for (int level = 1; level <= MAX_HEADING_LEVEL; level++) {
            Elements elements = document.select("h" + level);
            for (Element element : elements) {
                String text = element.text();
                if (!text.isBlank()) {
                    headings.add(new PageResult.Heading(level, text.trim()));
                }
            }
        }
        return headings;
    }

    private List<URI> extractLinks(Document document, URI baseUrl) {
        List<URI> links = new ArrayList<>();
        if (document == null || baseUrl == null) return links;

        Elements elements = document.select("a[href]");
        for (Element element : elements) {
            String href = element.attr("href");
            if (!href.isBlank()) {
                try {
                    links.add(baseUrl.resolve(href.trim()));
                } catch (IllegalArgumentException e) {
                    // Bad href syntax → still store as-is if possible since assignment asks for it!
                    try {
                        links.add(new URI(href.trim()));
                    } catch (URISyntaxException ignored) {
                        // Ignore completely invalid hrefs
                    }
                }
            }
        }
        return links;
    }
}
