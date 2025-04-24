package main.java.crawler.model;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Data Class that represents a result of a page consisting of headers, url, urls that are invalid etc.
 * method to fetch links to next websites as well (to be coordinated by crawler)
 */
public class PageResult {
    private final String url;
    private final int depth;
    private final boolean broken;
    private final List<Heading> headings; // based on insertion order, we can build DOM tree, using set would not guarantee order anymore
    private final List<URI> links; // we take URI instead of URl because URL cannot handle relative links well, with URI we can .resolve in the Fetcher for relative links e.g /about
    private Set<PageResult> children;

    public PageResult(String url, int depth, boolean broken, List<Heading> headings, List<URI> links) {
        this.url = url;
        this.depth = depth;
        this.broken = broken;
        this.headings = headings;
        this.links = links;
    }

    // yes we waste an object just for storing that a link is broken
    // but java handles empty objects fine in terms of storage
    public static PageResult brokenLink(String url, int depth) {
        return new PageResult(url, depth, true, List.of(), List.of());
    }

    public String getUrl() {
        return url;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isBroken() {
        return broken;
    }

    // #TODO check if we ever run the risk of deleting/updating a heading
    // remember that copyOf is always only a shallow copy..
    public List<Heading> getHeadings() {
        return List.copyOf(headings);
    }

    // #TODO check if we ever run the risk of deleting/updating a URI
    // remember that copyOf is always only a shallow copy..
    public List<URI> getLinks() {
        return List.copyOf(links);
    }

    // #TODO check if we ever run the risk of deleting/updating a child
    // remember that copyOf is always only a shallow copy..
    public Set<PageResult> getChildren() {
        return Set.copyOf(children);
    }

    // #TODO maybe do this in constructor and make class record?
    public void setChildren(Set<PageResult> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "PageResult{" +
                "url='" + url + '\'' +
                ", depth=" + depth +
                ", broken=" + broken +
                ", headings=" + headings +
                ", links=" + links +
                ", children=" + (children != null ? children.size() : 0) +
                '}';
    }
}

