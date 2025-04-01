# Web Crawler Assignment

The task of this assignment is to develop a Web-Crawler in Java, which provides a compact overview of the given website and linked websites by only listing the headings and the links. The attached example-report.md shows an example of how the overview could look (feel free to improve the suggested layout).

## DRAFT How to start the application:

 
On the command line that also contains the input arguments URL, depth, domains.  
e.g. `crawler www.sample.com 5`


## Must Have Features

The crawler MUST implement at least the following features:

- [ ] Input the URL, the depth of websites to crawl, and the domain(s) of websites to be crawled
- [ ] Create a compact overview of the crawled websites:
    - [ ] Record only the headings
    - [ ] Represent the depth of the crawled websites with proper indentation (see example)
    - [ ] Record the URLs of the crawled sites
    - [ ] Highlight broken links
- [ ] Find the links to other websites and recursively do the analysis for those websites:
    - [ ] That can be reached within the given depth (to avoid very long runtimes)
    - [ ] And is located in one of the specified domain(s)
    - [ ] Also note, each website should be crawled only once

## Additional Info

Note, also provide automated unit tests for each feature (we will not accept submissions without unit tests).

### Implementation

Regarding the implementation, please use:
- A modern IDE (Eclipse, IntelliJ, Visual Code, etc.)
- GitHub, GitLab, or BitBucket to version and share your sources
- The repository must contain a README file briefly describing the steps to build, run, and test your crawler
- A Java testing framework, e.g., JUnit for automating the tests
- A build tool like Maven or Gradle to automate the build and testing of your solution
- We suggest using an existing library, such as jsoup, for parsing HTML