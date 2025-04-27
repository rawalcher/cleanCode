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

/**
 * Parses PageFetcher output into PageResult.
 */
public class HtmlParser {
    // clean code means no magic numbers!
    private static final int MAX_HEADING_LEVEL = 6;

    /**
     * Parses a jsoup Document into a PageResult.
     *
     * @param url  The URL of the page
     * @param depth The current crawl depth
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
        for (int level = 1; level <= MAX_HEADING_LEVEL; level++) {
            Elements elements = document.select("h" + level);
            for (Element element : elements) {
                String text = element.text().trim();
                if (!text.isEmpty()) {
                    headings.add(new PageResult.Heading(level, text));
                }
            }
        }
        return headings;
    }

    private List<URI> extractLinks(Document document, URI baseUrl) {
        List<URI> links = new ArrayList<>();
        Elements elements = document.select("a[href]");
        for (Element element : elements) {
            String href = element.attr("href").trim();
            if (!href.isEmpty()) {
                try {
                    URI resolvedLink = baseUrl.resolve(href);
                    links.add(resolvedLink);
                } catch (IllegalArgumentException e) {
                    // Bad href syntax → still store as-is if possible since assignment asks for it!
                    try {
                        links.add(new URI(href));
                    } catch (URISyntaxException ex) {
                        // Completely invalid URI, skip
                    }
                }
            }
        }
        return links;
    }

}
