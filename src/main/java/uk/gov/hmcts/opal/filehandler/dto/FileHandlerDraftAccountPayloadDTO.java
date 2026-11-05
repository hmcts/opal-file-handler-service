package uk.gov.hmcts.opal.filehandler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileHandlerDraftAccountPayloadDTO {

    private DraftAccountResponseDto account;
    private String prosecutorCode;

}
