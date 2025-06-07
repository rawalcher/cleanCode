package crawler.parser;

import crawler.model.PageResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlParserTest {

    private final HtmlParser parser = new HtmlParser();
    private static final URI BASE_URI = URI.create("https://example.com");

    @Test
    void testBasicParsingWithHeadingsAndLinks() {
        String html = """
            <html><body>
                <h1>Main Heading</h1>
                <a href="https://example.com/first">Link 1</a>
                <a href="/second">Link 2</a>
                <h2>Sub Heading</h2>
                <a href="#fragment">Fragment Link</a>
            </body></html>
            """;

        Document doc = Jsoup.parse(html);
        PageResult result = parser.parse(BASE_URI, 0, doc);

        assertEquals(2, result.sections().size());

        // Check first section (Main Heading)
        PageResult.Section firstSection = result.sections().get(0);
        assertEquals("Main Heading", firstSection.heading().text());
        assertEquals(1, firstSection.heading().level());
        assertEquals(2, firstSection.links().size());

        // Check second section (Sub Heading)
        PageResult.Section secondSection = result.sections().get(1);
        assertEquals("Sub Heading", secondSection.heading().text());
        assertEquals(2, secondSection.heading().level());
        assertEquals(1, secondSection.links().size());
    }

    @Test
    void testLinksBeforeHeadings() {
        String html = """
            <html><body>
                <a href="/root-link">Root Link</a>
                <h1>First Heading</h1>
                <a href="/under-heading">Under Heading</a>
            </body></html>
            """;

        Document doc = Jsoup.parse(html);
        PageResult result = parser.parse(BASE_URI, 0, doc);

        assertEquals(2, result.sections().size());

        PageResult.Section rootSection = result.sections().getFirst();
        assertEquals(0, rootSection.heading().level());
        assertEquals(1, rootSection.links().size());

        PageResult.Section h1Section = result.sections().get(1);
        assertEquals(1, h1Section.heading().level());
        assertEquals("First Heading", h1Section.heading().text());
    }

    @Test
    void testIgnoresBrokenLinks() {
        String html = """
            <html><body>
                <h1>Test</h1>
                <a href="ht!tp://broken">Broken Link</a>
                <a href="/valid">Valid Link</a>
            </body></html>
            """;

        Document doc = Jsoup.parse(html);
        PageResult result = parser.parse(BASE_URI, 0, doc);

        assertEquals(1, result.sections().size());
        PageResult.Section section = result.sections().getFirst();
        assertEquals(1, section.links().size());
    }

    @Test
    void testEmptyDocument() {
        Document doc = Jsoup.parse("");
        PageResult result = parser.parse(BASE_URI, 0, doc);

        assertTrue(result.sections().isEmpty());
    }
}