package uk.gov.hmcts.opal.filehandler.support;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.opal.filehandler.db.FinesInterfaceFileTestDatabaseClient;

/** Prepares the Fines bank mapping for the CDER, Jacobs and Marston fixtures. */
@Slf4j
public final class BailiffFinesSeedFixture {

    private static boolean prepared;

    private BailiffFinesSeedFixture() {
    }

    public static synchronized void ensurePrepared() {
        if (prepared) {
            return;
        }
        try (FinesInterfaceFileTestDatabaseClient database = new FinesInterfaceFileTestDatabaseClient()) {
            database.prepareBailiffSeed();
            prepared = true;
            log.info("Prepared Fines destination bank mapping for CDER, Jacobs and Marston");
        }
    }
}
