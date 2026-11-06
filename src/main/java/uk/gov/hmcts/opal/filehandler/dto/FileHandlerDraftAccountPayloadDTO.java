package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileHandlerDraftAccountPayloadDTO {

    @JsonProperty("account")
    private AccountDto account;

    @JsonProperty("prosecutor_code")
    private String prosecutorCode;

}