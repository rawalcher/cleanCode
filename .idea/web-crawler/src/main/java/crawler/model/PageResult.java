package main.java.crawler.model;

import java.util.List;
import java.util.Map;

/**
 * Data Class that represents a result of a page consisting of headers, url, urls that are invalid etc.
 * method to fetch links to next websites aswell (to be coordinated by crawler)
 */
public class PageResult {
    List<String> headers;
    Map<String, Boolean> links; //maybe directly tell if valid or no
}
