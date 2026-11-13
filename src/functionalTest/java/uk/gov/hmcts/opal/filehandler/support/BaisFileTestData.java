package uk.gov.hmcts.opal.filehandler.support;

import java.util.Map;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;

/**
 * Stable local report definitions used by BAIS ingestion functional scenarios.
 */
public final class BaisFileTestData {

    public static final String SELECTED_ALLPAY_FILE = "selectedAllpayFile";
    public static final String SOURCE_INTERFACE_FILE_ID = "baisSourceInterfaceFileId";

    public static final BaisFileTestConfig BTECKOH = new BaisFileTestConfig(
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

    public static final BaisFileTestConfig CAPS = new BaisFileTestConfig(
        "CAPS",
        "CAPS_REPORT",
        "CAPSReport",
        TestEnvironment.getReportSftpUsername("CAPS_REPORT", "CAPS-report"),
        "BAIS_SFTP_CAPS_REPORT_USERNAME",
        "caps-report",
        "CAPS_REPORT_AZURE_STORAGE_CONTAINER",
        "CAPS_REPORT_FILE_TRANSFER_JOB_ENABLED",
        "CapFa.GB.20260701.173024.xml",
        "CapFa.GB.20260701.173024.txt",
        "9f5674b5b59771bffdd95f767fafd239",
        "test-data/caps-report/caps-test-file.xml"
    );

    public static final BaisFileTestConfig DWP = new BaisFileTestConfig(
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

    public static final BaisFileTestConfig ALLPAY = new BaisFileTestConfig(
        "Allpay",
        "ALLPAY",
        "AllpayFileTransferJob",
        TestEnvironment.getReportSftpUsername("ALLPAY", "AllPay"),
        "BAIS_SFTP_ALLPAY_USERNAME",
        "allpay",
        "ALLPAY_AZURE_STORAGE_CONTAINER",
        "ALLPAY_FILE_TRANSFER_JOB_ENABLED",
        "a121_00350005_300000.dat",
        "a121_00350005_300000.csv",
        "598a1f625810b1517b6764f10d11ee68",
        "test-data/bacs/a121_00000001_000001.dat"
    );

    private static final Map<String, String> ALLPAY_CHECKSUMS = Map.ofEntries(
        Map.entry("a121_00000001_000001.dat", "598a1f625810b1517b6764f10d11ee68"),
        Map.entry("a121_00000001_000002.dat", "ef5f44e8f3e7ee525e3440efbc5ccd22"),
        Map.entry("a121_00000001_000003.dat", "9d7f18e77328d4805474132127f6f2c7"),
        Map.entry("a121_00000001_000004.dat", "5a798e0b8e9ade2ccdc6e763e2cd37e2"),
        Map.entry("a121_00000001_000005.dat", "17bcdc6a3b9535945d283fbe7a799c38"),
        Map.entry("a121_00000001_000006.dat", "56e931f6122b646f0d8adac051f016ec"),
        Map.entry("a121_00000001_000007.dat", "f9201056a9c2355d71c4c267912b1efc"),
        Map.entry("a121_00000001_000008.dat", "dd0cd8debb1ddc930ce8b37b7340a086"),
        Map.entry("a121_00000001_000009.dat", "574503b7ed014216e51fed5417d0054c"),
        Map.entry("a121_00000001_000010.dat", "d5f0948858fbe0d4151811b3268c0e05"),
        Map.entry("a121_00000001_000011.dat", "b85eadc6d9214b55834153109eb17bb9"),
        Map.entry("a121_00000001_000012.dat", "349816dd89fdddfce3f0779131e60912"),
        Map.entry("a121_00000001_000013.dat", "dd6f42077a59e0deff4bd9e98ace792e"),
        Map.entry("a121_00000001_000014.dat", "e97557b30e446b352d7f7481b511c376"),
        Map.entry("a121_00000001_000015.dat", "4748b31d80825f52db0e8c3cba93dd10"),
        Map.entry("a121_00000001_000016.dat", "74fe9eeec9b01a0cf750b3fdfd7e9fca")
    );

    /**
     * Returns the fixture selected by an Allpay scenario-outline example.
     *
     * @param fileName local resource name and filename to upload to BAIS.
     * @return Allpay configuration with the example's filename and expected checksum.
     */
    public static BaisFileTestConfig allpayFile(String fileName) {
        String checksum = ALLPAY_CHECKSUMS.get(fileName);
        if (checksum == null) {
            throw new IllegalArgumentException("Unsupported Allpay test file: " + fileName);
        }
        return new BaisFileTestConfig(
            ALLPAY.displayName(), ALLPAY.source(), ALLPAY.automatedTaskName(), ALLPAY.sftpUsername(),
            ALLPAY.sftpUsernameEnvironmentVariable(), ALLPAY.blobContainerName(),
            ALLPAY.blobContainerEnvironmentVariable(), ALLPAY.jobFlagEnvironmentVariable(),
            fileName, fileName.replace(".dat", ".csv"), checksum, "test-data/bacs/" + fileName
        );
    }

    public static final BaisFileTestConfig NATWEST = new BaisFileTestConfig(
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
        "20a3c953aff32e0017aa15ee89f10184",
        "test-data/bacs/natwest-unique.dat"
    );

    public static final BaisFileTestConfig BARCLAYCARD = new BaisFileTestConfig(
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
        "054a5f946f89dc18c776a12e7ad0b5dc",
        "test-data/bacs/barclaycard-unique.dat"
    );

    public static final BaisFileTestConfig BTECKOH_TRANSFER = new BaisFileTestConfig(
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
        "0b37ec09e1e2122f934c00e1e59026b3",
        "test-data/bacs/bteckoh-unique.dat"
    );

    public static final BaisFileTestConfig JACOBS = new BaisFileTestConfig(
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

    public static final BaisFileTestConfig CDER = new BaisFileTestConfig(
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

    public static final BaisFileTestConfig MARSTON = new BaisFileTestConfig(
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

    private BaisFileTestData() {
    }

    /**
     * Resolves a report definition from its user-facing feature name.
     *
     * @param displayName name captured from a Cucumber step.
     * @return matching report definition.
     */
    public static BaisFileTestConfig forDisplayName(String displayName) {
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
    public static BaisFileTestConfig forSource(String source) {
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
