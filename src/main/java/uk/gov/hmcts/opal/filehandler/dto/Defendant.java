package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Defendant {

    @JsonProperty("defendant_account_id")
    private String defendantAccountId;

    @JsonProperty("organisation")
    private boolean organisation;

    @JsonProperty("organisation_name")
    private String organisationName;

    @JsonProperty("firstnames")
    private String firstnames;

    @JsonProperty("surname")
    private String surname;
}