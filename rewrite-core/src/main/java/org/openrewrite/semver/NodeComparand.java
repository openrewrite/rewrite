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
 * A primitive npm range clause, ported from node-semver's {@code Comparator}: an operator applied
 * to a version, or the ANY clause from {@code *}/{@code x}/an empty range. Also hosts the npm
 * grammar fragments the range classes share.
 */
final class NodeComparand {

    static final String XRANGE_ID = ParsedVersion.NUMERIC_ID + "|x|X|\\*";

    private static final String PRERELEASE_GROUP =
            "(?:-(" + ParsedVersion.PRERELEASE_ID + "(?:\\." + ParsedVersion.PRERELEASE_ID + ")*))";

    private static final String BUILD = "(?:\\+[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*)";

    // node-semver XRANGEPLAIN; groups: major, minor, patch, prerelease.
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
     * @return the parsed comparand, or {@code null}, which invalidates the whole range.
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

    static boolean isX(@Nullable String id) {
        return id == null || "x".equalsIgnoreCase(id) || "*".equals(id);
    }

    // num + 1; a result beyond long range fails strict parsing later, invalidating the range as node does.
    static String incr(String num) {
        return new BigInteger(num).add(BigInteger.ONE).toString();
    }
}
