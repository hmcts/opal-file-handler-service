package uk.gov.hmcts.opal.filehandler.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractBaisFileProcessorConfiguration extends AbstractFileProcessorConfiguration
    implements BaisFileProcessorConfiguration {

    private String sftpUsername;

}
