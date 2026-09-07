package uk.gov.hmcts.opal.filehandler.config;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.opal.filehandler.entity.Interface;
@Component("marstonBaisFileBaisFileProcessorConfig")
@ConfigurationProperties(prefix = "opal.file-handler-service.bailiffs.marston")
public class MarstonBaisFileBaisFileProcessorConfig
    implements BaisFileProcessorConfiguration {

    private String containerName;
    private String featureFlag;
    private String fileNameRegex;
    private String sftpUsername;
    private Interface source;
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

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public void setFeatureFlag(String featureFlag) {
        this.featureFlag = featureFlag;
    }

    public void setFileNameRegex(String fileNameRegex) {
        this.fileNameRegex = fileNameRegex;
    }

    public void setSftpUsername(String sftpUsername) {
        this.sftpUsername = sftpUsername;
    }

    public void setSource(Interface source) {
        this.source = source;
    }

    public void setTarget(Interface target) {
        this.target = target;
    }
}