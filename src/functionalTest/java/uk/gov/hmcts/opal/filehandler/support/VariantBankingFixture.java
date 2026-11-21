package uk.gov.hmcts.opal.filehandler.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import uk.gov.hmcts.opal.filehandler.blob.BlobStorageClient;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.db.DatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient;

/** Reserves filenames across workers without changing shared London reference data. */
public class VariantBankingFixture implements AutoCloseable {
    public static final String SORT_CODE = "650034";
    public static final String ACCOUNT_NUMBER = "65000034";
    public static final String BANK_NAME = "HMCTS London";
    private final DatabaseClient reservation = new DatabaseClient();
    private String fileName;

    public static BlobStorageClient blobs() {
        return new BlobStorageClient(TestEnvironment.get("VARIANT_BANKING_AZURE_STORAGE_CONTAINER")
            .orElse("variant-banking"));
    }

    public String reserve(String unit) {
        assertTrue(unit.matches("\\d{3}"), "Filename requires a three-digit business unit ID");
        for (int attempt = 0; attempt < 100; attempt++) {
            LocalDate date = LocalDate.of(2000, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(36525));
            String candidate = String.format(Locale.ROOT, "a121_%s_VB_%s_%02d.dat",
                date.format(DateTimeFormatter.ofPattern("yyMMdd")), unit,
                ThreadLocalRandom.current().nextInt(100));
            // Session advisory locks prevent two workers from claiming the same candidate before upload.
            boolean locked = reservation.queryFirstColumn(
                "SELECT pg_try_advisory_lock(8673, hashtext(?))", candidate).equals(List.of("t"));
            if (!locked) {
                continue;
            }
            try (InterfaceFileTestDatabaseClient files = new InterfaceFileTestDatabaseClient()) {
                if (files.findByFileName(candidate).isEmpty()) {
                    fileName = candidate;
                    return fileName;
                }
            }
            reservation.queryFirstColumn("SELECT pg_advisory_unlock(8673, hashtext(?))", candidate);
        }
        throw new IllegalStateException("Could not reserve a unique Variant Banking filename");
    }

    public void useFileName(String uploadedName) {
        fileName = uploadedName;
    }

    public void requireLondonUnit(short unit) {
        assertTrue(List.of((short) 65, (short) 73, (short) 77, (short) 78).contains(unit));
        assertEquals(List.of("FINES"), reservation.queryFirstColumn(
            "SELECT opal_domain::text FROM business_unit_bank_account WHERE business_unit_id = ?", unit),
            "London reference data must exist; filename numbers are IDs, not business_unit_code values");
        assertTrue(reservation.queryFirstColumn("SELECT business_unit_id FROM business_unit_bank_account "
            + "WHERE bank_sort_code = ? AND bank_account_number = ?", SORT_CODE, ACCOUNT_NUMBER).size() > 1,
            "Shared London destination bank must be ambiguous so a bank lookup cannot identify the unit");
    }

    public void requireMissingUnit(short unit) {
        assertTrue(reservation.queryFirstColumn(
            "SELECT business_unit_id FROM business_unit_bank_account WHERE business_unit_id = ?", unit).isEmpty(),
            "The negative test requires an unmapped ID; the processor has no London-only whitelist");
    }

    @Override
    public void close() {
        try {
            if (fileName != null) {
                try (InterfaceFileTestDatabaseClient files = new InterfaceFileTestDatabaseClient()) {
                    var owned = files.findByFileName(fileName);
                    // Retain inputs if a queued consumer is still pending, as in the shared banking fixture.
                    FinesPreprocessingFixture.cleanUp(owned);
                    var storage = blobs();
                    owned.stream().filter(row -> row.filestoreUuid() != null)
                        .forEach(row -> storage.deleteIfExists(row.filestoreUuid().toString()));
                    files.deleteByFileName(fileName);
                }
            }
        } finally {
            reservation.close();
        }
    }
}
