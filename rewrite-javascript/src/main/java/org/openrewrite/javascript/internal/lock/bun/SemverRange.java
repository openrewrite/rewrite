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
package org.openrewrite.javascript.internal.lock.bun;

/**
 * Port of bun's src/semver/SemverRange.zig (bun 1.3.10).
 */
final class SemverRange {

    enum Op {
        UNSET, EQL, LT, LTE, GT, GTE
    }

    static final class Comparator {
        static final Comparator UNSET = new Comparator(Op.UNSET, SemverVersion.ZERO);

        final Op op;
        final SemverVersion version;

        Comparator(Op op, SemverVersion version) {
            this.op = op;
            this.version = version;
        }

        boolean eql(Comparator rhs) {
            return op == rhs.op && version.eql(rhs.version);
        }

        boolean satisfies(SemverVersion version) {
            int order = version.orderWithoutBuild(this.version);
            if (order == 0) {
                return op == Op.LTE || op == Op.GTE || op == Op.EQL;
            } else if (order > 0) {
                return op == Op.GT || op == Op.GTE;
            }
            return op == Op.LT || op == Op.LTE;
        }

        @Override
        public String toString() {
            switch (op) {
                case UNSET:
                    return "";
                case EQL:
                    return "==" + version;
                case LT:
                    return "<" + version;
                case LTE:
                    return "<=" + version;
                case GT:
                    return ">" + version;
                default:
                    return ">=" + version;
            }
        }
    }

    final Comparator left;
    final Comparator right;

    SemverRange(Comparator left, Comparator right) {
        this.left = left;
        this.right = right;
    }

    SemverRange(Comparator left) {
        this(left, Comparator.UNSET);
    }

    static final SemverRange EMPTY = new SemverRange(Comparator.UNSET, Comparator.UNSET);

    static SemverRange initWildcard(SemverVersion version, SemverQuery.Wildcard wildcard) {
        switch (wildcard) {
            case NONE:
                return new SemverRange(new Comparator(Op.EQL, version));
            case MAJOR:
                return new SemverRange(new Comparator(Op.GTE, SemverVersion.ZERO));
            case MINOR:
                return new SemverRange(
                        new Comparator(Op.LT, SemverVersion.of(SemverVersion.saturatingIncrement(version.major), 0, 0)),
                        new Comparator(Op.GTE, SemverVersion.of(version.major, 0, 0)));
            default:
                return new SemverRange(
                        new Comparator(Op.LT,
                                SemverVersion.of(version.major, SemverVersion.saturatingIncrement(version.minor), 0)),
                        new Comparator(Op.GTE, SemverVersion.of(version.major, version.minor, 0)));
        }
    }

    boolean hasLeft() {
        return left.op != Op.UNSET;
    }

    boolean hasRight() {
        return right.op != Op.UNSET;
    }

    boolean eql(SemverRange rhs) {
        return left.eql(rhs.left) && right.eql(rhs.right);
    }

    boolean satisfies(SemverVersion version) {
        if (!hasLeft()) {
            return true;
        }
        if (!left.satisfies(version)) {
            return false;
        }
        return !hasRight() || right.satisfies(version);
    }

    boolean satisfiesPre(SemverVersion version, boolean[] preMatched) {
        if (!hasLeft()) {
            return true;
        }
        preMatched[0] = preMatched[0] ||
                        left.version.hasPre() && version.patch == left.version.patch &&
                        version.minor == left.version.minor && version.major == left.version.major ||
                        hasRight() && right.version.hasPre() && version.patch == right.version.patch &&
                        version.minor == right.version.minor && version.major == right.version.major;
        if (!left.satisfies(version)) {
            return false;
        }
        return !hasRight() || right.satisfies(version);
    }

    @Override
    public String toString() {
        if (!hasLeft() && !hasRight()) {
            return "";
        }
        return !hasRight() ? left.toString() : left + " " + right;
    }
}
