package uk.gov.hmcts.opal.filehandler.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;

/**
 * Lightweight JDBC helper for functional-test database fixtures and checks.
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
        return query(sql, resultSet -> resultSet.getString(1), parameters);
    }

    /**
     * Executes a query and maps every result row to a typed value.
     *
     * @param sql SQL statement to execute.
     * @param rowMapper mapper applied to every result row.
     * @param parameters ordered statement parameters.
     * @param <T> mapped row type.
     * @return mapped query results.
     */
    public <T> List<T> query(String sql, SqlRowMapper<T> rowMapper, Object... parameters) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(rowMapper.map(resultSet));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to execute database query", e);
        }
    }

    /**
     * Executes a parameterised update statement.
     *
     * @param sql SQL statement to execute.
     * @param parameters ordered statement parameters.
     * @return number of affected rows.
     */
    public int update(String sql, Object... parameters) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to execute database update", e);
        }
    }

    /**
     * Executes a SQL script from the functional-test classpath.
     *
     * @param resourcePath classpath-relative SQL script path.
     */
    public void executeScript(String resourcePath) {
        try {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(resourcePath));
        } catch (ScriptException e) {
            throw new IllegalStateException("Failed to execute database script: " + resourcePath, e);
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
     * Maps a JDBC result row to a typed functional-test value.
     *
     * @param <T> mapped row type.
     */
    @FunctionalInterface
    public interface SqlRowMapper<T> {

        T map(ResultSet resultSet) throws SQLException;
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
