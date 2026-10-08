package uk.gov.hmcts.opal.filehandler.support;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SftpRetry {

    private static final Logger log = LoggerFactory.getLogger(SftpRetry.class);

    private static final int MAX_ATTEMPTS = 10;
    private static final long BASE_SLEEP_MS = 10000L;

    private SftpRetry() {
    }

    /**
     * Retries the given action up to 5 times on RuntimeException (e.g. transient SFTP connection
     * resets). AssertionError propagates immediately without retrying, so assertion failures are
     * still reported correctly.
     */
    public static <T> T withRetry(Supplier<T> action) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                lastException = e;
                if (attempt < MAX_ATTEMPTS) {
                    long sleepMs = BASE_SLEEP_MS;
                    log.warn("SFTP attempt {}/{} failed: {} — retrying in {}ms",
                        attempt, MAX_ATTEMPTS, e.getMessage(), sleepMs);
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    log.error("SFTP failed after {} attempts: {}", MAX_ATTEMPTS, e.getMessage());
                }
            }
        }
        throw lastException;
    }

    public static void withRetry(Runnable action) {
        withRetry(() -> {
            action.run();
            return null;
        });
    }
}
