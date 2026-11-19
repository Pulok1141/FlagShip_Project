package io.flagship.core.strategy;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, robust Semantic Versioning (SemVer 2.0) parser and comparator.
 */
public final class SemVer implements Comparable<SemVer> {

    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^[vV]?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-([0-9A-Za-z.-]+))?(?:\\+([0-9A-Za-z.-]+))?$"
    );

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;
    private final String raw;

    public SemVer(int major, int minor, int patch, String preRelease, String raw) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease != null ? preRelease : "";
        this.raw = raw != null ? raw : major + "." + minor + "." + patch;
    }

    public static SemVer parse(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version string cannot be empty");
        }
        String clean = version.trim();
        Matcher matcher = SEMVER_PATTERN.matcher(clean);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid SemVer string: " + version);
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        String preRelease = matcher.group(4);

        return new SemVer(major, minor, patch, preRelease, clean);
    }

    public static SemVer parseOrNull(String version) {
        try {
            return parse(version);
        } catch (Exception e) {
            return null;
        }
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public String preRelease() {
        return preRelease;
    }

    public String raw() {
        return raw;
    }

    @Override
    public int compareTo(SemVer o) {
        if (o == null) return 1;
        int c = Integer.compare(this.major, o.major);
        if (c != 0) return c;
        c = Integer.compare(this.minor, o.minor);
        if (c != 0) return c;
        c = Integer.compare(this.patch, o.patch);
        if (c != 0) return c;

        // Normal versions have higher precedence than pre-release versions
        if (this.preRelease.isEmpty() && !o.preRelease.isEmpty()) return 1;
        if (!this.preRelease.isEmpty() && o.preRelease.isEmpty()) return -1;
        if (this.preRelease.isEmpty() && o.preRelease.isEmpty()) return 0;

        return this.preRelease.compareTo(o.preRelease);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SemVer semVer = (SemVer) o;
        return compareTo(semVer) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, preRelease);
    }

    @Override
    public String toString() {
        return raw;
    }
}
