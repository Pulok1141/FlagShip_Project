package io.flagship.server.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "flag_env_configs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_flag_env", columnNames = {"flag_id", "environment_id"})
})
public class FlagEnvConfig {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_id", nullable = false)
    private Flag flag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "variations_json", nullable = false, columnDefinition = "TEXT")
    private String variationsJson;

    @Column(name = "targets_json", nullable = false, columnDefinition = "TEXT")
    private String targetsJson = "[]";

    @Column(name = "rules_json", nullable = false, columnDefinition = "TEXT")
    private String rulesJson = "[]";

    @Column(name = "rollout_json", nullable = false, columnDefinition = "TEXT")
    private String rolloutJson = "{}";

    @Column(name = "default_variation_id", nullable = false, length = 64)
    private String defaultVariationId;

    @Column(name = "off_variation_id", length = 64)
    private String offVariationId;

    @Column(nullable = false)
    private long version = 1L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public FlagEnvConfig() {}

    public FlagEnvConfig(String id, Flag flag, Environment environment, boolean enabled,
                         String variationsJson, String targetsJson, String rulesJson, String rolloutJson,
                         String defaultVariationId, String offVariationId) {
        this.id = id;
        this.flag = flag;
        this.environment = environment;
        this.enabled = enabled;
        this.variationsJson = variationsJson;
        this.targetsJson = targetsJson != null ? targetsJson : "[]";
        this.rulesJson = rulesJson != null ? rulesJson : "[]";
        this.rolloutJson = rolloutJson != null ? rolloutJson : "{}";
        this.defaultVariationId = defaultVariationId;
        this.offVariationId = offVariationId;
        this.version = 1L;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Flag getFlag() { return flag; }
    public void setFlag(Flag flag) { this.flag = flag; }

    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getVariationsJson() { return variationsJson; }
    public void setVariationsJson(String variationsJson) { this.variationsJson = variationsJson; }

    public String getTargetsJson() { return targetsJson; }
    public void setTargetsJson(String targetsJson) { this.targetsJson = targetsJson; }

    public String getRulesJson() { return rulesJson; }
    public void setRulesJson(String rulesJson) { this.rulesJson = rulesJson; }

    public String getRolloutJson() { return rolloutJson; }
    public void setRolloutJson(String rolloutJson) { this.rolloutJson = rolloutJson; }

    public String getDefaultVariationId() { return defaultVariationId; }
    public void setDefaultVariationId(String defaultVariationId) { this.defaultVariationId = defaultVariationId; }

    public String getOffVariationId() { return offVariationId; }
    public void setOffVariationId(String offVariationId) { this.offVariationId = offVariationId; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
