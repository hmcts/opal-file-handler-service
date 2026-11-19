package uk.gov.hmcts.opal.filehandler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountNoteDto {

    private Integer accountNoteSerial;
    private String accountNoteText;
    private AccountType noteType;
}