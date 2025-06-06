package crawler.reporter;

import crawler.model.CrawlerConfig;
import crawler.model.PageResult;
import crawler.model.PageResult.Heading;
import crawler.model.PageResult.Section;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownReporterTest {

    private MarkdownReporter reporter;
    private CrawlerConfig    config;
    private URI             exampleUri;

    private StringWriter buffer;
    private PrintWriter  writer;

    private static final String NEWLINE = "\n";



    @BeforeEach
    void setUp() throws Exception {
        reporter   = new MarkdownReporter();
        exampleUri = new URI("http://example.com");
        config     = new CrawlerConfig(exampleUri, 2, "example.com");

        buffer = new StringWriter();
        writer = new PrintWriter(buffer);
    }

    private static String normalise(String s) {
        return s.replace("\r\n", "\n");
    }

    @Test
    void writeReportHeader_producesExpectedMarkdown() {
        reporter.writeReportHeader(config, writer);
        writer.flush();

        String expected = "# Crawl Report: http://example.com" + NEWLINE +
                "**Max Depth:** 2  "                 + NEWLINE +
                "**Domains:** example.com"            + NEWLINE + NEWLINE +
                "---"                                 + NEWLINE + NEWLINE;

        assertEquals(expected, normalise(buffer.toString()));
    }

    @Test
    void writePageHeader_okPage() {
        PageResult page = new PageResult(exampleUri, 1, false,
                Collections.emptyList(), Collections.emptySet());

        reporter.writePageHeader(page, writer);
        writer.flush();

        String expected = "## Page: http://example.com" + NEWLINE +
                "**Depth:** 1  "             + NEWLINE +
                "**Status:** OK"             + NEWLINE + NEWLINE;

        assertEquals(expected, normalise(buffer.toString()));
    }

    @Test
    void writePageHeader_brokenPage() {
        PageResult page = new PageResult(exampleUri, 1, true,
                Collections.emptyList(), Collections.emptySet());

        reporter.writePageHeader(page, writer);
        writer.flush();

        String expected = "## Page: http://example.com" + NEWLINE +
                "**Depth:** 1  "             + NEWLINE +
                "**Status:** Broken"         + NEWLINE + NEWLINE + NEWLINE; // extra blank‑line from println()

        assertEquals(expected, normalise(buffer.toString()));
    }

    @Test
    void writePageContent_whenEmpty() {
        PageResult page = new PageResult(exampleUri, 0, false,
                Collections.emptyList(), Collections.emptySet());

        reporter.writePageContent(page, writer);
        writer.flush();

        String expected = "### Content" + NEWLINE + NEWLINE +
                "*(No headings or links found)*" + NEWLINE + NEWLINE;

        assertEquals(expected, normalise(buffer.toString()));
    }

    @Test
    void writeSectionWithLinks_writesCorrectBlockQuote() {
        Heading heading = new Heading(2, "Test Heading");
        URI link1 = URI.create("http://example.com/link1");
        URI link2 = URI.create("http://example.com/link2");
        Section section = new Section(heading, new LinkedHashSet<>(List.of(link1, link2)));

        reporter.writeSectionWithLinks(section, writer);
        writer.flush();

        String expected =
                ">>**H2: Test Heading**"      + NEWLINE +
                        ">>* http://example.com/link1" + NEWLINE +
                        ">>* http://example.com/link2" + NEWLINE + NEWLINE;

        assertEquals(expected, normalise(buffer.toString()));
    }

    @Test
    void writeLinksBeforeHeadings_onlyFirstPreHeadingBlockIsWritten() {
        Heading rootHeading = new Heading(0, "Page Root");
        URI     link1       = URI.create("http://example.com/root1");
        URI     link2       = URI.create("http://example.com/root2");
        Section rootSection = new Section(rootHeading, new LinkedHashSet<>(List.of(link1, link2)));

        PageResult page = new PageResult(exampleUri, 0, false,
                List.of(rootSection), Collections.emptySet());

        reporter.writeLinksBeforeHeadings(page, writer);
        writer.flush();

        String expected = "**Links Before First Heading:**" + NEWLINE +
                "* http://example.com/root1"    + NEWLINE +
                "* http://example.com/root2"    + NEWLINE + NEWLINE;

        assertEquals(expected, normalise(buffer.toString()));
    }

    @Test
    void writePage_recursivelyWritesChildrenSeparatedByRules() {
        Heading h1 = new Heading(1, "Main Heading");
        Section rootSection = new Section(h1, new LinkedHashSet<>(List.of(URI.create("http://example.com/main"))));

        Heading h2 = new Heading(2, "Sub Heading");
        Section childSection = new Section(h2, new LinkedHashSet<>(List.of(URI.create("http://example.com/sub"))));

        PageResult childPage = new PageResult(URI.create("http://example.com/child"), 1, false,
                List.of(childSection), Collections.emptySet());

        PageResult rootPage = new PageResult(exampleUri, 0, false,
                List.of(rootSection), Set.of(childPage));

        reporter.writePage(rootPage, writer);
        writer.flush();

        String out = normalise(buffer.toString());

        // there should be exactly two-page headers – one for the root and one for the child
        long occurrences = out.lines().filter(IS_PAGE_HEADER).count();
        assertEquals(2, occurrences);

        assertTrue(out.contains("## Page: http://example.com"));
        assertTrue(out.contains("## Page: http://example.com/child"));

        // make sure the horizontal rule (---) between pages is present
        assertTrue(out.contains("---"));
    }

    private static final Predicate<String> IS_PAGE_HEADER = line -> line.startsWith("## Page:");
}
