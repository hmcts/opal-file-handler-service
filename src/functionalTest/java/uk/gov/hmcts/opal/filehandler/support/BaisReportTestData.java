package uk.gov.hmcts.opal.filehandler.support;

import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;

/**
 * Stable local report definitions used by BAIS ingestion functional scenarios.
 */
public final class BaisReportTestData {

    public static final BaisReportTestConfig BTECKOH = new BaisReportTestConfig(
        "BTEckoh",
        "BTECKOH_REPORT",
        "BTEckohReport",
        TestEnvironment.getReportSftpUsername("BTECKOH", "BTEckoh-report"),
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
        TestEnvironment.getReportSftpUsername("CAPS", "CAPS-report"),
        "BAIS_SFTP_CAPS_REPORT_USERNAME",
        "caps-report",
        "CAPS_REPORT_AZURE_STORAGE_CONTAINER",
        "CAPS_REPORT_FILE_TRANSFER_JOB_ENABLED",
        "CapFa.GB.20260701.173024.xml",
        "CapFa.GB.20260701.173024.txt",
        "9f5674b5b59771bffdd95f767fafd239",
        "test-data/caps-report/caps-test-file.xml"
    );

    public static final BaisReportTestConfig DWP = new BaisReportTestConfig(
        "DWP", "DWP", "DWPFileTransferJob",
        TestEnvironment.getReportSftpUsername("DWP", "DWP"),
        "BAIS_SFTP_DWP_USERNAME", "dwp", "DWP_AZURE_STORAGE_CONTAINER",
        "DWP_FILE_TRANSFER_JOB_ENABLED",
        "0000015232_dat_0000000612_08011008_111355.txt",
        "0000015232_dat_0000000612_08011008_111355.csv",
        "bdbbd6c4e0daba273d9387f466acb6b9",
        "test-data/dwp/0000015232_dat_0000000612_08011008_111355.txt"
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
            case "DWP" -> DWP;
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
            case "DWP" -> DWP;
            default -> throw new IllegalArgumentException("Unsupported BAIS report source: " + source);
        };
    }
}
