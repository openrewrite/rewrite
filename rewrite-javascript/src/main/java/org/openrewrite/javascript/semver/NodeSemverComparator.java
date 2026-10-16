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
package org.openrewrite.javascript.semver;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.semver.NodeSemver.Options;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of node-semver classes/comparator.js: an operator applied to a version, or the ANY clause from
 * {@code *}/{@code x}/an empty range.
 */
final class NodeSemverComparator {

    // node-semver re.js COMPARATOR: optional operator, optional whitespace, then a full version.
    // Only groups 1 (operator) and 2 (version) are read; the prerelease group inside is unused.
    private static final Pattern COMPARATOR = Pattern.compile(
            "^((?:<|>)?=?)\\s*(v?" +
                    "(?:" + NodeSemverVersion.NUMERIC_ID + ")\\.(?:" + NodeSemverVersion.NUMERIC_ID + ")\\.(?:" +
                        NodeSemverVersion.NUMERIC_ID + ")" +
                    NodeSemverVersion.PRERELEASE_GROUP + "?" + NodeSemverVersion.BUILD + "?)$");

    // node-semver re.js COMPARATORLOOSE.
    static final Pattern COMPARATOR_LOOSE = Pattern.compile(
            "^((?:<|>)?=?)\\s*(" + NodeSemverVersion.LOOSE_PLAIN + ")$|^$");

    private enum Op {
        LT("<"), LTE("<="), GT(">"), GTE(">="), EQ("=");

        private final String symbol;

        Op(String symbol) {
            this.symbol = symbol;
        }
    }

    static final NodeSemverComparator GTE_ZERO =
            new NodeSemverComparator(Op.GTE, NodeSemverVersion.parse("0.0.0", Options.DEFAULT));

    private final Op op;

    // null == the ANY comparator.
    private final @Nullable NodeSemverVersion version;

    private NodeSemverComparator(Op op, @Nullable NodeSemverVersion version) {
        this.op = op;
        this.version = version;
    }

    /**
     * @return the parsed comparator, or {@code null}, which invalidates the whole range.
     */
    static @Nullable NodeSemverComparator parse(String token, Options options) {
        String t = token.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) {
            return new NodeSemverComparator(Op.GTE, null);
        }
        Matcher m = (options.loose ? COMPARATOR_LOOSE : COMPARATOR).matcher(t);
        if (!m.matches() || m.group(2) == null) {
            return null;
        }
        NodeSemverVersion version = NodeSemverVersion.parse(m.group(2), options);
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
        return new NodeSemverComparator(op, version);
    }

    boolean isAny() {
        return version == null;
    }

    NodeSemverVersion getVersion() {
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

    boolean test(NodeSemverVersion candidate) {
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
    boolean intersects(NodeSemverComparator comp, Options options) {
        if (operator().isEmpty()) {
            if (value().isEmpty()) {
                return true;
            }
            NodeSemverRange range = NodeSemverRange.parse(comp.value(), options);
            return range != null && range.test(value());
        } else if (comp.operator().isEmpty()) {
            if (comp.value().isEmpty()) {
                return true;
            }
            NodeSemverRange range = NodeSemverRange.parse(value(), options);
            return range != null && range.test(comp.getVersion());
        }

        if (options.includePrerelease && ("<0.0.0-0".equals(value()) || "<0.0.0-0".equals(comp.value()))) {
            return false;
        }
        if (!options.includePrerelease && (value().startsWith("<0.0.0") || comp.value().startsWith("<0.0.0"))) {
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
}
