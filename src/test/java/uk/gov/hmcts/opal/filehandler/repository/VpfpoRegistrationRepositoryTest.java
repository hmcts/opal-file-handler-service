package uk.gov.hmcts.opal.filehandler.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationEntity;

class VpfpoRegistrationRepositoryTest {

    private final VpfpoRegistrationRepository repository =
        mock(VpfpoRegistrationRepository.class, CALLS_REAL_METHODS);

    @Test
    void groupsRegistrationsByFileGenerationAndStoredCtoAndLjaCodes() {
        VpfpoRegistrationEntity first = registration("vp000123_01_99990002_ack.xml", 1, 2);
        VpfpoRegistrationEntity second = registration("vp000123_01_99990002_ack.xml", 1, 2);
        second.setAcknowledged(true);
        VpfpoRegistrationEntity otherFileGeneration = registration("vp000124_01_99990002_ack.xml", 1, 2);
        VpfpoRegistrationEntity otherCto = registration("vp000123_01_99990002_ack.xml", 3, 2);
        VpfpoRegistrationEntity otherLja = registration("vp000123_01_99990002_ack.xml", 1, 4);
        doReturn(List.of(first, second, otherFileGeneration, otherCto, otherLja)).when(repository).findAll();

        var groups = repository.findAllGroupedByFileGenAndCtoCodeAndLjaCode();

        assertThat(groups).hasSize(4);
        assertThat(groups.get(new VpfpoRegistrationGroupKey("000123", 1, (short) 2)))
            .containsExactly(first, second);
        assertThat(groups.get(new VpfpoRegistrationGroupKey("000124", 1, (short) 2)))
            .containsExactly(otherFileGeneration);
        assertThat(groups.get(new VpfpoRegistrationGroupKey("000123", 3, (short) 2))).containsExactly(otherCto);
        assertThat(groups.get(new VpfpoRegistrationGroupKey("000123", 1, (short) 4))).containsExactly(otherLja);
    }

    @Test
    void returnsNoGroupsWhenThereAreNoRegistrations() {
        doReturn(List.of()).when(repository).findAll();

        assertThat(repository.findAllGroupedByFileGenAndCtoCodeAndLjaCode()).isEmpty();
    }

    @Test
    void rejectsFilenamesWithoutASixDigitFileGeneration() {
        doReturn(List.of(registration("vp123_01_99990002_ack.xml", 1, 2))).when(repository).findAll();

        assertThatThrownBy(repository::findAllGroupedByFileGenAndCtoCodeAndLjaCode)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("vp123_01_99990002_ack.xml");
    }

    private static VpfpoRegistrationEntity registration(String fileName, int ctoCode, int ljaCode) {
        return VpfpoRegistrationEntity.builder()
            .interfaceFile(InterfaceFileEntity.builder().fileName(fileName).build())
            .ctoCode(ctoCode)
            .ljaCode((short) ljaCode)
            .build();
    }
}
