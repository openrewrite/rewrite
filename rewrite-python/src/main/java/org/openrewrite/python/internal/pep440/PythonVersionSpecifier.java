/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal.pep440;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single PEP 440 version specifier clause ({@code ==}, {@code !=}, {@code <=}, {@code >=},
 * {@code <}, {@code >}, {@code ~=}, {@code ===}, including prefix matching such as
 * {@code ==1.2.*}), ported from pypa/packaging's {@code specifiers.Specifier}.
 */
public class PythonVersionSpecifier {

    /**
     * Port of packaging's {@code Specifier._specifier_regex_str}; also used by the PEP 508
     * requirement tokenizer.
     */
    public static final String SPECIFIER_REGEX = "(?:" +
            // Arbitrary equality: an exact string match escape hatch.
            "(?:===\\s*[^\\s;)]*)" +
            "|" +
            // (Non)equality, which additionally allow wildcards and local versions.
            "(?:(?:==|!=)\\s*v?(?:[0-9]+!)?[0-9]+(?:\\.[0-9]+)*" +
            "(?:\\.\\*|" +
            "(?:[-_.]?(?:alpha|beta|preview|pre|a|b|c|rc)[-_.]?[0-9]*)?" +
            "(?:(?:-[0-9]+)|(?:[-_.]?(?:post|rev|r)[-_.]?[0-9]*))?" +
            "(?:[-_.]?dev[-_.]?[0-9]*)?" +
            "(?:\\+[a-z0-9]+(?:[-_.][a-z0-9]+)*)?" +
            ")?)" +
            "|" +
            // Compatible release, requiring at least two release segments.
            "(?:~=\\s*v?(?:[0-9]+!)?[0-9]+(?:\\.[0-9]+)+" +
            "(?:[-_.]?(?:alpha|beta|preview|pre|a|b|c|rc)[-_.]?[0-9]*)?" +
            "(?:(?:-[0-9]+)|(?:[-_.]?(?:post|rev|r)[-_.]?[0-9]*))?" +
            "(?:[-_.]?dev[-_.]?[0-9]*)?)" +
            "|" +
            // Ordered comparisons: no wildcards, no local versions.
            "(?:(?:<=|>=|<|>)\\s*v?(?:[0-9]+!)?[0-9]+(?:\\.[0-9]+)*" +
            "(?:[-_.]?(?:alpha|beta|preview|pre|a|b|c|rc)[-_.]?[0-9]*)?" +
            "(?:(?:-[0-9]+)|(?:[-_.]?(?:post|rev|r)[-_.]?[0-9]*))?" +
            "(?:[-_.]?dev[-_.]?[0-9]*)?)" +
            ")";

    private static final Pattern PATTERN = Pattern.compile(
            "[\\p{IsWhite_Space}]*" + SPECIFIER_REGEX + "[\\p{IsWhite_Space}]*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PREFIX_SPLIT = Pattern.compile("^([0-9]+)((?:a|b|c|rc)[0-9]+)$");

    private final String operator;
    private final String version;

    private PythonVersionSpecifier(String operator, String version) {
        this.operator = operator;
        this.version = version;
    }

    public static @Nullable PythonVersionSpecifier parse(@Nullable String spec) {
        if (spec == null || !PATTERN.matcher(spec).matches()) {
            return null;
        }
        String s = strip(spec);
        String operator;
        if (s.startsWith("===")) {
            operator = "===";
        } else if (s.startsWith("~=") || s.startsWith("==") || s.startsWith("!=") ||
                s.startsWith("<=") || s.startsWith(">=")) {
            operator = s.substring(0, 2);
        } else {
            operator = s.substring(0, 1);
        }
        return new PythonVersionSpecifier(operator, strip(s.substring(operator.length())));
    }

    public String getOperator() {
        return operator;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Whether this clause matches the version under pure per-operator PEP 440 semantics
     * (prereleases permitted).
     */
    public boolean contains(PythonVersion version) {
        switch (operator) {
            case "~=":
                return compareCompatible(version, this.version);
            case "==":
                return compareEqual(version, this.version);
            case "!=":
                return !compareEqual(version, this.version);
            case "<=":
                return version.comparePublicKey(requireVersion(this.version)) <= 0;
            case ">=":
                return version.comparePublicKey(requireVersion(this.version)) >= 0;
            case "<":
                return compareLessThan(version, this.version);
            case ">":
                return compareGreaterThan(version, this.version);
            case "===":
                return version.toString().toLowerCase(Locale.ROOT).equals(this.version.toLowerCase(Locale.ROOT));
            default:
                throw new IllegalStateException("Unexpected operator: " + operator);
        }
    }

    /**
     * As {@link #contains(PythonVersion)}, but with {@code includePrereleases == false}
     * pre-release and dev versions never match.
     */
    public boolean contains(PythonVersion version, boolean includePrereleases) {
        if (!includePrereleases && version.isPrerelease()) {
            return false;
        }
        return contains(version);
    }

    /**
     * Whether the clause itself references a pre-release version, following packaging's
     * autodetection: {@code !=} and wildcard {@code ==} clauses never imply prereleases.
     */
    public boolean referencesPrerelease() {
        if ("!=".equals(operator) || ("==".equals(operator) && version.endsWith(".*"))) {
            return false;
        }
        PythonVersion parsed = PythonVersion.parse(version);
        return parsed != null && parsed.isPrerelease();
    }

    private static PythonVersion requireVersion(String version) {
        PythonVersion parsed = PythonVersion.parse(version);
        if (parsed == null) {
            throw new IllegalStateException("Invalid specifier version: " + version);
        }
        return parsed;
    }

    private boolean compareCompatible(PythonVersion prospective, String spec) {
        // ~=2.2 is equivalent to >=2.2,==2.*
        List<String> split = versionSplit(spec);
        List<String> prefixParts = new ArrayList<>();
        for (String part : split) {
            if (isSuffix(part)) {
                break;
            }
            prefixParts.add(part);
        }
        prefixParts.remove(prefixParts.size() - 1);
        String prefix = versionJoin(prefixParts);
        return prospective.comparePublicKey(requireVersion(spec)) >= 0 &&
                compareEqual(prospective, prefix + ".*");
    }

    private boolean compareEqual(PythonVersion prospective, String spec) {
        if (spec.endsWith(".*")) {
            // Prefix matching ignores the local segment.
            String normalizedProspective = canonicalizeVersion(prospective.getPublicVersion(), false);
            String normalizedSpec = canonicalizeVersion(spec.substring(0, spec.length() - 2), false);
            List<String> splitSpec = versionSplit(normalizedSpec);
            List<String> splitProspective = versionSplit(normalizedProspective);
            List<String> padded = padRelease(splitProspective, splitSpec);
            List<String> shortened = padded.subList(0, Math.min(splitSpec.size(), padded.size()));
            return shortened.equals(splitSpec);
        }
        PythonVersion specVersion = requireVersion(spec);
        if (specVersion.getLocal() == null) {
            // Without a local segment on the specifier, ignore the prospective one.
            return prospective.comparePublicKey(specVersion) == 0;
        }
        return prospective.compareTo(specVersion) == 0;
    }

    private static final String MIN_VERSION = "0.dev0";

    private boolean compareLessThan(PythonVersion prospective, String specStr) {
        PythonVersion spec = requireVersion(specStr);
        // <V excludes pre-releases of V itself when V is not one; V.dev0 is the
        // earliest pre-release of V (packaging _ranges.standard_ranges).
        PythonVersion bound = spec.isPrerelease() ? spec : replaceVersion(spec, spec.getPost(), 0L);
        if (bound.compareTo(requireVersion(MIN_VERSION)) <= 0) {
            return false;
        }
        return prospective.compareTo(bound) < 0;
    }

    private boolean compareGreaterThan(PythonVersion prospective, String specStr) {
        PythonVersion spec = requireVersion(specStr);
        if (spec.getDev() != null) {
            // >V.devN: dev versions have no post-releases, so the next real version is V.dev(N+1).
            return prospective.compareTo(replaceVersion(spec, spec.getPost(), spec.getDev() + 1)) >= 0;
        }
        if (spec.getPost() != null) {
            // >V.postN: next real version is V.post(N+1).dev0.
            return prospective.compareTo(replaceVersion(spec, spec.getPost() + 1, 0L)) >= 0;
        }
        // >V (final or pre-release): exclude V itself, V+local, and every V.postN.
        if (prospective.compareTo(spec) <= 0) {
            return false;
        }
        boolean family = prospective.baseEquals(spec) &&
                preEquals(prospective, spec) &&
                (prospective.getDev() == null || prospective.getPost() != null);
        return !family;
    }

    private static boolean preEquals(PythonVersion a, PythonVersion b) {
        if (a.getPreLetter() == null || b.getPreLetter() == null) {
            return a.getPreLetter() == null && b.getPreLetter() == null;
        }
        return a.getPreLetter().equals(b.getPreLetter()) && a.getPre().equals(b.getPre());
    }

    // Equivalent of packaging Version.__replace__(post=..., dev=..., local=None).
    private static PythonVersion replaceVersion(PythonVersion v, @Nullable Long post, @Nullable Long dev) {
        StringBuilder sb = new StringBuilder();
        if (v.getEpoch() != 0) {
            sb.append(v.getEpoch()).append('!');
        }
        long[] release = v.getRelease();
        for (int i = 0; i < release.length; i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(release[i]);
        }
        if (v.getPreLetter() != null) {
            sb.append(v.getPreLetter()).append(v.getPre());
        }
        if (post != null) {
            sb.append(".post").append(post);
        }
        if (dev != null) {
            sb.append(".dev").append(dev);
        }
        return requireVersion(sb.toString());
    }

    /**
     * Port of packaging's {@code canonicalize_version}: invalid versions are returned unaltered.
     */
    static String canonicalizeVersion(String version, boolean stripTrailingZero) {
        PythonVersion parsed = PythonVersion.parse(version);
        if (parsed == null) {
            return version;
        }
        return stripTrailingZero ? parsed.toTrimmedString() : parsed.toString();
    }

    // Port of packaging's _version_split.
    private static List<String> versionSplit(String version) {
        List<String> result = new ArrayList<>();
        int bang = version.lastIndexOf('!');
        String epoch = bang < 0 ? "" : version.substring(0, bang);
        String rest = bang < 0 ? version : version.substring(bang + 1);
        result.add(epoch.isEmpty() ? "0" : epoch);
        for (String item : rest.split("\\.", -1)) {
            Matcher m = PREFIX_SPLIT.matcher(item);
            if (m.matches()) {
                result.add(m.group(1));
                result.add(m.group(2));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    private static String versionJoin(List<String> components) {
        StringBuilder sb = new StringBuilder(components.get(0)).append('!');
        for (int i = 1; i < components.size(); i++) {
            if (i > 1) {
                sb.append('.');
            }
            sb.append(components.get(i));
        }
        return sb.toString();
    }

    private static boolean isSuffix(String segment) {
        return segment.startsWith("dev") || segment.startsWith("a") || segment.startsWith("b") ||
                segment.startsWith("rc") || segment.startsWith("post");
    }

    // Port of packaging's _pad_version, applied to the left side only.
    private static List<String> padRelease(List<String> left, List<String> right) {
        int leftDigits = countLeadingDigits(left);
        int rightDigits = countLeadingDigits(right);
        int padding = rightDigits - leftDigits;
        if (padding <= 0) {
            return left;
        }
        List<String> padded = new ArrayList<>(left.subList(0, leftDigits));
        for (int i = 0; i < padding; i++) {
            padded.add("0");
        }
        padded.addAll(left.subList(leftDigits, left.size()));
        return padded;
    }

    private static int countLeadingDigits(List<String> parts) {
        int n = 0;
        while (n < parts.size() && isAllDigits(parts.get(n))) {
            n++;
        }
        return n;
    }

    private static boolean isAllDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return false;
            }
        }
        return !s.isEmpty();
    }

    /**
     * Strips Python {@code str.strip()} whitespace (Unicode-aware).
     */
    static String strip(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && isPythonWhitespace(s.charAt(start))) {
            start++;
        }
        while (end > start && isPythonWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(start, end);
    }

    private static boolean isPythonWhitespace(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\u0085';
    }

    // Canonical (operator, version) pair, mirroring packaging's Specifier._canonical_spec.
    private String[] canonicalSpec() {
        if ("===".equals(operator) || version.endsWith(".*")) {
            return new String[]{operator, version};
        }
        return new String[]{operator, canonicalizeVersion(version, !"~=".equals(operator))};
    }

    @Override
    public String toString() {
        return operator + version;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PythonVersionSpecifier)) {
            return false;
        }
        return Arrays.equals(canonicalSpec(), ((PythonVersionSpecifier) o).canonicalSpec());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(canonicalSpec());
    }
}
