package crawler.adapters;

import java.net.URI;
import java.util.List;

/**
 * Abstraction for an HTML document that can be parsed for headings and links.
 * This interface shields the application from specific HTML parsing library details.
 */
public interface HtmlDocument {

    /**
     * Extracts all headings from the document.
     *
     * @return list of headings in document order
     */
    List<DocumentHeading> getHeadings();

    /**
     * Extracts all links from the document.
     *
     * @param baseUri the base URI for resolving relative links
     * @return list of links in document order
     */
    List<URI> getLinks(URI baseUri);

    /**
     * Gets the document title.
     *
     * @return the document title, or empty string if none
     */
    String getTitle();

    /**
     * Represents a heading found in the document.
     */
    record DocumentHeading(int level, String text) {
        public DocumentHeading {
            if (level < 1 || level > 6) {
                throw new IllegalArgumentException("Heading level must be between 1 and 6");
            }
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("Heading text cannot be null or empty");
            }
        }
    }
}
