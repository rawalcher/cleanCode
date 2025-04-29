# Web Crawler Assignment – Group 12

## Group Members
- Carolin Aigensberger
- Raphael Walcher

**Note:**  
The project was developed entirely using the *Code Together* feature of IntelliJ. Therefore, all Git commits appear under Raphael Walcher's name, but the development was performed via *pair programming* by both members.

---

## Introduction
The task of this assignment was to develop a web crawler in Java, which creates a compact overview of a given website and its linked websites. The overview includes only headings and links, organized according to crawl depth.

The crawler saves its output to a single Markdown (`.md`) file, following the example structure provided in `example-report.md`.

---

## How to Build and Run

### Build
```bash
mvn clean install
```

### Run
```bash
java -jar target/webcrawler.jar <URL> <depth> <allowed-domains>
```
- `<URL>` – Starting URL to crawl
- `<depth>` – Maximum depth to follow links
- `<allowed-domains>` – Comma-separated list of domains to restrict crawling to

**Example:**
```bash
java -jar target/webcrawler.jar https://example.com 2 example.com,example.org
```

Alternatively, if you are using IntelliJ IDEA, you can use the provided *Run Configuration*.

---

## Features

- [x] Accept input for URL, crawl depth, and allowed domains.
- [x] Generate a compact overview:
  - [x] List only headings (`<h1>`, `<h2>`, etc.).
  - [x] Indent according to crawl depth.
  - [x] Record URLs of the crawled pages.
  - [x] Highlight broken links.
- [x] Recursively crawl links:
  - [x] Respect maximum depth.
  - [x] Only crawl within allowed domains.
  - [x] Ensure each website is crawled only once.
- [x] Store results in a single `.md` file (Markdown format).

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

Remember: This assignment is the foundation for Assignment 2. We ensured clean code, proper naming conventions, single-responsibility methods, and meaningful unit tests according to the project grading guidelines.
