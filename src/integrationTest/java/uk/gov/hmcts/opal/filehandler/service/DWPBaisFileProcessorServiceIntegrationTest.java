package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.DWPBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;
import uk.gov.hmcts.opal.filehandler.testdata.BusinessUnitBankAccountEntityTestData;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraEpic;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraStory;

@ActiveProfiles("integration")
@Slf4j
public class DWPBaisFileProcessorServiceIntegrationTest
    extends AbstractBaisFileProcessorServiceIntegrationTest {

    private static final String CHECKSUM = "bdbbd6c4e0daba273d9387f466acb6b9";
    private static final String FILE = "0000015232_dat_0000000612_08011008_111355.txt";
    private static final String RESOURCE = "bais-emulator/" + FILE;
    private static final String CONTAINER = "/home/DWP/" + FILE;
    private static final String BUSINESS_UNIT_CODE = "DW01";

    @Autowired
    private DWPBaisFileProcessorService service;

    @Autowired
    private DWPBaisFileProcessorConfiguration config;

    @Autowired
    private BusinessUnitBankAccountEntityTestData businessUnitBankAccountEntityTestData;

    @MockitoSpyBean
    private MaintenanceInterfaceFilePreprocessQueueService maintenanceQueueService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        businessUnitBankAccountEntityTestData.clear();
        BusinessUnitBankAccountEntity bu = BusinessUnitBankAccountEntity.builder()
            .id(1L)
            .businessUnitCode(BUSINESS_UNIT_CODE)
            .domain(Domain.MAINTENANCE)
            .bankSortCode("010101")
            .bankAccountNumber("12341234")
            .dwpCourtCode("DWP1234567")
            .build();
        businessUnitBankAccountEntityTestData.saveAndFlushBusinessUnitBankAccount(bu);
        blobServiceClient.createBlobContainerIfNotExists(config.getContainerName());
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.BTEckoh-Report-file-transfer-job=true"
    })
    public class BankingInterfacesDisabled {

        @Test
        @DisplayName("Feature flag 'release-1c-banking-interfaces' is false")
        @JiraStory("PO-6436")
        @JiraEpic("PO-3497")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                service.run(config));

            assertThat(exception).hasMessage(FeatureFlags.RELEASE_1C_BANKING_INTERFACES + " is not enabled");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
        "launchdarkly.default-flag-values.dwp-file-transfer-job=false"
    })
    public class DWPFileTransferJobDisabled {

        @Test
        @DisplayName("Feature flag 'dwp-file-transfer-job' is false")
        @JiraStory("PO-6436")
        @JiraEpic("PO-3497")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                service.run(config));

            assertThat(exception).hasMessage("dwp-file-transfer-job is not enabled");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.dwp-file-transfer-job=false"
    })
    public class BothFeatureFlagsDisabled {

        @Test
        @DisplayName("Both feature flags are false")
        @JiraStory("PO-6436")
        @JiraEpic("PO-3497")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                service.run(config));

            assertThat(exception).hasMessage(FeatureFlags.RELEASE_1C_BANKING_INTERFACES + " is not enabled");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "opal.file-handler-service.file-types.dwp.sftp-username=DWP",
        "launchdarkly.default-flag-values.dwp-file-transfer-job=true",
    })
    public class FeatureOnTests {

        @Test
        @DisplayName("DWP file is present, read and stored correctly")
        @JiraStory("PO-6436")
        @JiraEpic("PO-3497")
        void dwpBaisFileProcessorServiceShouldRunSuccesfully() {
            uploadResourceToSftp(RESOURCE, CONTAINER);
            service.run(config);

            InterfaceFileEntity parentEntity = assertSuccessfulInterfaceFile(FILE, CHECKSUM, Interface.DWP,
                Type.SOURCE, Domain.MAINTENANCE);
            InterfaceFileEntity childEntity = assertSuccessfulSourceJsonInterfaceFile(FILE, Interface.DWP,
                Domain.MAINTENANCE, parentEntity.getInterfaceFileId());
            assertBlobChecksum(FILE, CHECKSUM, config.getContainerName());
            assertNumberOfSftpFiles(config.getSftpUsername(), 0);

            verify(maintenanceQueueService, times(1))
                .send(eq(childEntity.getInterfaceFileId()));
        }
    }
}
