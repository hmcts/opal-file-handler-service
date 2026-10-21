package uk.gov.hmcts.opal.filehandler.entity;

import uk.gov.hmcts.opal.generated.model.PaymentTypeEnumTypes;

public enum PaymentType {
    CASH,
    CHEQUE;

    public static PaymentType valueOf(PaymentTypeEnumTypes paymentType) {
        if (paymentType == null) {
            return null;
        }
        return PaymentType.valueOf(paymentType.name());
    }
}

