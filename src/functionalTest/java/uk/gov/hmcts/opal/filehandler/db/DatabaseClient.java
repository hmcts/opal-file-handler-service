package uk.gov.hmcts.opal.filehandler.db;

import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight JDBC helper for optional functional-test database checks.
 */
public class DatabaseClient implements AutoCloseable {

    private final Connection connection;

    /**
     * Opens a database connection using the configured functional-test environment settings.
     */
    public DatabaseClient() {
        try {
            this.connection = DriverManager.getConnection(
                TestEnvironment.getDatabaseUrl(),
                TestEnvironment.getDatabaseUsername(),
                TestEnvironment.getDatabasePassword()
            );
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to connect to functional test database", e);
        }
    }

    /**
     * Validates that the database connection is healthy.
     *
     * @return {@code true} when the connection is valid.
     */
    public boolean ping() {
        try {
            return connection.isValid(5);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to validate database connection", e);
        }
    }

    /**
     * Executes a query and returns the first column from every row as strings.
     *
     * @param sql SQL statement to execute.
     * @param parameters ordered statement parameters.
     * @return first-column values returned by the query.
     */
    public List<String> queryFirstColumn(String sql, Object... parameters) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(resultSet.getString(1));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to execute database query", e);
        }
    }

    /**
     * Binds ordered parameters to a prepared statement.
     *
     * @param statement statement to populate.
     * @param parameters ordered statement parameters.
     * @throws SQLException when parameter binding fails.
     */
    private static void bindParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }

    /**
     * Closes the underlying database connection.
     */
    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to close database connection", e);
        }
    }
}
