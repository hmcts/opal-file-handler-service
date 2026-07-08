package uk.gov.hmcts.opal.filehandler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "interface_files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Setter
public class InterfaceFilesEntity {

    @Id
    @Column(nullable = false)
    private Long interfaceFileId;

    @Column(nullable = false)
    private long interfaceJobId;

    @Column(nullable = false)
    @NonNull
    private String fileName;

    @Column
    private Date createdDatetime;

    @Column
    private String records;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterfaceFileSource source;

    @Column(nullable = false)
    private boolean overrideInhibits;

    @Column
    private short recordCount;

    @Column
    private double totalAmount;

}
