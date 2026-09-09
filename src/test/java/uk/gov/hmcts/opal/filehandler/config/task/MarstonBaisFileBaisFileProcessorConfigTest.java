package uk.gov.hmcts.opal.filehandler.config.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import uk.gov.hmcts.opal.filehandler.config.MarstonBaisFileBaisFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.entity.Interface;

public class MarstonBaisFileBaisFileProcessorConfigTest {

    private MarstonBaisFileBaisFileProcessorConfig config;

    @BeforeEach
    void setUp() {
        config = new MarstonBaisFileBaisFileProcessorConfig();

        ReflectionTestUtils.setField(config, "containerName", "MARSTON");
        ReflectionTestUtils.setField(config, "featureFlag", "bailiffs.marston-file-transfer-job");
        ReflectionTestUtils.setField(config, "fileNameRegex", "\\d{10}dat\\d{10}\\.xml");
        ReflectionTestUtils.setField(config, "sftpUsername", "MARSTON");
        ReflectionTestUtils.setField(config, "source", Interface.MARSTON);
        ReflectionTestUtils.setField(config, "target", Interface.OPAL);

    }

    @Test
    void shouldReturnContainerName() {
        assertThat(config.getContainerName()).isEqualTo("MARSTON");
    }

    @Test
    void shouldReturnFeatureFlag() {
        assertThat(config.getFeatureFlag())
            .isEqualTo("bailiffs.marston-file-transfer-job");
    }

    @Test
    void shouldReturnCompiledFileNameRegex() {
        Pattern pattern = config.getFileNameRegex();

        assertThat(pattern.pattern())
            .isEqualTo("\\d{10}dat\\d{10}\\.xml");
    }

    @Test
    void shouldReturnSource() {
        assertThat(config.getSource()).isEqualTo(Interface.MARSTON);
    }

    @Test
    void shouldReturnTarget() {
        assertThat(config.getTarget()).isEqualTo(Interface.OPAL);
    }

    @Test
    void shouldReturnSftpUsername() {
        assertThat(config.getSftpUsername()).isEqualTo("MARSTON");
    }
}