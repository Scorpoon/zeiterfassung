package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.person;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.integration.urlaubsverwaltung.person")
class PersonRabbitmqConfigurationProperties {

    private boolean enabled = false;

    private boolean manageTopology = false;

    @NotEmpty
    private String topic = "person.topic";

    @NotEmpty
    private String routingKeyCreated = "created";

    @NotEmpty
    private String routingKeyUpdated = "updated";

    @NotEmpty
    private String routingKeyDisabled = "disabled";

    @NotEmpty
    private String routingKeyDeleted = "deleted";


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isManageTopology() {
        return manageTopology;
    }

    public void setManageTopology(boolean manageTopology) {
        this.manageTopology = manageTopology;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getRoutingKeyCreated() {
        return routingKeyCreated;
    }

    public void setRoutingKeyCreated(String routingKeyCreated) {
        this.routingKeyCreated = routingKeyCreated;
    }

    public String getRoutingKeyUpdated() {
        return routingKeyUpdated;
    }

    public void setRoutingKeyUpdated(String routingKeyUpdated) {
        this.routingKeyUpdated = routingKeyUpdated;
    }

    public String getRoutingKeyDisabled() {
        return routingKeyDisabled;
    }

    public void setRoutingKeyDisabled(String routingKeyDisabled) {
        this.routingKeyDisabled = routingKeyDisabled;
    }

    public String getRoutingKeyDeleted() {
        return routingKeyDeleted;
    }

    public void setRoutingKeyDeleted(String routingKeyDeleted) {
        this.routingKeyDeleted = routingKeyDeleted;
    }
}
