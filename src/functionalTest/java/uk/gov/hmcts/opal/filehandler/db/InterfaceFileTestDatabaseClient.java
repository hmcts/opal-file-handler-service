package uk.gov.hmcts.opal.filehandler.db;

import java.util.List;
import java.util.UUID;

/**
 * Provides typed database access for interface-file functional-test setup and assertions.
 */
public class InterfaceFileTestDatabaseClient implements AutoCloseable {

    private static final String FIND_BY_FILE_NAME = """
        SELECT interface_file_id, source::text, target::text, type::text, opal_domain::text,
               file_name, filestore_uuid, checksum, status::text
        FROM public.interface_files
        WHERE file_name = ?
        ORDER BY created_datetime, interface_file_id
        """;

    private static final String DELETE_BY_FILE_NAME = """
        DELETE FROM public.interface_files
        WHERE file_name = ?
        """;

    private final DatabaseClient databaseClient = new DatabaseClient();

    /**
     * Finds all interface-file records with the supplied filename.
     *
     * @param fileName interface filename to find.
     * @return matching interface-file records in creation order.
     */
    public List<InterfaceFileRecord> findByFileName(String fileName) {
        return databaseClient.query(FIND_BY_FILE_NAME, resultSet -> new InterfaceFileRecord(
            resultSet.getLong("interface_file_id"),
            resultSet.getString("source"),
            resultSet.getString("target"),
            resultSet.getString("type"),
            resultSet.getString("opal_domain"),
            resultSet.getString("file_name"),
            resultSet.getObject("filestore_uuid", UUID.class),
            resultSet.getString("checksum"),
            resultSet.getString("status")
        ), fileName);
    }

    /**
     * Deletes all interface-file records with the supplied filename.
     *
     * @param fileName interface filename owned by the functional test.
     */
    public void deleteByFileName(String fileName) {
        databaseClient.update(DELETE_BY_FILE_NAME, fileName);
    }

    @Override
    public void close() {
        databaseClient.close();
    }

    /**
     * Typed projection of the interface-file fields asserted by ingestion functional tests.
     */
    public record InterfaceFileRecord(
        long id,
        String source,
        String target,
        String type,
        String domain,
        String fileName,
        UUID filestoreUuid,
        String checksum,
        String status
    ) {
    }
}
