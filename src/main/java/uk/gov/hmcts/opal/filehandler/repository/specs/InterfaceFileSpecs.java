package uk.gov.hmcts.opal.filehandler.repository.specs;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.common.repository.jpa.EntitySpecs;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity_;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;

@Component
public class InterfaceFileSpecs extends EntitySpecs<InterfaceFileEntity> {
    public Specification<InterfaceFileEntity> findBySearchCriteria(SearchInterfaceFilesDto searchDto) {
        return Specification.allOf(specificationList(
            Optional.ofNullable(searchDto.getSource()).map(InterfaceFileSpecs::equalsSource),
            Optional.ofNullable(searchDto.getTarget()).map(InterfaceFileSpecs::equalsTarget),
            Optional.ofNullable(searchDto.getType()).map(InterfaceFileSpecs::equalsType),
            Optional.ofNullable(searchDto.getDomain()).map(InterfaceFileSpecs::equalsOpalDomain),
            Optional.ofNullable(searchDto.getStatus()).map(InterfaceFileSpecs::equalsStatus),
            Optional.ofNullable(searchDto.getFromDate()).map(InterfaceFileSpecs::fromDate),
            Optional.ofNullable(searchDto.getToDate()).map(InterfaceFileSpecs::toDate)
        ));
    }

    static Specification<InterfaceFileEntity> equalsSource(Interface source) {
        return (root, query, builder)
            -> builder.equal(root.get(InterfaceFileEntity_.source), source);
    }

    static Specification<InterfaceFileEntity> equalsTarget(Interface target) {
        return (root, query, builder)
            -> builder.equal(root.get(InterfaceFileEntity_.target), target);
    }

    static Specification<InterfaceFileEntity> equalsType(Type type) {
        return (root, query, builder)
            -> builder.equal(root.get(InterfaceFileEntity_.type), type);
    }

    static Specification<InterfaceFileEntity> equalsOpalDomain(Domain domain) {
        return (root, query, builder)
            -> builder.equal(root.get(InterfaceFileEntity_.opalDomain), domain);
    }

    static Specification<InterfaceFileEntity> equalsStatus(Status status) {
        return (root, query, builder)
            -> builder.equal(root.get(InterfaceFileEntity_.status), status);
    }

    static Specification<InterfaceFileEntity> fromDate(LocalDateTime fromDate) {
        Date date = java.sql.Timestamp.valueOf(fromDate);
        return (root, query, builder)
            -> builder.greaterThanOrEqualTo(root.get(InterfaceFileEntity_.createdDatetime), date);
    }

    static Specification<InterfaceFileEntity> toDate(LocalDateTime toDate) {
        Date date = java.sql.Timestamp.valueOf(toDate);
        return (root, query, builder)
            -> builder.lessThanOrEqualTo(root.get(InterfaceFileEntity_.createdDatetime), date);
    }
}
