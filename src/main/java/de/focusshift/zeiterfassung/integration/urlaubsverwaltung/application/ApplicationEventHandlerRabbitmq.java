package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application;

import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationAllowedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCreatedFromSickNoteEventDTO;
import de.focusshift.zeiterfassung.absence.AbsenceColor;
import de.focusshift.zeiterfassung.absence.AbsenceType;
import de.focusshift.zeiterfassung.absence.AbsenceWrite;
import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application.ApplicationRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_ALLOWED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application.ApplicationRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CANCELLED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application.ApplicationRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CREATED_FROM_SICKNOTE_QUEUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class ApplicationEventHandlerRabbitmq {
    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final AbsenceWriteService absenceWriteService;

    ApplicationEventHandlerRabbitmq(AbsenceWriteService absenceWriteService) {
        this.absenceWriteService = absenceWriteService;
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_ALLOWED_QUEUE})
    void on(ApplicationAllowedEventDTO event) {

        LOG.info("Received ApplicationAllowedEvent id={} for person={} and tenantId={}",
            event.getId(), event.getPerson(), event.getTenantId());

        toAbsence(new ApplicationEventDtoAdapter(event))
            .ifPresentOrElse(
                absenceWriteService::addAbsence,
                () -> LOG.info("could not map ApplicationAllowedEvent to Absence -> could not add Absence"));
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CREATED_FROM_SICKNOTE_QUEUE})
    void on(ApplicationCreatedFromSickNoteEventDTO event) {
        LOG.info("Received ApplicationCreatedFromSicknoteEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
        toAbsence(new ApplicationEventDtoAdapter(event))
            .ifPresentOrElse(
                absenceWriteService::addAbsence,
                () -> LOG.info("could not map ApplicationCreatedFromSicknoteEvent to Absence -> could not add Absence")
            );
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CANCELLED_QUEUE})
    void on(ApplicationCancelledEventDTO event) {
        LOG.info("Received ApplicationCancelledEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
        toAbsence(new ApplicationEventDtoAdapter(event))
            .ifPresentOrElse(
                absenceWriteService::deleteAbsence,
                () -> LOG.info("could not map ApplicationCancelledEvent to Absence -> could not delete Absence")
            );
    }

    private static Optional<AbsenceWrite> toAbsence(ApplicationEventDtoAdapter event) {

        final List<LocalDate> absentWorkingDays = event.getAbsentWorkingDays().stream().sorted().toList();
        final Optional<DayLength> maybeDayLength = toDayLength(event.getPeriod().getDayLength());
        final Optional<AbsenceType> maybeAbsenceType = toAbsenceType(event.getVacationType().getCategory(), event.getVacationType().getSourceId());
        final Optional<AbsenceColor> maybeAbsenceColor = toAbsenceColor(event.getVacationType().getColor());

        if (absentWorkingDays.isEmpty() || maybeDayLength.isEmpty() || maybeAbsenceType.isEmpty() || maybeAbsenceColor.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new AbsenceWrite(
            new TenantId(event.getTenantId()),
            event.getSourceId().longValue(),
            new UserId(event.getPerson().getUsername()),
            event.getPeriod().getStartDate(),
            event.getPeriod().getEndDate(),
            maybeDayLength.get(),
            maybeAbsenceType.get(),
            maybeAbsenceColor.get()
        ));
    }

    private static Optional<DayLength> toDayLength(de.focus_shift.urlaubsverwaltung.extension.api.application.DayLength dayLength) {
        return map(dayLength.name(), DayLength::valueOf)
            .or(peek(() -> LOG.info("could not map dayLength")));
    }

    private static Optional<AbsenceType> toAbsenceType(String vacationTypeCategory, Integer sourceId) {

        if (AbsenceType.isValidVacationTypeCategory(vacationTypeCategory)) {
            LOG.info("could not map vacationTypeCategory to AbsenceType");
            return Optional.empty();
        }

        return Optional.of(new AbsenceType(vacationTypeCategory, sourceId));
    }

    private static Optional<AbsenceColor> toAbsenceColor(String vacationTypeColor) {
        return map(vacationTypeColor, AbsenceColor::valueOf)
            .or(peek(() -> LOG.info("could not map vacationTypeColor to AbsenceColor")));
    }

    private static <R, T> Optional<R> map(T t, Function<T, R> mapper) {
        try {
            return Optional.of(mapper.apply(t));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static <T> Supplier<Optional<T>> peek(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                //
            }
            return Optional.empty();
        };
    }
}
