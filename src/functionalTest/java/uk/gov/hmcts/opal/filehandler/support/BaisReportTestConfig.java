package uk.gov.hmcts.opal.filehandler.support;

/**
 * Report-specific settings consumed by the reusable BAIS ingestion functional-test helpers.
 *
 * @param displayName report name used in feature steps and diagnostics.
 * @param source interface-file source persisted by the application.
 * @param automatedTaskName application automated-task name.
 * @param sftpUsername BAIS SFTP user whose home contains the report.
 * @param sftpUsernameEnvironmentVariable application environment variable for the SFTP user.
 * @param blobContainerName report-specific blob container.
 * @param blobContainerEnvironmentVariable application environment variable for the blob container.
 * @param jobFlagEnvironmentVariable application environment variable enabling the report job.
 * @param fileName valid report fixture filename.
 * @param unsupportedFileName fixture filename which does not match the report configuration.
 * @param checksum expected MD5 checksum persisted by the application.
 * @param resourcePath classpath resource containing the report fixture.
 */
public record BaisReportTestConfig(
    String displayName,
    String source,
    String automatedTaskName,
    String sftpUsername,
    String sftpUsernameEnvironmentVariable,
    String blobContainerName,
    String blobContainerEnvironmentVariable,
    String jobFlagEnvironmentVariable,
    String fileName,
    String unsupportedFileName,
    String checksum,
    String resourcePath
) {
}
