/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven;

import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static org.openrewrite.Validated.required;

/**
 * Upgrade the version a group or group and artifact using Node Semver
 * <a href="https://github.com/npm/node-semver#advanced-range-syntax">advanced range selectors</a>, allowing
 * more precise control over version updates to patch or minor releases.
 */
public class UpgradeVersion extends MavenRefactorVisitor {
    private String groupId;

    @Nullable
    private String artifactId;

    /**
     * Node Semver range syntax.
     */
    private String toVersion;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(@Nullable String artifactId) {
        this.artifactId = artifactId;
    }

    public void setToVersion(String toVersion) {
        this.toVersion = toVersion;
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId))
                .and(required("toVersion", toVersion));
    }

    @Override
    public Maven visitProperty(Maven.Property property) {
        Maven.Property p = refactor(property, super::visitProperty);
        Maven.Pom pom = getCursor().firstEnclosing(Maven.Pom.class);

        if (!property.getValue().equals(toVersion) &&
                property.isDependencyVersionProperty(pom, groupId, artifactId)) {
            p = p.withValue(toVersion);
        }

        return p;
    }

    private interface VersionComparator extends Comparator<String> {
        Pattern RELEASE_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

        boolean isValid(String version);
    }

    /**
     * <a href="https://github.com/npm/node-semver#hyphen-ranges-xyz---abc">Hyphen ranges</a>.
     */
    static class HyphenRange extends LatestRelease {
        private static final Pattern HYPHEN_RANGE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?(\\.\\d+)?)\\s*-\\s*(\\d+(\\.\\d+)?(\\.\\d+)?)");

        private final String upper;
        private final String lower;

        private HyphenRange(String lower, String upper) {
            this.lower = fillPartialVersionWithZeroes(lower);
            this.upper = fillPartialVersionWithZeroes(upper);
        }

        private static String fillPartialVersionWithZeroes(String version) {
            if (version.chars().filter(c -> c == '.').count() < 2) {
                return fillPartialVersionWithZeroes(version + ".0");
            }
            return version;
        }

        @Override
        public boolean isValid(String version) {
            return super.isValid(version) &&
                    super.compare(version, upper) <= 0 &&
                    super.compare(version, lower) >= 0;
        }

        public static Validated build(String pattern) {
            Matcher matcher = HYPHEN_RANGE_PATTERN.matcher(pattern);
            if (!matcher.matches()) {
                return Validated.invalid("hyphenRange", pattern, "not a hyphen range");
            }
            return Validated.valid("hyphenRange", new HyphenRange(matcher.group(1), matcher.group(4)));
        }
    }

    /**
     * Any of X, x, or * may be used to "stand in" for one of the numeric values in the [major, minor, patch] tuple.
     * <a href="https://github.com/npm/node-semver#x-ranges-12x-1x-12-">X-Ranges</a>.
     */
    static class XRange extends LatestRelease {
        private static final Pattern X_RANGE_PATTERN = Pattern.compile("([*xX]|\\d+)(?:\\.([*xX]|\\d+)(?:\\.([*xX]|\\d+))?)?");

        private final String major;
        private final String minor;
        private final String patch;

        XRange(String major, String minor, String patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public boolean isValid(String version) {
            if (!super.isValid(version)) {
                return false;
            }

            if (major.equals("*")) {
                return true;
            }

            Matcher gav = RELEASE_PATTERN.matcher(version);
            gav.matches();

            if (!gav.group(1).equals(major)) {
                return false;
            }

            if (minor.equals("*")) {
                return true;
            } else if (gav.group(2) == null || !gav.group(2).equals(minor)) {
                return false;
            }

            if (patch.equals("*")) {
                return true;
            }

            return gav.group(3) == null || !gav.group(3).equals(patch);
        }

        public static Validated build(String pattern) {
            Matcher matcher = X_RANGE_PATTERN.matcher(pattern);
            if (!matcher.matches()) {
                return Validated.invalid("xRange", pattern, "not an x-range");
            }

            String major = normalizeWildcard(matcher.group(1));
            String minor = normalizeWildcard(matcher.group(2) == null ? "0" : matcher.group(2));
            String patch = normalizeWildcard(matcher.group(3) == null ? "0" : matcher.group(3));

            if (major.equals("*") && (matcher.group(2) != null || matcher.group(3) != null)) {
                return Validated.invalid("xRange", pattern, "not an x-range: nothing can follow a wildcard");
            } else if (minor.equals("*") && matcher.group(3) != null) {
                return Validated.invalid("xRange", pattern, "not an x-range: nothing can follow a wildcard");
            }

            return Validated.valid("xRange", new XRange(major, minor, patch));
        }

        private static String normalizeWildcard(String part) {
            return part.equals("*") || part.equals("x") || part.equals("X") ? "*" : part;
        }
    }

    /**
     * Allows changes that do not modify the left-most non-zero element in the [major, minor, patch] tuple.
     * <a href="https://github.com/npm/node-semver#caret-ranges-123-025-004">Caret ranges</a>.
     */
    static class CaretRange extends LatestRelease {
        private static final Pattern CARET_RANGE_PATTERN = Pattern.compile("\\^(\\d+)(?:\\.([*xX]|\\d+))?(?:\\.([*xX]|\\d+))?");

        private final String upperExclusive;
        private final String lower;

        private CaretRange(String lower, String upperExclusive) {
            this.lower = lower;
            this.upperExclusive = upperExclusive;
        }

        @Override
        public boolean isValid(String version) {
            return super.isValid(version) &&
                    super.compare(version, upperExclusive) < 0 &&
                    super.compare(version, lower) >= 0;
        }

        public static Validated build(String pattern) {
            Matcher matcher = CARET_RANGE_PATTERN.matcher(pattern);
            if (!matcher.matches()) {
                return Validated.invalid("caretRange", pattern, "not a caret range");
            }

            String major = matcher.group(1);
            String minor = normalizeWildcard(matcher.group(2));
            String patch = normalizeWildcard(matcher.group(3));

            if ("*".equals(minor) && matcher.group(3) != null) {
                return Validated.invalid("caretRange", pattern, "not a caret range: nothing can follow a wildcard");
            }

            String lower;
            String upper;

            if (minor == null) {
                // A missing patch value desugars to the number 0, but will allow flexibility
                // within that value, even if the major and minor versions are both 0.
                lower = major + ".0.0";
            } else if (patch == null) {
                // A missing minor and patch values will desugar to zero, but also allow flexibility
                // within those values, even if the major version is zero.
                lower = major + "." + minor + ".0";
            } else {
                lower = major + "." + minor + "." + patch;
            }

            if (!"0".equals(major) || minor == null) {
                upper = Integer.toString(parseInt(major) + 1);
            } else if (!"0".equals(minor) || patch == null) {
                upper = major + "." + (parseInt(minor) + 1);
            } else {
                upper = major + "." + minor + "." + patch;
            }

            return Validated.valid("caretRange", new CaretRange(lower, upper));
        }

        private static String normalizeWildcard(@Nullable String part) {
            return "*".equals(part) || "x".equals(part) || "X".equals(part) ? null : part;
        }
    }

    static class LatestRelease implements VersionComparator {
        @Override
        public boolean isValid(String version) {
            return RELEASE_PATTERN.matcher(version).matches();
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public int compare(String v1, String v2) {
            Matcher v1Gav = RELEASE_PATTERN.matcher(v1);
            Matcher v2Gav = RELEASE_PATTERN.matcher(v2);

            v1Gav.matches();
            v2Gav.matches();

            for (int i = 1; i <= 3; i++) {
                String v1Part = v1Gav.group(i);
                String v2Part = v2Gav.group(i);
                if (v1Part == null) {
                    return v2Part == null ? 0 : -11;
                } else if (v2Part == null) {
                    return 1;
                }

                int diff = parseInt(v1Part) - parseInt(v2Part);
                if (diff != 0) {
                    return diff;
                }
            }

            return 0;
        }

        public static Validated build(String pattern) {
            return pattern.equals("latest.release") ?
                    Validated.valid("latestRelease", new LatestRelease()) :
                    Validated.invalid("latestRelease", pattern, "not a hyphen range");
        }
    }
}
