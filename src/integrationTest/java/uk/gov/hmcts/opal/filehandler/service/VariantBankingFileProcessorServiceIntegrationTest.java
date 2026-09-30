package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.VariantBankingFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import org.springframework.core.io.ClassPathResource;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;
import uk.gov.hmcts.opal.filehandler.testdata.BusinessUnitBankAccountEntityTestData;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraEpic;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraStory;

import static org.mockito.Mockito.verify;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "launchdarkly.default-flag-values.variant-banking=true"
})
@DisplayName("Variant Banking File Processor Service Integration Tests")
class VariantBankingFileProcessorServiceIntegrationTest
    extends AbstractBaisFileProcessorServiceIntegrationTest {

    @Autowired
    private VariantBankingFileProcessorService service;

    @Autowired
    private VariantBankingFileProcessorConfig config;

    @MockitoBean
    private MaintenanceInterfaceFilePreprocessQueueService maintenanceQueueService;

    @Autowired
    private BusinessUnitBankAccountEntityTestData businessUnitBankAccountEntityTestData;



    @BeforeEach
    void setUp() {
        repository.deleteAll();
        businessUnitBankAccountEntityTestData.clear();

        BusinessUnitBankAccountEntity bu = BusinessUnitBankAccountEntity.builder()
            .id(1L)
            .businessUnitCode("001")
            .domain(Domain.MAINTENANCE)
            .bankSortCode("560033")
            .bankAccountNumber("27048527")
            .dwpCourtCode("VB001")
            .build();

        businessUnitBankAccountEntityTestData.saveAndFlushBusinessUnitBankAccount(bu);
        blobServiceClient.createBlobContainerIfNotExists(config.getContainerName());
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.variant-banking=true"
    })
    class BankingInterfacesDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'release-1c-banking-interfaces' is false")
        void bankingInterfacesIsDisabled() {

            FeatureDisabledException exception =
                assertThrows(
                    FeatureDisabledException.class,
                    () -> service.run(config)
                );

            assertThat(exception)
                .hasMessage(
                    FeatureFlags.RELEASE_1C_BANKING_INTERFACES
                        + " is not enabled"
                );
        }
    }

    @Nested
    @JiraStory("PO-8744")
    @JiraEpic("PO-3952")
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
        "launchdarkly.default-flag-values.variant-banking=false"
    })
    class VariantBankingDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'variant-banking' is false")
        void variantBankingIsDisabled() {

            FeatureDisabledException exception =
                assertThrows(
                    FeatureDisabledException.class,
                    () -> service.run(config)
                );

            assertThat(exception)
                .hasMessage("variant-banking is not enabled");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.variant-banking=false"
    })
    class BothFeatureFlagsDisabled {

        @Test
        @DisplayName("AC1: Both feature flags are false")
        void bothFeatureFlagsAreDisabled() {

            FeatureDisabledException exception =
                assertThrows(
                    FeatureDisabledException.class,
                    () -> service.run(config)
                );

            assertThat(exception)
                .hasMessage(
                    FeatureFlags.RELEASE_1C_BANKING_INTERFACES
                        + " is not enabled"
                );
        }
    }

    @Test
    @DisplayName("AC2: Uploaded file is accepted for processing")
    void shouldProcessUploadedFile() throws Exception {

        byte[] fileBytes = new ClassPathResource("bais-emulator/a121_240101VB001_01.dat")
            .getInputStream().readAllBytes();

        service.processUploadedFile(config,"a121_240101VB001_01.dat", fileBytes);

        InterfaceFileEntity entity = assertSuccessfulInterfaceFileByFileName(
            "a121_240101VB001_01.dat",
            Interface.VARIANT_BANKING,
            Type.SOURCE_JSON,
            Domain.MAINTENANCE
        );

        verify(maintenanceQueueService, times(1)).send(eq(entity.getInterfaceFileId()));

        assertBlobChecksum(entity.getFileName(), entity.getChecksum(), config.getContainerName());
    }

    @Test
    @DisplayName("AC3: Duplicate detection uses filename only when checksum differs")
    void shouldMarkDuplicateWhenFilenameMatchesAndChecksumDiffers() throws Exception {

        InterfaceFileEntity existingFile = InterfaceFileEntity.builder()
            .source(Interface.VARIANT_BANKING)
            .target(Interface.OPAL)
            .type(Type.SOURCE_JSON)
            .opalDomain(Domain.MAINTENANCE)
            .fileName("VB001.dat")
            .checksum("existing-checksum")
            .status(Status.SUCCESS)
            .createdDatetime(LocalDateTime.now())
            .build();

        repository.saveAndFlush(existingFile);

        byte[] fileBytes = "different-content".getBytes();

        service.processUploadedFile(config,"VB001.dat", fileBytes);

        InterfaceFileEntity duplicateFile =
            repository.findAll().stream()
                .filter(file -> file.getFileName().equals("VB001.dat"))
                .filter(file -> file.getStatus() == Status.DUPLICATE)
                .findFirst()
                .orElseThrow();

        assertThat(duplicateFile.getChecksum()).isNotEqualTo("existing-checksum");
        assertThat(duplicateFile.getStatus()).isEqualTo(Status.DUPLICATE);
    }

    @Test
    @DisplayName("AC4: Processed files use VARIANT_BANKING as source and OPAL as target")
    void shouldUseConfiguredSourceAndTarget() throws Exception {

        byte[] fileBytes = new ClassPathResource("bais-emulator/a121_240101VB001_01.dat").getInputStream()
            .readAllBytes();

        service.processUploadedFile(config,"a121_240101VB001_01.dat", fileBytes);

        InterfaceFileEntity entity = repository.findAll()
            .stream()
            .findFirst()
            .orElseThrow();

        assertThat(entity.getSource()).isEqualTo(Interface.VARIANT_BANKING);
        assertThat(entity.getTarget()).isEqualTo(Interface.OPAL);
    }

    @Test
    @DisplayName("AC5: Valid Variant Banking filename is accepted for processing")
    void shouldAcceptValidVariantBankingFilename() throws Exception {

        byte[] fileBytes = new ClassPathResource(
            "bais-emulator/a121_240101VB001_01.dat")
            .getInputStream()
            .readAllBytes();

        service.processUploadedFile(config,"a121_240101VB001_01.dat", fileBytes);

        InterfaceFileEntity entity = assertSuccessfulInterfaceFileByFileName(
            "a121_240101VB001_01.dat",
                Interface.VARIANT_BANKING,
                Type.SOURCE_JSON,
                Domain.MAINTENANCE
            );

        assertThat(entity.getFileName()).isEqualTo("a121_240101VB001_01.dat");
    }
}
