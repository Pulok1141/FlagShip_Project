package io.flagship.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.flagship.core.ast.RuleNode;
import io.flagship.core.model.*;
import io.flagship.server.domain.Environment;
import io.flagship.server.domain.Flag;
import io.flagship.server.domain.FlagEnvConfig;
import io.flagship.server.domain.Segment;
import io.flagship.server.repository.EnvironmentRepository;
import io.flagship.server.repository.FlagEnvConfigRepository;
import io.flagship.server.repository.SegmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class SnapshotService {

    private final FlagEnvConfigRepository flagEnvConfigRepository;
    private final SegmentRepository segmentRepository;
    private final EnvironmentRepository environmentRepository;
    private final ObjectMapper mapper = SnapshotJsonMapper.getMapper();

    public SnapshotService(
            FlagEnvConfigRepository flagEnvConfigRepository,
            SegmentRepository segmentRepository,
            EnvironmentRepository environmentRepository
    ) {
        this.flagEnvConfigRepository = flagEnvConfigRepository;
        this.segmentRepository = segmentRepository;
        this.environmentRepository = environmentRepository;
    }

    @Transactional(readOnly = true)
    public EnvironmentSnapshot buildSnapshot(String projectId, String envKey) {
        List<FlagEnvConfig> configs = flagEnvConfigRepository.findByProjectIdAndEnvKey(projectId, envKey);
        List<Segment> segmentEntities = segmentRepository.findByProjectId(projectId);

        long maxVersion = 1L;
        Instant latestUpdate = Instant.EPOCH;

        Map<String, FlagDefinition> flagMap = new HashMap<>();
        for (FlagEnvConfig config : configs) {
            FlagDefinition fd = mapToFlagDefinition(config);
            flagMap.put(fd.key(), fd);
            if (config.getVersion() > maxVersion) {
                maxVersion = config.getVersion();
            }
            if (config.getUpdatedAt().isAfter(latestUpdate)) {
                latestUpdate = config.getUpdatedAt();
            }
        }

        Map<String, SegmentDefinition> segmentMap = new HashMap<>();
        for (Segment s : segmentEntities) {
            SegmentDefinition sd = mapToSegmentDefinition(s);
            segmentMap.put(sd.key(), sd);
            if (s.getUpdatedAt().isAfter(latestUpdate)) {
                latestUpdate = s.getUpdatedAt();
            }
        }

        return new EnvironmentSnapshot(
                projectId,
                envKey,
                maxVersion,
                latestUpdate,
                flagMap,
                segmentMap
        );
    }

    public FlagDefinition mapToFlagDefinition(FlagEnvConfig config) {
        Flag flag = config.getFlag();
        List<Variation> variations = parseList(config.getVariationsJson(), new TypeReference<List<Variation>>() {});
        List<TargetRule> targets = parseList(config.getTargetsJson(), new TypeReference<List<TargetRule>>() {});
        List<SegmentRule> rules = parseList(config.getRulesJson(), new TypeReference<List<SegmentRule>>() {});
        PercentageRollout rollout = parseObject(config.getRolloutJson(), PercentageRollout.class);

        List<String> tags = (flag.getTags() != null && !flag.getTags().isBlank()) ?
                Arrays.asList(flag.getTags().split(",")) : List.of();

        return new FlagDefinition(
                flag.getFlagKey(),
                flag.getName(),
                flag.getDescription(),
                flag.getFlagType(),
                config.isEnabled(),
                variations,
                targets,
                rules,
                rollout,
                config.getDefaultVariationId(),
                config.getOffVariationId(),
                config.getVersion(),
                config.getUpdatedAt(),
                tags
        );
    }

    public SegmentDefinition mapToSegmentDefinition(Segment segment) {
        RuleNode condition = parseObject(segment.getConditionJson(), RuleNode.class);
        return new SegmentDefinition(
                segment.getSegmentKey(),
                segment.getName(),
                segment.getDescription(),
                condition
        );
    }

    public String computeETag(EnvironmentSnapshot snapshot) {
        return "\"snap-" + snapshot.version() + "-" + snapshot.updatedAt().toEpochMilli() + "\"";
    }

    private <T> List<T> parseList(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            return mapper.readValue(json, typeRef);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return null;
        }
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }
}
