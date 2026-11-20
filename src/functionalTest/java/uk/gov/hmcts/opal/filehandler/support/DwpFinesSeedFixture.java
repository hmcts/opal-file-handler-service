package uk.gov.hmcts.opal.filehandler.support;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.opal.filehandler.db.FinesInterfaceFileTestDatabaseClient;

/** Prepares the Fines bank mapping and APR lookup data for the DWP fixtures. */
@Slf4j
public final class DwpFinesSeedFixture {

    private static boolean prepared;

    private DwpFinesSeedFixture() {
    }

    public static synchronized void ensurePrepared() {
        if (prepared) {
            return;
        }
        try (FinesInterfaceFileTestDatabaseClient database = new FinesInterfaceFileTestDatabaseClient()) {
            database.prepareDwpSeed();
            prepared = true;
            log.info("Prepared Fines bank mapping and APR lookup data for DWP");
        }
    }
}
