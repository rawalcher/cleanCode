package crawler.reporter;

import crawler.constants.CrawlerConstants;
import crawler.model.CrawlerConfig;
import crawler.model.PageResult;
import crawler.model.PageResult.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

/**
 * Writes the crawl results into a Markdown file with blockquote hierarchy.
 */
public class MarkdownReporter {
    private static final Logger logger = LoggerFactory.getLogger(MarkdownReporter.class);

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

        try (PrintWriter writer = new PrintWriter(new FileWriter(CrawlerConstants.REPORT_FILENAME))) {
            writeReportHeader(config, writer);
            writePage(root, writer);
            logger.info("Successfully wrote report to '{}'", CrawlerConstants.REPORT_FILENAME);
        } catch (IOException e) {
            logger.error("Failed to write report: {}", e.getMessage());
        }
    }

    /**
     * Writes the report header with configuration info.
     */
    void writeReportHeader(CrawlerConfig config, PrintWriter writer) {
        writer.printf("# Crawl Report: %s%n", config.getRootUrl());
        writer.printf("**Max Depth:** %d  %n", config.getMaxDepth());
        writer.printf("**Domains:** %s%n%n", String.join(", ", config.getAllowedDomains()));
        writer.println("---\n");
    }

    /**
     * Recursively writes a page and its children to the report.
     */
    void writePage(PageResult page, PrintWriter writer) {
        writePageHeader(page, writer);

        if (!page.broken()) {
            writePageContent(page, writer);
        }

        for (PageResult child : page.children()) {
            writer.println("---\n");
            writePage(child, writer);
        }
    }

    /**
     * Writes the page header with URL, depth and status.
     */
    void writePageHeader(PageResult page, PrintWriter writer) {
        writer.printf("## Page: %s%n", page.url());
        writer.printf("**Depth:** %d  %n", page.depth());
        writer.printf("**Status:** %s%n%n", page.broken() ? "Broken" : "OK");

        if (page.broken()) {
            writer.println();
        }
    }

    /**
     * Writes the content section with headings and links.
     */
    void writePageContent(PageResult page, PrintWriter writer) {
        writer.println("### Content\n");

        if (page.sections().isEmpty()) {
            writer.println("*(No headings or links found)*\n");
            return;
        }

        writeLinksBeforeHeadings(page, writer);

        for (Section section : page.sections()) {
            if (section.heading().level() > 0) {
                writeSectionWithLinks(section, writer);
            }
        }
    }

    /**
     * Writes a section with its heading and associated links.
     */
    void writeSectionWithLinks(Section section, PrintWriter writer) {
        String blockquote = ">".repeat(section.heading().level());

        writer.printf("%s**H%d: %s**%n",
                blockquote,
                section.heading().level(),
                section.heading().text());

        for (URI link : section.links()) {
            writer.printf("%s* %s%n", blockquote, link.toString());
        }

        writer.println();
    }

    /**
     * Writes links that appear before any heading on the page.
     */
    void writeLinksBeforeHeadings(PageResult page, PrintWriter writer) {
        for (Section section : page.sections()) {
            if (section.heading().level() == 0 && !section.links().isEmpty()) {
                writer.println("**Links Before First Heading:**");
                for (URI link : section.links()) {
                    writer.printf("* %s%n", link.toString());
                }
                writer.println();
                break;
            }
        }
    }
}