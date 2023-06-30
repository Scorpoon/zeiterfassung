package de.focusshift.zeiterfassung.absence;

public interface AbsenceWriteService {

    /**
     * Add a new {@linkplain AbsenceWrite}
     *
     * @param absence absence to add
     */
    void addAbsence(AbsenceWrite absence);

    /**
     * Delete an {@linkplain AbsenceWrite}
     *
     * @param absence to delete
     */
    void deleteAbsence(AbsenceWrite absence);
}
