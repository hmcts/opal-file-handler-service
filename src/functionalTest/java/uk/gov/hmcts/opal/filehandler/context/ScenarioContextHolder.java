package uk.gov.hmcts.opal.filehandler.context;

/**
 * Provides thread-bound access to the current functional-test scenario context.
 */
public final class ScenarioContextHolder {

    private static final ThreadLocal<ScenarioContext> CURRENT = ThreadLocal.withInitial(ScenarioContext::new);

    private ScenarioContextHolder() {
    }

    /**
     * Returns the scenario context bound to the current thread.
     *
     * @return current thread-bound scenario context.
     */
    public static ScenarioContext current() {
        return CURRENT.get();
    }

    /**
     * Resets the scenario context bound to the current thread.
     */
    public static void reset() {
        CURRENT.get().reset();
    }
}
