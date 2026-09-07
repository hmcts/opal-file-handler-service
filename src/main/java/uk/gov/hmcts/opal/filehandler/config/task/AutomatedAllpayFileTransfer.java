package uk.gov.hmcts.opal.filehandler.config.task;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.AllpayBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.AllpayBaisFileProcessorService;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "opal.automated-task", havingValue = "AllpayFileTransferJob")
@Slf4j
public class AutomatedAllpayFileTransfer implements ApplicationRunner {

    private final AllpayBaisFileProcessorService fileProcessorService;
    private final AllpayBaisFileProcessorConfiguration configuration;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        log.info("Starting Allpay File Transfer Job");

        fileProcessorService.run(configuration);

        log.info("Completed Allpay File Transfer Job");
    }

}
