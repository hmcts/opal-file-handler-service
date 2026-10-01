package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.DWPBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.config.VariantBankingFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import org.springframework.core.io.ClassPathResource;
import uk.gov.hmcts.opal.filehandler.repository.BusinessUnitBankAccountRepository;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;
import uk.gov.hmcts.opal.filehandler.testdata.BusinessUnitBankAccountEntityTestData;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraEpic;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraStory;

import static org.mockito.Mockito.verify;


@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "launchdarkly.default-flag-values.variant-banking=true",
    "launchdarkly.default-flag-values.dwp-file-transfer-job=true",
    "opal.file-handler-service.file-types.dwp.sftp-username=DWP"
})
@DisplayName("Variant Banking File Processor Service Integration Tests")
class VariantBankingFileProcessorServiceIntegrationTest
    extends AbstractBaisFileProcessorServiceIntegrationTest {

    @Autowired
    private VariantBankingFileProcessorService service;

    @Autowired
    private VariantBankingFileProcessorConfig config;

    @Autowired
    DWPBaisFileProcessorService dwpBaisFileProcessorService;

    @Autowired
    DWPBaisFileProcessorConfiguration dwpConfig;

    @MockitoBean
    private MaintenanceInterfaceFilePreprocessQueueService maintenanceQueueService;

    @Autowired
    BusinessUnitBankAccountRepository businessUnitBankAccountRepository;

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
        @JiraStory("PO-8744")
        @JiraEpic("PO-3952")
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
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
        "launchdarkly.default-flag-values.variant-banking=false"
    })
    class VariantBankingDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'variant-banking' is false")
        @JiraStory("PO-8744")
        @JiraEpic("PO-3952")
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
        @JiraStory("PO-8744")
        @JiraEpic("PO-3952")
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
    @JiraStory("PO-8744")
    @JiraEpic("PO-3952")
    void shouldProcessUploadedFile() throws Exception {

        byte[] fileBytes = new ClassPathResource("bais-emulator/a121_240101VB001_01.dat")
            .getInputStream().readAllBytes();
        service.processUploadedFile(config,"a121_240101VB001_01.dat", fileBytes);

        InterfaceFileEntity sourceJsonEntity = assertSuccessfulInterfaceFileByFileName(
                "a121_240101VB001_01.dat",
                Interface.VARIANT_BANKING,
                Type.SOURCE_JSON,
                Domain.MAINTENANCE
            );

        assertSuccessfulSourceJsonInterfaceFile(
            sourceJsonEntity.getFileName(),
            Interface.VARIANT_BANKING,
            Domain.MAINTENANCE,
            sourceJsonEntity.getRelatedInterfaceFile().getInterfaceFileId()
        );

        InterfaceFileEntity entity = assertSuccessfulInterfaceFile(
            sourceJsonEntity.getFileName(),
            sourceJsonEntity.getChecksum(),
            Interface.VARIANT_BANKING,
            Type.SOURCE_JSON,
            Domain.MAINTENANCE
        );

        verify(maintenanceQueueService, times(1)).send(eq(entity.getInterfaceFileId()));
        assertBlobChecksum(entity.getFileName(), entity.getChecksum(), config.getContainerName());

    }

    @Test
    @DisplayName("AC3: Duplicate detection uses filename only when checksum differs")
    @JiraStory("PO-8744")
    @JiraEpic("PO-3952")
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
    @DisplayName("AC4: Existing processors are unaffected by non Variant Banking changes")
    @JiraStory("PO-8744")
    @JiraEpic("PO-3952")
    void shouldProcessExistingInterfaceFilesUnchanged() {

        businessUnitbanking();
        blobServiceClient.createBlobContainerIfNotExists(dwpConfig.getContainerName());

        String file = "0000015232_dat_0000000612_08011008_111355.txt";
        String checksum = "bdbbd6c4e0daba273d9387f466acb6b9";
        String resource = "bais-emulator/" + file;
        String container = "/home/DWP/" + file;

        uploadResourceToSftp(resource, container);
        businessUnitBankAccountRepository.findAll()
            .forEach(System.out::println);
        dwpBaisFileProcessorService.run(dwpConfig);

        InterfaceFileEntity parentEntity =
            assertSuccessfulInterfaceFile(
                file,
                checksum,
                Interface.DWP,
                Type.SOURCE,
                Domain.MAINTENANCE
            );

        InterfaceFileEntity childEntity = assertSuccessfulSourceJsonInterfaceFile(
                file,
                Interface.DWP,
                Domain.MAINTENANCE,
                parentEntity.getInterfaceFileId()
            );
        assertThat(childEntity.getStatus()).isEqualTo(Status.SUCCESS);

    }

    @Test
    @DisplayName("AC5: Duplicate detection remains specific to Variant Banking")
    @JiraStory("PO-8744")
    @JiraEpic("PO-3952")
    void shouldOnlyApplyFilenameOnlyDuplicateCheckToVariantBanking() {

        businessUnitbanking();

        blobServiceClient.createBlobContainerIfNotExists(
            dwpConfig.getContainerName()
        );

        String file = "0000015232_dat_0000000612_08011008_111355.txt";
        String checksum = "bdbbd6c4e0daba273d9387f466acb6b9";
        String resource = "bais-emulator/" + file;
        String container = "/home/DWP/" + file;

        // First processing
        uploadResourceToSftp(resource, container);
        dwpBaisFileProcessorService.run(dwpConfig);

        InterfaceFileEntity firstFile =
            assertSuccessfulInterfaceFile(
                file,
                checksum,
                Interface.DWP,
                Type.SOURCE,
                Domain.MAINTENANCE
            );

        // Process duplicate
        uploadResourceToSftp(resource, container);
        dwpBaisFileProcessorService.run(dwpConfig);


        List<InterfaceFileEntity> dwpSourceFiles = repository.findAll()
            .stream()
            .filter(f -> f.getSource() == Interface.DWP)
            .filter(f -> f.getType() == Type.SOURCE)
            .toList();

        assertThat(dwpSourceFiles).hasSize(2);
        assertThat(firstFile).isNotNull();
        assertThat(dwpSourceFiles)
            .extracting(InterfaceFileEntity::getStatus)
            .containsExactlyInAnyOrder(
                Status.SUCCESS,
                Status.DUPLICATE
            );
    }

    private void businessUnitbanking() {
        businessUnitBankAccountEntityTestData
            .saveAndFlushBusinessUnitBankAccount(
                BusinessUnitBankAccountEntity.builder()
                    .id(2L)
                    .businessUnitCode("DW01")
                    .domain(Domain.MAINTENANCE)
                    .bankSortCode("010101")
                    .bankAccountNumber("12341234")
                    .dwpCourtCode("DWP1234567")
                    .build()
            );
    }
}
