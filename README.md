# Web Crawler Assignment – Group 12

## Group Members
- Carolin Aigensberger
- Raphael Walcher

**Note:**  
The project was developed entirely using the *Code Together* feature of IntelliJ. Therefore, all Git commits appear under Raphael Walcher's name, but the development was performed via *pair programming* by both members.

---

## Introduction
This assignment extends the original web crawler with advanced features including **concurrent crawling**, **comprehensive error handling**, and **thread-safe operations**. The crawler creates a compact overview of a given website and its linked websites, organized according to crawl depth, while providing robust error reporting and parallel processing capabilities.

---

## How to Build and Run

### Build
```bash
mvn clean install
```

### Run - Sequential Mode (Original)
```bash
java -jar target/webcrawler.jar <URL> <depth> <allowed-domains>
```

### Run - Concurrent Mode (New)
```bash
java -jar target/webcrawler.jar --concurrent <URL> <depth> <allowed-domains> [thread-count]
```

#### Parameters:
- `<URL>` – Starting URL to crawl
- `<depth>` – Maximum depth to follow links
- `<allowed-domains>` – Comma-separated list of domains to restrict crawling to
- `[thread-count]` – Optional: Number of threads to use (defaults to CPU cores × 2)

#### Examples:
```bash
# Sequential crawling (original mode)
java -jar target/webcrawler.jar https://example.com 2 example.com,example.org

# Concurrent crawling with default thread count
java -jar target/webcrawler.jar --concurrent https://example.com 2 example.com,example.org

# Concurrent crawling with 8 threads
java -jar target/webcrawler.jar --concurrent https://example.com 2 example.com,example.org 8
```

Alternatively, if you are using IntelliJ IDEA, you can use the provided *Run Configuration*.

---

## Features

### Original Features
- Accept input for URL, crawl depth, and allowed domains
- Generate a compact overview with headings and links
- Indent, according to crawl depth
- Record URLs of crawled pages
- Highlight broken links
- Recursively crawl links with depth and domain restrictions
- Ensure each website is crawled only once
- Store results in Markdown format

### New Features (Assignment 2)
- **Concurrent crawling** with configurable ThreadPool for improved performance
- **Thread-safe operations** ensuring data consistency across multiple threads
- **Comprehensive error handling** with categorization and detailed error tracking
- **Error reporting** in crawl results with statistics and error summaries
- **Clean third-party library boundaries** using an adapter pattern to isolate dependencies

---

## Architecture

### Core Components
- **WebCrawler**: Main orchestrator supporting both sequential and concurrent modes
- **PageFetcher**: HTTP client wrapper for fetching web pages
- **HtmlParser**: Extracts headings and links from HTML documents
- **RobotsTxtHandler**: Respects robots.txt rules and crawl delays
- **LinkFilter**: Thread-safe URL filtering and visit tracking
- **MarkdownReporter**: Generates formatted crawl reports
- **ErrorCollector**: Thread-safe error collection and statistics

### Design Patterns
- **Strategy Pattern**: Configurable error handling strategies
- **Adapter Pattern**: Clean abstraction over third-party HTML parsing libraries
- **Factory Pattern**: Crawler creation with different configurations
- **Builder Pattern**: Complex configuration object construction

---

## Configuration

The crawler supports various configuration options:

- **Thread Count**: Adjustable concurrency level
- **Timeout Settings**: Configurable connection and read timeouts
- **Domain Filtering**: Strict domain boundary enforcement
- **Depth Limiting**: Configurable crawl depth to prevent infinite loops
- **Error Strategies**: Configurable error handling behavior

---

## Output Format

The crawler generates a structured Markdown report containing:

- **Header**: Crawl configuration and summary
- **Page Sections**: Organized by headings with associated links
- **Depth Indication**: Visual indentation showing link hierarchy
- **Status Information**: Clear marking of broken or inaccessible pages
- **Error Summary**: Statistics and categorization of encountered errors

---

## Notes

This enhanced implementation maintains **backward compatibility** with the original crawler while adding powerful new features:

- Sequential mode produces identical output to the original implementation
- Concurrent mode provides the same functionality with improved performance
- Error handling is optional and doesn't affect core crawling behavior
- Thread safety ensures reliable operation in production environments
- Clean architecture supports easy extension and maintenance