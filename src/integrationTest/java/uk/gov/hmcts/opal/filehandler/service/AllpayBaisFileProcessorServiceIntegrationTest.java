package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.AllpayBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "opal.file-handler-service.file-types.allpay.sftp-username=AllPay",
    "launchdarkly.default-flag-values.allpay-file-transfer-Job=true"
})
public class AllpayBaisFileProcessorServiceIntegrationTest extends AbstractBaisFileProcessorServiceIntegrationTest {

    private static final String ALLPAY_FILE = "a121_00350005_300000";
    private static final String ALLPAY_FILE_CHECKSUM = "bbecbed9c565374b110b7113ecceae03";
    private static final String ALLPAY_FILE_RESOURCE = "bais-emulator/" + ALLPAY_FILE;
    private static final String ALLPAY_FILE_CONTAINER = "/home/AllPay/" + ALLPAY_FILE;

    @Autowired
    private AllpayBaisFileProcessorService allpayBaisFileProcessorService;

    @Autowired
    private AllpayBaisFileProcessorConfiguration allpayBaisFileProcessorConfiguration;

    @MockitoSpyBean
    private MaintenanceInterfaceFilePreprocessQueueService maintenanceQueueService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        blobServiceClient.createBlobContainerIfNotExists(allpayBaisFileProcessorConfiguration.getContainerName());
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.allpay-file-transfer-Job=true"
    })
    public class BankingInterfacesDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'release-1c-banking-interfaces' is false")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                allpayBaisFileProcessorService.run(allpayBaisFileProcessorConfiguration));

            assertThat(exception).hasMessage(FeatureFlags.RELEASE_1C_BANKING_INTERFACES + " is not enabled");
        }

    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
        "launchdarkly.default-flag-values.allpay-file-transfer-Job=false"
    })
    public class AllpayFileTransferJobDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'allpay-file-transfer-Job' is false")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                allpayBaisFileProcessorService.run(allpayBaisFileProcessorConfiguration));

            assertThat(exception).hasMessage("allpay-file-transfer-Job is not enabled");
        }

    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.allpay-file-transfer-Job=false"
    })
    public class BothFeatureFlagsDisabled {

        @Test
        @DisplayName("AC1: Both feature flags are false")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                allpayBaisFileProcessorService.run(allpayBaisFileProcessorConfiguration));

            assertThat(exception).hasMessage(FeatureFlags.RELEASE_1C_BANKING_INTERFACES + " is not enabled");
        }

    }

    @Test
    @DisplayName("AC2: An Allpay DAT file is stored and transformed to SOURCE_JSON")
    @Sql(
        scripts = "classpath:db/insertData/insert_into_business_unit_bank_account.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
        scripts = "classpath:db/deleteData/delete_from_business_unit_bank_account.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void whenAllpayDatFileIsPresentReadStoreAndTransformCorrectly() {
        String file = ALLPAY_FILE + ".dat";

        uploadResourceToSftp(ALLPAY_FILE_RESOURCE + ".dat", ALLPAY_FILE_CONTAINER + ".dat");
        allpayBaisFileProcessorService.run(allpayBaisFileProcessorConfiguration);

        InterfaceFileEntity sourceFile = assertSuccessfulInterfaceFile(
            file, ALLPAY_FILE_CHECKSUM, Interface.ALLPAY, Type.SOURCE, Domain.MAINTENANCE);
        InterfaceFileEntity sourceJsonFile = assertSuccessfulSourceJsonInterfaceFile(
            file, Interface.ALLPAY, Domain.MAINTENANCE, sourceFile.getInterfaceFileId());
        verify(maintenanceQueueService).send(sourceJsonFile.getInterfaceFileId());
        assertBlobChecksum(file, ALLPAY_FILE_CHECKSUM, allpayBaisFileProcessorConfiguration.getContainerName());
        assertNumberOfSftpFiles(allpayBaisFileProcessorConfiguration.getSftpUsername(), 0);
    }

    @ParameterizedTest
    @DisplayName("AC2: A non-DAT Allpay file is stored without being transformed to SOURCE_JSON")
    @ValueSource(strings = {".crf", ".dir", ".err", ".sta"})
    void whenNonDatAllpayFileIsPresentReadAndStoreWithoutTransforming(String fileEnding) {
        String resource = ALLPAY_FILE_RESOURCE + fileEnding;
        String container = ALLPAY_FILE_CONTAINER + fileEnding;

        uploadResourceToSftp(resource, container);
        allpayBaisFileProcessorService.run(allpayBaisFileProcessorConfiguration);

        String file = ALLPAY_FILE + fileEnding;

        assertSuccessfulInterfaceFile(
            file, ALLPAY_FILE_CHECKSUM, Interface.ALLPAY, Type.SOURCE, Domain.MAINTENANCE);
        assertThat(repository.findAll())
            .filteredOn(interfaceFile -> interfaceFile.getType() == Type.SOURCE_JSON)
            .filteredOn(interfaceFile -> file.equals(interfaceFile.getFileName()))
            .isEmpty();
        verify(maintenanceQueueService, never()).send(anyLong());
        assertBlobChecksum(file, ALLPAY_FILE_CHECKSUM, allpayBaisFileProcessorConfiguration.getContainerName());
        assertNumberOfSftpFiles(allpayBaisFileProcessorConfiguration.getSftpUsername(), 0);
    }

}
