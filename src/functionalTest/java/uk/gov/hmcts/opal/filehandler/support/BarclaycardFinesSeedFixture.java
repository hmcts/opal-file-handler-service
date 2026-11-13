package uk.gov.hmcts.opal.filehandler.support;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.opal.filehandler.db.FinesInterfaceFileTestDatabaseClient;

/** Prepares only Barclaycard's Fines fixtures, independently of the Allpay seed. */
@Slf4j
public final class BarclaycardFinesSeedFixture {

    private static boolean prepared;

    private BarclaycardFinesSeedFixture() {
    }

    /** Creates/validates dedicated accounts, APR and bank mapping before the first Barclaycard scenario. */
    public static synchronized void ensurePrepared() {
        if (prepared) {
            return;
        }
        try (FinesInterfaceFileTestDatabaseClient database = new FinesInterfaceFileTestDatabaseClient()) {
            database.prepareBarclaycardSeed();
            prepared = true;
            log.info("Prepared dedicated Fines Barclaycard accounts, APR and bank mapping");
        }
    }
}
