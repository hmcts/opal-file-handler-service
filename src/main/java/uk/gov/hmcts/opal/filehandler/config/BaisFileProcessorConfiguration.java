package uk.gov.hmcts.opal.filehandler.config;

public interface BaisFileProcessorConfiguration  extends FileProcessorConfiguration {

    String getContainerName();

    String getSftpUsername();

}
