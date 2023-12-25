package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.DateRange;
import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.publicholiday.PublicHolidayCalendar;
import de.focusshift.zeiterfassung.publicholiday.PublicHolidaysService;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import static java.util.stream.Collectors.toUnmodifiableSet;

@Component
class WorkingTimeCalendarServiceImpl implements WorkingTimeCalendarService {

    private final WorkingTimeService workingTimeService;
    private final PublicHolidaysService publicHolidaysService;

    WorkingTimeCalendarServiceImpl(WorkingTimeService workingTimeService, PublicHolidaysService publicHolidaysService) {
        this.workingTimeService = workingTimeService;
        this.publicHolidaysService = publicHolidaysService;
    }

    @Override
    public WorkingTimeCalendar getWorkingTimeCalender(LocalDate from, LocalDate toExclusive, UserLocalId userLocalId) {
        return getWorkingTimeCalendarForUsers(from, toExclusive, List.of(userLocalId))
            .values().stream().toList().getFirst();
    }

    @Override
    public Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimeCalendarForAllUsers(LocalDate from, LocalDate toExclusive) {
        return toWorkingTimeCalendar(from, toExclusive, workingTimeService.getAllWorkingTimes(from, toExclusive));
    }

    @Override
    public Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimeCalendarForUsers(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds) {
        return toWorkingTimeCalendar(from, toExclusive, workingTimeService.getWorkingTimesByUsers(from, toExclusive, userLocalIds));
    }

    private Map<UserIdComposite, WorkingTimeCalendar> toWorkingTimeCalendar(LocalDate from, LocalDate toExclusive, Map<UserIdComposite, List<WorkingTime>> sortedWorkingTimes) {

        final HashMap<UserIdComposite, WorkingTimeCalendar> result = new HashMap<>();

        final Set<FederalState> federalStates = sortedWorkingTimes.values()
            .stream()
            .flatMap(List::stream)
            .map(WorkingTime::federalState)
            .collect(toUnmodifiableSet());

        final Map<FederalState, PublicHolidayCalendar> publicHolidayCalendars =
            publicHolidaysService.getPublicHolidays(from, toExclusive, federalStates);

        final BiPredicate<LocalDate, FederalState> isPublicHoliday = (localDate, federalState) ->
            publicHolidayCalendars.containsKey(federalState) && publicHolidayCalendars.get(federalState).isPublicHoliday(localDate);

        sortedWorkingTimes.forEach((userIdComposite, workingTimes) -> {
            final WorkingTimeCalendar workingTimeCalendar = toWorkingTimeCalendar(from, toExclusive, workingTimes, isPublicHoliday);
            result.put(userIdComposite, workingTimeCalendar);
        });

        return result;
    }

    private WorkingTimeCalendar toWorkingTimeCalendar(LocalDate from, LocalDate toExclusive, List<WorkingTime> sortedWorkingTimes, BiPredicate<LocalDate, FederalState> isPublicHoliday) {

        final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = new HashMap<>();

        LocalDate nextEnd = toExclusive.minusDays(1);

        for (WorkingTime workingTime : sortedWorkingTimes) {

            final DateRange workingTimeDateRange = getDateRange(from, workingTime, nextEnd);

            for (LocalDate localDate : workingTimeDateRange) {
                if (workingTime.worksOnPublicHoliday() || !isPublicHoliday.test(localDate, workingTime.federalState())) {
                    plannedWorkingHoursByDate.put(localDate, workingTime.getForDayOfWeek(localDate.getDayOfWeek()));
                } else {
                    plannedWorkingHoursByDate.put(localDate, PlannedWorkingHours.ZERO);
                }
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

    private static DateRange getDateRange(LocalDate from, WorkingTime workingTime, LocalDate nextEnd) {
        final LocalDate validFrom = workingTime.validFrom().orElse(null);
        if (validFrom == null || validFrom.isBefore(from)) {
            return new DateRange(from, nextEnd);
        } else {
            return new DateRange(validFrom, nextEnd);
        }
    }
}
