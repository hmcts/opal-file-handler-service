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
        TestEnvironment.getReportSftpUsername("BTECKOH_REPORT", "BTEckoh-report"),
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
        "DWP",
        "DWP",
        "DWPFileTransferJob",
        TestEnvironment.getReportSftpUsername("DWP", "DWP"),
        "BAIS_SFTP_DWP_USERNAME",
        "dwp",
        "DWP_AZURE_STORAGE_CONTAINER",
        "DWP_FILE_TRANSFER_JOB_ENABLED",
        "0000015232_dat_0000000612_08011008_111355.txt",
        "0000015232_dat_0000000612_08011008_111355.csv",
        "bdbbd6c4e0daba273d9387f466acb6b9",
        "test-data/dwp/0000015232_dat_0000000612_08011008_111355.txt"
    );

    public static final BaisReportTestConfig ALLPAY = new BaisReportTestConfig(
        "Allpay",
        "ALLPAY",
        "AllpayFileTransferJob",
        TestEnvironment.getReportSftpUsername("ALLPAY", "AllPay"),
        "BAIS_SFTP_ALLPAY_USERNAME",
        "allpay",
        "ALLPAY_AZURE_STORAGE_CONTAINER",
        "ALLPAY_FILE_TRANSFER_JOB_ENABLED",
        "a121_00350005_300000.dat",
        "a121_00350005_300000.err",
        "f3f29c87cf2058c337fa6f130dd66b28",
        "test-data/bacs/a121_00350005_300000.dat"
    );

    public static final BaisReportTestConfig NATWEST = new BaisReportTestConfig(
        "NatWest",
        "NATWEST",
        "NatWestFileTransferJob",
        TestEnvironment.getReportSftpUsername("NATWEST", "NATWEST"),
        "BAIS_SFTP_NATWEST_USERNAME",
        "natwest",
        "NATWEST_AZURE_STORAGE_CONTAINER",
        "NATWEST_FILE_TRANSFER_JOB_ENABLED",
        "Y01A.CARS.#D.SBURZ38.D080426",
        "Y01A.CARS.#D.SBURZ38.D080426.txt",
        "1e92f31abf52cc7310036f0d8bd094cc",
        "test-data/bacs/natwest-unique.dat"
    );

    public static final BaisReportTestConfig BARCLAYCARD = new BaisReportTestConfig(
        "Barclaycard",
        "BARCLAYCARD",
        "BarclaycardFileTransferJob",
        TestEnvironment.getReportSftpUsername("BARCLAYCARD", "BARCLAYCARD"),
        "BAIS_SFTP_BARCLAYCARD_USERNAME",
        "barclaycard",
        "BARCLAYCARD_AZURE_STORAGE_CONTAINER",
        "BARCLAYCARD_FILE_TRANSFER_JOB_ENABLED",
        "a121_00010065_317608.dat",
        "a121_00010065_317608.err",
        "88fd8d02bab170ff28058fe7e006e2f8",
        "test-data/bacs/barclaycard-unique.dat"
    );

    public static final BaisReportTestConfig BTECKOH_TRANSFER = new BaisReportTestConfig(
        "BTEckoh transfer",
        "BTECKOH",
        "BTEckohFileTransferJob",
        TestEnvironment.getReportSftpUsername("BTECKOH", "BTEckoh"),
        "BAIS_SFTP_BTECKOH_USERNAME",
        "bteckoh",
        "BTECKOH_AZURE_STORAGE_CONTAINER",
        "BTECKOH_FILE_TRANSFER_JOB_ENABLED",
        "a121_00350005_300000.dat",
        "a121_00350005_300000.err",
        "f20903378187bea34d27a65b7728ac75",
        "test-data/bacs/bteckoh-unique.dat"
    );

    public static final BaisReportTestConfig JACOBS = new BaisReportTestConfig(
        "Jacobs",
        "JACOBS",
        "JacobsFileTransferJob",
        TestEnvironment.getReportSftpUsername("JACOBS", "Jacobs"),
        "BAIS_SFTP_JACOBS_USERNAME",
        "jacobs",
        "JACOBS_AZURE_STORAGE_CONTAINER",
        "JACOBS_FILE_TRANSFER_JOB_ENABLED",
        "0000031712_dat_0000098475_20260408_103500.txt",
        "0000031712_dat_0000098475_20260408_103500.csv",
        "704540e1dd09077352c715efcd253cb2",
        "test-data/bailiffs/jacobs-unique.txt"
    );

    public static final BaisReportTestConfig CDER = new BaisReportTestConfig(
        "Cder",
        "CDER",
        "CderFileTransferJob",
        TestEnvironment.getReportSftpUsername("CDER", "CDER"),
        "BAIS_SFTP_CDER_USERNAME",
        "cder",
        "CDER_AZURE_STORAGE_CONTAINER",
        "CDER_FILE_TRANSFER_JOB_ENABLED",
        "0000031712_dat_0000098475_20260408_103500.txt",
        "0000031712_dat_0000098475_20260408_103500.csv",
        "f10a7bdc5853ca8c7583de8763b254c1",
        "test-data/bailiffs/cder-unique.txt"
    );

    public static final BaisReportTestConfig MARSTON = new BaisReportTestConfig(
        "Marston",
        "MARSTON",
        "MarstonFileTransferJob",
        TestEnvironment.getReportSftpUsername("MARSTON", "MARSTON"),
        "BAIS_SFTP_MARSTON_USERNAME",
        "marston",
        "MARSTON_AZURE_STORAGE_CONTAINER",
        "MARSTON_FILE_TRANSFER_JOB_ENABLED",
        "1234567890_dat_0987654321_20260910_143015.txt",
        "1234567890_dat_0987654321_20260910_143015.csv",
        "917c82d4d3144b3dfcdb08d7eb25e3f0",
        "test-data/bailiffs/marston-unique.txt"
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
            case "Allpay" -> ALLPAY;
            case "NatWest" -> NATWEST;
            case "Barclaycard" -> BARCLAYCARD;
            case "BTEckoh transfer" -> BTECKOH_TRANSFER;
            case "Jacobs" -> JACOBS;
            case "Cder" -> CDER;
            case "Marston" -> MARSTON;
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
            case "ALLPAY" -> ALLPAY;
            case "NATWEST" -> NATWEST;
            case "BARCLAYCARD" -> BARCLAYCARD;
            case "BTECKOH" -> BTECKOH_TRANSFER;
            case "JACOBS" -> JACOBS;
            case "CDER" -> CDER;
            case "MARSTON" -> MARSTON;
            default -> throw new IllegalArgumentException("Unsupported BAIS report source: " + source);
        };
    }
}
