package uk.gov.hmcts.opal.filehandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.entity.Domain;

@Component("BTEckohReportBaisFileProcessorConfig")
@ConfigurationProperties("opal.file-handler-service.file-types.bteckoh-report")
public class BTEckohReportBaisFileProcessorConfiguration extends AbstractBaisFileProcessorConfiguration {

    @Override
    public Domain getDefaultDomain() {
        return Domain.FINES;
    }
}
