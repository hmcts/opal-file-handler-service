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

/** Reserves filenames and a test-owned Fines bank mapping for Variant Banking scenarios. */
public class VariantBankingFixture implements AutoCloseable {
    public static final short BUSINESS_UNIT_ID = 65;
    public static final String SORT_CODE = "874465";
    public static final String ACCOUNT_NUMBER = "87440065";
    public static final String BANK_NAME = "HMCTS London";
    private static final String FINES_MAPPING_MARKER = "VARIANT_BANKING_FUNCTIONAL_BU65";
    private final DatabaseClient reservation = new DatabaseClient();
    private final DatabaseClient finesReservation = new DatabaseClient(TestEnvironment.getFinesDatabaseUrl(),
        TestEnvironment.getFinesDatabaseUsername(), TestEnvironment.getFinesDatabasePassword());
    private String fileName;
    private boolean finesMappingOwned;

    public static BlobStorageClient blobs() {
        return new BlobStorageClient(TestEnvironment.get("VARIANT_BANKING_AZURE_STORAGE_CONTAINER")
            .orElse("variant-banking"));
    }

    public String reserve(String unit) {
        // The service uploads SOURCE files into this interface-specific container. Other banking
        // fixtures provision their containers during setup; Variant Banking must do the same.
        blobs().createContainerIfNotExists();
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

    public void configureFinesBankMapping(short unit) {
        assertEquals(BUSINESS_UNIT_ID, unit, "Variant Banking functional coverage is scoped to BU 65");
        finesReservation.queryFirstColumn("SELECT pg_advisory_lock(8744, 65)");
        assertTrue(!finesReservation.queryFirstColumn(
            "SELECT business_unit_id FROM public.business_units WHERE business_unit_id = ?", unit).isEmpty(),
            "The Fines business unit must exist: " + unit);

        List<String> ownedRows = finesReservation.queryFirstColumn("""
            SELECT configuration_item_id
            FROM public.configuration_items
            WHERE business_unit_id = ? AND item_name = 'BANK_ACCOUNTS' AND item_value = ?
            """, unit, FINES_MAPPING_MARKER);
        if (ownedRows.isEmpty()) {
            List<String> conflictingUnits = finesReservation.queryFirstColumn("""
                SELECT DISTINCT c.business_unit_id
                FROM public.configuration_items c
                CROSS JOIN LATERAL jsonb_array_elements(
                    CASE WHEN jsonb_typeof(c.item_values::jsonb) = 'array'
                         THEN c.item_values::jsonb ELSE '[]'::jsonb END
                ) bank_account
                WHERE c.item_name = 'BANK_ACCOUNTS'
                  AND bank_account->>'sort_code' = ?
                  AND bank_account->>'account_number' = ?
                """, SORT_CODE, ACCOUNT_NUMBER);
            assertTrue(conflictingUnits.isEmpty(), "Variant test bank is already mapped to Fines units: "
                + conflictingUnits);
            finesReservation.update("""
                INSERT INTO public.configuration_items
                    (configuration_item_id, business_unit_id, item_name, item_value, item_values)
                VALUES (nextval('public.configuration_item_id_seq'), ?, 'BANK_ACCOUNTS', ?,
                    json_build_array(json_build_object(
                        'sort_code', ?::text, 'account_number', ?::text, 'name', ?::text)))
                """, unit, FINES_MAPPING_MARKER, SORT_CODE, ACCOUNT_NUMBER, BANK_NAME);
        } else {
            assertEquals(List.of("1"), finesReservation.queryFirstColumn("""
                SELECT count(*)
                FROM public.configuration_items c
                CROSS JOIN LATERAL jsonb_array_elements(c.item_values::jsonb) bank_account
                WHERE c.business_unit_id = ? AND c.item_name = 'BANK_ACCOUNTS' AND c.item_value = ?
                  AND bank_account->>'sort_code' = ? AND bank_account->>'account_number' = ?
                  AND bank_account->>'name' = ?
                """, unit, FINES_MAPPING_MARKER, SORT_CODE, ACCOUNT_NUMBER, BANK_NAME),
                "Existing Variant Banking Fines mapping differs from this fixture");
        }
        finesMappingOwned = true;
    }

    public void requireBusinessUnit65(short unit) {
        assertEquals(BUSINESS_UNIT_ID, unit);
        assertEquals(List.of("FINES"), reservation.queryFirstColumn(
            "SELECT opal_domain::text FROM business_unit_bank_account WHERE business_unit_id = ?", unit),
            "File-handler reference data must contain the London BU 65 FINES unit");
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
                    if (finesMappingOwned) {
                        finesReservation.update("DELETE FROM public.configuration_items "
                                + "WHERE business_unit_id = ? AND item_name = 'BANK_ACCOUNTS' AND item_value = ?",
                            BUSINESS_UNIT_ID, FINES_MAPPING_MARKER);
                    }
                    var storage = blobs();
                    owned.stream().filter(row -> row.filestoreUuid() != null)
                        .forEach(row -> storage.deleteIfExists(row.filestoreUuid().toString()));
                    files.deleteByFileName(fileName);
                }
            }
        } finally {
            finesReservation.close();
            reservation.close();
        }
    }
}
