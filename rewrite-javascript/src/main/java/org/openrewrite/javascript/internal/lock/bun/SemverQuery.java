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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;

import static org.openrewrite.javascript.internal.lock.bun.SemverVersion.MAX;
import static org.openrewrite.javascript.internal.lock.bun.SemverVersion.saturatingIncrement;

/**
 * Port of bun's src/semver/SemverQuery.zig (bun 1.3.10), with SemverQuery.rs (1.4.0) beside it: a range is a
 * {@link Group} of OR'd {@link List}s of AND'd queries.
 */
final class SemverQuery {

    enum Wildcard {
        NONE, MAJOR, MINOR, PATCH
    }

    SemverRange range = SemverRange.EMPTY;
    @Nullable SemverQuery next;

    boolean eql(SemverQuery rhs) {
        if (!range.eql(rhs.range)) {
            return false;
        }
        if (next == null) {
            return rhs.next == null;
        }
        return rhs.next != null && next.eql(rhs.next);
    }

    boolean satisfies(SemverVersion version, BunRelease release) {
        return range.satisfies(version, release) && (next == null || next.satisfies(version, release));
    }

    boolean satisfiesPre(SemverVersion version, boolean[] preMatched, BunRelease release) {
        return range.satisfiesPre(version, preMatched, release) &&
               (next == null || next.satisfiesPre(version, preMatched, release));
    }

    @Override
    public String toString() {
        if (next != null && (next.range.hasLeft() || next.range.hasRight())) {
            return range + " && " + next.range;
        }
        return range.toString();
    }

    static final class List {
        final SemverQuery head = new SemverQuery();
        @Nullable SemverQuery tail;
        @Nullable List next;

        boolean satisfies(SemverVersion version, BunRelease release) {
            return head.satisfies(version, release) || next != null && next.satisfies(version, release);
        }

        boolean satisfiesPre(SemverVersion version, BunRelease release) {
            boolean[] preMatched = {false};
            return head.satisfiesPre(version, preMatched, release) && preMatched[0] ||
                   next != null && next.satisfiesPre(version, release);
        }

        boolean eql(List rhs) {
            if (!head.eql(rhs.head)) {
                return false;
            }
            if (next == null) {
                return rhs.next == null;
            }
            return rhs.next != null && next.eql(rhs.next);
        }

        void andRange(SemverRange range) {
            if (!head.range.hasLeft() && !head.range.hasRight()) {
                head.range = range;
                return;
            }
            SemverQuery tail = new SemverQuery();
            tail.range = range;
            SemverQuery lastTail = this.tail != null ? this.tail : head;
            lastTail.next = tail;
            this.tail = tail;
        }

        @Override
        public String toString() {
            return next != null ? head + " || " + next : head.toString();
        }
    }

    /**
     * A range as the release that parsed it reads it, and compares versions with it ({@link SemverVersion#orderPre}).
     */
    static final class Group {
        static final int FLAG_BUILD = 1;
        static final int FLAG_PRE = 2;

        final List head = new List();
        @Nullable List tail;
        final String input;
        final BunRelease release;
        int flags;

        Group(String input, BunRelease release) {
            this.input = input;
            this.release = release;
        }

        boolean isStar() {
            SemverRange.Comparator left = head.head.range.left;
            return head.head.range.right.op == SemverRange.Op.UNSET && left.op == SemverRange.Op.GTE &&
                   head.next == null && head.head.next == null && left.version.isZero() &&
                   (flags & FLAG_BUILD) == 0;
        }

        boolean eql(Group rhs) {
            return head.eql(rhs.head);
        }

        void orVersion(SemverVersion version) {
            if (tail == null && !head.head.range.hasLeft()) {
                head.head.range = new SemverRange(new SemverRange.Comparator(SemverRange.Op.EQL, version),
                        head.head.range.right);
                return;
            }
            List newTail = new List();
            newTail.head.range = new SemverRange(new SemverRange.Comparator(SemverRange.Op.EQL, version));
            List prevTail = tail != null ? tail : head;
            prevTail.next = newTail;
            tail = newTail;
        }

        void andRange(SemverRange range) {
            (tail != null ? tail : head).andRange(range);
        }

        void orRange(SemverRange range) {
            if (tail == null && head.tail == null && !head.head.range.hasLeft()) {
                head.head.range = range;
                return;
            }
            List newTail = new List();
            newTail.head.range = range;
            List prevTail = tail != null ? tail : head;
            prevTail.next = newTail;
            tail = newTail;
        }

        boolean satisfies(SemverVersion version) {
            return version.hasPre() ? head.satisfiesPre(version, release) : head.satisfies(version, release);
        }

        @Override
        public String toString() {
            if (tail == null && head.tail == null && !head.head.range.hasLeft()) {
                return "";
            }
            if (tail == null && head.tail == null) {
                return head.toString();
            }
            StringBuilder out = new StringBuilder();
            List list = head;
            while (list.next != null) {
                out.append(list).append(" && ");
                list = list.next;
            }
            return out.append(list).toString();
        }
    }

    enum TokenTag {
        NONE, GT, GTE, LT, LTE, VERSION, TILDA, CARET
    }

    static final class Token {
        TokenTag tag = TokenTag.NONE;
        Wildcard wildcard = Wildcard.NONE;

        private static SemverRange left(SemverRange.Op op, SemverVersion version) {
            return new SemverRange(new SemverRange.Comparator(op, version));
        }

        /**
         * {@code toRange}, an upper bound that would pass the ceiling ending at it from 1.4.0
         * ({@link SemverRange.Comparator#ltNextMajor}), the ceiling being the release's ({@link SemverVersion#max}).
         */
        SemverRange toRange(SemverVersion.Partial version, BunRelease release) {
            long max = SemverVersion.max(release);
            switch (tag) {
                case CARET: {
                    if (version.major == null) {
                        return SemverRange.EMPTY;
                    }
                    long major = version.major;
                    SemverVersion left = SemverVersion.of(major, 0, 0);
                    SemverRange.Comparator right = null;
                    if (version.minor != null) {
                        long minor = version.minor;
                        left = SemverVersion.of(major, minor, 0);
                        if (version.patch != null) {
                            long patch = version.patch;
                            left = new SemverVersion(major, minor, patch, version.pre, version.build);
                            if (major == 0) {
                                right = minor == 0 ? SemverRange.Comparator.ltNextPatch(0, 0, patch, release) :
                                        SemverRange.Comparator.ltNextMinor(0, minor, release);
                            }
                        } else if (major == 0) {
                            right = SemverRange.Comparator.ltNextMinor(0, minor, release);
                        }
                    }
                    if (right == null) {
                        right = SemverRange.Comparator.ltNextMajor(major, release);
                    }
                    return new SemverRange(new SemverRange.Comparator(SemverRange.Op.GTE, left), right);
                }
                case TILDA: {
                    if (version.major == null) {
                        return SemverRange.EMPTY;
                    }
                    long major = version.major;
                    SemverVersion left = SemverVersion.of(major, 0, 0);
                    SemverRange.Comparator right;
                    if (version.minor != null) {
                        long minor = version.minor;
                        left = SemverVersion.of(major, minor, 0);
                        if (version.patch != null) {
                            left = new SemverVersion(major, minor, version.patch, version.pre, version.build);
                        }
                        right = SemverRange.Comparator.ltNextMinor(major, minor, release);
                    } else {
                        right = SemverRange.Comparator.ltNextMajor(major, release);
                    }
                    return new SemverRange(new SemverRange.Comparator(SemverRange.Op.GTE, left), right);
                }
                case NONE:
                    throw new IllegalStateException("unreachable");
                case VERSION:
                    if (wildcard != Wildcard.NONE) {
                        return SemverRange.initWildcard(version.min(), wildcard, release);
                    }
                    return new SemverRange(new SemverRange.Comparator(SemverRange.Op.EQL, version.min()));
                default:
                    break;
            }
            long major = version.major == null ? 0 : version.major;
            long minor = version.minor == null ? 0 : version.minor;
            switch (wildcard) {
                case MAJOR:
                    return new SemverRange(new SemverRange.Comparator(SemverRange.Op.GTE, version.min()),
                            new SemverRange.Comparator(SemverRange.Op.LTE, SemverVersion.of(max, max, max)));
                case MINOR:
                    switch (tag) {
                        case LTE:
                            return left(SemverRange.Op.LTE, SemverVersion.of(major, max, max));
                        case LT:
                            return left(SemverRange.Op.LT, SemverVersion.of(major, 0, 0));
                        case GT:
                            return left(SemverRange.Op.GT, SemverVersion.of(major, max, max));
                        default:
                            return left(SemverRange.Op.GTE, SemverVersion.of(major, 0, 0));
                    }
                case PATCH:
                    switch (tag) {
                        case LTE:
                            return left(SemverRange.Op.LTE, SemverVersion.of(major, minor, max));
                        case LT:
                            return left(SemverRange.Op.LT, SemverVersion.of(major, minor, 0));
                        case GT:
                            return left(SemverRange.Op.GT, SemverVersion.of(major, minor, max));
                        default:
                            return left(SemverRange.Op.GTE, SemverVersion.of(major, minor, 0));
                    }
                default: {
                    SemverRange.Op op;
                    switch (tag) {
                        case GT:
                            op = SemverRange.Op.GT;
                            break;
                        case GTE:
                            op = SemverRange.Op.GTE;
                            break;
                        case LT:
                            op = SemverRange.Op.LT;
                            break;
                        default:
                            op = SemverRange.Op.LTE;
                            break;
                    }
                    return new SemverRange(new SemverRange.Comparator(op, version.min()));
                }
            }
        }
    }

    static Group parse(String input) {
        return parse(input, BunRelease.PORTED);
    }

    /**
     * {@code parse} as the release reads a range: from 1.4.0 a chunk without a comparator is skipped wherever it stands
     * ({@link BunRelease#skipsARangeChunkWithoutAComparator()}), and versions and bounds are read as
     * {@link SemverVersion#parse(String, BunRelease)} and {@link SemverRange.Comparator#ltNextMajor} read them.
     *
     * @throws CannotKnow before 1.4.0 for such a chunk after a comparator, where bun reaches code it takes for
     *                    unreachable, which its release build does not check
     */
    static Group parse(String input, BunRelease release) {
        int i = 0;
        Group list = new Group(input, release);
        Token token = new Token();
        Token prevToken = new Token();
        int count = 0;
        boolean skipRound;
        boolean isOr = false;

        while (i < input.length()) {
            skipRound = false;
            char c = input.charAt(i);
            switch (c) {
                case '>':
                    if (input.length() > i + 1 && input.charAt(i + 1) == '=') {
                        token.tag = TokenTag.GTE;
                        i++;
                    } else {
                        token.tag = TokenTag.GT;
                    }
                    i++;
                    i = skipSpaces(input, i);
                    break;
                case '<':
                    if (input.length() > i + 1 && input.charAt(i + 1) == '=') {
                        token.tag = TokenTag.LTE;
                        i++;
                    } else {
                        token.tag = TokenTag.LT;
                    }
                    i++;
                    i = skipSpaces(input, i);
                    break;
                case '=':
                case 'v':
                    token.tag = TokenTag.VERSION;
                    isOr = true;
                    i++;
                    i = skipSpaces(input, i);
                    break;
                case '~':
                    token.tag = TokenTag.TILDA;
                    i++;
                    if (i < input.length() && input.charAt(i) == '>') {
                        i++;
                    }
                    i = skipSpaces(input, i);
                    break;
                case '^':
                    token.tag = TokenTag.CARET;
                    i++;
                    i = skipSpaces(input, i);
                    break;
                case '|':
                    i++;
                    while (i < input.length() && input.charAt(i) == '|') {
                        i++;
                    }
                    i = skipSpaces(input, i);
                    isOr = true;
                    token.tag = TokenTag.NONE;
                    skipRound = true;
                    break;
                case '-':
                    i++;
                    i = skipSpaces(input, i);
                    break;
                case ' ':
                    i++;
                    i = skipSpaces(input, i);
                    skipRound = true;
                    break;
                default:
                    if (c >= '0' && c <= '9' || c == 'X' || c == 'x' || c == '*') {
                        token.tag = TokenTag.VERSION;
                        isOr = true;
                    } else {
                        i++;
                        token.tag = TokenTag.NONE;
                        while (i < input.length() && input.charAt(i) != ' ' && input.charAt(i) != '|') {
                            i++;
                        }
                        skipRound = true;
                    }
                    break;
            }

            if (skipRound) {
                continue;
            }
            SemverVersion.ParseResult parseResult = SemverVersion.parse(input.substring(i), release);
            SemverVersion version = parseResult.version.min();
            if (version.hasBuild()) {
                list.flags |= Group.FLAG_BUILD;
            }
            if (version.hasPre()) {
                list.flags |= Group.FLAG_PRE;
            }
            token.wildcard = parseResult.wildcard;
            i += parseResult.len;
            int rollback = i;
            boolean maybeHyphenate = i < input.length() && (input.charAt(i) == ' ' || input.charAt(i) == '-');
            boolean hyphenate = false;
            if (maybeHyphenate) {
                hyphenate = true;
                i += leadingWhitespace(input, i);
                if (!(i < input.length() && input.charAt(i) == '-')) {
                    hyphenate = false;
                } else {
                    i++;
                    i += leadingWhitespace(input, i);
                    if (i == input.length()) {
                        hyphenate = false;
                    } else {
                        if (input.charAt(i) == 'v' || input.charAt(i) == '=') {
                            i++;
                        }
                        if (i == input.length()) {
                            hyphenate = false;
                        } else {
                            i += leadingWhitespace(input, i);
                            if (i == input.length()) {
                                hyphenate = false;
                            } else {
                                char d = input.charAt(i);
                                if (!(d >= '0' && d <= '9' || d == 'X' || d == 'x' || d == '*')) {
                                    hyphenate = false;
                                }
                            }
                        }
                    }
                }
            }
            if (!hyphenate) {
                i = rollback;
            }
            i += hyphenate ? 0 : 1;

            if (hyphenate) {
                SemverVersion.ParseResult secondParsed = SemverVersion.parse(input.substring(i), release);
                SemverVersion second = secondParsed.version.min();
                if (second.hasBuild()) {
                    list.flags |= Group.FLAG_BUILD;
                }
                if (second.hasPre()) {
                    list.flags |= Group.FLAG_PRE;
                }
                boolean inclusiveCeiling = release.boundsARangeAtTheCeilingInclusively();
                SemverRange range;
                switch (secondParsed.wildcard) {
                    case MAJOR:
                        range = new SemverRange(new SemverRange.Comparator(SemverRange.Op.GTE, version));
                        break;
                    case MINOR:
                        // the bound keeps the second version's tag, but not at the ceiling
                        range = new SemverRange(new SemverRange.Comparator(SemverRange.Op.GTE, version),
                                inclusiveCeiling && second.major == MAX ?
                                        SemverRange.Comparator.ltNextMajor(second.major, release) :
                                        new SemverRange.Comparator(SemverRange.Op.LT,
                                                second.with(saturatingIncrement(second.major, release), 0, 0)));
                        break;
                    case PATCH:
                        range = new SemverRange(new SemverRange.Comparator(SemverRange.Op.GTE, version),
                                inclusiveCeiling && second.minor == MAX ?
                                        SemverRange.Comparator.ltNextMinor(second.major, second.minor, release) :
                                        new SemverRange.Comparator(SemverRange.Op.LT,
                                                second.with(second.major, saturatingIncrement(second.minor, release),
                                                        0)));
                        break;
                    default:
                        range = new SemverRange(new SemverRange.Comparator(SemverRange.Op.GTE, version),
                                new SemverRange.Comparator(SemverRange.Op.LTE, second));
                        break;
                }
                if (isOr) {
                    list.orRange(range);
                } else {
                    list.andRange(range);
                }
                i += secondParsed.len + 1;
            } else if (token.tag == TokenTag.NONE && release.skipsARangeChunkWithoutAComparator()) {
                // like "boop" in "1.0.0 || boop", keeping a pending "||": "--foo", and the "- foo" of "1 || - foo"
                token.wildcard = Wildcard.NONE;
                continue;
            } else if (count == 0 && token.tag == TokenTag.VERSION) {
                if (parseResult.wildcard == Wildcard.NONE) {
                    list.orVersion(version);
                } else {
                    list.orRange(token.toRange(parseResult.version, release));
                }
            } else if (count == 0) {
                if (token.tag == TokenTag.NONE) {
                    isOr = false;
                    token.wildcard = Wildcard.NONE;
                    prevToken.tag = TokenTag.NONE;
                    continue;
                }
                list.andRange(token.toRange(parseResult.version, release));
            } else if (token.tag == TokenTag.NONE) {
                throw new CannotKnow("what bun " + release + " makes of the range \"" + input + "\": a chunk " +
                                     "without a comparator after the first has its parser reach code it takes for " +
                                     "unreachable");
            } else if (isOr) {
                list.orRange(token.toRange(parseResult.version, release));
            } else {
                list.andRange(token.toRange(parseResult.version, release));
            }

            isOr = false;
            count++;
            token.wildcard = Wildcard.NONE;
            prevToken.tag = token.tag;
        }
        return list;
    }

    private static int skipSpaces(String input, int i) {
        while (i < input.length() && input.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private static int leadingWhitespace(String input, int i) {
        int n = 0;
        while (i + n < input.length()) {
            char c = input.charAt(i + n);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000B' || c == '\u000C') {
                n++;
            } else {
                break;
            }
        }
        return n;
    }
}
