package uk.gov.hmcts.opal.filehandler.db;

import java.math.BigDecimal;
import java.util.List;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;

/** Provides source-JSON-specific access to Fines preprocessing results and test-owned cleanup. */
public class FinesInterfaceFileTestDatabaseClient implements AutoCloseable {

    private static final String FIND_RESULT = """
        SELECT f.interface_file_id, f.source_json_id, f.transformed_json_id,
               f.record_count, f.total_amount, j.interface_job_id, j.business_unit_id, j.status::text
        FROM public.interface_files f
        JOIN public.interface_jobs j USING (interface_job_id)
        WHERE f.source_json_id = ?
        """;

    private final DatabaseClient database = new DatabaseClient(
        TestEnvironment.getFinesDatabaseUrl(), TestEnvironment.getFinesDatabaseUsername(),
        TestEnvironment.getFinesDatabasePassword());

    /** Prepares the fixture under the same database lock used by its setup and cleanup scripts. */
    public boolean prepareAllpaySeed() {
        database.update("SET lock_timeout = '10s'");
        database.queryFirstColumn("SELECT pg_advisory_lock(770077, 10)");
        try {
            int accountCount = Integer.parseInt(database.queryFirstColumn("""
                SELECT count(*) FROM public.defendant_accounts
                WHERE defendant_account_id BETWEEN 88007700000001 AND 88007700000010
                """).getFirst());
            if (accountCount != 0 && accountCount != 10) {
                throw new IllegalStateException("Partial Allpay Fines base seed: expected zero or ten reserved "
                    + "accounts, found " + accountCount + ". Existing data was not replaced.");
            }
            boolean createdBase = accountCount == 0;
            if (createdBase) {
                database.executePostgresScript("db/allpay/fines/01_seed_bu77_10_accounts.sql");
            }
            // This script validates ownership of existing accounts and refuses conflicting mappings.
            database.executePostgresScript("db/allpay/fines/02_setup_preprocessing.sql");
            return createdBase;
        } finally {
            database.queryFirstColumn("SELECT pg_advisory_unlock(770077, 10)");
        }
    }

    /** Prepares only Barclaycard-owned accounts, APR and bank mapping. */
    public void prepareBarclaycardSeed() {
        database.executePostgresScript("db/barclaycard/fines/setup.sql");
    }

    /** Prepares only BTEckoh transfer's accounts, APR and bank mapping. */
    public void prepareBteckohSeed() {
        database.executePostgresScript("db/bteckoh-transfer/fines/setup.sql");
    }

    /** Returns only results linked to the precise source JSON created by a scenario. */
    public List<PreprocessingResult> findBySourceJsonId(long sourceJsonId) {
        return findResults(sourceJsonId, false);
    }

    private List<PreprocessingResult> findResults(long sourceJsonId, boolean lock) {
        return database.query(FIND_RESULT + (lock ? " FOR UPDATE OF f, j" : ""), row -> new PreprocessingResult(
            row.getLong("interface_file_id"), row.getLong("source_json_id"),
            row.getObject("transformed_json_id", Long.class), row.getObject("record_count", Short.class),
            row.getBigDecimal("total_amount"), row.getLong("interface_job_id"),
            row.getShort("business_unit_id"), row.getString("status")
        ), sourceJsonId);
    }

    /** Removes only an unprocessed, single-file job belonging to the supplied source JSON. */
    public void deleteBySourceJsonId(long sourceJsonId) {
        database.inTransaction(() -> {
            for (PreprocessingResult result : findResults(sourceJsonId, true)) {
                if (!"CREATED".equals(result.status())) {
                    throw new IllegalStateException("Preserving Fines job " + result.jobId()
                        + " because its status is " + result.status());
                }
                List<String> counts = database.queryFirstColumn(
                    "SELECT count(*) FROM public.interface_files WHERE interface_job_id = ?", result.jobId());
                if (!List.of("1").equals(counts)) {
                    throw new IllegalStateException("Preserving Fines job with other associated files: "
                        + result.jobId());
                }
                database.update("DELETE FROM public.interface_files WHERE interface_file_id = ? AND source_json_id = ?",
                    result.fileId(), sourceJsonId);
                database.update("DELETE FROM public.interface_jobs "
                    + "WHERE interface_job_id = ? AND status::text = 'CREATED'",
                    result.jobId());
            }
        });
    }

    @Override
    public void close() {
        database.close();
    }

    /** Typed state produced after Fines transforms a source JSON file. */
    public record PreprocessingResult(
        long fileId,
        long sourceJsonId,
        Long transformedJsonId,
        Short recordCount,
        BigDecimal totalAmount,
        long jobId,
        short businessUnitId,
        String status
    ) {
    }
}
