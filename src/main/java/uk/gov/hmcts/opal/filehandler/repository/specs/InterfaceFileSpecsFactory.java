package uk.gov.hmcts.opal.filehandler.repository.specs;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity_;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;

@Component
public class InterfaceFileSpecsFactory {

    public Specification<InterfaceFileEntity> createSearchSpecs(SearchInterfaceFilesDto searchDto) {
        List<Specification<InterfaceFileEntity>> specs = new ArrayList<>();
        if (searchDto.getSource() != null) {
            specs.add(equalsSource(searchDto.getSource()));
        }
        if (searchDto.getTarget() != null) {
            specs.add(equalsTarget(searchDto.getTarget()));
        }
        if (searchDto.getNotTarget() != null) {
            specs.add(notEqualsTarget(searchDto.getNotTarget()));
        }
        if (searchDto.getTypes() != null && !searchDto.getTypes().isEmpty()) {
            specs.add(hasTypeIn(searchDto.getTypes()));
        }
        if (searchDto.getDomain() != null) {
            specs.add(equalsOpalDomain(searchDto.getDomain()));
        }
        if (searchDto.getStatus() != null) {
            specs.add(equalsStatus(searchDto.getStatus()));
        }
        if (searchDto.getNotStatuses() != null && !searchDto.getNotStatuses().isEmpty()) {
            specs.add(hasStatusNotIn(searchDto.getNotStatuses()));
        }
        if (searchDto.getBusinessUnitCode() != null) {
            specs.add(hasBusinessUnitCode(searchDto.getBusinessUnitCode()));
        }
        if (searchDto.getFromDate() != null) {
            specs.add(fromDate(searchDto.getFromDate()));
        }
        if (searchDto.getToDate() != null) {
            specs.add(toDate(searchDto.getToDate()));
        }

        return Specification.allOf(specs);
    }

    public static Specification<InterfaceFileEntity> sourceFilesWithJsonFailuresWithinRetryLimit(
        Interface source,
        int maxSuperseded
    ) {
        return (sourceFile, query, builder) -> {
            Subquery<Long> failedExists = query.subquery(Long.class);
            Root<InterfaceFileEntity> failed = failedExists.from(InterfaceFileEntity.class);

            Subquery<Long> supersededCount = failedExists.subquery(Long.class);
            Root<InterfaceFileEntity> superseded = supersededCount.from(InterfaceFileEntity.class);

            supersededCount.select(builder.count(superseded));
            supersededCount.where(
                builder.equal(superseded.get(InterfaceFileEntity_.relatedInterfaceFile), sourceFile),
                builder.equal(superseded.get(InterfaceFileEntity_.type), Type.SOURCE_JSON),
                builder.equal(superseded.get(InterfaceFileEntity_.status), Status.FAILED_SUPERSEDED),
                builder.equal(superseded.get(InterfaceFileEntity_.fileName),
                    failed.get(InterfaceFileEntity_.fileName)),
                builder.equal(superseded.get(InterfaceFileEntity_.checksum),
                    failed.get(InterfaceFileEntity_.checksum)));

            failedExists.select(failed.get(InterfaceFileEntity_.interfaceFileId));
            failedExists.where(
                builder.equal(failed.get(InterfaceFileEntity_.relatedInterfaceFile), sourceFile),
                builder.equal(failed.get(InterfaceFileEntity_.source), source),
                builder.equal(failed.get(InterfaceFileEntity_.type), Type.SOURCE_JSON),
                builder.equal(failed.get(InterfaceFileEntity_.status), Status.FAILED),
                builder.lessThanOrEqualTo(supersededCount, (long) maxSuperseded));

            return builder.and(
                builder.equal(sourceFile.get(InterfaceFileEntity_.type), Type.SOURCE),
                builder.exists(failedExists));
        };
    }

    private static Specification<InterfaceFileEntity> equalsSource(Interface source) {
        return (root, query, builder)
            -> builder.equal(root.get(InterfaceFileEntity_.source).cast(String.class), source.toString());
    }

    private static Specification<InterfaceFileEntity> equalsTarget(Interface target) {
        return (root, query, builder)
            -> builder.equal(root.get(InterfaceFileEntity_.target).cast(String.class), target.toString());
    }

    private static Specification<InterfaceFileEntity> notEqualsTarget(Interface target) {
        return (root, query, builder)
            -> builder.notEqual(root.get(InterfaceFileEntity_.target).cast(String.class), target.toString());
    }

    private static Specification<InterfaceFileEntity> hasTypeIn(Set<Type> types) {
        return (root, query, builder)
            -> root.get(InterfaceFileEntity_.type).in(types);
    }

    private static Specification<InterfaceFileEntity> equalsOpalDomain(Domain domain) {
        return (root, query, builder)
            -> builder.equal(root.get(InterfaceFileEntity_.opalDomain).cast(String.class), domain.toString());
    }

    private static Specification<InterfaceFileEntity> equalsStatus(Status status) {
        return (root, query, builder)
            -> builder.equal(root.get(InterfaceFileEntity_.status).cast(String.class), status.toString());
    }

    private static Specification<InterfaceFileEntity> fromDate(LocalDateTime fromDate) {
        return (root, query, builder)
            -> builder.greaterThanOrEqualTo(root.get(InterfaceFileEntity_.createdDatetime), fromDate);
    }

    private static Specification<InterfaceFileEntity> toDate(LocalDateTime toDate) {
        return (root, query, builder)
            -> builder.lessThanOrEqualTo(root.get(InterfaceFileEntity_.createdDatetime), toDate);
    }

    private static Specification<InterfaceFileEntity> hasStatusNotIn(Set<Status> statuses) {
        return (root, query, builder)
            -> root.get(InterfaceFileEntity_.STATUS)
                .in(statuses)
                .not();
    }

    private static Specification<InterfaceFileEntity> hasBusinessUnitCode(String businessUnitCode) {
        return (root, query, builder) -> {
            // Note - the postgres array_position function actually returns null when the item does not exist,
            // but hibernate is wrapping the call in a "coalesce" and returning 0 instead, which works because
            // postgres arrays are not zero indexed.
            //
            // (I also tried using isMember() instead of native SQL function, but hibernate seemed to be recognising
            // the field as a string instead of a string array so it did not work.)
            Expression<Collection<String>> pathExp = root.get(InterfaceFileEntity_.BUSINESS_UNIT_CODE);
            Expression<String> valueExp = builder.literal(businessUnitCode);
            Expression<Integer> funcExp = builder.function("array_position", Integer.class, pathExp, valueExp);
            return builder.notEqual(funcExp, 0);
        };
    }
}
