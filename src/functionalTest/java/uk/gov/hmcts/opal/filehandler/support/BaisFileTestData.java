package uk.gov.hmcts.opal.filehandler.support;

import java.util.Map;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;

/**
 * Stable local report definitions used by BAIS ingestion functional scenarios.
 */
public final class BaisFileTestData {

    public static final String SELECTED_BAIS_FILE = "selectedBaisFile";
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
        "test-data/bteckoh-report/2498-MCPLDB-MOJ-Payments-Report-Daily-2026-04-08-06-00-14.xlsx"
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
        "test-data/caps-report/CapFa.GB.20260311.173015.xml"
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

    public static final BaisFileTestConfig DWP_APR = new BaisFileTestConfig(
        "DWP", DWP.source(), DWP.automatedTaskName(), DWP.sftpUsername(),
        DWP.sftpUsernameEnvironmentVariable(), DWP.blobContainerName(),
        DWP.blobContainerEnvironmentVariable(), DWP.jobFlagEnvironmentVariable(),
        "0000015232_dat_0000000612_08011008_111356.txt",
        "0000015232_dat_0000000612_08011008_111356.csv",
        "41102b9042c29e45a911db0d1f0ae9d1",
        "test-data/dwp/0000015232_dat_0000000612_08011008_111356.txt"
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
        "f5b277b252676554397193a58e976187",
        "test-data/bacs/a121_00000001_000001.dat"
    );

    private static final Map<String, String> ALLPAY_CHECKSUMS = Map.ofEntries(
        Map.entry("a121_00000001_000001.dat", "f5b277b252676554397193a58e976187"),
        Map.entry("a121_00000001_000002.dat", "c4e24c4a6774da6472481969455705c6"),
        Map.entry("a121_00000001_000003.dat", "aaccf5735f4acb8cd2a8ee5d93ba6a51"),
        Map.entry("a121_00000001_000004.dat", "c821a553cf85d41e37922e504d3acb59"),
        Map.entry("a121_00000001_000005.dat", "90f3a541406cd322fad69dd836c7462f"),
        Map.entry("a121_00000001_000006.dat", "6b6d7d33138c4109dad9666bfcc9908f"),
        Map.entry("a121_00000001_000007.dat", "5c7cf5af8627a5367269c0bf710f7ea1"),
        Map.entry("a121_00000001_000008.dat", "d8a84fc0516a634f6c6a04a92b7b4f57"),
        Map.entry("a121_00000001_000009.dat", "697bfe92ce979228622df71215ce932d"),
        Map.entry("a121_00000001_000010.dat", "ba7c857c55b56c7c3b01dd6f79986370"),
        Map.entry("a121_00000001_000011.dat", "0229a706d8c2425af69425f2ebc3f886"),
        Map.entry("a121_00000001_000012.dat", "8c61b6714c63e7f459312a07221992bc"),
        Map.entry("a121_00000001_000013.dat", "7520adce395964fb5bcdd1bec0e1a7e4"),
        Map.entry("a121_00000001_000014.dat", "a12db8feec4da7ce523a276b0a15f734"),
        Map.entry("a121_00000001_000015.dat", "2f5620c1451a54bad4bf446d5ebe7430"),
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

    /** Returns a supported named fixture for the requested interface. */
    public static BaisFileTestConfig namedFile(String displayName, String fileName) {
        if ("Allpay".equals(displayName)) {
            return allpayFile(fileName);
        }
        if ("DWP".equals(displayName) && DWP.fileName().equals(fileName)) {
            return DWP;
        }
        if ("DWP".equals(displayName) && DWP_APR.fileName().equals(fileName)) {
            return DWP_APR;
        }
        throw new IllegalArgumentException("Unsupported " + displayName + " test file: " + fileName);
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
        "a1b994a631b8b4d4b480b6b309905f65",
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
        "317090629247b298f0f175de074de949",
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
        "ffc1c3f557962f7b44bb93766f341430",
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
        "4ecc1fc96542d34f0812f01479630a37",
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
        "8034567890_dat_0987654321_20260910_143015.txt",
        "8034567890_dat_0987654321_20260910_143015.csv",
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
