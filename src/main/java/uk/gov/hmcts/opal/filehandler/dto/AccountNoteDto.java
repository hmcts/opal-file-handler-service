package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.filehandler.types.FileHandlerTypes;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountNoteDto {

    @JsonProperty("account_note_serial")
    private Integer accountNoteSerial;

    @JsonProperty("account_note_text")
    private String accountNoteText;

    @JsonProperty("note_type")
    private FileHandlerTypes noteType;
}