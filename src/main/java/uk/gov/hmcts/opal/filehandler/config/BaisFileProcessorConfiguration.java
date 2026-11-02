package uk.gov.hmcts.opal.filehandler.config;

import uk.gov.hmcts.opal.filehandler.entity.Domain;

public interface BaisFileProcessorConfiguration extends FileProcessorConfiguration {

    String getSftpUsername();

    default Domain getDefaultDomain() {
        return null;
    }
}
