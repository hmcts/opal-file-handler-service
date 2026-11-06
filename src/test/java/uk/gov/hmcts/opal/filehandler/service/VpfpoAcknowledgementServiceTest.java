package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.filehandler.config.VpfpoAcknowledgementsConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationEntity;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationStatus;
import uk.gov.hmcts.opal.filehandler.repository.VpfpoRegistrationRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;

@ExtendWith(MockitoExtension.class)
class VpfpoAcknowledgementServiceTest {

    @Mock
    private VpfpoRegistrationRepository repository;

    @Spy
    private VpfpoAcknowledgementXmlService xmlService;

    @Mock
    private InterfaceFileBlobStoreService blobStore;

    private VpfpoAcknowledgementService service;

    @BeforeEach
    void setUp() {
        VpfpoAcknowledgementsConfiguration configuration = new VpfpoAcknowledgementsConfiguration();
        configuration.setContainerName("vpfpo-acknowledgements");
        service = new VpfpoAcknowledgementService(repository, xmlService, blobStore, configuration);
    }

    @Captor
    private ArgumentCaptor<byte[]> uploadedXml;

    @Captor
    private ArgumentCaptor<List<VpfpoRegistrationEntity>> groups;

    @Test
    void generatesPublishedGroupWithoutUpdatingAcknowledgementState() {
        VpfpoRegistrationEntity first = registration("000123", 1, 2);
        VpfpoRegistrationEntity second = registration("000123", 1, 2);
        when(repository.findAll()).thenReturn(List.of(first, second));

        service.generateAcknowledgements();

        verify(xmlService).generate(List.of(first, second));
        verify(blobStore).uploadXmlFile(eq("vp000123_01_99990002_ack.xml"),
            eq("vpfpo-acknowledgements"), uploadedXml.capture());
        assertThat(new String(uploadedXml.getValue(), StandardCharsets.UTF_8))
            .contains("<TicketNumber>T0100145</TicketNumber>", "<FineAccountNumber>12345</FineAccountNumber>");
        assertThat(first.getAcknowledged()).isFalse();
        assertThat(second.getAcknowledged()).isFalse();
        assertThat(first.getAcknowledgementFileName()).isNull();
        assertThat(second.getAcknowledgementFileName()).isNull();
        verify(repository, never()).saveAllAndFlush(any());
    }

    @Test
    void separatesGroupsByFileGenerationCtoAndLja() {
        VpfpoRegistrationEntity first = registration("000123", 1, 2);
        VpfpoRegistrationEntity sameGroup = registration("000123", 1, 2);
        VpfpoRegistrationEntity otherGeneration = registration("000124", 1, 2);
        VpfpoRegistrationEntity otherCto = registration("000123", 3, 2);
        VpfpoRegistrationEntity otherLja = registration("000123", 1, 4);
        when(repository.findAll()).thenReturn(List.of(first, sameGroup, otherGeneration, otherCto, otherLja));

        service.generateAcknowledgements();

        verify(xmlService, times(4)).generate(groups.capture());
        assertThat(groups.getAllValues()).containsExactlyInAnyOrder(
            List.of(first, sameGroup), List.of(otherGeneration), List.of(otherCto), List.of(otherLja));
    }

    @ParameterizedTest
    @ValueSource(shorts = {0, 4, 5})
    void waitsForUnpublishedRegistrationUpToAndIncludingFiveRetries(short retryCount) {
        VpfpoRegistrationEntity pending = registration("000123", 1, 2);
        pending.setRegistrationStatus(VpfpoRegistrationStatus.PUBLISHING_FAILED);
        pending.setRetryCount(retryCount);
        when(repository.findAll()).thenReturn(List.of(registration("000123", 1, 2), pending));

        service.generateAcknowledgements();

        verifyNoInteractions(xmlService, blobStore);
    }

    @ParameterizedTest
    @EnumSource(value = VpfpoRegistrationStatus.class, names = "PUBLISHED", mode = EnumSource.Mode.EXCLUDE)
    void excludesUnpublishedRegistrationAfterMoreThanFiveRetries(VpfpoRegistrationStatus status) {
        VpfpoRegistrationEntity exhausted = registration("000123", 1, 2);
        exhausted.setRegistrationStatus(status);
        exhausted.setRetryCount((short) 6);
        exhausted.setDefendantAccountId(null);
        VpfpoRegistrationEntity published = registration("000123", 1, 2);
        when(repository.findAll()).thenReturn(List.of(published, exhausted));

        service.generateAcknowledgements();

        verify(xmlService).generate(List.of(published));
        assertThat(exhausted.getAcknowledged()).isFalse();
        assertThat(exhausted.getAcknowledgementFileName()).isNull();
        assertThat(exhausted.getRetryCount()).isEqualTo((short) 6);
    }

    @Test
    void includesPublishedRegistrationEvenAfterMoreThanFiveRetries() {
        VpfpoRegistrationEntity published = registration("000123", 1, 2);
        published.setRetryCount((short) 6);
        when(repository.findAll()).thenReturn(List.of(published));

        service.generateAcknowledgements();

        verify(xmlService).generate(List.of(published));
    }

    @Test
    void doesNotGenerateEmptyXmlWhenAllRegistrationsHaveExhaustedRetries() {
        VpfpoRegistrationEntity exhausted = registration("000123", 1, 2);
        exhausted.setRegistrationStatus(VpfpoRegistrationStatus.EXTRACTION_FAILED);
        exhausted.setRetryCount((short) 6);
        when(repository.findAll()).thenReturn(List.of(exhausted));

        service.generateAcknowledgements();

        verifyNoInteractions(xmlService, blobStore);
    }

    @Test
    void groupsDerivedJsonFilesUsingTheirOriginalInputFilename() {
        InterfaceFileEntity source = InterfaceFileEntity.builder().fileName("vp000359_01_99990002_frc.xml").build();
        VpfpoRegistrationEntity first = registration("000123", 1, 2);
        first.getInterfaceFile().setFileName("first-derived.json");
        first.getInterfaceFile().setRelatedInterfaceFile(source);
        VpfpoRegistrationEntity second = registration("000124", 1, 2);
        second.getInterfaceFile().setFileName("second-derived.json");
        second.getInterfaceFile().setRelatedInterfaceFile(source);
        when(repository.findAll()).thenReturn(List.of(first, second));

        service.generateAcknowledgements();

        verify(xmlService).generate(List.of(first, second));
    }

    @Test
    void skipsAlreadyAcknowledgedGroup() {
        VpfpoRegistrationEntity acknowledged = registration("000123", 1, 2);
        acknowledged.setAcknowledged(true);
        when(repository.findAll()).thenReturn(List.of(acknowledged));

        service.generateAcknowledgements();

        verifyNoInteractions(xmlService, blobStore);
    }

    @Test
    void exhaustedFailureDoesNotRegenerateAlreadyAcknowledgedSuccesses() {
        VpfpoRegistrationEntity acknowledged = registration("000123", 1, 2);
        acknowledged.setAcknowledged(true);
        VpfpoRegistrationEntity exhausted = registration("000123", 1, 2);
        exhausted.setRegistrationStatus(VpfpoRegistrationStatus.PUBLISHING_FAILED);
        exhausted.setRetryCount((short) 6);
        when(repository.findAll()).thenReturn(List.of(acknowledged, exhausted));

        service.generateAcknowledgements();

        verifyNoInteractions(xmlService, blobStore);
    }

    @Test
    void includesAllPublishedMembersWhenGroupIsPartiallyAcknowledged() {
        VpfpoRegistrationEntity acknowledged = registration("000123", 1, 2);
        acknowledged.setAcknowledged(true);
        VpfpoRegistrationEntity pending = registration("000123", 1, 2);
        when(repository.findAll()).thenReturn(List.of(acknowledged, pending));

        service.generateAcknowledgements();

        verify(xmlService).generate(List.of(acknowledged, pending));
        assertThat(acknowledged.getAcknowledged()).isTrue();
        assertThat(pending.getAcknowledged()).isFalse();
    }

    @Test
    void unreadyGroupDoesNotBlockOtherGroups() {
        VpfpoRegistrationEntity published = registration("000123", 1, 2);
        VpfpoRegistrationEntity pending = registration("000124", 1, 2);
        pending.setRegistrationStatus(VpfpoRegistrationStatus.SUBMITTED);
        when(repository.findAll()).thenReturn(List.of(published, pending));

        service.generateAcknowledgements();

        verify(xmlService).generate(List.of(published));
    }

    @Test
    void handlesNoRegistrations() {
        when(repository.findAll()).thenReturn(List.of());

        service.generateAcknowledgements();

        verifyNoInteractions(xmlService, blobStore);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "0001234", "ABCDEF"})
    void rejectsMalformedFileGeneration(String fileGen) {
        when(repository.findAll()).thenReturn(List.of(registration(fileGen, 1, 2)));

        assertThatThrownBy(service::generateAcknowledgements).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("vp" + fileGen + "_");
        verifyNoInteractions(xmlService, blobStore);
    }

    @Test
    void propagatesXmlValidationFailureWithoutUpdatingRegistration() {
        VpfpoRegistrationEntity published = registration("000123", 1, 2);
        published.setDefendantAccountId(null);
        when(repository.findAll()).thenReturn(List.of(published));

        assertThatThrownBy(service::generateAcknowledgements).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Incomplete or invalid VPFPO acknowledgement data");
        assertThat(published.getAcknowledged()).isFalse();
        assertThat(published.getAcknowledgementFileName()).isNull();
        verify(repository, never()).saveAllAndFlush(any());
    }

    @Test
    void propagatesUploadFailureWithoutChangingAcknowledgementState() {
        VpfpoRegistrationEntity published = registration("000123", 1, 2);
        when(repository.findAll()).thenReturn(List.of(published));
        IllegalStateException failure = new IllegalStateException("Storage unavailable");
        doThrow(failure).when(blobStore).uploadXmlFile(any(), any(), any());

        assertThatThrownBy(service::generateAcknowledgements).isSameAs(failure);

        assertThat(published.getAcknowledged()).isFalse();
        assertThat(published.getAcknowledgementFileName()).isNull();
        verify(repository, never()).saveAllAndFlush(any());
    }

    private static VpfpoRegistrationEntity registration(String fileGen, int cto, int lja) {
        return VpfpoRegistrationEntity.builder()
            .interfaceFile(InterfaceFileEntity.builder().fileName("vp" + fileGen + "_01_99990002_frc.xml").build())
            .registrationStatus(VpfpoRegistrationStatus.PUBLISHED)
            .ctoCode(cto)
            .ljaCode((short) lja)
            .ticketNumber("T0100145")
            .defendantAccountId(12345L)
            .build();
    }
}
