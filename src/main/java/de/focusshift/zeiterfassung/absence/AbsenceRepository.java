package de.focusshift.zeiterfassung.absence;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface AbsenceRepository extends CrudRepository<AbsenceWriteEntity, Long> {

    Optional<AbsenceWriteEntity> findByTenantIdAndSourceIdAndType(String tenantId, Long sourceId, AbsenceType absenceType);

    /**
     * Finds all absences of tenantId, userId of set and intersection with interval from and toExclusive-1
     */
    List<AbsenceWriteEntity> findAllByTenantIdAndUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(
        String tenantId, List<String> userIds, Instant toExclusive, Instant from
    );

    /**
     * Finds all absences of tenantId and intersection with interval from and toExclusive-1
     */
    List<AbsenceWriteEntity> findAllByTenantIdAndStartDateLessThanAndEndDateGreaterThanEqual(
            String tenantId, Instant toExclusive, Instant from
    );

    @Modifying
    @Transactional
    int deleteByTenantIdAndSourceIdAndType(String tenantId, Long sourceId, AbsenceType type);
}
