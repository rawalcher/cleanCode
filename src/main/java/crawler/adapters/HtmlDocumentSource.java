package crawler.adapters;

import java.net.URI;

/**
 * Abstraction for HTML document retrieval and parsing.
 * This interface provides a clean boundary to third-party HTML parsing libraries.
 */
public interface HtmlDocumentSource {

    /**
     * Retrieves and parses an HTML document from the given URI.
     *
     * @param uri the URI to fetch
     * @return the parsed HTML document
     * @throws DocumentRetrievalException if the document cannot be retrieved or parsed
     */
    HtmlDocument fetchDocument(URI uri) throws DocumentRetrievalException;

    /**
     * Exception thrown when document retrieval fails.
     */
    class DocumentRetrievalException extends Exception {
        private final ErrorType errorType;
        private final int statusCode;

        public DocumentRetrievalException(String message, Throwable cause, ErrorType errorType) {
            super(message, cause);
            this.errorType = errorType;
            this.statusCode = -1;
        }

        public DocumentRetrievalException(String message, Throwable cause, ErrorType errorType, int statusCode) {
            super(message, cause);
            this.errorType = errorType;
            this.statusCode = statusCode;
        }

        public ErrorType getErrorType() { return errorType; }
        public int getStatusCode() { return statusCode; }

        public enum ErrorType {
            NETWORK_ERROR,
            HTTP_ERROR,
            TIMEOUT,
            INVALID_URL,
            PARSING_ERROR
        }
    }
}