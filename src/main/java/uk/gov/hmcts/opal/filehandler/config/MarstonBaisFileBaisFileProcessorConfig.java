package uk.gov.hmcts.opal.filehandler.config;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.opal.filehandler.entity.Interface;

@Component("marstonBaisFileBaisFileProcessorConfig")
public class MarstonBaisFileBaisFileProcessorConfig
    implements BaisFileProcessorConfiguration {

    @Value("${opal.file-handler-service.bailiffs.marston.account-name}")
    private String containerName;

    @Value("${opal.file-handler-service.bailiffs.marston.feature-flag}")
    private String featureFlag;

    @Value("${opal.file-handler-service.bailiffs.marston.file-name-regex}")
    private String fileNameRegex;

    @Value("${opal.file-handler-service.bailiffs.marston.sftp-username}")
    private String sftpUsername;

    @Value("${opal.file-handler-service.bailiffs.marston.source}")
    private Interface source;

    @Value("${opal.file-handler-service.bailiffs.marston.target}")
    private Interface target;

    @Override
    public String getContainerName() {
        return containerName;
    }

    @Override
    public String getFeatureFlag() {
        return featureFlag;
    }

    @Override
    public Pattern getFileNameRegex() {
        return Pattern.compile(fileNameRegex);
    }

    @Override
    public Interface getSource() {
        return source;
    }

    @Override
    public Interface getTarget() {
        return target;
    }

    @Override
    public String getSftpUsername() {
        return sftpUsername;
    }
}