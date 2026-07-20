package uk.gov.hmcts.opal.filehandler.repository.specs;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
        if (searchDto.getType() != null) {
            specs.add(equalsType(searchDto.getType()));
        }
        if (searchDto.getDomain() != null) {
            specs.add(equalsOpalDomain(searchDto.getDomain()));
        }
        if (searchDto.getStatus() != null) {
            specs.add(equalsStatus(searchDto.getStatus()));
        }
        if (searchDto.getFromDate() != null) {
            specs.add(fromDate(searchDto.getToDate()));
        }
        if (searchDto.getToDate() != null) {
            specs.add(toDate(searchDto.getToDate()));
        }

        return Specification.allOf(specs);
    }

    private static Specification<InterfaceFileEntity> equalsSource(Interface source) {
        return (root, query, builder)
            -> builder.equal(root.get(InterfaceFileEntity_.source).cast(String.class), source.toString());
    }

    private static Specification<InterfaceFileEntity> equalsTarget(Interface target) {
        return (root, query, builder)
            -> builder.equal(root.get(InterfaceFileEntity_.target).cast(String.class), target.toString());
    }

    private static Specification<InterfaceFileEntity> equalsType(Type type) {
        return (root, query, builder)
            -> builder.equal(root.get(InterfaceFileEntity_.type).cast(String.class), type.toString());
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
        Date date = java.sql.Timestamp.valueOf(fromDate);
        return (root, query, builder)
            -> builder.greaterThanOrEqualTo(root.get(InterfaceFileEntity_.createdDatetime), date);
    }

    private static Specification<InterfaceFileEntity> toDate(LocalDateTime toDate) {
        Date date = java.sql.Timestamp.valueOf(toDate);
        return (root, query, builder)
            -> builder.lessThanOrEqualTo(root.get(InterfaceFileEntity_.createdDatetime), date);
    }
}
