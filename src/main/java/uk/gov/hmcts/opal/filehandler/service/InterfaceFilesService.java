package uk.gov.hmcts.opal.filehandler.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.repository.specs.InterfaceFileSpecs;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;

@RequiredArgsConstructor
@Service
public class InterfaceFilesService {
    private final InterfaceFilesRepository repository;
    private final InterfaceFileSpecs specBuilder;

    @Transactional(readOnly = true)
    void searchInterfaceFiles(SearchInterfaceFilesDto request) {
        List<InterfaceFileEntity> interfacesFiles = repository.findAll(
            specBuilder.findBySearchCriteria(request),
            Sort.by(Direction.ASC, TypedPropertyPath.of(InterfaceFileEntity::getCreatedDatetime))
        );


    }
}
