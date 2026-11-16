package uk.gov.hmcts.opal.filehandler.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.db.DatabaseClient;

/** Shared account and APR preconditions for banking-interface scenarios. */
public class FinesReferenceDataStepDef {

    @Given("the following defendant accounts exist:")
    public void ensureDefendantAccountsExist(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        assertFalse(rows.isEmpty(), "Specify at least one defendant account");
        try (DatabaseClient fines = new DatabaseClient(TestEnvironment.getFinesDatabaseUrl(),
            TestEnvironment.getFinesDatabaseUsername(), TestEnvironment.getFinesDatabasePassword())) {
            fines.inTransaction(() -> {
                for (Map<String, String> row : rows) {
                    short unitId = Short.parseShort(row.get("businessUnitId"));
                    long accountId = Long.parseLong(row.get("defendantAccountId"));
                    String accountNumber = row.get("defendantAccountNumber");
                    assertTrue(unitId > 0, "Business unit ID must be positive");
                    assertTrue(accountId > 0, "Defendant account ID must be positive");
                    assertTrue(accountNumber.matches("[0-9]{8}[a-zA-Z]"),
                        "Account number must contain eight digits followed by a letter");
                    List<String> existingIds = fines.queryFirstColumn(
                        "SELECT defendant_account_id FROM public.defendant_accounts "
                            + "WHERE business_unit_id = ? AND account_number = ?", unitId, accountNumber);
                    if (!existingIds.isEmpty()) {
                        assertEquals(List.of(Long.toString(accountId)), existingIds,
                            "Existing account number must belong to the requested defendant ID");
                        continue;
                    }
                    assertTrue(fines.queryFirstColumn(
                        "SELECT defendant_account_id FROM public.defendant_accounts WHERE defendant_account_id = ?",
                        accountId).isEmpty(), "Defendant account ID is already in use: " + accountId);
                    fines.update("INSERT INTO public.defendant_accounts "
                            + "(defendant_account_id,business_unit_id,account_number,amount_imposed,amount_paid,"
                            + "account_balance,account_status,account_type,version_number) "
                            + "VALUES (?, ?, ?, 0, 0, 0, 'L', 'Fine', 1)",
                        accountId, unitId, accountNumber);
                    assertEquals(List.of(Long.toString(accountId)), fines.queryFirstColumn(
                        "SELECT defendant_account_id FROM public.defendant_accounts "
                            + "WHERE business_unit_id = ? AND account_number = ?", unitId, accountNumber));
                }
            });
        }
    }

    @Given("the following defendant accounts have these outstanding balances:")
    public void configureOutstandingBalances(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        assertFalse(rows.isEmpty(), "Specify at least one account balance");
        try (DatabaseClient fines = new DatabaseClient(TestEnvironment.getFinesDatabaseUrl(),
            TestEnvironment.getFinesDatabaseUsername(), TestEnvironment.getFinesDatabasePassword())) {
            fines.inTransaction(() -> {
                for (Map<String, String> row : rows) {
                    short unitId = Short.parseShort(row.get("businessUnitId"));
                    long accountId = Long.parseLong(row.get("defendantAccountId"));
                    long pence = Long.parseLong(row.get("outstandingBalancePence"));
                    assertTrue(pence >= 0, "Outstanding balance must be nonnegative");
                    assertEquals(List.of(Long.toString(accountId)), fines.queryFirstColumn(
                        "SELECT defendant_account_id FROM public.defendant_accounts "
                            + "WHERE defendant_account_id = ? AND business_unit_id = ?", accountId, unitId));
                    // Account summary values use pounds and represent debt as a negative balance.
                    BigDecimal balance = BigDecimal.valueOf(pence, 2).negate();
                    fines.update("UPDATE public.defendant_accounts SET amount_imposed = ?, amount_paid = 0, "
                        + "account_balance = ? WHERE defendant_account_id = ? AND business_unit_id = ?",
                        balance, balance, accountId, unitId);
                    List<BigDecimal> stored = fines.query(
                        "SELECT account_balance FROM public.defendant_accounts "
                            + "WHERE defendant_account_id = ? AND business_unit_id = ?",
                        result -> result.getBigDecimal(1), accountId, unitId);
                    assertEquals(0, balance.compareTo(stored.getFirst()), "Stored balance must match the feature");
                }
            });
        }
    }

    @Given("the following defendant account consolidations exist:")
    public void ensureConsolidationsExist(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        assertFalse(rows.isEmpty(), "Specify at least one master/child relationship");
        try (DatabaseClient fines = new DatabaseClient(TestEnvironment.getFinesDatabaseUrl(),
            TestEnvironment.getFinesDatabaseUsername(), TestEnvironment.getFinesDatabasePassword())) {
            fines.inTransaction(() -> {
                for (Map<String, String> row : rows) {
                    short unitId = Short.parseShort(row.get("businessUnitId"));
                    long masterId = Long.parseLong(row.get("masterAccountId"));
                    long childId = Long.parseLong(row.get("childAccountId"));
                    assertTrue(masterId != childId, "Master and child must be different accounts");
                    assertEquals(List.of("2"), fines.queryFirstColumn(
                        "SELECT count(*) FROM public.defendant_accounts WHERE business_unit_id = ? "
                            + "AND defendant_account_id IN (?, ?)", unitId, masterId, childId),
                        "Both accounts must already exist in the specified business unit");
                    assertEquals(List.of("L"), fines.queryFirstColumn(
                        "SELECT account_status::text FROM public.defendant_accounts WHERE defendant_account_id = ?",
                        masterId), "Master must be a live account");
                    assertTrue(fines.queryFirstColumn(
                        "SELECT defendant_transaction_id FROM public.defendant_transactions "
                            + "WHERE defendant_account_id = ? AND transaction_type::text = 'WRTOFF' "
                            + "AND text LIKE 'CONSOLIDATED%' AND associated_record_id IS DISTINCT FROM ?",
                        childId, Long.toString(masterId)).isEmpty(), "Child already has another master");
                    fines.update("UPDATE public.defendant_accounts SET consolidated_account_type = 'M' "
                        + "WHERE defendant_account_id = ?", masterId);
                    fines.update("UPDATE public.defendant_accounts SET consolidated_account_type = 'C', "
                        + "account_status = 'CS' WHERE defendant_account_id = ?", childId);
                    addConsolidationTransaction(fines, childId, masterId, "WRTOFF", "CONSOLIDATED BANKING TEST CHILD");
                    addConsolidationTransaction(
                        fines, masterId, childId, "CONSOL", "BANKING TEST MASTER CONSOLIDATION");
                    assertEquals(List.of(Long.toString(masterId)), fines.queryFirstColumn(
                        "SELECT public.f_get_master_account_id(?)", childId), "Master lookup must resolve the child");
                    assertEquals(List.of(Long.toString(childId)), fines.queryFirstColumn(
                        "SELECT child_account_id FROM public.v_consolidated_accounts "
                            + "WHERE master_account_id = ? AND child_account_id::bigint = ?", masterId, childId),
                        "Consolidated account view must expose the relationship");
                }
            });
        }
    }

    private static void addConsolidationTransaction(DatabaseClient fines, long accountId, long relatedId,
                                                     String type, String description) {
        List<String> existing = fines.queryFirstColumn(
            "SELECT defendant_transaction_id FROM public.defendant_transactions WHERE defendant_account_id = ? "
                + "AND transaction_type::text = ? AND associated_record_type::text = 'defendant_accounts' "
                + "AND associated_record_id = ?", accountId, type, Long.toString(relatedId));
        if (existing.isEmpty()) {
            fines.update("INSERT INTO public.defendant_transactions "
                    + "(defendant_transaction_id,defendant_account_id,posted_date,posted_by,transaction_type,"
                    + "text,associated_record_type,associated_record_id) "
                    + "VALUES (nextval('public.defendant_transaction_id_seq'), ?, CURRENT_TIMESTAMP, "
                    + "'BANKINGQA', ?::public.t_defendant_transaction_type_enum, ?, 'defendant_accounts', ?)",
                accountId, type, description, Long.toString(relatedId));
        }
    }

    @Given("the following Fines records do not exist, removing any existing matches:")
    public void ensureRecordsDoNotExist(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        assertFalse(rows.isEmpty(), "Specify the Fines records that must be absent");
        for (Map<String, String> row : rows) {
            assertTrue(List.of("defendantAccount", "APR").contains(row.get("recordType")),
                "Supported record types are defendantAccount and APR");
            assertTrue(Short.parseShort(row.get("businessUnitId")) > 0, "Business unit ID must be positive");
            assertFalse(row.get("reference").isBlank(), "A nonblank reference is required for scoped deletion");
        }
        try (DatabaseClient fines = new DatabaseClient(TestEnvironment.getFinesDatabaseUrl(),
            TestEnvironment.getFinesDatabaseUsername(), TestEnvironment.getFinesDatabasePassword())) {
            fines.inTransaction(() -> {
                // Remove only the explicitly listed APRs before deleting their possible target accounts.
                for (Map<String, String> row : rows) {
                    if ("APR".equals(row.get("recordType"))) {
                        fines.update("DELETE FROM public.alternate_payment_references "
                                + "WHERE business_unit_id = ? AND apr_text = ?",
                            Short.parseShort(row.get("businessUnitId")), row.get("reference"));
                    }
                }
                for (Map<String, String> row : rows) {
                    if ("defendantAccount".equals(row.get("recordType"))) {
                        // Foreign keys intentionally prevent deleting accounts with other dependent data.
                        fines.update("DELETE FROM public.defendant_accounts "
                                + "WHERE business_unit_id = ? AND account_number = ?",
                            Short.parseShort(row.get("businessUnitId")), row.get("reference"));
                    }
                }
                for (Map<String, String> row : rows) {
                    String query = "APR".equals(row.get("recordType"))
                        ? "SELECT alternate_payment_reference_id FROM public.alternate_payment_references "
                            + "WHERE business_unit_id = ? AND apr_text = ?"
                        : "SELECT defendant_account_id FROM public.defendant_accounts "
                            + "WHERE business_unit_id = ? AND account_number = ?";
                    assertTrue(fines.queryFirstColumn(query, Short.parseShort(row.get("businessUnitId")),
                        row.get("reference")).isEmpty(), "Fines record still exists: " + row);
                }
            });
        }
    }
}
