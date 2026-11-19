package io.flagship.core.model;

import io.flagship.core.ast.CompositeRuleNode;
import io.flagship.core.ast.LeafConditionNode;
import io.flagship.core.ast.OperatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotJsonMapperTest {

    @Test
    @DisplayName("Serialize and deserialize complete EnvironmentSnapshot with AST rules")
    void testJsonRoundTrip() {
        Variation v1 = new Variation("v1", "Variation 1", true);
        Variation v2 = new Variation("v2", "Variation 2", false);

        FlagDefinition flag = new FlagDefinition(
                "checkout-v2",
                "Checkout V2",
                "Flag for checkout migration",
                FlagType.BOOLEAN,
                true,
                List.of(v1, v2),
                List.of(new TargetRule("v1", Set.of("user-admin"))),
                List.of(new SegmentRule("r1", "Beta Rule",
                        CompositeRuleNode.and(
                                LeafConditionNode.of("tier", OperatorType.EQUALS, "enterprise"),
                                LeafConditionNode.of("version", OperatorType.SEMVER_GTE, "2.0.0")
                        ), "v1")),
                PercentageRollout.of(List.of(new RolloutBucket("v1", 5000))),
                "v2",
                "v2",
                10L,
                Instant.now(),
                List.of("checkout", "v2")
        );

        SegmentDefinition segment = new SegmentDefinition(
                "enterprise-users",
                "Enterprise Users",
                "Users on enterprise plan",
                LeafConditionNode.of("plan", OperatorType.EQUALS, "enterprise")
        );

        EnvironmentSnapshot snapshot = new EnvironmentSnapshot(
                "proj-1",
                "production",
                42L,
                Instant.now(),
                Map.of(flag.key(), flag),
                Map.of(segment.key(), segment)
        );

        String json = SnapshotJsonMapper.toJson(snapshot);
        assertThat(json).isNotBlank();

        EnvironmentSnapshot deserialized = SnapshotJsonMapper.fromJson(json, EnvironmentSnapshot.class);
        assertThat(deserialized.projectId()).isEqualTo("proj-1");
        assertThat(deserialized.environmentKey()).isEqualTo("production");
        assertThat(deserialized.version()).isEqualTo(42L);

        FlagDefinition deserializedFlag = deserialized.getFlag("checkout-v2");
        assertThat(deserializedFlag).isNotNull();
        assertThat(deserializedFlag.key()).isEqualTo("checkout-v2");
        assertThat(deserializedFlag.rules()).hasSize(1);
        assertThat(deserializedFlag.rules().get(0).condition()).isInstanceOf(CompositeRuleNode.class);

        SegmentDefinition deserializedSegment = deserialized.segments().get("enterprise-users");
        assertThat(deserializedSegment).isNotNull();
        assertThat(deserializedSegment.condition()).isInstanceOf(LeafConditionNode.class);
    }
}
