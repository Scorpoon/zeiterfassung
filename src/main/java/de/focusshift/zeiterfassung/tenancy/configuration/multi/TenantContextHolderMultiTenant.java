package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;

@Component
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
class TenantContextHolderMultiTenant implements TenantContextHolder {

    private static final Logger LOG = LoggerFactory.getLogger(TenantContextHolderMultiTenant.class);

    private static final InheritableThreadLocal<TenantId> currentTenantId = new InheritableThreadLocal<>();

    @Override
    public Optional<TenantId> getCurrentTenantId() {
        return Optional.ofNullable(currentTenantId.get());
    }

    @Override
    public void setTenantId(TenantId tenantId) {
        LOG.debug("Setting tenantId to {}", tenantId);
        currentTenantId.set(tenantId);
    }

    @Override
    public void clear() {
        LOG.debug("Clearing tenantId");
        currentTenantId.remove();
    }
}
