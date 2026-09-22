package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.VariantBankingFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.service.VariantBankingFileProcessorService;

@Component
@ConditionalOnExpression(
    "'${opal.automated-task}'.equals('VariantBankingFileTransferJob') "
        + "or ${opal.testing-support-endpoints.enabled:false}"
)
@Slf4j
@RequiredArgsConstructor
public class AutomatedVariantBankingFileTransferJob implements TaskConfiguration {

    private final VariantBankingFileProcessorService processorService;
    private final VariantBankingFileProcessorConfig processorConfiguration;

    @Override
    public void run() {
        log.info("Starting automated Variant Banking processing");

        processorService.run(processorConfiguration);

        log.info("Completed automated Variant Banking processing");
    }
}