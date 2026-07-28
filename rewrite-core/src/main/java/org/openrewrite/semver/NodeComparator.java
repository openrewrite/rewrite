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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single primitive npm range clause: an operator ({@code < <= > >= =}) applied to a concrete
 * {@link NodeVersion}, or the special {@code ANY} clause (matches every version) produced by
 * {@code *}/{@code x}/an empty range. Ported from node-semver's {@code Comparator}. All range sugar
 * ({@code ^ ~ x hyphen} and bare versions) is desugared into these primitives by {@link NodeRange}.
 */
final class NodeComparator {

    enum Op {
        LT("<"), LTE("<="), GT(">"), GTE(">="), EQ("=");

        private final String symbol;

        Op(String symbol) {
            this.symbol = symbol;
        }
    }

    // node-semver re.js COMPARATOR: optional operator, optional whitespace, then a full version.
    private static final Pattern COMPARATOR = Pattern.compile(
            "^((?:<|>)?=?)\\s*(v?" +
                    "(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)" +
                    "(?:-(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?" +
                    "(?:\\+[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*)?)$");

    static final NodeComparator GTE_ZERO = new NodeComparator(Op.GTE, NodeVersion.parse("0.0.0"));

    private final Op op;
    // null == the ANY comparator.
    private final @Nullable NodeVersion semver;

    private NodeComparator(Op op, @Nullable NodeVersion semver) {
        this.op = op;
        this.semver = semver;
    }

    /**
     * @return the parsed comparator, or {@code null} if the token is not a valid comparator (which
     * invalidates the whole range).
     */
    static @Nullable NodeComparator parse(String token) {
        String t = token.trim();
        if (t.isEmpty()) {
            return new NodeComparator(Op.GTE, null);
        }
        Matcher m = COMPARATOR.matcher(t);
        if (!m.matches()) {
            return null;
        }
        NodeVersion version = NodeVersion.parse(m.group(2));
        if (version == null) {
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
        return new NodeComparator(op, version);
    }

    boolean isAny() {
        return semver == null;
    }

    Op getOp() {
        return op;
    }

    NodeVersion getSemver() {
        //noinspection DataFlowIssue
        return semver;
    }

    boolean test(NodeVersion version) {
        if (semver == null) {
            return true;
        }
        int c = version.compareTo(semver);
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
        if (semver == null) {
            return "";
        }
        // node-semver renders "=" and "" operators as just the version.
        return (op == Op.EQ ? "" : op.symbol) + semver;
    }
}
