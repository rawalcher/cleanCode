package crawler.model;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void constructorStoresValues() {
        URI url = URI.create("http://example.com");
        PageResult.Heading h1 = new PageResult.Heading(1, "Main");
        LinkedHashSet<URI> links = new LinkedHashSet<>(List.of(
                URI.create("http://example.com/a"),
                URI.create("http://example.com/b")
        ));
        PageResult.Section section = new PageResult.Section(h1, links);

        PageResult page =
                new PageResult(url, 1, false, List.of(section), Set.of());

        assertEquals(url, page.url());
        assertEquals(1, page.depth());
        assertFalse(page.broken());
        assertEquals(1, page.sections().size());
        assertEquals("Main", page.sections().getFirst().heading().text());
    }

    @Test
    void brokenLinkFactoryMarksBroken() {
        URI missing = URI.create("http://example.com/404");
        PageResult broken = PageResult.brokenLink(missing, 2);

        assertTrue(broken.broken());
        assertEquals(missing, broken.url());
        assertEquals(2, broken.depth());
        assertTrue(broken.sections().isEmpty());
        assertTrue(broken.children().isEmpty());
    }

    @Test
    void getAllLinksReturnsInsertionOrder() {
        URI a = URI.create("http://example.com/a");
        URI b = URI.create("http://example.com/b");

        LinkedHashSet<URI> links = new LinkedHashSet<>(List.of(a, b));
        PageResult.Section section =
                new PageResult.Section(new PageResult.Heading(2, "H2"), links);

        PageResult page =
                new PageResult(URI.create("http://example.com"), 0, false,
                        List.of(section), Set.of());

        // LinkedHashSet preserves insertion order; getAllLinks() should too.
        assertIterableEquals(List.of(a, b), page.getAllLinks());
    }

    @Test
    void childrenAreStored() {
        URI rootUrl = URI.create("http://example.com");
        URI childUrl = URI.create("http://example.com/child");

        PageResult child = new PageResult(
                childUrl, 1, false,
                List.of(), Set.of());

        PageResult root = new PageResult(
                rootUrl, 0, false,
                List.of(), Set.of(child));

        assertEquals(1, root.children().size());
        assertEquals(childUrl, root.children().iterator().next().url());
    }

    @Test
    void emptySectionsProduceNoLinks() {
        PageResult empty = new PageResult(
                URI.create("http://example.com/empty"), 0, false,
                List.of(), Set.of());

        assertTrue(empty.getAllLinks().isEmpty());
    }

    @Test
    void testPageResultWithNestedChildren() throws Exception {
        PageResult child1 = new PageResult(
                new URI("https://example.com/child1"),
                1,
                false,
                List.of(),
                Set.of()
        );

        PageResult child2 = new PageResult(
                new URI("https://example.com/child2"),
                1,
                false,
                List.of(),
                Set.of()
        );

        PageResult parent = new PageResult(
                new URI("https://example.com"),
                0,
                false,
                List.of(),
                Set.of(child1, child2)
        );

        assertEquals(new URI("https://example.com"), parent.url());
        assertEquals(0, parent.depth());
        assertFalse(parent.broken());
        assertEquals(2, parent.children().size());
        assertTrue(parent.children().contains(child1));
        assertTrue(parent.children().contains(child2));
    }
}