package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.DigestUtils;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.NatWestBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.PaymentType;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.service.queue.FinesInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;
import uk.gov.hmcts.opal.filehandler.testdata.BusinessUnitBankAccountEntityTestData;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "opal.file-handler-service.file-types.natwest.sftp-username=NATWEST",
    "launchdarkly.default-flag-values.natwest-file-transfer-Job=true",
})
public class NatWestBaisFileProcessorServiceIntegrationTest extends AbstractBaisFileProcessorServiceIntegrationTest {

    private static final String NATWEST_FILE = "Y01A.CARS.#D.SBURZ38.D080426";
    private static final String NATWEST_FILE_CHECKSUM = "3e7eb40eae410fee9a8d999bdcd7c302";
    private static final String NATWEST_FILE_RESOURCE = "bais-emulator/" + NATWEST_FILE;
    private static final String NATWEST_FILE_CONTAINER = "/home/NATWEST/" + NATWEST_FILE;
    private static final String BUSINESS_UNIT_CODE = "BC12";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private NatWestBaisFileProcessorService natWestBaisFileProcessorService;

    @Autowired
    private NatWestBaisFileProcessorConfiguration natWestBaisFileProcessorConfiguration;

    @Autowired
    private BusinessUnitBankAccountEntityTestData businessUnitBankAccountEntityTestData;

    @MockitoBean
    private FinesInterfaceFilePreprocessQueueService finesQueueService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        businessUnitBankAccountEntityTestData.clear();
        businessUnitBankAccountEntityTestData.saveTypicalBusinessUnitBankAccount(1L, BUSINESS_UNIT_CODE);
        blobServiceClient.createBlobContainerIfNotExists(natWestBaisFileProcessorConfiguration.getContainerName());
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.natwest-file-transfer-Job=true"
    })
    public class BankingInterfacesDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'release-1c-banking-interfaces' is false")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                natWestBaisFileProcessorService.run(natWestBaisFileProcessorConfiguration));

            assertThat(exception).hasMessage(FeatureFlags.RELEASE_1C_BANKING_INTERFACES + " is not enabled");
        }

    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
        "launchdarkly.default-flag-values.natwest-file-transfer-Job=false"
    })
    public class NatWestFileTransferJobDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'natwest-file-transfer-Job' is false")
        void natWestFileTransferJobIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                natWestBaisFileProcessorService.run(natWestBaisFileProcessorConfiguration));

            assertThat(exception).hasMessage("natwest-file-transfer-Job is not enabled");
        }

    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.natwest-file-transfer-Job=false"
    })
    public class BothFeatureFlagsDisabled {

        @Test
        @DisplayName("AC1: Both feature flags are false")
        void bothFeatureFlagsAreDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                natWestBaisFileProcessorService.run(natWestBaisFileProcessorConfiguration));

            assertThat(exception).hasMessage(FeatureFlags.RELEASE_1C_BANKING_INTERFACES + " is not enabled");
        }

    }

    @Test
    @DisplayName("AC2: NatWest file is present, read, extracted and stored correctly")
    void natWestBaisFileProcessorServiceShouldRunSuccessfully() throws Exception {
        uploadResourceToSftp(NATWEST_FILE_RESOURCE, NATWEST_FILE_CONTAINER);

        natWestBaisFileProcessorService.run(natWestBaisFileProcessorConfiguration);

        List<InterfaceFileEntity> entities = repository.findAll();

        assertThat(entities).hasSize(2);
        InterfaceFileEntity source = entities.stream()
            .filter(entity -> entity.getType() == Type.SOURCE)
            .findFirst()
            .orElseThrow();
        InterfaceFileEntity sourceJson = entities.stream()
            .filter(entity -> entity.getType() == Type.SOURCE_JSON)
            .findFirst()
            .orElseThrow();

        assertSourceFile(source);
        assertSourceJson(sourceJson, source);
        assertBlobChecksum(source, NATWEST_FILE_CHECKSUM);
        assertBlobChecksum(sourceJson, sourceJson.getChecksum());
        assertSourceJsonContents(sourceJson);
        assertNumberOfSftpFiles(natWestBaisFileProcessorConfiguration.getSftpUsername(), 0);
        verify(finesQueueService, times(1)).send(sourceJson.getInterfaceFileId());
    }

    private void assertSourceFile(InterfaceFileEntity source) {
        assertThat(source.getSource()).isEqualTo(Interface.NATWEST);
        assertThat(source.getTarget()).isEqualTo(Interface.OPAL);
        assertThat(source.getType()).isEqualTo(Type.SOURCE);
        assertThat(source.getOpalDomain()).isEqualTo(Domain.FINES);
        assertThat(source.getFileName()).isEqualTo(NATWEST_FILE);
        assertThat(source.getFilestoreUuid()).isNotNull();
        assertThat(source.getChecksum()).isEqualTo(NATWEST_FILE_CHECKSUM);
        assertThat(source.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(source.getBusinessUnitCode()).containsExactly(BUSINESS_UNIT_CODE);
        assertThat(source.getPaymentType()).isNull();
        assertThat(source.getRelatedInterfaceFile()).isNull();
        assertThat(source.getErrors()).isNull();
    }

    private void assertSourceJson(InterfaceFileEntity sourceJson, InterfaceFileEntity source) {
        assertThat(sourceJson.getSource()).isEqualTo(Interface.NATWEST);
        assertThat(sourceJson.getTarget()).isEqualTo(Interface.OPAL);
        assertThat(sourceJson.getType()).isEqualTo(Type.SOURCE_JSON);
        assertThat(sourceJson.getOpalDomain()).isEqualTo(Domain.FINES);
        assertThat(sourceJson.getFileName()).isEqualTo(NATWEST_FILE);
        assertThat(sourceJson.getFilestoreUuid()).isNotNull();
        assertThat(sourceJson.getChecksum()).isNotBlank();
        assertThat(sourceJson.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(sourceJson.getBusinessUnitCode()).containsExactly(BUSINESS_UNIT_CODE);
        assertThat(sourceJson.getPaymentType()).isEqualTo(PaymentType.CASH);
        assertThat(sourceJson.getRelatedInterfaceFile().getInterfaceFileId()).isEqualTo(source.getInterfaceFileId());
        assertThat(sourceJson.getErrors()).isNull();
    }

    private void assertBlobChecksum(InterfaceFileEntity entity, String expectedChecksum) {
        BlobClient client = blobServiceClient
            .getBlobContainerClient(natWestBaisFileProcessorConfiguration.getContainerName())
            .getBlobClient(entity.getFilestoreUuid().toString());

        assertThat(client.exists()).isTrue();

        byte[] content = client.downloadContent().toBytes();
        BlobProperties properties = client.getProperties();

        assertThat(DigestUtils.md5DigestAsHex(content)).isEqualTo(expectedChecksum);
        assertThat(HexFormat.of().formatHex(properties.getContentMd5())).isEqualTo(expectedChecksum);
    }

    private void assertSourceJsonContents(InterfaceFileEntity sourceJson) throws Exception {
        BlobClient client = blobServiceClient
            .getBlobContainerClient(natWestBaisFileProcessorConfiguration.getContainerName())
            .getBlobClient(sourceJson.getFilestoreUuid().toString());

        JsonNode json = objectMapper.readTree(client.downloadContent().toBytes());

        assertThat(json.get("file_name").asText()).isEqualTo(NATWEST_FILE);
        assertThat(json.get("payment_type").asText()).isEqualTo("CASH");
        assertThat(json.at("/destination_details/bank_details/sort_code").asText()).isEqualTo("560033");
        assertThat(json.at("/destination_details/bank_details/account_number").asText()).isEqualTo("27048527");
        assertThat(json.get("transactions").size()).isEqualTo(2);
    }

}
