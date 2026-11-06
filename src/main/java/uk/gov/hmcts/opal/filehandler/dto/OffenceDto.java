package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OffenceDto {

    @JsonProperty("date_of_sentence")
    private LocalDate dateOfSentence;

    @JsonProperty("imposing_court_id")
    private Integer imposingCourtId;

    @JsonProperty("offence_id")
    private Integer offenceId;

    @JsonProperty("impositions")
    private List<ImpositionDto> impositions;

}