package uk.gov.hmcts.opal.filehandler.support;

import java.time.Duration;
import java.util.List;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.db.FinesInterfaceFileTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.FinesInterfaceFileTestDatabaseClient.PreprocessingResult;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient.InterfaceFileRecord;

/** Waits for Fines to finish using source blobs before removing scenario-owned preprocessing records. */
public final class FinesPreprocessingFixture {

    private FinesPreprocessingFixture() {
    }

    /** Waits for a committed CREATED job with transformed JSON, reporting its exact source ID on timeout. */
    public static PreprocessingResult awaitResult(long sourceJsonId) {
        long deadline = System.nanoTime()
            + Duration.ofSeconds(TestEnvironment.getFinesPreprocessingTimeoutSeconds()).toNanos();
        List<PreprocessingResult> results = List.of();
        try (FinesInterfaceFileTestDatabaseClient database = new FinesInterfaceFileTestDatabaseClient()) {
            do {
                results = database.findBySourceJsonId(sourceJsonId);
                if (results.size() > 1) {
                    throw new IllegalStateException("Multiple Fines results for source JSON " + sourceJsonId
                        + ": " + results);
                }
                if (!results.isEmpty()) {
                    PreprocessingResult result = results.getFirst();
                    if (!"CREATED".equals(result.status())) {
                        throw new IllegalStateException("Unexpected Fines job status for source JSON " + sourceJsonId
                            + ": " + result);
                    }
                    if (result.transformedJsonId() != null) {
                        return result;
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted waiting for Fines source JSON " + sourceJsonId,
                        exception);
                }
            } while (System.nanoTime() < deadline);
        }
        throw new IllegalStateException("Fines preprocessing did not complete for source JSON " + sourceJsonId
            + " within " + TestEnvironment.getFinesPreprocessingTimeoutSeconds() + " seconds; results: " + results
            + ". Source records and blobs are preserved for diagnosis.");
    }

    /** Waits for queued files, then deletes their Fines jobs before file-handler deletes source data. */
    public static void cleanUp(List<InterfaceFileRecord> records) {
        List<InterfaceFileRecord> sourceJsonFiles = records.stream()
            .filter(record -> "SOURCE_JSON".equals(record.type())).toList();
        if (sourceJsonFiles.isEmpty()) {
            if (records.stream().anyMatch(record -> "SOURCE".equals(record.type())
                && "SUCCESS".equals(record.status()))) {
                throw new IllegalStateException("Preserving successful banking source data without SOURCE_JSON; "
                    + "extraction may still be running. Records: " + records);
            }
            return;
        }
        for (InterfaceFileRecord sourceJson : sourceJsonFiles) {
            if ("SUCCESS".equals(sourceJson.status())) {
                awaitResult(sourceJson.id());
            }
        }
        try (FinesInterfaceFileTestDatabaseClient database = new FinesInterfaceFileTestDatabaseClient()) {
            for (InterfaceFileRecord sourceJson : sourceJsonFiles) {
                database.deleteBySourceJsonId(sourceJson.id());
            }
        }
    }
}
