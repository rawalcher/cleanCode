package crawler.reporter;

import crawler.model.CrawlerConfig;
import crawler.model.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

/**
 * Writes the crawl results into a Markdown file.
 */
public class MarkdownReporter {
    private static final Logger logger = LoggerFactory.getLogger(MarkdownReporter.class);
    private static final String OUTPUT_FILE = "report.md";

    /**
     * Writes the crawl results starting from the root PageResult into a Markdown file.
     *
     * @param root   The root page of the crawl result
     * @param config The crawler configuration
     */
    public void writeReport(PageResult root, CrawlerConfig config) {
        if (root == null || config == null) {
            logger.error("Cannot write report: root or config is null.");
            return;
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE))) {
            writeToWriter(root, config, writer);
            logger.info("Successfully wrote report to '{}'", OUTPUT_FILE);
        } catch (IOException e) {
            logger.error("Failed to write report: {}", e.getMessage());
        }
    }

    /**
     * Writes the crawl report to the provided PrintWriter. This is also used for Testing
     *
     * @param root   The root page of the crawl result
     * @param config The crawler configuration
     * @param writer The PrintWriter to write the report to
     */
    void writeToWriter(PageResult root, CrawlerConfig config, PrintWriter writer) {
        writer.printf("# Crawl Report: %s%n", config.getRootUrl());
        writer.printf("**Max Depth:** %d  %n", config.getMaxDepth());
        writer.printf("**Domains:** %s%n%n", String.join(", ", config.getAllowedDomains()));
        writer.println("---\n");

        writePage(root, writer);
    }

    /**
     * Recursively writes a single page and its children into the Markdown report.
     *
     * @param page   The page to write
     * @param writer The PrintWriter to write to
     */
    void writePage(PageResult page, PrintWriter writer) {
        writer.printf("## Page: %s%n", page.url());
        writer.printf("**Depth:** %d  %n", page.depth());
        writer.printf("**Status:** %s%n%n", page.broken() ? "Broken" : "OK");

        if (page.broken()) {
            writer.println();
            return;
        }

        writer.println("### Headings");
        if (page.headings().isEmpty()) {
            writer.println("- (none)");
        } else {
            for (PageResult.Heading h : page.headings()) {
                String indent = "  ".repeat(h.level() - 1);
                writer.printf("%s- H%d: %s%n", indent, h.level(), h.text());
            }
        }

        writer.println("\n### Links");
        if (page.links().isEmpty()) {
            writer.println("- (none)");
        } else {
            for (URI link : page.links()) {
                writer.printf("- %s%n", link.toString());
            }
        }

        for (PageResult child : page.children()) {
            writer.println("\n---\n");
            writePage(child, writer);
        }
    }
}
