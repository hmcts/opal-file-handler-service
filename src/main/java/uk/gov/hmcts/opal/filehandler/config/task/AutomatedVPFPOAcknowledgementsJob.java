package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression(
    "'${opal.automated-task}'.equals('VPFPOAcknowledgementsJob') or ${opal.testing-support-endpoints.enabled}")
@Slf4j
public class AutomatedVPFPOAcknowledgementsJob implements TaskConfiguration {

    @Override
    public void run() {
        log.info("Starting VPFPO Acknowledgements Job");

        // TODO

        log.info("Completed VPFPO Acknowledgements Job");
    }
}
