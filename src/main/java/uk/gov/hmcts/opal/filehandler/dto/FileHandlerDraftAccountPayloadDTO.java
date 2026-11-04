package uk.gov.hmcts.opal.filehandler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.filehandler.entity.Account;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileHandlerDraftAccountPayloadDTO {

    private Account account;
    private String prosecutorCode;

}
