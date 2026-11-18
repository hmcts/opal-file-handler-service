package uk.gov.hmcts.opal.filehandler.dto;

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


    private LocalDate dateOfSentence;
    private Integer imposingCourtId;
    private Integer offenceId;
    private List<ImpositionDto> impositions;

}