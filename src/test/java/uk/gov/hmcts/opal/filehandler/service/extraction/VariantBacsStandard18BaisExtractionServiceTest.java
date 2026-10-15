package uk.gov.hmcts.opal.filehandler.service.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.repository.BusinessUnitBankAccountRepository;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;

class VariantBacsStandard18BaisExtractionServiceTest {

    private static final String FILE_NAME = "a121_003500VB123_13.dat";
    private static final String INVALID_FILE_NAME = "invalid.dat";

    private final InterfaceFilesRepository repository = mock(InterfaceFilesRepository.class);
    private final BusinessUnitBankAccountRepository bubaRepository = mock(BusinessUnitBankAccountRepository.class);
    private final VariantBacsStandard18BaisExtractionService service =
        new VariantBacsStandard18BaisExtractionService(repository, bubaRepository);

    private final InterfaceFileCommonDataExtract input = mock(InterfaceFileCommonDataExtract.class);

    @Test
    void shouldReturnWhenDataIsFound() {
        when(input.getFileName()).thenReturn(FILE_NAME);
        when(bubaRepository.findByBusinessUnitCode(eq("123")))
            .thenReturn(Optional.of(mock(BusinessUnitBankAccountEntity.class)));

        BusinessUnitBankAccountEntity response = service.getBusinessUnitBankAccount(input);

        verify(bubaRepository, times(1)).findByBusinessUnitCode(eq("123"));
        assertThat(response).isNotNull();
    }

    @Test
    void shouldThrowErrorWithInvalidFilename() {
        when(input.getFileName()).thenReturn(INVALID_FILE_NAME);
        when(bubaRepository.findByBusinessUnitCode(eq("123")))
            .thenReturn(Optional.of(mock(BusinessUnitBankAccountEntity.class)));

        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> service.getBusinessUnitBankAccount(input)
        );

        assertThat(exception).hasMessage("Business unit bank account code cannot be found for file_name 'invalid.dat'");
    }

    @Test
    void shouldThrowErrorWhenBusinessUnitNotFound() {
        when(input.getFileName()).thenReturn(FILE_NAME);
        when(bubaRepository.findByBusinessUnitCode(eq("123")))
            .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> service.getBusinessUnitBankAccount(input)
        );

        assertThat(exception).hasMessage("Business unit bank account with business unit code '123' could not be "
            + "located for file_name 'a121_003500VB123_13.dat'");
    }
}
