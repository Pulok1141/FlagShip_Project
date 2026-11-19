package io.flagship.server.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "flag_versions")
public class FlagVersion {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_env_config_id", nullable = false)
    private FlagEnvConfig flagEnvConfig;

    @Column(nullable = false)
    private long version;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "diff_json", columnDefinition = "TEXT")
    private String diffJson;

    @Column(name = "author_email", nullable = false)
    private String authorEmail;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public FlagVersion() {}

    public FlagVersion(String id, FlagEnvConfig flagEnvConfig, long version, String snapshotJson,
                       String diffJson, String authorEmail, String reason) {
        this.id = id;
        this.flagEnvConfig = flagEnvConfig;
        this.version = version;
        this.snapshotJson = snapshotJson;
        this.diffJson = diffJson;
        this.authorEmail = authorEmail;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public FlagEnvConfig getFlagEnvConfig() { return flagEnvConfig; }
    public void setFlagEnvConfig(FlagEnvConfig flagEnvConfig) { this.flagEnvConfig = flagEnvConfig; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }

    public String getDiffJson() { return diffJson; }
    public void setDiffJson(String diffJson) { this.diffJson = diffJson; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
