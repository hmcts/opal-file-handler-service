package uk.gov.hmcts.opal.filehandler.support;

import java.util.function.Supplier;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.db.DatabaseClient;

/** Checks both functional-test database connections before any scenario seeds or modifies data. */
public final class FunctionalTestDatabaseCheck {

    private FunctionalTestDatabaseCheck() {
    }

    public static void main(String[] args) {
        verify("File-handler", DatabaseClient::new);
        verify("Fines", () -> new DatabaseClient(TestEnvironment.getFinesDatabaseUrl(),
            TestEnvironment.getFinesDatabaseUsername(), TestEnvironment.getFinesDatabasePassword()));
    }

    private static void verify(String service, Supplier<DatabaseClient> connections) {
        try (DatabaseClient database = connections.get()) {
            if (!database.ping()) {
                throw new IllegalStateException("Database connection is not valid");
            }
            System.out.println(service + " functional-test database connection verified");
        } catch (RuntimeException exception) {
            throw new IllegalStateException(service + " functional-test database preflight failed", exception);
        }
    }
}
