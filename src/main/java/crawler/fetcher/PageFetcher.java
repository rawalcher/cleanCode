package crawler.fetcher;

import crawler.constants.CrawlerConstants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.net.URI;

/**
 * Fetches HTML with jsoup, handles connections, timeouts etc.
 */
public class PageFetcher {

    /**
     * Fetches a page and returns its Jsoup Document.
     *
     * @param url the URL to fetch
     * @return Document if successful
     * @throws FetchException if any problem occurs (network, invalid page, etc.)
     */
    public Document fetch(URI url) throws FetchException {
        try {
            return Jsoup.connect(url.toString())
                    .timeout(CrawlerConstants.CONNECTION_TIMEOUT_MS)
                    .userAgent("SimpleWebCrawlerBot/1.0")
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .get();
        } catch (HttpStatusException e) {
            throw new FetchException("HTTP error fetching URL: " + url + ", Status: " + e.getStatusCode(), e);
        } catch (IOException e) {
            throw new FetchException("I/O error fetching URL: " + url, e);
        } catch (IllegalArgumentException e) {
            throw new FetchException("Invalid URL: " + url, e);
        }
    }

    /**
     * Custom exception for fetch errors.
     */
    public static class FetchException extends Exception {
        public FetchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
