package uk.gov.hmcts.opal.filehandler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.filehandler.dto.types.FileHandlerTypes.RecipientType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnforcementDto {
    private RecipientType resultId;
    private List<EnforcementResultResponseDto> enforcementResultResponses;
}