package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AliasDto {

    @JsonProperty("alias_forenames")
    private String aliasForenames;

    @JsonProperty("alias_surname")
    private String aliasSurname;

    @JsonProperty("alias_company_name")
    private String aliasCompanyName;
}