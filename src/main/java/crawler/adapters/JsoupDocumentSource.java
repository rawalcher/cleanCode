package crawler.adapters;

import crawler.constants.CrawlerConstants;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Jsoup-based implementation of HtmlDocumentSource.
 * This adapter encapsulates all jsoup-specific logic and provides a clean interface.
 */
public class JsoupDocumentSource implements HtmlDocumentSource {
    private static final Logger logger = LoggerFactory.getLogger(JsoupDocumentSource.class);

    private final int timeoutMs;
    private final String userAgent;

    public JsoupDocumentSource() {
        this(CrawlerConstants.CONNECTION_TIMEOUT_MS, CrawlerConstants.USER_AGENT);
    }

    public JsoupDocumentSource(int timeoutMs, String userAgent) {
        this.timeoutMs = timeoutMs;
        this.userAgent = userAgent;
    }

    @Override
    public HtmlDocument fetchDocument(URI uri) throws DocumentRetrievalException {
        try {
            logger.debug("Fetching document from: {}", uri);

            Document document = Jsoup.connect(uri.toString())
                    .timeout(timeoutMs)
                    .userAgent(userAgent)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .get();

            return new JsoupHtmlDocument(document);

        } catch (HttpStatusException e) {
            throw new DocumentRetrievalException(
                    "HTTP error fetching " + uri + ": " + e.getStatusCode(),
                    e,
                    DocumentRetrievalException.ErrorType.HTTP_ERROR,
                    e.getStatusCode()
            );
        } catch (SocketTimeoutException e) {
            throw new DocumentRetrievalException(
                    "Timeout fetching " + uri,
                    e,
                    DocumentRetrievalException.ErrorType.TIMEOUT
            );
        } catch (IOException e) {
            throw new DocumentRetrievalException(
                    "Network error fetching " + uri,
                    e,
                    DocumentRetrievalException.ErrorType.NETWORK_ERROR
            );
        } catch (IllegalArgumentException e) {
            throw new DocumentRetrievalException(
                    "Invalid URL: " + uri,
                    e,
                    DocumentRetrievalException.ErrorType.INVALID_URL
            );
        }
    }

    /**
     * Jsoup-specific implementation of HtmlDocument.
     */
    private static class JsoupHtmlDocument implements HtmlDocument {
        private final Document document;

        JsoupHtmlDocument(Document document) {
            this.document = document;
        }

        @Override
        public List<DocumentHeading> getHeadings() {
            List<DocumentHeading> headings = new ArrayList<>();

            for (int level = 1; level <= CrawlerConstants.MAX_HEADING_LEVEL; level++) {
                Elements elements = document.select("h" + level);
                for (Element element : elements) {
                    String text = element.text().trim();
                    if (!text.isEmpty()) {
                        headings.add(new DocumentHeading(level, text));
                    }
                }
            }

            return headings;
        }

        @Override
        public List<URI> getLinks(URI baseUri) {
            List<URI> links = new ArrayList<>();
            Elements linkElements = document.select("a[href]");

            for (Element link : linkElements) {
                String href = link.attr("href").trim();
                if (href.isEmpty()) continue;

                try {
                    URI resolvedUri = baseUri.resolve(href);
                    links.add(resolvedUri);
                } catch (IllegalArgumentException e) {
                    // Try parsing as absolute URI
                    try {
                        links.add(new URI(href));
                    } catch (URISyntaxException ex) {
                        logger.debug("Skipping malformed link: {}", href);
                        // Skip malformed links
                    }
                }
            }

            return links;
        }

        @Override
        public String getTitle() {
            return document.title();
        }
    }
}