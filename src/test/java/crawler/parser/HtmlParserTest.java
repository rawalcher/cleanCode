package crawler.parser;

import crawler.model.PageResult;
import crawler.model.PageResult.Section;
import crawler.model.PageResult.Heading;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class HtmlParserTest {

    private final HtmlParser parser = new HtmlParser();
    private static final URI BASE_URI = URI.create("https://example.com");

    @Test
    void parsesHeadingsAndLinks_preservesOrder() {
        Document doc = Jsoup.parse("");
        Element body = doc.body();
        body.appendElement("h1").text("Main Heading");
        body.appendElement("a").attr("href", "https://example.com/first").text("L1");
        body.appendElement("a").attr("href", "/second").text("L2");

        PageResult pr = parser.parse(BASE_URI, 0, doc);
        assertEquals(1, pr.sections().size(), "One section expected");

        Section s = pr.sections().getFirst();
        assertEquals(new Heading(1, "Main Heading"), s.heading());
        assertEquals(2, s.links().size());

        List<String> linkStrings = new ArrayList<>(s.links())
                .stream().map(URI::toString).toList();
        assertEquals(List.of(
                "https://example.com/first",
                "https://example.com/second"), linkStrings);
    }

    @Test
    void emptyDocument_givesNoSections() {
        Document empty = Jsoup.parse("");
        PageResult pr = parser.parse(BASE_URI, 0, empty);
        assertTrue(pr.sections().isEmpty());
    }

    @Test
    void discardsCompletelyBrokenLinks() {
        Document doc = Jsoup.parse("");
        doc.body().appendElement("a").attr("href", "ht!tp://bad").text("bad");
        PageResult pr = parser.parse(BASE_URI, 0, doc);
        assertTrue(pr.sections().isEmpty(), "Broken link should be ignored so root section filtered out");
    }

    @Test
    void resolvesRelativeAndFragmentLinks_underRootHeading() {
        Document doc = Jsoup.parse("");
        Element body = doc.body();
        body.appendElement("a").attr("href", "/contact");
        body.appendElement("a").attr("href", "#top");

        PageResult pr = parser.parse(BASE_URI, 0, doc);
        assertEquals(1, pr.sections().size());
        Section root = pr.sections().getFirst();
        assertEquals(0, root.heading().level());

        Set<String> links = root.links().stream()
                .map(URI::toString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertEquals(Set.of("https://example.com/contact", "https://example.com#top"), links);
    }

    @Test
    void ignoresHeadingsAboveH6() {
        Document doc = Jsoup.parse("");
        doc.body().appendElement("h7").text("SkipMe");
        doc.body().appendElement("h1").text("Valid");
        PageResult pr = parser.parse(BASE_URI, 0, doc);
        assertEquals(1, pr.sections().size());
        assertEquals("Valid", pr.sections().getFirst().heading().text());
    }

    @Test
    void ignoresEmptyHeadingText() {
        Document doc = Jsoup.parse("");
        doc.body().appendElement("h2").text("   ");
        doc.body().appendElement("h3").text("Good");
        PageResult pr = parser.parse(BASE_URI, 0, doc);
        assertEquals(1, pr.sections().size());
        assertEquals("Good", pr.sections().getFirst().heading().text());
    }
}