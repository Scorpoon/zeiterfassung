package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.security.SecurityRole;

import java.time.Instant;
import java.util.Set;

public record TenantUser(String id, Long localId, String givenName, String familyName, EMailAddress eMail, Instant firstLoginAt, Set<SecurityRole> authorities, Instant createdAt, Instant updatedAt, Instant deactivatedAt, Instant deletedAt, UserStatus status) {

    public TenantUser(String id, Long localId, String givenName, String familyName, EMailAddress eMail, Set<SecurityRole> authorities) {
        this(id, localId, givenName, familyName, eMail, null, authorities, null, null, null, null, UserStatus.UNKNOWN);
    }

    @Override
    public String toString() {
        return "TenantUser{" +
            "id='" + id + '\'' +
            ", localId=" + localId +
            '}';
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE || status == UserStatus.UNKNOWN;
    }

}
