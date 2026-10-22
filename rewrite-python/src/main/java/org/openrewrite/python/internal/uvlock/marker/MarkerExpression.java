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
package org.openrewrite.python.internal.uvlock.marker;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.pep440.PythonVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * One comparison a marker is built from, as uv-pep508's {@code MarkerExpression} (uv 0.13.4
 * {@code marker/tree.rs}) with the keys, operators and canonical variables of {@code marker/lowering.rs}.
 */
public abstract class MarkerExpression {

    /** A version-valued environment key. */
    public enum VersionKey {
        IMPLEMENTATION_VERSION("implementation_version"),
        PYTHON_FULL_VERSION("python_full_version"),
        PYTHON_VERSION("python_version");

        final String display;

        VersionKey(String display) {
            this.display = display;
        }
    }

    /** A string-valued environment key, including the deprecated dotted spellings uv still reads. */
    public enum StringKey {
        IMPLEMENTATION_NAME("implementation_name", CanonicalString.IMPLEMENTATION_NAME),
        OS_NAME("os_name", CanonicalString.OS_NAME),
        OS_NAME_DEPRECATED("os_name", CanonicalString.OS_NAME),
        PLATFORM_MACHINE("platform_machine", CanonicalString.PLATFORM_MACHINE),
        PLATFORM_MACHINE_DEPRECATED("platform_machine", CanonicalString.PLATFORM_MACHINE),
        PLATFORM_PYTHON_IMPLEMENTATION("platform_python_implementation",
                CanonicalString.PLATFORM_PYTHON_IMPLEMENTATION),
        PLATFORM_PYTHON_IMPLEMENTATION_DEPRECATED("platform_python_implementation",
                CanonicalString.PLATFORM_PYTHON_IMPLEMENTATION),
        PYTHON_IMPLEMENTATION_DEPRECATED("platform_python_implementation",
                CanonicalString.PLATFORM_PYTHON_IMPLEMENTATION),
        PLATFORM_RELEASE("platform_release", CanonicalString.PLATFORM_RELEASE),
        PLATFORM_SYSTEM("platform_system", CanonicalString.PLATFORM_SYSTEM),
        PLATFORM_VERSION("platform_version", CanonicalString.PLATFORM_VERSION),
        PLATFORM_VERSION_DEPRECATED("platform_version", CanonicalString.PLATFORM_VERSION),
        SYS_PLATFORM("sys_platform", CanonicalString.SYS_PLATFORM),
        SYS_PLATFORM_DEPRECATED("sys_platform", CanonicalString.SYS_PLATFORM);

        final String display;
        final CanonicalString canonical;

        StringKey(String display, CanonicalString canonical) {
            this.display = display;
            this.canonical = canonical;
        }
    }

    /** A string variable of the decision diagram, declared in its variable order. */
    public enum CanonicalString {
        OS_NAME("os_name"),
        SYS_PLATFORM("sys_platform"),
        PLATFORM_SYSTEM("platform_system"),
        PLATFORM_MACHINE("platform_machine"),
        PLATFORM_PYTHON_IMPLEMENTATION("platform_python_implementation"),
        PLATFORM_RELEASE("platform_release"),
        PLATFORM_VERSION("platform_version"),
        IMPLEMENTATION_NAME("implementation_name");

        final String display;

        CanonicalString(String display) {
            this.display = display;
        }

        /** Whether the variable takes part in a known incompatible pair of values. */
        boolean isConflicting() {
            return compareTo(PLATFORM_SYSTEM) <= 0;
        }

        StringKey key() {
            switch (this) {
                case OS_NAME: return StringKey.OS_NAME;
                case SYS_PLATFORM: return StringKey.SYS_PLATFORM;
                case PLATFORM_SYSTEM: return StringKey.PLATFORM_SYSTEM;
                case PLATFORM_MACHINE: return StringKey.PLATFORM_MACHINE;
                case PLATFORM_PYTHON_IMPLEMENTATION: return StringKey.PLATFORM_PYTHON_IMPLEMENTATION;
                case PLATFORM_RELEASE: return StringKey.PLATFORM_RELEASE;
                case PLATFORM_VERSION: return StringKey.PLATFORM_VERSION;
                default: return StringKey.IMPLEMENTATION_NAME;
            }
        }
    }

    /** A version variable of the decision diagram, declared in its variable order. */
    public enum CanonicalVersion {
        IMPLEMENTATION_VERSION("implementation_version"),
        PYTHON_FULL_VERSION("python_full_version");

        final String display;

        CanonicalVersion(String display) {
            this.display = display;
        }

        VersionKey key() {
            return this == IMPLEMENTATION_VERSION ? VersionKey.IMPLEMENTATION_VERSION : VersionKey.PYTHON_FULL_VERSION;
        }
    }

    /** A list-valued environment key, only ever on the right of {@code in}. */
    public enum ListKey {
        EXTRAS("extras"),
        DEPENDENCY_GROUPS("dependency_groups");

        final String display;

        ListKey(String display) {
            this.display = display;
        }
    }

    /** A marker comparison operator. */
    public enum Operator {
        EQUAL("=="), NOT_EQUAL("!="), GREATER_THAN(">"), GREATER_EQUAL(">="), LESS_THAN("<"),
        LESS_EQUAL("<="), TILDE_EQUAL("~="), IN("in"), NOT_IN("not in"), CONTAINS("in"), NOT_CONTAINS("not in");

        final String display;

        Operator(String display) {
            this.display = display;
        }

        Specifier.@Nullable Op toPep440() {
            switch (this) {
                case EQUAL: return Specifier.Op.EQUAL;
                case NOT_EQUAL: return Specifier.Op.NOT_EQUAL;
                case GREATER_THAN: return Specifier.Op.GREATER_THAN;
                case GREATER_EQUAL: return Specifier.Op.GREATER_THAN_EQUAL;
                case LESS_THAN: return Specifier.Op.LESS_THAN;
                case LESS_EQUAL: return Specifier.Op.LESS_THAN_EQUAL;
                case TILDE_EQUAL: return Specifier.Op.TILDE_EQUAL;
                default: return null;
            }
        }

        Operator invert() {
            switch (this) {
                case LESS_THAN: return GREATER_THAN;
                case LESS_EQUAL: return GREATER_EQUAL;
                case GREATER_THAN: return LESS_THAN;
                case GREATER_EQUAL: return LESS_EQUAL;
                case IN: return CONTAINS;
                case NOT_IN: return NOT_CONTAINS;
                case CONTAINS: return IN;
                case NOT_CONTAINS: return NOT_IN;
                default: return this;
            }
        }

        static @Nullable Operator parse(String s) {
            switch (s) {
                case "==": return EQUAL;
                case "!=": return NOT_EQUAL;
                case ">": return GREATER_THAN;
                case ">=": return GREATER_EQUAL;
                case "<": return LESS_THAN;
                case "<=": return LESS_EQUAL;
                case "~=": return TILDE_EQUAL;
                case "in": return IN;
                default: return null;
            }
        }
    }

    /**
     * A PEP 440 version comparison as uv-pep440's {@code VersionSpecifier} builds it. The star forms are
     * operators of their own, and a local version or a short {@code ~=} is rejected.
     */
    @Getter
    @EqualsAndHashCode
    public static final class Specifier {
        public enum Op {
            EQUAL("=="), EQUAL_STAR("=="), EXACT_EQUAL("==="), NOT_EQUAL("!="), NOT_EQUAL_STAR("!="),
            TILDE_EQUAL("~="), LESS_THAN("<"), LESS_THAN_EQUAL("<="), GREATER_THAN(">"), GREATER_THAN_EQUAL(">=");

            final String display;

            Op(String display) {
                this.display = display;
            }

            boolean isStar() {
                return this == EQUAL_STAR || this == NOT_EQUAL_STAR;
            }
        }

        private final Op op;
        private final PythonVersion version;

        Specifier(Op op, PythonVersion version) {
            this.op = op;
            this.version = version;
        }

        /** A version that may end in {@code .*}, or null where uv rejects the combination. */
        static @Nullable Specifier fromPattern(Op op, String pattern) {
            boolean star = pattern.endsWith(".*");
            String text = star ? pattern.substring(0, pattern.length() - 2) : pattern;
            if (star && !text.matches("[ \\t\\n\\f\\r]*[vV]?(\\d+!)?\\d+(\\.\\d+)*")) {
                return null;
            }
            PythonVersion version = PythonVersion.parse(text);
            if (version == null) {
                return null;
            }
            if (star) {
                if (op == Op.EQUAL) {
                    op = Op.EQUAL_STAR;
                } else if (op == Op.NOT_EQUAL) {
                    op = Op.NOT_EQUAL_STAR;
                } else {
                    return null;
                }
            }
            return fromVersion(op, version);
        }

        static @Nullable Specifier fromVersion(Op op, PythonVersion version) {
            if (!fitsReleaseArithmetic(version)) {
                return null;
            }
            boolean localCompatible = op == Op.EQUAL || op == Op.EXACT_EQUAL || op == Op.NOT_EQUAL;
            if (version.getLocal() != null && !localCompatible) {
                return null;
            }
            if (op == Op.TILDE_EQUAL && version.getRelease().length < 2) {
                return null;
            }
            return new Specifier(op, version);
        }

        /** The same comparison with every segment but the release dropped. */
        Specifier onlyRelease() {
            return new Specifier(op, releaseVersion(version.getRelease(), version.getRelease().length));
        }

        @Override
        public String toString() {
            return op.display + version + (op.isStar() ? ".*" : "");
        }
    }

    /** Whether every release segment has a successor, as uv's u64 segments stop short of their maximum. */
    static boolean fitsReleaseArithmetic(PythonVersion version) {
        for (long segment : version.getRelease()) {
            if (segment == Long.MAX_VALUE) {
                return false;
            }
        }
        return true;
    }

    static PythonVersion releaseVersion(long[] release, int length) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                b.append('.');
            }
            b.append(release[i]);
        }
        PythonVersion v = PythonVersion.parse(b.toString());
        assert v != null;
        return v;
    }

    /** The release with trailing zeros dropped, {@code 0} for a release of zeros. */
    static PythonVersion onlyReleaseTrimmed(PythonVersion version) {
        long[] release = version.getRelease();
        int last = release.length - 1;
        while (last >= 0 && release[last] == 0) {
            last--;
        }
        return last < 0 ? releaseVersion(new long[]{0}, 1) : releaseVersion(release, last + 1);
    }

    /** uv's normalized extra, group or package name, or null for a name uv rejects. */
    static @Nullable String normalizeName(String name) {
        if (name.isEmpty()) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        char last = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                b.append((char) (c - 'A' + 'a'));
            } else if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                b.append(c);
            } else if (c == '-' || c == '_' || c == '.') {
                if (last == 0) {
                    return null;
                }
                if (last != '-' && last != '_' && last != '.') {
                    b.append('-');
                }
            } else {
                return null;
            }
            last = c;
        }
        return last == '-' || last == '_' || last == '.' ? null : b.toString();
    }

    // ---- the expression kinds

    /** {@code <version key> <op> '<version>'}. */
    @Getter
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    public static final class Version extends MarkerExpression {
        private final VersionKey key;
        private final Specifier specifier;

        @Override
        public String toString() {
            return key.display + " " + specifier.getOp().display + " '" + specifier.getVersion() +
                    (specifier.getOp().isStar() ? ".*" : "") + "'";
        }
    }

    /** {@code <version key> [not] in '<version> <version> ...'}, matched version-aware. */
    @Getter
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    public static final class VersionIn extends MarkerExpression {
        private final VersionKey key;
        private final List<PythonVersion> versions;
        private final boolean notIn;

        @Override
        public String toString() {
            List<String> parts = new ArrayList<>();
            for (PythonVersion v : versions) {
                parts.add(v.toString());
            }
            return key.display + (notIn ? " not in '" : " in '") + String.join(" ", parts) + "'";
        }
    }

    /** {@code <string key> <op> '<value>'}, with {@code CONTAINS} for {@code '<value>' in <key>}. */
    @Getter
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    public static final class Str extends MarkerExpression {
        private final StringKey key;
        private final Operator operator;
        private final String value;

        @Override
        public String toString() {
            if (operator == Operator.CONTAINS || operator == Operator.NOT_CONTAINS) {
                return "'" + value + "' " + operator.invert().display + " " + key.display;
            }
            return key.display + " " + operator.display + " '" + value + "'";
        }
    }

    /** {@code '<value>' [not] in extras} or {@code dependency_groups}, with the value normalized where uv can. */
    @Getter
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class ListPair extends MarkerExpression {
        /** The pair's kind, in uv's {@code CanonicalMarkerListPair} order of extras, group and arbitrary value. */
        enum Kind { EXTRAS, DEPENDENCY_GROUP, ARBITRARY }

        private final Kind kind;
        private final ListKey key;
        private final String value;
        private final boolean notIn;

        @Override
        public String toString() {
            return "'" + value + "' " + (notIn ? "not in" : "in") + " " + key.display;
        }
    }

    /** {@code extra == '<name>'} or {@code !=}. An invalid name is kept as given and is always false. */
    @Getter
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    public static final class Extra extends MarkerExpression {
        private final String name;
        private final boolean valid;
        private final boolean notEqual;

        @Override
        public String toString() {
            return "extra " + (notEqual ? "!=" : "==") + " '" + name + "'";
        }
    }
}
