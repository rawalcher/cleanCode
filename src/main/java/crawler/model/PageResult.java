package crawler.model;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the result of crawling a single web page, organized by sections.
 * This record encapsulates all relevant information extracted from a webpage,
 * including its URL, sections (headings with their associated links), and child crawl results.
 *
 * @param url      The URL of the page.
 * @param depth    The crawl depth relative to the root page.
 * @param broken   Indicates whether the page could not be fetched (e.g. 404).
 * @param sections The list of page sections, where each section has a heading and associated links.
 * @param children The set of child PageResults representing successfully crawled linked pages.
 */
public record PageResult(
        URI url,
        int depth,
        boolean broken,
        List<Section> sections,
        Set<PageResult> children
) {

    public record Section(Heading heading, LinkedHashSet<URI> links) {}
    public record Heading(int level, String text) {}

    public static PageResult brokenLink(URI url, int depth) {
        return new PageResult(url, depth, true, List.of(), Set.of());
    }
    public PageResult withChildren(Set<PageResult> newChildren) {
        return new PageResult(url, depth, broken, sections, newChildren);
    }

    public List<URI> getAllLinks() {
        return sections.stream()
                .flatMap(section -> section.links().stream())
                .toList();
    }
}