package uk.gov.hmcts.opal.filehandler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "interface_files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Setter
public class InterfaceFileEntity {

    @Id
    @Column(nullable = false)
    private Long interfaceFileId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NonNull
    private Interface source;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NonNull
    private Interface target;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NonNull
    private Type type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NonNull
    private Domain opalDomain;

    @Column(nullable = false)
    @NonNull
    private String fileName;

    @Column
    private UUID filestoreUuid;

    @Column
    private String checksum;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NonNull
    private Status status;

    @Column(nullable = false)
    @NonNull
    private Date createdDatetime;

    @Column
    private String errors;

}
