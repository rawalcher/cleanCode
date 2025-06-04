package crawler.parser;

import crawler.model.PageResult;
import crawler.model.PageResult.Heading;
import crawler.model.PageResult.Section;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Predicate;

import static crawler.constants.CrawlerConstants.MAX_HEADING_LEVEL;

/**
 * HTML parser that converts a jsoup Document
 * into a PageResult. It preserves document order and guarantees deterministic link ordering.
 */
public class HtmlParser {
    private static final Heading ROOT_HEADING = new Heading(0, "Page Root");

    /**
     * Parses the given document into a PageResult.
     *
     * @param url      page URL
     * @param depth    crawl depth
     * @param document jsoup document
     * @return corresponding {@code PageResult}
     */
    public PageResult parse(URI url, int depth, Document document) {
        if (document == null) {
            return new PageResult(url, depth, false, List.of(), java.util.Set.of());
        }
        List<Section> sections = extractSections(document, url);
        return new PageResult(url, depth, false, sections, java.util.Set.of());
    }


    private List<Section> extractSections(Document doc, URI baseUrl) {
        Map<Heading, LinkedHashSet<URI>> buckets = new LinkedHashMap<>();
        Heading current = ROOT_HEADING;
        buckets.put(current, new LinkedHashSet<>());

        for (Element el : doc.getAllElements()) {
            Optional<Heading> maybeHeading = extractHeading(el);
            if (maybeHeading.isPresent()) {
                current = maybeHeading.get();
                buckets.put(current, new LinkedHashSet<>());
                continue;
            }

            URI link = extractLink(el, baseUrl);
            if (link != null) {
                buckets.get(current).add(link);
            }
        }

        return buckets.entrySet().stream()
                .filter(IS_VALID_SECTION)
                .map(e -> new Section(e.getKey(), e.getValue()))
                .toList();
    }

    private static final Predicate<Map.Entry<Heading, LinkedHashSet<URI>>> IS_VALID_SECTION =
            entry -> entry.getKey().level() != 0 || !entry.getValue().isEmpty();

    private static Optional<Heading> extractHeading(Element el) {
        String tag = el.tagName();
        if (tag.length() == 2 && tag.charAt(0) == 'h') {
            int level = tag.charAt(1) - '0';
            if (level >= 1 && level <= MAX_HEADING_LEVEL) {
                String text = el.text().trim();
                if (!text.isEmpty()) {
                    return Optional.of(new Heading(level, text));
                }
            }
        }
        return Optional.empty();
    }

    private static URI extractLink(Element el, URI base) {
        if (!"a".equals(el.tagName())) {
            return null;
        }
        String href = el.attr("href");
        if (href.isBlank()) {
            return null;
        }
        String trimmed = href.trim();
        try {
            return base.resolve(trimmed);
        } catch (IllegalArgumentException e) { // bad relative link
            try {
                return new URI(trimmed);
            } catch (URISyntaxException ex) {
                return null; // hopelessly malformed
            }
        }
    }
}
