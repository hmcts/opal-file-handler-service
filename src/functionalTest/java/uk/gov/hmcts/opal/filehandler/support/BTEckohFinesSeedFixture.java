package uk.gov.hmcts.opal.filehandler.support;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.opal.filehandler.db.FinesInterfaceFileTestDatabaseClient;

/** Prepares only BTEckoh's Fines fixtures, independently of the Allpay seed. */
@Slf4j
public final class BTEckohFinesSeedFixture {

    private static boolean prepared;

    private BTEckohFinesSeedFixture() {
    }

    /** Creates/validates dedicated accounts, APR and bank mapping before the first BTEckoh scenario. */
    public static synchronized void ensurePrepared() {
        if (prepared) {
            return;
        }
        try (FinesInterfaceFileTestDatabaseClient database = new FinesInterfaceFileTestDatabaseClient()) {
            database.prepareBteckohSeed();
            prepared = true;
            log.info("Prepared dedicated Fines BTEckoh accounts, APR and bank mapping");
        }
    }
}
