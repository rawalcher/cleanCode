package crawler.concurrent;

import crawler.error.ErrorHandlingStrategy;

/**
 * Configuration class for the concurrent web crawler.
 * Encapsulates all settings related to thread management, timeouts, and error handling.
 */
public class ConcurrentCrawlerConfig {
    private final int maxThreads;
    private final int coreThreads;
    private final long timeoutSeconds;
    private final long keepAliveSeconds;
    private final ErrorHandlingStrategy errorStrategy;
    private final boolean enableDetailedErrorReporting;

    private ConcurrentCrawlerConfig(Builder builder) {
        this.maxThreads = builder.maxThreads;
        this.coreThreads = builder.coreThreads;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.keepAliveSeconds = builder.keepAliveSeconds;
        this.errorStrategy = builder.errorStrategy;
        this.enableDetailedErrorReporting = builder.enableDetailedErrorReporting;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ConcurrentCrawlerConfig defaultConfig() {
        return builder().build();
    }

    public int getMaxThreads() { return maxThreads; }
    public int getCoreThreads() { return coreThreads; }
    public long getTimeoutSeconds() { return timeoutSeconds; }
    public long getKeepAliveSeconds() { return keepAliveSeconds; }
    public ErrorHandlingStrategy getErrorStrategy() { return errorStrategy; }
    public boolean isDetailedErrorReportingEnabled() { return enableDetailedErrorReporting; }

    public static class Builder {
        private int maxThreads = Runtime.getRuntime().availableProcessors() * 2;
        private int coreThreads = Math.min(maxThreads, 4);
        private long timeoutSeconds = 300; // 5 minutes
        private long keepAliveSeconds = 60;
        private ErrorHandlingStrategy errorStrategy;
        private boolean enableDetailedErrorReporting = true;

        public Builder maxThreads(int maxThreads) {
            if (maxThreads <= 0) {
                throw new IllegalArgumentException("Max threads must be positive");
            }
            this.maxThreads = maxThreads;
            this.coreThreads = Math.min(this.coreThreads, maxThreads);
            return this;
        }

        public Builder coreThreads(int coreThreads) {
            if (coreThreads <= 0) {
                throw new IllegalArgumentException("Core threads must be positive");
            }
            this.coreThreads = coreThreads;
            return this;
        }

        public Builder timeoutSeconds(long timeoutSeconds) {
            if (timeoutSeconds <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder keepAliveSeconds(long keepAliveSeconds) {
            if (keepAliveSeconds < 0) {
                throw new IllegalArgumentException("Keep alive time cannot be negative");
            }
            this.keepAliveSeconds = keepAliveSeconds;
            return this;
        }

        public Builder errorStrategy(ErrorHandlingStrategy errorStrategy) {
            this.errorStrategy = errorStrategy;
            return this;
        }

        public Builder enableDetailedErrorReporting(boolean enable) {
            this.enableDetailedErrorReporting = enable;
            return this;
        }

        public ConcurrentCrawlerConfig build() {
            if (errorStrategy == null) {
                errorStrategy = new crawler.error.DefaultErrorHandlingStrategy();
            }
            return new ConcurrentCrawlerConfig(this);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "ConcurrentCrawlerConfig{maxThreads=%d, coreThreads=%d, timeoutSeconds=%d, " +
                        "keepAliveSeconds=%d, errorStrategy=%s, detailedErrorReporting=%s}",
                maxThreads, coreThreads, timeoutSeconds, keepAliveSeconds,
                errorStrategy.getClass().getSimpleName(), enableDetailedErrorReporting
        );
    }
}
