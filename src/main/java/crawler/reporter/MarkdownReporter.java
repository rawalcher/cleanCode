package crawler.reporter;

import crawler.model.CrawlerConfig;
import crawler.model.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.net.URI;

public class MarkdownReporter {
    private static final Logger logger = LoggerFactory.getLogger(MarkdownReporter.class);
    private static final String OUTPUT_FILE = "report.md";

    public void writeReport(PageResult root, CrawlerConfig config) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE))) {
            writeToWriter(root, config, writer);
        } catch (IOException e) {
            logger.error("Failed to write report: {}", e.getMessage());
        }
    }

    void writeToWriter(PageResult root, CrawlerConfig config, PrintWriter writer) {
        if (writer == null || root == null || config == null) return;

        writer.printf("# Crawl Report: %s%n", config.getRootUrl());
        writer.printf("**Max Depth:** %d  %n", config.getMaxDepth());
        writer.printf("**Domains:** %s%n%n", String.join(", ", config.getAllowedDomains()));
        writer.println("---\n");

        writePage(root, writer);
    }

    void writePage(PageResult page, PrintWriter writer) {
        writer.printf("## Page: %s%n", page.url());
        writer.printf("**Depth:** %d  %n", page.depth());
        writer.printf("**Status:** %s%n%n", page.broken() ? "Broken" : "OK");

        if (page.broken()) {
            writer.println();
            return;
        }

        // Headings
        writer.println("### Headings");
        if (page.headings().isEmpty()) {
            writer.println("- (none)");
        } else {
            for (PageResult.Heading h : page.headings()) {
                String indent = "  ".repeat(h.level() - 1);
                writer.printf("%s- H%d: %s%n", indent, h.level(), h.text());
            }
        }

        // Links
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