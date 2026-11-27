package uk.gov.hmcts.opal.filehandler.support;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.opal.filehandler.db.FinesInterfaceFileTestDatabaseClient;

/** Prepares the Fines Allpay seed once, when the first Allpay scenario starts. */
@Slf4j
public final class FinesSeedFixture {

    private static boolean prepared;

    private FinesSeedFixture() {
    }

    /** Creates absent base accounts and validates/applies the supplemental configuration before ingestion. */
    public static synchronized void ensurePrepared() {
        if (prepared) {
            return;
        }
        try (FinesInterfaceFileTestDatabaseClient database = new FinesInterfaceFileTestDatabaseClient()) {
            boolean createdBase = database.prepareAllpaySeed();
            prepared = true;
            log.info("Prepared Fines Allpay seed; base accounts created: {}", createdBase);
        }
    }
}
