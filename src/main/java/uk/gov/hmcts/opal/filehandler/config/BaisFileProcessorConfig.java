package uk.gov.hmcts.opal.filehandler.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j(topic = "opal.BaisFileProcessorConfig")
@Configuration
public class BaisFileProcessorConfig {

    @Value("opal.blob-storage.container-names.BTEckoh")
    @Getter
    private static String BTEckohContainerName = "bteckoh-report";

    @Value("opal.blob-storage.container-names.CAPS")
    @Getter
    private static String CAPSContainerName = "caps-report";

    @Value("opal.blob-storage.container-names.Opal")
    @Getter
    private static String OpalContainerName = "opal-report";

}
