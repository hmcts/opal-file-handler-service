package uk.gov.hmcts.opal.filehandler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.Length;

@Entity
@Table(name = "vpfpo_registrations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Setter
public class VpfpoRegistrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vpfpo_registration_id_seq_generator")
    @SequenceGenerator(name = "vpfpo_registration_id_seq_generator", sequenceName = "vpfpo_registration_id_seq",
        allocationSize = 1)
    @Column(nullable = false)
    private Long vpfpoRegistrationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interface_file_id", nullable = false)
    @NotNull
    private InterfaceFileEntity interfaceFile;

    @Column(nullable = false, columnDefinition = "t_vpfpo_registration_status_enum")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @NotNull
    private VpfpoRegistrationStatus registrationStatus;

    @Column(nullable = false)
    @NotNull
    @Builder.Default
    private Boolean acknowledged = false;

    @Column(length = 200)
    @Length(max = 200)
    private String acknowledgementFileName;

    @Column
    private Short ljaCode;

    @Column
    private Integer ctoCode;

    @Column(length = 16)
    @Length(max = 16)
    private String ticketNumber;

    @Column
    private Long defendantAccountId;

    @Column(columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private String errors;

    @Column(nullable = false)
    @NotNull
    @Builder.Default
    private Short retryCount = 0;
}
