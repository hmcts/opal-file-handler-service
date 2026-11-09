package uk.gov.hmcts.opal.filehandler.db;

import java.util.List;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;

/** Reads the Fines job/file evidence correlated to a connected banking input. */
public class ConnectedFinesTestDatabaseClient implements AutoCloseable {
    private final DatabaseClient database = new DatabaseClient(TestEnvironment.getFinesDatabaseUrl(),
        TestEnvironment.getFinesDatabaseUsername(), TestEnvironment.getFinesDatabasePassword());

    public List<FinesFile> findByFileName(short businessUnit, String fileName) {
        return database.query("""
            SELECT f.source_json_id, f.transformed_json_id, f.record_count, f.total_amount, j.status::text
            FROM interface_files f JOIN interface_jobs j ON j.interface_job_id = f.interface_job_id
            WHERE j.business_unit_id = ? AND f.file_name = ?
            """, row -> new FinesFile(row.getLong(1), row.getLong(2), row.getInt(3), row.getLong(4),
                row.getString(5)), businessUnit, fileName);
    }

    @Override
    public void close() {
        database.close();
    }

    public record FinesFile(long sourceId, long transformedId, int recordCount, long totalAmount, String jobStatus) { }
}
