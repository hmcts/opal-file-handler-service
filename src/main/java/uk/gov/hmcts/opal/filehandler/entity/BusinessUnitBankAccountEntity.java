package uk.gov.hmcts.opal.filehandler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.Length;

@Entity
@Table(name = "business_unit_bank_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Setter
public class BusinessUnitBankAccountEntity {

    @Id
    @Column(name = "business_unit_bank_account_id", nullable = false)
    private Long id;

    @Column(name = "business_unit_code", nullable = false)
    @Length(max = 4)
    private String businessUnitCode;

    @Column(name = "opal_domain", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @NonNull
    private Domain domain;

    @Column(name = "bank_sort_code", nullable = false)
    @Length(max = 6)
    private String bankSortCode;

    @Column(name = "bank_account_number", nullable = false)
    @Length(max = 10)
    private String bankAccountNumber;

    @Column(name = "dwp_court_code")
    @Length(max = 10)
    private String dwpCourtCode;
}