package uk.gov.hmcts.opal.filehandler.config;

import uk.gov.hmcts.opal.filehandler.entity.Interface;

public interface FileProcessorConfiguration {

    String getFeatureFlag();

    Interface getSource();

    Interface getTarget();

}
