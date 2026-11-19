package io.flagship.core.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemVerTest {

    @Test
    @DisplayName("Parse standard semantic versions")
    void parseStandardVersions() {
        SemVer v1 = SemVer.parse("1.2.3");
        assertThat(v1.major()).isEqualTo(1);
        assertThat(v1.minor()).isEqualTo(2);
        assertThat(v1.patch()).isEqualTo(3);
        assertThat(v1.preRelease()).isEmpty();

        SemVer v2 = SemVer.parse("v2.0.0-beta.1");
        assertThat(v2.major()).isEqualTo(2);
        assertThat(v2.minor()).isEqualTo(0);
        assertThat(v2.patch()).isEqualTo(0);
        assertThat(v2.preRelease()).isEqualTo("beta.1");
    }

    @Test
    @DisplayName("Compare semantic versions according to SemVer 2.0 precedence")
    void compareVersions() {
        SemVer v100 = SemVer.parse("1.0.0");
        SemVer v101 = SemVer.parse("1.0.1");
        SemVer v110 = SemVer.parse("1.1.0");
        SemVer v200 = SemVer.parse("2.0.0");
        SemVer v200alpha = SemVer.parse("2.0.0-alpha");

        assertThat(v100).isLessThan(v101);
        assertThat(v101).isLessThan(v110);
        assertThat(v110).isLessThan(v200);
        assertThat(v200alpha).isLessThan(v200);
        assertThat(v100).isEqualTo(SemVer.parse("v1.0.0"));
    }

    @Test
    @DisplayName("Reject invalid semver strings")
    void rejectInvalidStrings() {
        assertThatThrownBy(() -> SemVer.parse("invalid"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemVer.parse(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
