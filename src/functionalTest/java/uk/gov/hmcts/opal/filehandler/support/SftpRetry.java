package uk.gov.hmcts.opal.filehandler.support;

import java.util.function.Supplier;

public final class SftpRetry {

    private static final int MAX_ATTEMPTS = 3;

    private SftpRetry() {
    }

    /**
     * Retries the given action up to 3 times on RuntimeException (e.g. transient SFTP connection
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
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
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
