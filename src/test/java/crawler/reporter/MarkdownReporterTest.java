package crawler.reporter;

import crawler.model.CrawlerConfig;
import crawler.model.Heading;
import crawler.model.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownReporterTest {

    private MarkdownReporter reporter;
    private CrawlerConfig config;

    @BeforeEach
    void setup() {
        reporter = new MarkdownReporter();
        try {
            config = new CrawlerConfig(URI.create("http://example.com"), 2, "example.com");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testWriteReport_CreatesOutputFile() throws IOException {
        PageResult root = new PageResult(URI.create("http://example.com"), 0, false,
                Collections.emptyList(), Collections.emptyList(), Collections.emptySet());

        reporter.writeReport(root, config);

        Path outputPath = Path.of("report.md");
        assertTrue(Files.exists(outputPath));

        String content = Files.readString(outputPath);
        assertTrue(content.contains("# Crawl Report: http://example.com"));

        Files.deleteIfExists(outputPath);  // Cleanup after test
    }


    @Test
    void testWriteReport_GeneratesCorrectHeader() {
        StringWriter sw = new StringWriter();
        PageResult root = new PageResult(URI.create("http://example.com"), 0, false,
                Collections.emptyList(), Collections.emptyList(), Collections.emptySet());
        reporter.writeToWriter(root, config, new PrintWriter(sw));

        String output = sw.toString();
        assertTrue(output.contains("# Crawl Report: http://example.com"));
        assertTrue(output.contains("**Max Depth:** 2"));
        assertTrue(output.contains("**Domains:** example.com"));
    }

    @Test
    void testWritePage_IncludesBrokenStatus() {
        StringWriter sw = new StringWriter();
        PageResult brokenPage = new PageResult(URI.create("http://broken.com"), 1, true,
                Collections.emptyList(), Collections.emptyList(), Collections.emptySet());

        reporter.writePage(brokenPage, new PrintWriter(sw));

        String output = sw.toString();
        assertTrue(output.contains("**Status:** Broken"));
    }

    @Test
    void testWritePage_IncludesHeadings() {
        StringWriter sw = new StringWriter();
        Heading h1 = new Heading(1, "Main Heading");
        Heading h2 = new Heading(2, "Sub Heading");
        PageResult pageWithHeadings = new PageResult(URI.create("http://example.com"), 0, false,
                Arrays.asList(h1, h2), Collections.emptyList(), Collections.emptySet());

        reporter.writePage(pageWithHeadings, new PrintWriter(sw));

        String output = sw.toString();
        assertTrue(output.contains("- H1: Main Heading"));
        assertTrue(output.contains("  - H2: Sub Heading"));
    }

    @Test
    void testWritePage_IncludesLinks() {
        StringWriter sw = new StringWriter();
        URI link1 = URI.create("http://example.com/page1");
        URI link2 = URI.create("http://example.com/page2");
        PageResult pageWithLinks = new PageResult(URI.create("http://example.com"), 0, false,
                Collections.emptyList(), Arrays.asList(link1, link2), Collections.emptySet());

        reporter.writePage(pageWithLinks, new PrintWriter(sw));

        String output = sw.toString();
        assertTrue(output.contains("- http://example.com/page1"));
        assertTrue(output.contains("- http://example.com/page2"));
    }

    @Test
    void testWritePage_RecursiveChildrenPages() {
        StringWriter sw = new StringWriter();
        PageResult childPage = new PageResult(URI.create("http://example.com/child"), 1, false,
                Collections.singletonList(new Heading(1, "Child Heading")),
                Collections.singletonList(URI.create("http://example.com/link")),
                Collections.emptySet());

        PageResult root = new PageResult(URI.create("http://example.com"), 0, false,
                Collections.singletonList(new Heading(1, "Root Heading")),
                Collections.singletonList(URI.create("http://example.com/rootlink")),
                Set.of(childPage));

        reporter.writePage(root, new PrintWriter(sw));

        String output = sw.toString();
        assertTrue(output.contains("## Page: http://example.com"));
        assertTrue(output.contains("- H1: Root Heading"));
        assertTrue(output.contains("- http://example.com/rootlink"));

        assertTrue(output.contains("## Page: http://example.com/child"));
        assertTrue(output.contains("- H1: Child Heading"));
        assertTrue(output.contains("- http://example.com/link"));
    }

    @Test
    void testWritePage_EmptyHeadingsAndLinks() {
        StringWriter sw = new StringWriter();
        PageResult emptyPage = new PageResult(URI.create("http://example.com/empty"), 1, false,
                Collections.emptyList(), Collections.emptyList(), Collections.emptySet());

        reporter.writePage(emptyPage, new PrintWriter(sw));

        List<String> lines = Arrays.asList(sw.toString().split("\\R"));

        // Check headings
        int headingsIndex = lines.indexOf("### Headings");
        assertTrue(headingsIndex >= 0, "Headings header missing.");
        assertEquals("- (none)", lines.get(headingsIndex + 1).trim(), "Headings content incorrect.");

        // Check links
        int linksIndex = lines.indexOf("### Links");
        assertTrue(linksIndex >= 0, "Links header missing.");
        assertEquals("- (none)", lines.get(linksIndex + 1).trim(), "Links content incorrect.");
    }

}
