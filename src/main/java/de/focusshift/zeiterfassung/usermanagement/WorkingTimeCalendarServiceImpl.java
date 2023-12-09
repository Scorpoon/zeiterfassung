package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.DateRange;
import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
class WorkingTimeCalendarServiceImpl implements WorkingTimeCalendarService {

    private final WorkingTimeService workingTimeService;

    WorkingTimeCalendarServiceImpl(WorkingTimeService workingTimeService) {
        this.workingTimeService = workingTimeService;
    }

    @Override
    public WorkingTimeCalendar getWorkingTimeCalender(LocalDate from, LocalDate toExclusive, UserLocalId userLocalId) {
        return getWorkingTimeCalendarForUsers(from, toExclusive, List.of(userLocalId))
            .values().stream().toList().getFirst();
    }

    @Override
    public Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimeCalendarForAllUsers(LocalDate from, LocalDate toExclusive) {
        return toWorkingTimeCalendar(from, toExclusive, workingTimeService.getAllWorkingTimesByUsers());
    }

    @Override
    public Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimeCalendarForUsers(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds) {
        return toWorkingTimeCalendar(from, toExclusive, workingTimeService.getWorkingTimesByUsers(userLocalIds));
    }

    private Map<UserIdComposite, WorkingTimeCalendar> toWorkingTimeCalendar(LocalDate from, LocalDate toExclusive, Map<UserIdComposite, List<WorkingTime>> sortedWorkingTimes) {

        final HashMap<UserIdComposite, WorkingTimeCalendar> result = new HashMap<>();

        sortedWorkingTimes.forEach((userIdComposite, workingTimes) -> {
            final WorkingTimeCalendar workingTimeCalendar = toWorkingTimeCalendar(from, toExclusive, workingTimes);
            result.put(userIdComposite, workingTimeCalendar);
        });

        return result;
    }

    private WorkingTimeCalendar toWorkingTimeCalendar(LocalDate from, LocalDate toExclusive, List<WorkingTime> sortedWorkingTimes) {

        final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = new HashMap<>();

        LocalDate nextEnd = toExclusive.minusDays(1);

        for (WorkingTime workingTime : sortedWorkingTimes.reversed()) {

            final DateRange workingTimeDateRange;
            if (workingTime.validFrom().map(date -> date.isBefore(from)).orElse(true)) {
                workingTimeDateRange = new DateRange(from, nextEnd);
            } else {
                workingTimeDateRange = new DateRange(workingTime.validFrom().get(), nextEnd);
            }

            for (LocalDate localDate : workingTimeDateRange) {
                final PlannedWorkingHours plannedWorkingHours = workingTime.getForDayOfWeek(localDate.getDayOfWeek())
                    .map(WorkDay::duration)
                    .map(PlannedWorkingHours::new)
                    .orElse(PlannedWorkingHours.ZERO);
                plannedWorkingHoursByDate.put(localDate, plannedWorkingHours);
            }

            if (workingTimeDateRange.startDate().equals(from)) {
                break;
            } else {
                nextEnd = workingTime.validFrom().map(date -> date.minusDays(1))
                    .orElseThrow(() -> new IllegalStateException("from cannot be before the very first workingTime with validFrom=null."));
            }
        }

        return new WorkingTimeCalendar(plannedWorkingHoursByDate);
    }
}
