# Web Crawler Assignment – Group 12

## Group Members
- Carolin Aigensberger
- Raphael Walcher

**Note:**  
The project was developed entirely using the *Code Together* feature of IntelliJ. Therefore, all Git commits appear under Raphael Walcher's name, but the development was performed via *pair programming* by both members.

---

## Introduction
This assignment extends the original web crawler with advanced features including **concurrent crawling**. The crawler creates a compact overview of a given website and its linked websites, organized according to crawl depth, while providing robust error reporting and parallel processing capabilities.

The crawler saves its output to a single Markdown (`.md`) file, following the example structure provided in `example-report.md`.

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

## Features

### Original Features
- [x] Accept input for URL, crawl depth, and allowed domains
- [x] Generate a compact overview with headings and links
- [x] Indent according to crawl depth
- [x] Record URLs of crawled pages
- [x] Highlight broken links
- [x] Recursively crawl links with depth and domain restrictions
- [x] Ensure each website is crawled only once
- [x] Store results in Markdown format

### New Features (Assignment 2)
- [x] **Concurrent crawling** with configurable ThreadPool
- [x] **Thread-safe operations** ensuring data consistency
- [x] **Comprehensive error handling** with categorization
- [x] **Error reporting** in crawl results with statistics
- [x] **Clean third-party library boundaries** using an adapter pattern
- [x] **Enhanced reporting** with error summaries
- [x] **Robust application** that doesn't crash on individual failures
- [x] **Performance optimization** through parallel processing

---

## Implementation Details

- Programming Language: **Java**
- HTML Parsing: **jsoup**
- Build Tool: **Maven**
- Testing Framework: **JUnit 5**
- Version Control: **GitHub**

---

## Testing

Automated **JUnit** tests are provided to cover all key features of the crawler, ensuring correctness and reliability.

---

## Notes

This enhanced implementation maintains **backward compatibility** with the original crawler while adding powerful new features.