/*
 * Copyright 2026 the original author or authors.
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

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single primitive npm range clause: an operator ({@code < <= > >= =}) applied to a concrete
 * strict-SemVer version, or the special ANY clause (matches every version) produced by
 * {@code *}/{@code x}/an empty range. Ported from node-semver's {@code Comparator}. All npm range
 * sugar ({@code ^ ~ x hyphen} and bare versions) desugars into these primitives — the caret, tilde,
 * x-range and hyphen rewrites live on {@link CaretRange}, {@link TildeRange}, {@link XRange} and
 * {@link HyphenRange} beside their Maven-flavored interpretations, and {@link UnionRange} assembles
 * the resulting clauses into an OR of AND-groups.
 * <p>
 * This class also hosts the npm grammar fragments those rewrites share.
 */
final class NodeComparand {

    /**
     * A version component in npm's "x-range" position: a numeric identifier or an
     * {@code x}/{@code X}/{@code *} wildcard.
     */
    static final String XRANGE_ID = ParsedVersion.NUMERIC_ID + "|x|X|\\*";

    private static final String PRERELEASE_GROUP =
            "(?:-(" + ParsedVersion.PRERELEASE_ID + "(?:\\." + ParsedVersion.PRERELEASE_ID + ")*))";

    private static final String BUILD = "(?:\\+[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*)";

    /**
     * node-semver {@code XRANGEPLAIN}: major, optional minor, optional patch, optional
     * prerelease/build, any component possibly a wildcard. Four capture groups: major, minor, patch,
     * prerelease.
     */
    static final String XRANGE_PLAIN =
            "[v=\\s]*(" + XRANGE_ID + ")(?:\\.(" + XRANGE_ID + ")(?:\\.(" + XRANGE_ID + ")" +
                    PRERELEASE_GROUP + "?" + BUILD + "?)?)?";

    // node-semver re.js COMPARATOR: optional operator, optional whitespace, then a full version.
    // Only groups 1 (operator) and 2 (version) are read; the prerelease group inside is unused.
    private static final Pattern COMPARATOR = Pattern.compile(
            "^((?:<|>)?=?)\\s*(v?" +
                    "(?:" + ParsedVersion.NUMERIC_ID + ")\\.(?:" + ParsedVersion.NUMERIC_ID + ")\\.(?:" + ParsedVersion.NUMERIC_ID + ")" +
                    PRERELEASE_GROUP + "?" + BUILD + "?)$");

    enum Op {
        LT("<"), LTE("<="), GT(">"), GTE(">="), EQ("=");

        private final String symbol;

        Op(String symbol) {
            this.symbol = symbol;
        }
    }

    static final NodeComparand GTE_ZERO = new NodeComparand(Op.GTE, ParsedVersion.parse("0.0.0"));

    private final Op op;

    // null == the ANY comparand.
    private final @Nullable ParsedVersion version;

    private NodeComparand(Op op, @Nullable ParsedVersion version) {
        this.op = op;
        this.version = version;
    }

    /**
     * @return the parsed comparand, or {@code null} if the token is not a valid comparator (which
     * invalidates the whole range).
     */
    static @Nullable NodeComparand parse(String token) {
        String t = token.trim();
        if (t.isEmpty()) {
            return new NodeComparand(Op.GTE, null);
        }
        Matcher m = COMPARATOR.matcher(t);
        if (!m.matches()) {
            return null;
        }
        ParsedVersion version = ParsedVersion.parse(m.group(2));
        if (!version.isStrictSemver()) {
            return null;
        }
        String operator = m.group(1);
        Op op;
        switch (operator) {
            case "<":
                op = Op.LT;
                break;
            case "<=":
                op = Op.LTE;
                break;
            case ">":
                op = Op.GT;
                break;
            case ">=":
                op = Op.GTE;
                break;
            default:
                // "" and "=" both mean exact equality in node-semver.
                op = Op.EQ;
                break;
        }
        return new NodeComparand(op, version);
    }

    boolean isAny() {
        return version == null;
    }

    Op getOp() {
        return op;
    }

    ParsedVersion getVersion() {
        //noinspection DataFlowIssue
        return version;
    }

    boolean test(ParsedVersion candidate) {
        if (version == null) {
            return true;
        }
        int c = candidate.comparePrecedence(version);
        switch (op) {
            case LT:
                return c < 0;
            case LTE:
                return c <= 0;
            case GT:
                return c > 0;
            case GTE:
                return c >= 0;
            default:
                return c == 0;
        }
    }

    @Override
    public String toString() {
        if (version == null) {
            return "";
        }
        // node-semver renders "=" and "" operators as just the version.
        return (op == Op.EQ ? "" : op.symbol) + version.strictToString();
    }

    /**
     * @return whether {@code id} is a wildcard (or absent) component of an x-range position.
     */
    static boolean isX(@Nullable String id) {
        return id == null || "x".equalsIgnoreCase(id) || "*".equals(id);
    }

    /**
     * The numeric component one greater than {@code num}, used when desugaring sugar into an
     * exclusive upper bound. Total for any numeric input: a component beyond {@code long} range
     * yields a bound that later fails strict parsing, invalidating the range (node-semver likewise
     * rejects such components).
     */
    static String incr(String num) {
        return new BigInteger(num).add(BigInteger.ONE).toString();
    }
}
