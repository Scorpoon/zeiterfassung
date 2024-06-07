package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Set;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static java.lang.invoke.MethodHandles.lookup;

@Component
class TenantUserCreatorAndUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final TenantContextHolder tenantContextHolder;
    private final TenantUserService tenantUserService;

    TenantUserCreatorAndUpdater(TenantContextHolder tenantContextHolder, TenantUserService tenantUserService) {
        this.tenantContextHolder = tenantContextHolder;
        this.tenantUserService = tenantUserService;
    }

    @EventListener
    public void handle(InteractiveAuthenticationSuccessEvent event) {

        if (event.getAuthentication() instanceof final OAuth2AuthenticationToken oauthToken) {
            try {
                final TenantId tenantId = new TenantId(oauthToken.getAuthorizedClientRegistrationId());
                if (!tenantId.valid()) {
                    LOG.warn("Ignoring InteractiveAuthenticationSuccessEvent for invalid tenantId={}", tenantId.tenantId());
                } else {
                    tenantContextHolder.setTenantId(tenantId);
                    createOrUpdateTenantUser(tenantId, oauthToken.getPrincipal());
                }
            } finally {
                tenantContextHolder.clear();
            }
        } else {
            LOG.warn("Ignoring InteractiveAuthenticationSuccessEvent for unexpected authentication token type={}", event.getAuthentication().getClass());
        }
    }

    private void createOrUpdateTenantUser(TenantId tenantId, OAuth2User oauth2User) {
        if (oauth2User instanceof final DefaultOidcUser oidcUser) {
            final EMailAddress eMailAddress = new EMailAddress(oidcUser.getEmail());
            final UserId userId = new UserId(oidcUser.getSubject());

            tenantUserService.findById(userId).ifPresentOrElse(user -> {
                final TenantUser tenantUser = new TenantUser(user.id(), user.localId(), oidcUser.getGivenName(), oidcUser.getFamilyName(), eMailAddress, user.authorities());
                LOG.info("updating existing user={} of tenantId={} with data from oidc token", userId.value(), tenantId.tenantId());
                tenantUserService.updateUser(tenantUser);
            }, () -> {
                LOG.info("creating new user={} for tenantId={} with data from oidc token", userId.value(), tenantId.tenantId());
                tenantUserService.createNewUser(oidcUser.getSubject(), oidcUser.getGivenName(), oidcUser.getFamilyName(), eMailAddress, Set.of(ZEITERFASSUNG_USER));
            });
        }
    }
}
