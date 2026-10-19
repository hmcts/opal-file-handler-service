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
     * Retries the given action up to 10 times on RuntimeException, including transient SFTP
     * connection resets. AssertionError propagates immediately without retrying, so assertion
     * failures are still reported correctly.
     */
    public static <T> T withRetry(Supplier<T> action) {
        return withRetry("SFTP operation", action);
    }

    /**
     * Retries the given action with an operation label for useful CI diagnostics.
     *
     * @param operation description of the SFTP operation being retried.
     * @param action SFTP action to execute.
     * @param <T> action result type.
     * @return action result.
     */
    public static <T> T withRetry(String operation, Supplier<T> action) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                lastException = e;
                if (attempt < MAX_ATTEMPTS) {
                    long sleepMs = BASE_SLEEP_MS;
                    log.warn("SFTP operation '{}' attempt {}/{} failed: {} — retrying in {}ms",
                        operation, attempt, MAX_ATTEMPTS, describe(e), sleepMs);
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    log.error("SFTP operation '{}' failed after {} attempts", operation, MAX_ATTEMPTS, e);
                }
            }
        }
        throw lastException;
    }

    public static void withRetry(Runnable action) {
        withRetry("SFTP operation", action);
    }

    /**
     * Retries a SFTP operation that does not return a result.
     *
     * @param operation description of the SFTP operation being retried.
     * @param action SFTP action to execute.
     */
    public static void withRetry(String operation, Runnable action) {
        withRetry(operation, () -> {
            action.run();
            return null;
        });
    }

    private static String describe(RuntimeException exception) {
        Throwable rootCause = exception;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
    }
}
