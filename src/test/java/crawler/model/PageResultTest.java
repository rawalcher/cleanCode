package crawler.model;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void testCreatePageResult() {
        URI url = URI.create("http://example.com");
        PageResult.Heading heading = new PageResult.Heading(1, "Main Heading");
        PageResult page = new PageResult(url, 1, false, List.of(heading), List.of(), Set.of());

        assertEquals(url, page.url());
        assertEquals(1, page.depth());
        assertFalse(page.broken());
        assertEquals(1, page.headings().size());
        assertEquals("Main Heading", page.headings().getFirst().text());
    }

    @Test
    void testCreateBrokenPage() {
        URI url = URI.create("http://broken.com");
        PageResult brokenPage = PageResult.brokenLink(url, 0);

        assertTrue(brokenPage.broken());
        assertEquals(url, brokenPage.url());
        assertEquals(0, brokenPage.depth());
    }

    @Test
    void testNestedPageResult() {
        URI rootUrl = URI.create("http://example.com");
        URI childUrl = URI.create("http://example.com/child");

        PageResult.Heading rootHeading = new PageResult.Heading(1, "Root Heading");
        PageResult.Heading childHeading = new PageResult.Heading(2, "Child Heading");

        PageResult childPage = new PageResult(childUrl, 1, false, List.of(childHeading), List.of(), Set.of());
        PageResult rootPage = new PageResult(rootUrl, 0, false, List.of(rootHeading), List.of(), Set.of(childPage));

        assertEquals(1, rootPage.children().size());
        assertEquals("Child Heading", rootPage.children().iterator().next().headings().getFirst().text());
    }

    @Test
    void testEmptyHeadingsAndLinks() {
        URI url = URI.create("http://example.com/empty");
        PageResult emptyPage = new PageResult(url, 1, false, List.of(), List.of(), Set.of());

        assertTrue(emptyPage.headings().isEmpty());
        assertTrue(emptyPage.links().isEmpty());
    }
}
