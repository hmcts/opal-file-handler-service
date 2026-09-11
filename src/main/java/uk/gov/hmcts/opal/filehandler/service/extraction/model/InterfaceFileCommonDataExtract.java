package uk.gov.hmcts.opal.filehandler.service.extraction.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import uk.gov.hmcts.opal.filehandler.entity.PaymentType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterfaceFileCommonDataExtract {

    private String fileName;
    private DestinationDetails destinationDetails;
    private PaymentType paymentType;
    private List<Transaction> transactions;
    private String dwpCourtCode;

}
