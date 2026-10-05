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
 * to a version, or the ANY clause from {@code *}/{@code x}/an empty range.
 */
final class NodeComparand {

    // node-semver re.js COMPARATOR: optional operator, optional whitespace, then a full version.
    // Only groups 1 (operator) and 2 (version) are read; the prerelease group inside is unused.
    private static final Pattern COMPARATOR = Pattern.compile(
            "^((?:<|>)?=?)\\s*(v?" +
                    "(?:" + ParsedVersion.NUMERIC_ID + ")\\.(?:" + ParsedVersion.NUMERIC_ID + ")\\.(?:" + ParsedVersion.NUMERIC_ID + ")" +
                    ParsedVersion.PRERELEASE_GROUP + "?" + ParsedVersion.BUILD + "?)$");

    // node-semver re.js COMPARATORLOOSE.
    static final Pattern COMPARATOR_LOOSE = Pattern.compile(
            "^((?:<|>)?=?)\\s*(" + ParsedVersion.LOOSE_PLAIN + ")$|^$");

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

    static @Nullable NodeComparand parse(String token) {
        return parse(token, false);
    }

    /**
     * @return the parsed comparand, or {@code null}, which invalidates the whole range.
     */
    static @Nullable NodeComparand parse(String token, boolean loose) {
        String t = token.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) {
            return new NodeComparand(Op.GTE, null);
        }
        Matcher m = (loose ? COMPARATOR_LOOSE : COMPARATOR).matcher(t);
        if (!m.matches() || m.group(2) == null) {
            return null;
        }
        ParsedVersion version = ParsedVersion.parseNode(m.group(2), loose);
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

    // node-semver's Comparator.operator: "" for equality and for ANY.
    String operator() {
        return version == null || op == Op.EQ ? "" : op.symbol;
    }

    // node-semver's Comparator.value.
    String value() {
        return version == null ? "" : operator() + version.nodeVersion();
    }

    boolean isNullSet() {
        return "<0.0.0-0".equals(value());
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

    // Port of node-semver classes/comparator.js intersects.
    boolean intersects(NodeComparand comp, boolean loose, boolean includePrerelease) {
        if (operator().isEmpty()) {
            if (value().isEmpty()) {
                return true;
            }
            UnionRange range = UnionRange.parse(comp.value(), includePrerelease, loose);
            return range != null && range.test(value(), loose);
        } else if (comp.operator().isEmpty()) {
            if (comp.value().isEmpty()) {
                return true;
            }
            UnionRange range = UnionRange.parse(value(), includePrerelease, loose);
            return range != null && range.test(comp.getVersion());
        }

        if (includePrerelease && ("<0.0.0-0".equals(value()) || "<0.0.0-0".equals(comp.value()))) {
            return false;
        }
        if (!includePrerelease && (value().startsWith("<0.0.0") || comp.value().startsWith("<0.0.0"))) {
            return false;
        }

        String thisOp = operator();
        String compOp = comp.operator();
        if (thisOp.startsWith(">") && compOp.startsWith(">")) {
            return true;
        }
        if (thisOp.startsWith("<") && compOp.startsWith("<")) {
            return true;
        }
        if (getVersion().nodeVersion().equals(comp.getVersion().nodeVersion()) &&
                thisOp.contains("=") && compOp.contains("=")) {
            return true;
        }
        int c = getVersion().comparePrecedence(comp.getVersion());
        if (c < 0 && thisOp.startsWith(">") && compOp.startsWith("<")) {
            return true;
        }
        return c > 0 && thisOp.startsWith("<") && compOp.startsWith(">");
    }

    @Override
    public String toString() {
        return value();
    }

    // num + 1; a result beyond long range fails strict parsing later, invalidating the range as node does.
    static String incr(String num) {
        return new BigInteger(num).add(BigInteger.ONE).toString();
    }
}
