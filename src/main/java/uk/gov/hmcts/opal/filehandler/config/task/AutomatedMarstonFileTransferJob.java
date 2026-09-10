package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.MarstonBaisFileBaisFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.service.MarstonBaisFileProcessorService;

@Component
@ConditionalOnProperty(name = "opal.automated-task", havingValue = "MarstonFileTransferJob")
@Slf4j
@RequiredArgsConstructor
public class AutomatedMarstonFileTransferJob implements TaskConfiguration {

    private final MarstonBaisFileProcessorService processorService;
    private final MarstonBaisFileBaisFileProcessorConfig processorConfiguration;

    @Override
    public void run() {
        log.info("Starting automated Marston processing");

        processorService.run(processorConfiguration);

        log.info("Completed automated Marston processing");
    }
}