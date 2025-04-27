package crawler.parser;

import crawler.model.PageResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HtmlParserTest {

    private final HtmlParser parser = new HtmlParser();

    @Test
    void testParseNormalHeadingsAndLinks() throws Exception {
        Document doc = createDocumentWithHeadingsAndLinks();
        URI baseUrl = new URI("https://example.com");

        PageResult result = parser.parse(baseUrl, 0, doc);

        List<PageResult.Heading> headings = result.headings();
        List<URI> links = result.links();

        assertEquals(2, headings.size());
        assertEquals(2, links.size());
    }

    @Test
    void testParseEmptyDocument() throws Exception {
        Document emptyDoc = Jsoup.parse("");
        URI baseUrl = new URI("https://example.com");

        PageResult result = parser.parse(baseUrl, 0, emptyDoc);

        assertTrue(result.headings().isEmpty());
        assertTrue(result.links().isEmpty());
    }

    @Test
    void testParseCompletelyBrokenLinks() throws Exception {
        Document doc = createDocumentWithBrokenLinks();
        URI baseUrl = new URI("https://example.com");

        PageResult result = parser.parse(baseUrl, 0, doc);

        assertTrue(result.links().isEmpty());
    }

    @Test
    void testParseRelativeAndFragmentLinks() throws Exception {
        Document doc = createDocumentWithRelativeAndFragmentLinks();
        URI baseUrl = new URI("https://example.com");

        PageResult result = parser.parse(baseUrl, 0, doc);
        List<URI> links = result.links();

        assertEquals(2, links.size());
        assertTrue(links.stream().anyMatch(uri -> uri.toString().equals("https://example.com/contact")));
        assertTrue(links.stream().anyMatch(uri -> uri.toString().equals("https://example.com#top")));
    }

    @Test
    void testParseIgnoreHeadingsAboveH6() throws Exception {
        Document doc = createDocumentWithInvalidHeading();
        URI baseUrl = new URI("https://example.com");

        PageResult result = parser.parse(baseUrl, 0, doc);
        List<PageResult.Heading> headings = result.headings();

        assertEquals(1, headings.size());
        assertEquals("Valid Heading", headings.get(0).text());
    }

    @Test
    void testParseIgnoreEmptyHeadings() throws Exception {
        Document doc = createDocumentWithEmptyAndValidHeading();
        URI baseUrl = new URI("https://example.com");

        PageResult result = parser.parse(baseUrl, 0, doc);
        List<PageResult.Heading> headings = result.headings();

        assertEquals(1, headings.size());
        assertEquals("Valid Heading", headings.get(0).text());
    }

    private Document createDocumentWithHeadingsAndLinks() {
        Document doc = Jsoup.parse("");
        Element body = doc.body();
        body.appendElement("h1").text("Main Heading");
        body.appendElement("h2").text("Sub Heading");
        body.appendElement("a").attr("href", "https://example.com/page1").text("Link 1");
        body.appendElement("a").attr("href", "/relative/path").text("Relative Link");
        return doc;
    }

    private Document createDocumentWithBrokenLinks() {
        Document doc = Jsoup.parse("");
        Element body = doc.body();
        body.appendElement("a").attr("href", "ht!tp://invalid-url").text("Broken Link");
        body.appendElement("a").text("No href link");
        return doc;
    }

    private Document createDocumentWithRelativeAndFragmentLinks() {
        Document doc = Jsoup.parse("");
        Element body = doc.body();
        body.appendElement("a").attr("href", "/contact").text("Contact");
        body.appendElement("a").attr("href", "#top").text("Top");
        return doc;
    }

    private Document createDocumentWithInvalidHeading() {
        Document doc = Jsoup.parse("");
        Element body = doc.body();
        body.appendElement("h7").text("Invalid Heading"); // Should be ignored
        body.appendElement("h1").text("Valid Heading");   // Should be picked up
        return doc;
    }

    private Document createDocumentWithEmptyAndValidHeading() {
        Document doc = Jsoup.parse("");
        Element body = doc.body();
        body.appendElement("h1").text("    "); // Empty text
        body.appendElement("h2").text("Valid Heading");
        return doc;
    }
}
