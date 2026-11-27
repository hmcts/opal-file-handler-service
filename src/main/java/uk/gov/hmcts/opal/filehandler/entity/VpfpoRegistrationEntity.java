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
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vpfpo_registrations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Setter
public class VpfpoRegistrationEntity {
    @Id
    @Column(name = "vpfpo_registration_id", nullable = false)
    private long vpfpoRegistrationId;

    @Column(name = "interface_file_id", nullable = false)
    private long interfaceFileId;

    @ColumnTransformer(write = "?::t_vpfpo_registration_status_enum")
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, columnDefinition = "t_vpfpo_registration_status_enum")
    private VpfpoRegistrationStatus registrationStatus;

    @Column(name = "acknowledged", nullable = false)
    private boolean acknowledged;

    @Column(name = "acknowledgement_file_name")
    private String acknowledgementFileName;

    @Column(name = "lja_code")
    private Integer ljaCode;

    @Column(name = "cto_code")
    private Integer ctoCode;

    @Column(name = "ticket_number")
    private String tickerNumber;

    @Column(name = "defendant_account_id")
    private Long defendantAccountId;

    @Column(name = "errors")
    @JdbcTypeCode(SqlTypes.JSON)
    private String errors;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

}
