package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.filehandler.types.FileHandlerTypes;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnforcementDto {

    @JsonProperty("result_id")
    private FileHandlerTypes resultId;

    @JsonProperty("enforcement_result_responses")
    private List<EnforcementResultResponseDto> enforcementResultResponses;
}