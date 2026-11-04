package uk.gov.hmcts.opal.filehandler.dto;

import com.azure.storage.common.implementation.connectionstring.StorageAuthenticationSettings.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileHandlerDraftAccountPayloadDTO {

    private Account account;
    private String prosecutorCode;

}
