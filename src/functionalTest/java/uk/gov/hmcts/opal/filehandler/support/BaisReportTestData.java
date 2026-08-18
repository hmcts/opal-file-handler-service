package uk.gov.hmcts.opal.filehandler.support;

/**
 * Stable local report definitions used by BAIS ingestion functional scenarios.
 */
public final class BaisReportTestData {

    public static final BaisReportTestConfig BTECKOH = new BaisReportTestConfig(
        "BTEckoh",
        "BTECKOH_REPORT",
        "BTEckohReport",
        "BTEckoh-report",
        "BAIS_SFTP_BTECKOH_REPORT_USERNAME",
        "bteckoh-report",
        "BTECKOH_REPORT_AZURE_STORAGE_CONTAINER",
        "BTECKOH_REPORT_FILE_TRANSFER_JOB_ENABLED",
        "2498-MCPLDB-MOJ-Payments-Report-Daily-2026-07-06-06-00-18.xlsx",
        "2498-MCPLDB-MOJ-Payments-Report-Daily-2026-07-06-06-00-18.txt",
        "d553f8f289bd08e5c513de5c000c0374",
        "test-data/bteckoh-report/bteckoh-test-file.xlsx"
    );

    public static final BaisReportTestConfig CAPS = new BaisReportTestConfig(
        "CAPS",
        "CAPS_REPORT",
        "CAPSReport",
        "CAPS-report",
        "BAIS_SFTP_CAPS_REPORT_USERNAME",
        "caps-report",
        "CAPS_REPORT_AZURE_STORAGE_CONTAINER",
        "CAPS_REPORT_FILE_TRANSFER_JOB_ENABLED",
        "CapFa.GB.20260701.173024.xml",
        "CapFa.GB.20260701.173024.txt",
        "9f5674b5b59771bffdd95f767fafd239",
        "test-data/caps-report/caps-test-file.xml"
    );

    private BaisReportTestData() {
    }

    /**
     * Resolves a report definition from its user-facing feature name.
     *
     * @param displayName name captured from a Cucumber step.
     * @return matching report definition.
     */
    public static BaisReportTestConfig forDisplayName(String displayName) {
        return switch (displayName) {
            case "BTEckoh" -> BTECKOH;
            case "CAPS" -> CAPS;
            default -> throw new IllegalArgumentException("Unsupported BAIS report: " + displayName);
        };
    }

    /**
     * Resolves a report definition from its persisted interface-file source.
     *
     * @param source source captured from a Cucumber step.
     * @return matching report definition.
     */
    public static BaisReportTestConfig forSource(String source) {
        return switch (source) {
            case "BTECKOH_REPORT" -> BTECKOH;
            case "CAPS_REPORT" -> CAPS;
            default -> throw new IllegalArgumentException("Unsupported BAIS report source: " + source);
        };
    }
}
