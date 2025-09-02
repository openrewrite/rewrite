/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.semver;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Validated;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.semver.Semver.isVersion;

/**
 * Any of X, x, or * may be used to "stand in" for one of the numeric values in the [major, minor, patch] tuple.
 * <a href="https://github.com/npm/node-semver#x-ranges-12x-1x-12-">X-Ranges</a>.
 */
public class XRange extends LatestRelease {
    private static final Pattern X_RANGE_PATTERN = Pattern.compile("([*xX]|\\d+)(?:\\.([*xX]|\\d+)(?:\\.([*xX]|\\d+))?(?:\\.([*xX]|\\d+))?)?");

    private final String major;
    private final String minor;
    private final String patch;
    private final String micro;

    XRange(String major, String minor, String patch, String micro, @Nullable String metadataPattern) {
        super(metadataPattern);
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.micro = micro;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        if (!super.isValid(currentVersion, version)) {
            return false;
        }

        if ("*".equals(major)) {
            return true;
        }

        Matcher gav = VersionComparator.RELEASE_PATTERN.matcher(normalizeVersion(version));
        gav.matches();

        if (!gav.group(1).equals(major)) {
            return false;
        }

        if ("*".equals(minor)) {
            return true;
        } else if (gav.group(2) == null || !gav.group(2).equals(minor)) {
            return false;
        }

        if ("*".equals(patch)) {
            return true;
        } else if (gav.group(3) == null || !gav.group(3).equals(patch)) {
            return false;
        }

        return gav.group(4) == null || !gav.group(4).equals(micro);
    }

    public static Validated<XRange> build(String pattern, @Nullable String metadataPattern) {
        Matcher matcher = X_RANGE_PATTERN.matcher(pattern);
        if (!matcher.matches() || !(pattern.contains("x") || pattern.contains("X") || pattern.contains("*"))) {
            return Validated.invalid("xRange", pattern, "not an x-range");
        }

        String major = normalizeWildcard(matcher.group(1));
        String minor = normalizeWildcard(matcher.group(2) == null ? "0" : matcher.group(2));
        String patch = normalizeWildcard(matcher.group(3) == null ? "0" : matcher.group(3));
        String micro = normalizeWildcard(matcher.group(4) == null ? "0" : matcher.group(4));

        if ("*".equals(major) && (matcher.group(2) != null || matcher.group(3) != null || matcher.group(4) != null)) {
            return Validated.invalid("xRange", pattern, "not an x-range: nothing can follow a wildcard");
        } else if ("*".equals(minor) && (matcher.group(3) != null || matcher.group(4) != null)) {
            return Validated.invalid("xRange", pattern, "not an x-range: nothing can follow a wildcard");
        } else if ("*".equals(patch) && matcher.group(4) != null) {
            return Validated.invalid("xRange", pattern, "not an x-range: nothing can follow a wildcard");
        }

        return Validated.valid("xRange", new XRange(major, minor, patch, micro, metadataPattern));
    }

    private static String normalizeWildcard(String part) {
        return "*".equals(part) || "x".equals(part) || "X".equals(part) ? "*" : part;
    }

    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        Validated<XRange> maybeXRangeV1 = build(v1, null);
        Validated<XRange> maybeXRangeV2 = build(v2, null);
        if (maybeXRangeV1.isValid() && maybeXRangeV2.isValid()) {
            XRange xrangeV1 = requireNonNull(maybeXRangeV1.getValue());
            XRange xrangeV2 = requireNonNull(maybeXRangeV2.getValue());

            if ("*".equals(xrangeV1.major) && "*".equals(xrangeV2.major)) {
                return 0;
            } else if ("*".equals(xrangeV1.major)) {
                return 1;
            } else if ("*".equals(xrangeV2.major)) {
                return -1;
            }

            int compare = xrangeV1.major.compareTo(xrangeV2.major);
            if (compare != 0) {
                return compare;
            }

            if ("*".equals(xrangeV1.minor) && "*".equals(xrangeV2.minor)) {
                return 0;
            } else if ("*".equals(xrangeV1.minor)) {
                return 1;
            } else if ("*".equals(xrangeV2.minor)) {
                return -1;
            }

            compare =  xrangeV1.minor.compareTo(xrangeV2.minor);
            if (compare != 0) {
                return compare;
            }

            if ("*".equals(xrangeV1.patch) && "*".equals(xrangeV2.patch)) {
                return 0;
            } else if ("*".equals(xrangeV1.patch)) {
                return 1;
            } else if ("*".equals(xrangeV2.patch)) {
                return -1;
            }

            // Micro is guaranteed to be the same, so we can stop here
            return xrangeV1.patch.compareTo(xrangeV2.patch);
        } else if (maybeXRangeV1.isValid()) {
            if (!isVersion(v2)) {
                return 1;
            }

            XRange xrangeV1 = requireNonNull(maybeXRangeV1.getValue());
            if ("*".equals(xrangeV1.major)) {
                return 0;
            }

            Matcher gav = VersionComparator.RELEASE_PATTERN.matcher(normalizeVersion(v2));
            gav.matches();

            if (!xrangeV1.major.equals(gav.group(1))) {
                return xrangeV1.major.compareTo(gav.group(1));
            }

            if ("*".equals(xrangeV1.minor)) {
                return 0;
            } else if (gav.group(2) == null || !xrangeV1.minor.equals(gav.group(2))) {
                return xrangeV1.minor.compareTo(gav.group(2));
            }

            if ("*".equals(xrangeV1.patch)) {
                return 0;
            } else if (gav.group(3) == null || !xrangeV1.patch.equals(gav.group(3))) {
                return xrangeV1.patch.compareTo(gav.group(3));
            }

            return 0;
        } else if (maybeXRangeV2.isValid()) {
            if (!isVersion(v1)) {
                return -1;
            }

            XRange xrangeV2 = requireNonNull(maybeXRangeV2.getValue());
            if ("*".equals(xrangeV2.major)) {
                return 0;
            }

            Matcher gav = VersionComparator.RELEASE_PATTERN.matcher(normalizeVersion(v1));
            gav.matches();

            if (!gav.group(1).equals(xrangeV2.major)) {
                return gav.group(1).compareTo(xrangeV2.major);
            }

            if ("*".equals(xrangeV2.minor)) {
                return 0;
            } else if (gav.group(2) == null || !gav.group(2).equals(xrangeV2.minor)) {
                return gav.group(2).compareTo(xrangeV2.minor);
            }

            if ("*".equals(xrangeV2.patch)) {
                return 0;
            } else if (gav.group(3) == null || !gav.group(3).equals(xrangeV2.patch)) {
                return gav.group(3).compareTo(xrangeV2.patch);
            }

            return 0;
        }

        return super.compare(currentVersion, v1, v2);
    }
}
