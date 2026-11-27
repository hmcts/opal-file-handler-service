package uk.gov.hmcts.opal.filehandler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.filehandler.dto.types.NoteType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountNoteDto {

    private Integer accountNoteSerial;
    private String accountNoteText;
    private NoteType noteType;
}