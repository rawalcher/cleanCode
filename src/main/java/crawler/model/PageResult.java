package crawler.model;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Represents the result of crawling a single web page.
 * This record encapsulates all relevant information extracted from a webpage,
 * including its URL, heading structure (with indentation), outgoing links, and child crawl results.
 *
 * @param url      The URL of the page (as a string, not normalized).
 * @param depth    The crawl depth relative to the root page.
 * @param broken   Indicates whether the page could not be fetched (e.g. 404).
 * @param headings The list of HTML headings (h1–h6), in document order.
 * @param links    The list of extracted hyperlinks (may include relative URIs).
 * @param children The set of child PageResults representing successfully crawled linked pages.
 */
public record PageResult(
        String url,
        int depth,
        boolean broken,
        List<Heading> headings,
        List<URI> links,
        Set<PageResult> children
) {
    public static PageResult brokenLink(String url, int depth) {
        return new PageResult(url, depth, true, List.of(), List.of(), Set.of());
    }
}
