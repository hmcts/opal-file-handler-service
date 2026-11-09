package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.filehandler.types.FileHandlerTypes.EnforcementResultId;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnforcementDto {

    @JsonProperty("result_id")
    private EnforcementResultId resultId;

    @JsonProperty("enforcement_result_responses")
    private List<EnforcementResultResponseDto> enforcementResultResponses;
}