/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.xml;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.xml.internal.grammar.XPathLexer;
import org.openrewrite.xml.internal.grammar.XPathParser;
import org.openrewrite.xml.internal.grammar.XPathParserBaseVisitor;
import org.openrewrite.xml.trait.Namespaced;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

import static java.util.Collections.singleton;

/**
 * Supports a limited set of XPath expressions, specifically those documented on <a
 * href="https://www.w3schools.com/xml/xpath_syntax.asp">this page</a>.
 * Additionally, supports `local-name()` and `namespace-uri()` conditions, `and`/`or` operators, and chained conditions.
 * <p>
 * Used for checking whether a visitor's cursor meets a certain XPath expression.
 * <p>
 * The "current node" for XPath evaluation is always the root node of the document. As a result, '.' and '..' are not
 * recognized.
 */
public class XPathMatcher {

    // Step characteristic flags (bitmask)
    private static final int FLAG_ABSOLUTE_PATH = 1;
    private static final int FLAG_DESCENDANT_OR_SELF = 1 << 1;
    private static final int FLAG_HAS_DESCENDANT = 1 << 2;
    private static final int FLAG_HAS_ABBREVIATED_STEP = 1 << 3;
    private static final int FLAG_HAS_AXIS_STEP = 1 << 4;
    private static final int FLAG_HAS_ATTRIBUTE_STEP = 1 << 5;
    private static final int FLAG_HAS_NODE_TYPE_TEST = 1 << 6;

    // Expression type constants
    private static final int EXPR_PATH = 0;           // Simple path (compiled)
    private static final int EXPR_BOOLEAN = 1;        // Boolean expression (needs visitor)
    private static final int EXPR_FILTER = 2;         // Filter expression (needs visitor)

    private final String expression;
    private XPathParser.@Nullable XpathExpressionContext parsed;

    // Pre-compiled step information (computed lazily on first parse)
    private CompiledStep @Nullable [] compiledSteps;
    private int stepFlags;
    private int compiledElementSteps;
    private int exprType = EXPR_PATH;  // Default to path, set during precompilation

    // Thread-local reusable path array to avoid allocation in matches()
    // Most XML documents have depth < 32
    private static final int MAX_CACHED_DEPTH = 32;
    private static final ThreadLocal<Xml.Tag[]> PATH_CACHE = ThreadLocal.withInitial(() -> new Xml.Tag[MAX_CACHED_DEPTH]);

    public XPathMatcher(String expression) {
        this.expression = expression;
    }

    /**
     * Checks if the given XPath expression matches the provided cursor.
     *
     * @param cursor the cursor representing the XML document
     * @return true if the expression matches the cursor, false otherwise
     */
    public boolean matches(Cursor cursor) {
        // Single pass: collect tags into cached array (in reverse/cursor order)
        Xml.Tag[] cached = PATH_CACHE.get();
        int tagCount = 0;
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            if (c.getValue() instanceof Xml.Tag) {
                if (tagCount < MAX_CACHED_DEPTH) {
                    cached[tagCount] = c.getValue();
                }
                tagCount++;
            }
        }

        // Build path in root-first order
        Xml.Tag[] path;
        int pathLength;
        if (tagCount <= MAX_CACHED_DEPTH) {
            // Reverse in place within cached array
            path = cached;
            pathLength = tagCount;
            for (int i = 0, j = tagCount - 1; i < j; i++, j--) {
                Xml.Tag tmp = path[i];
                path[i] = path[j];
                path[j] = tmp;
            }
        } else {
            // Deep document - fall back to allocation
            path = new Xml.Tag[tagCount];
            pathLength = tagCount;
            int idx = tagCount - 1;
            for (Cursor c = cursor; c != null; c = c.getParent()) {
                if (c.getValue() instanceof Xml.Tag) {
                    path[idx--] = c.getValue();
                }
            }
        }

        XPathParser.XpathExpressionContext ctx = parse();

        // Fast path: for compiled path expressions, bypass the visitor entirely
        if (exprType == EXPR_PATH && compiledSteps != null) {
            // Early rejection for simple absolute paths: if path length doesn't match expected elements, fail fast
            // Only applies when there's no descendant axis, backtracking, or other features that affect path length
            if ((stepFlags & FLAG_ABSOLUTE_PATH) != 0 &&
                (stepFlags & (FLAG_HAS_DESCENDANT | FLAG_HAS_ABBREVIATED_STEP | FLAG_HAS_AXIS_STEP | FLAG_HAS_ATTRIBUTE_STEP | FLAG_HAS_NODE_TYPE_TEST)) == 0 &&
                pathLength != compiledElementSteps) {
                return false;
            }

            XPathMatcherVisitor visitor = new XPathMatcherVisitor(path, pathLength, cursor,
                    compiledSteps, stepFlags, compiledElementSteps);
            return visitor.matchCompiledPath();
        }

        // Complex expressions (boolean, filter) still need the visitor
        XPathMatcherVisitor visitor = new XPathMatcherVisitor(path, pathLength, cursor,
                compiledSteps, stepFlags, compiledElementSteps);
        return visitor.visit(ctx);
    }

    private XPathParser.XpathExpressionContext parse() {
        if (parsed == null) {
            XPathLexer lexer = new XPathLexer(CharStreams.fromString(expression));
            XPathParser parser = new XPathParser(new CommonTokenStream(lexer));
            parsed = parser.xpathExpression();
            precompileSteps(parsed);
        }
        return parsed;
    }

    /**
     * Pre-compile step information for common path expressions.
     * This avoids re-extracting and analyzing steps on every matches() call.
     */
    private void precompileSteps(XPathParser.XpathExpressionContext ctx) {
        // Determine expression type first - this is checked once during parsing
        if (ctx.booleanExpr() != null) {
            exprType = EXPR_BOOLEAN;
            return;
        } else if (ctx.filterExpr() != null) {
            exprType = EXPR_FILTER;
            return;
        }
        // Otherwise it's a path expression
        exprType = EXPR_PATH;

        XPathParser.RelativeLocationPathContext relPath = null;
        int flags = 0;

        if (ctx.absoluteLocationPath() != null) {
            XPathParser.AbsoluteLocationPathContext absCtx = ctx.absoluteLocationPath();
            if (absCtx.DOUBLE_SLASH() != null) {
                flags |= FLAG_DESCENDANT_OR_SELF;
            } else {
                flags |= FLAG_ABSOLUTE_PATH;
            }
            relPath = absCtx.relativeLocationPath();
        } else if (ctx.relativeLocationPath() != null) {
            relPath = ctx.relativeLocationPath();
        }

        if (relPath != null) {
            // Extract steps
            List<XPathParser.StepContext> stepCtxs = relPath.step();
            List<XPathParser.PathSeparatorContext> separators = relPath.pathSeparator();
            compiledSteps = new CompiledStep[stepCtxs.size()];

            for (int i = 0; i < stepCtxs.size(); i++) {
                boolean isDescendant = false;
                if (i > 0 && i - 1 < separators.size()) {
                    isDescendant = separators.get(i - 1).DOUBLE_SLASH() != null;
                }
                compiledSteps[i] = CompiledStep.fromStepContext(stepCtxs.get(i), isDescendant);
            }

            // Set nextIsBacktrack flag and compute step characteristics
            for (int i = 0; i < compiledSteps.length; i++) {
                CompiledStep s = compiledSteps[i];
                if (s.isDescendant) flags |= FLAG_HAS_DESCENDANT;

                switch (s.type) {
                    case ABBREVIATED_DOT:
                    case ABBREVIATED_DOTDOT:
                        flags |= FLAG_HAS_ABBREVIATED_STEP;
                        break;
                    case AXIS_STEP:
                        flags |= FLAG_HAS_AXIS_STEP;
                        break;
                    case ATTRIBUTE_STEP:
                        flags |= FLAG_HAS_ATTRIBUTE_STEP;
                        break;
                    case NODE_TYPE_TEST:
                        flags |= FLAG_HAS_NODE_TYPE_TEST;
                        break;
                    case NODE_TEST:
                        compiledElementSteps++;
                        break;
                }

                // Set nextIsBacktrack for lookahead optimization
                if (i + 1 < compiledSteps.length && compiledSteps[i + 1].isBacktrack()) {
                    s.setNextIsBacktrack();
                }
            }
        }
        this.stepFlags = flags;
    }

    /**
     * Visitor that evaluates XPath expressions against the cursor path.
     */
    private static class XPathMatcherVisitor extends XPathParserBaseVisitor<Boolean> {
        // Path array in root-first order: index 0 = root, last index = current tag
        private final Xml.Tag[] path;
        private final int pathLength;
        private final Cursor cursor;
        private Xml.@Nullable Tag rootTag;

        // Pre-compiled step information (may be null for complex expressions)
        private final CompiledStep @Nullable [] compiledSteps;
        private final int stepFlags;
        private final int compiledElementSteps;

        XPathMatcherVisitor(Xml.Tag[] path, int pathLength, Cursor cursor,
                           CompiledStep @Nullable [] compiledSteps,
                           int stepFlags,
                           int compiledElementSteps) {
            this.path = path;
            this.pathLength = pathLength;
            this.cursor = cursor;
            this.compiledSteps = compiledSteps;
            this.stepFlags = stepFlags;
            this.compiledElementSteps = compiledElementSteps;
        }

        @Override
        protected Boolean defaultResult() {
            return false;
        }

        /**
         * Direct entry point for compiled path expressions - bypasses visitor.visit() overhead.
         * Called directly from matches() when expression type is EXPR_PATH.
         */
        boolean matchCompiledPath() {
            if ((stepFlags & FLAG_DESCENDANT_OR_SELF) != 0) {
                return matchDescendantOrRelativeCompiled(true);
            } else if ((stepFlags & FLAG_ABSOLUTE_PATH) != 0) {
                return matchAbsoluteCompiled();
            } else {
                // Relative path
                return matchDescendantOrRelativeCompiled(false);
            }
        }

        @Override
        public Boolean visitXpathExpression(XPathParser.XpathExpressionContext ctx) {
            // Handle complex expressions (booleanExpr, filterExpr) - these still need visitor
            if (ctx.booleanExpr() != null) {
                return visitBooleanExpr(ctx.booleanExpr());
            } else if (ctx.filterExpr() != null) {
                return visitFilterExpr(ctx.filterExpr());
            }

            // Path expressions should use matchCompiledPath() directly, but fall back here if needed
            if (compiledSteps != null) {
                return matchCompiledPath();
            }

            // Should not reach here - precompilation handles all absoluteLocationPath/relativeLocationPath cases
            return false;
        }

        /**
         * Visit filter expression: (/path/expr)[predicate] or (/path/expr)[predicate]/trailing
         * This collects all matching nodes first, then applies predicates to the result set.
         * If there's a trailing path after the predicates, it continues matching from the filtered nodes.
         */
        public Boolean visitFilterExpr(XPathParser.FilterExprContext ctx) {
            // Get the path expression (absolute or relative) - first one is inside parentheses
            String pathExpr;
            if (ctx.absoluteLocationPath() != null) {
                pathExpr = ctx.absoluteLocationPath().getText();
            } else if (!ctx.relativeLocationPath().isEmpty()) {
                // First relativeLocationPath is inside the parentheses
                pathExpr = ctx.relativeLocationPath(0).getText();
            } else {
                return false;
            }

            // Find all matching nodes in the document
            Xml.Tag root = getRootTag();
            if (root == null) {
                return false;
            }

            Set<Xml.Tag> allMatches = findChildTags(root, pathExpr);
            if (allMatches.isEmpty()) {
                return false;
            }

            // Convert to list for positional access
            List<Xml.Tag> matchList = new ArrayList<>(allMatches);

            // Apply predicates to filter the result set
            List<Xml.Tag> filteredMatches = new ArrayList<>();
            int size = matchList.size();
            for (int i = 0; i < matchList.size(); i++) {
                Xml.Tag tag = matchList.get(i);
                int position = i + 1; // 1-based
                boolean allPredicatesMatch = true;
                for (XPathParser.PredicateContext predicate : ctx.predicate()) {
                    if (!evaluatePredicate(predicate, tag, position, size)) {
                        allPredicatesMatch = false;
                        break;
                    }
                }
                if (allPredicatesMatch) {
                    filteredMatches.add(tag);
                }
            }

            if (filteredMatches.isEmpty()) {
                return false;
            }

            // Check if there's a trailing path after the predicates: (/foo/bar)[1]/baz
            // The second relativeLocationPath (index 1) would be the trailing path
            boolean hasTrailingPath = ctx.pathSeparator() != null && ctx.relativeLocationPath().size() > 1;

            if (hasTrailingPath) {
                // Get the trailing path
                XPathParser.RelativeLocationPathContext trailingPath = ctx.relativeLocationPath(1);
                String trailingPathExpr = trailingPath.getText();
                boolean isDescendant = ctx.pathSeparator().DOUBLE_SLASH() != null;

                // From the filtered matches, find children matching the trailing path
                Set<Xml.Tag> trailingMatches = new LinkedHashSet<>();
                for (Xml.Tag filteredTag : filteredMatches) {
                    if (isDescendant) {
                        // FIXME: Descendant axis in trailing path not fully implemented yet
                        findDescendants(filteredTag, trailingPathExpr, trailingMatches);
                    } else {
                        trailingMatches.addAll(findChildTags(filteredTag, trailingPathExpr));
                    }
                }

                // Check if current cursor is on one of the trailing matches
                Xml.Tag currentTag = null;
                if (cursor.getValue() instanceof Xml.Tag) {
                    currentTag = cursor.getValue();
                }
                return currentTag != null && trailingMatches.contains(currentTag);
            } else {
                // No trailing path - check if cursor is on one of the filtered matches
                Xml.Tag currentTag = null;
                if (cursor.getValue() instanceof Xml.Tag) {
                    currentTag = cursor.getValue();
                }
                return currentTag != null && filteredMatches.contains(currentTag);
            }
        }

        @Override
        public Boolean visitBooleanExpr(XPathParser.BooleanExprContext ctx) {
            XPathParser.FunctionCallContext funcCall = ctx.functionCall();
            Object funcResult = evaluateFunctionCallExpr(funcCall);

            boolean result;
            if (ctx.comparisonOp() != null && ctx.comparand() != null) {
                // Has comparison: function() op value
                Object comparand = evaluateComparand(ctx.comparand());
                result = evaluateComparison(funcResult, ctx.comparisonOp(), comparand);
            } else {
                // Just the function call - treat result as boolean
                result = toBool(funcResult);
            }

            // For boolean expressions, we need to be more careful about when to "match".
            // Only return true if the boolean result is true AND we're at a relevant position.
            // For simplicity, only match at the root element to avoid multiple matches.
            if (result && pathLength == 1) {
                return true;
            }
            return false;
        }

        /**
         * Evaluate a function call expression and return its result.
         */
        private Object evaluateFunctionCallExpr(XPathParser.FunctionCallContext funcCall) {
            // Get the function name
            String funcName;
            if (funcCall.LOCAL_NAME() != null) {
                funcName = "local-name";
            } else if (funcCall.NAMESPACE_URI() != null) {
                funcName = "namespace-uri";
            } else if (funcCall.NCNAME() != null) {
                funcName = funcCall.NCNAME().getText();
            } else {
                return null;
            }

            List<Object> args = new ArrayList<>();
            if (funcCall.functionArgs() != null) {
                for (XPathParser.FunctionArgContext arg : funcCall.functionArgs().functionArg()) {
                    args.add(evaluateFunctionArg(arg));
                }
            }

            return evaluateFunction(funcName, args);
        }

        /**
         * Evaluate a function argument.
         */
        private Object evaluateFunctionArg(XPathParser.FunctionArgContext arg) {
            if (arg.absoluteLocationPath() != null) {
                return evaluatePathExpression(arg.absoluteLocationPath(), null);
            } else if (arg.relativeLocationPath() != null) {
                return evaluatePathExpression(null, arg.relativeLocationPath());
            } else if (arg.stringLiteral() != null) {
                return unquote(arg.stringLiteral().STRING_LITERAL().getText());
            } else if (arg.NUMBER() != null) {
                String numStr = arg.NUMBER().getText();
                if (numStr.contains(".")) {
                    return Double.parseDouble(numStr);
                } else {
                    return Integer.parseInt(numStr);
                }
            } else if (arg.functionCall() != null) {
                return evaluateFunctionCallExpr(arg.functionCall());
            }
            return null;
        }

        /**
         * Evaluate a path expression and return the text content of the matching node(s).
         * For function arguments, we need to find the node matching the path in the document
         * and return its text value, regardless of the current cursor position.
         */
        private Object evaluatePathExpression(XPathParser.@Nullable AbsoluteLocationPathContext absPath,
                                              XPathParser.@Nullable RelativeLocationPathContext relPath) {
            Xml.Tag root = getRootTag();
            if (root == null) {
                return "";
            }

            // Locate the element at the path
            if (absPath != null || relPath != null) {
                String pathStr = absPath != null ? absPath.getText() : relPath.getText();
                Set<Xml.Tag> found = findChildTags(root, pathStr);
                if (!found.isEmpty()) {
                    // Return the text content of the first matching tag
                    return found.iterator().next().getValue().orElse("");
                }
            }
            return "";
        }

        /**
         * Get the root tag of the document, caching the result for performance.
         */
        private Xml.@Nullable Tag getRootTag() {
            if (rootTag == null) {
                // Find the root of the document from the cursor
                Cursor rootCursor = cursor;
                while (rootCursor.getParent() != null && !(rootCursor.getParent().getValue() instanceof Xml.Document)) {
                    rootCursor = rootCursor.getParent();
                }
                Object root = rootCursor.getValue();
                if (root instanceof Xml.Tag) {
                    rootTag = (Xml.Tag) root;
                }
            }
            return rootTag;
        }

        /**
         * Evaluate the comparand (right side of comparison).
         */
        private Object evaluateComparand(XPathParser.ComparandContext comparand) {
            if (comparand.stringLiteral() != null) {
                return unquote(comparand.stringLiteral().STRING_LITERAL().getText());
            } else if (comparand.NUMBER() != null) {
                String numStr = comparand.NUMBER().getText();
                if (numStr.contains(".")) {
                    return Double.parseDouble(numStr);
                } else {
                    return Integer.parseInt(numStr);
                }
            }
            return null;
        }

        /**
         * Evaluate comparison between two values.
         */
        private boolean evaluateComparison(@Nullable Object left, XPathParser.ComparisonOpContext op, @Nullable Object right) {
            if (left == null || right == null) {
                return false;
            }

            // Convert to numbers if comparing with numbers
            if (left instanceof Number || right instanceof Number) {
                double leftNum = toNumber(left);
                double rightNum = toNumber(right);

                if (op.EQUALS() != null) {
                    return leftNum == rightNum;
                } else if (op.NOT_EQUALS() != null) {
                    return leftNum != rightNum;
                } else if (op.LT() != null) {
                    return leftNum < rightNum;
                } else if (op.GT() != null) {
                    return leftNum > rightNum;
                } else if (op.LTE() != null) {
                    return leftNum <= rightNum;
                } else if (op.GTE() != null) {
                    return leftNum >= rightNum;
                }
            } else {
                // String comparison
                String leftStr = left.toString();
                String rightStr = right.toString();

                if (op.EQUALS() != null) {
                    return leftStr.equals(rightStr);
                } else if (op.NOT_EQUALS() != null) {
                    return !leftStr.equals(rightStr);
                }
            }
            return false;
        }

        /**
         * Evaluate a function by name with arguments.
         */
        private Object evaluateFunction(String funcName, List<Object> args) {
            switch (funcName) {
                case "contains":
                    if (args.size() >= 2) {
                        String str = args.get(0) != null ? args.get(0).toString() : "";
                        String substring = args.get(1) != null ? args.get(1).toString() : "";
                        return str.contains(substring);
                    }
                    return false;
                case "starts-with":
                    if (args.size() >= 2) {
                        String str = args.get(0) != null ? args.get(0).toString() : "";
                        String prefix = args.get(1) != null ? args.get(1).toString() : "";
                        return str.startsWith(prefix);
                    }
                    return false;
                case "ends-with":
                    if (args.size() >= 2) {
                        String str = args.get(0) != null ? args.get(0).toString() : "";
                        String suffix = args.get(1) != null ? args.get(1).toString() : "";
                        return str.endsWith(suffix);
                    }
                    return false;
                case "string-length":
                    if (args.size() >= 1) {
                        String str = args.get(0) != null ? args.get(0).toString() : "";
                        return str.length();
                    }
                    return 0;
                case "substring-before":
                    if (args.size() >= 2) {
                        String str = args.get(0) != null ? args.get(0).toString() : "";
                        String delimiter = args.get(1) != null ? args.get(1).toString() : "";
                        int idx = str.indexOf(delimiter);
                        return idx >= 0 ? str.substring(0, idx) : "";
                    }
                    return "";
                case "substring-after":
                    if (args.size() >= 2) {
                        String str = args.get(0) != null ? args.get(0).toString() : "";
                        String delimiter = args.get(1) != null ? args.get(1).toString() : "";
                        int idx = str.indexOf(delimiter);
                        return idx >= 0 ? str.substring(idx + delimiter.length()) : "";
                    }
                    return "";
                case "not":
                    if (args.size() >= 1) {
                        return !toBool(args.get(0));
                    }
                    return true;
                case "count":
                    // count() should count the number of nodes matching the path argument
                    // The argument should already be evaluated, but for count we need special handling
                    // If we have a non-empty result from path evaluation, count it as 1
                    // For proper count, we'd need to return the set of nodes, not just the first one's value
                    if (args.size() >= 1) {
                        Object arg = args.get(0);
                        if (arg instanceof String && !((String) arg).isEmpty()) {
                            // A non-empty string means at least one match was found
                            return 1;
                        } else if (arg instanceof Number) {
                            return ((Number) arg).intValue();
                        }
                    }
                    return 0;
                default:
                    // Unknown function
                    return null;
            }
        }

        /**
         * Convert value to boolean.
         */
        private boolean toBool(Object value) {
            if (value == null) {
                return false;
            } else if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue() != 0;
            } else if (value instanceof String) {
                return !((String) value).isEmpty();
            }
            return true;
        }

        /**
         * Convert value to number.
         */
        private double toNumber(Object value) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    return Double.NaN;
                }
            }
            return 0;
        }

        // ==================== COMPILED FAST PATH METHODS ====================
        // These use the fully compiled step array to avoid ANTLR tree navigation

        /**
         * Fast path for absolute paths using compiled steps.
         */
        private boolean matchAbsoluteCompiled() {
            if ((stepFlags & FLAG_HAS_DESCENDANT) != 0) {
                return matchWithDescendantCompiled();
            }

            if ((stepFlags & FLAG_HAS_ABBREVIATED_STEP) != 0 || (stepFlags & FLAG_HAS_AXIS_STEP) != 0) {
                return matchStepsAgainstPathCompiled(0);
            }

            if (!((stepFlags & FLAG_HAS_ATTRIBUTE_STEP) != 0)) {
                if (!((stepFlags & FLAG_HAS_NODE_TYPE_TEST) != 0) && pathLength != compiledElementSteps) {
                    return false;
                }
            }
            if (((stepFlags & FLAG_HAS_ATTRIBUTE_STEP) != 0 || (stepFlags & FLAG_HAS_NODE_TYPE_TEST) != 0) && pathLength != compiledElementSteps) {
                return false;
            }

            return matchStepsAgainstPathCompiled(0);
        }

        /**
         * Fast path for descendant/relative paths using compiled steps.
         */
        private boolean matchDescendantOrRelativeCompiled(boolean isDescendantOrSelf) {
            // Check for internal // (already computed during precompilation as hasDescendant for steps after first)
            boolean hasInternalDescendant = false;
            for (int i = 1; i < compiledSteps.length; i++) {
                if (compiledSteps[i].isDescendant) {
                    hasInternalDescendant = true;
                    break;
                }
            }

            if (hasInternalDescendant) {
                return matchWithDescendantCompiled();
            }

            if (pathLength < compiledElementSteps) {
                return false;
            }

            int maxStartPos = pathLength - compiledElementSteps;
            for (int startPos = maxStartPos; startPos >= 0; startPos--) {
                if (matchStepsAgainstPathCompiled(startPos)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Fast path for paths with descendant axis using compiled steps.
         */
        private boolean matchWithDescendantCompiled() {
            // Find where the // occurs
            int descendantIndex = -1;
            for (int i = 0; i < compiledSteps.length; i++) {
                if (compiledSteps[i].isDescendant) {
                    descendantIndex = i;
                    break;
                }
            }

            if (descendantIndex == -1) {
                return matchStepsAgainstPathCompiled(0);
            }

            // Match the prefix (steps before //)
            for (int i = 0; i < descendantIndex; i++) {
                if (i >= pathLength) {
                    return false;
                }
                Xml.Tag currentTag = path[i];
                CompiledStep step = compiledSteps[i];
                int position = 1;
                int size = 1;
                // Only compute position if predicates need it
                if (step.needsPositionInfo() && i > 0) {
                    Xml.Tag parentTag = path[i - 1];
                    String expectedName = step.name != null ? step.name : "*";
                    long posInfo = computePositionPacked(parentTag, currentTag, expectedName);
                    position = positionOf(posInfo);
                    size = sizeOf(posInfo);
                }
                if (!matchCompiledStep(step, currentTag, position, size)) {
                    return false;
                }
            }

            // Count element steps in the suffix
            int afterElementSteps = 0;
            for (int i = descendantIndex; i < compiledSteps.length; i++) {
                if (compiledSteps[i].type == StepType.NODE_TEST) afterElementSteps++;
            }

            // Try to match after steps from any position
            int startSearchAt = descendantIndex;
            for (int pathIdx = startSearchAt; pathIdx <= pathLength - afterElementSteps; pathIdx++) {
                if (matchStepsAgainstPathCompiledFrom(descendantIndex, pathIdx)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Fast path step matching using compiled array.
         */
        private boolean matchStepsAgainstPathCompiled(int pathStartIdx) {
            return matchStepsAgainstPathCompiledFrom(0, pathStartIdx);
        }

        /**
         * Match steps from a given step index against path from a given path index.
         * Uses fully compiled steps - no ANTLR tree access in the hot path.
         */
        private boolean matchStepsAgainstPathCompiledFrom(int stepStartIdx, int pathStartIdx) {
            int pathIdx = pathStartIdx;
            boolean didSiblingCheck = false;

            for (int i = stepStartIdx; i < compiledSteps.length; i++) {
                CompiledStep step = compiledSteps[i];

                // Handle step based on compiled type - switch on enum is fast
                switch (step.type) {
                    case ABBREVIATED_DOT:
                        // . - self, no path change
                        continue;

                    case ABBREVIATED_DOTDOT:
                        // .. - parent
                        if (didSiblingCheck) {
                            didSiblingCheck = false;
                        } else {
                            if (pathIdx > pathLength) {
                                pathIdx = pathLength;
                            }
                            if (pathIdx <= 0) {
                                return false;
                            }
                            pathIdx--;
                        }
                        continue;

                    case AXIS_STEP:
                        switch (step.axisType) {
                            case PARENT:
                                if (didSiblingCheck) {
                                    didSiblingCheck = false;
                                    if (pathIdx > 0) {
                                        Xml.Tag parentTag = path[pathIdx - 1];
                                        String nodeTestName = step.name;
                                        if (!"*".equals(nodeTestName) && !"node".equals(nodeTestName) &&
                                                !parentTag.getName().equals(nodeTestName)) {
                                            return false;
                                        }
                                    }
                                    continue;
                                }
                                if (pathIdx <= 0) {
                                    return false;
                                }
                                pathIdx--;
                                Xml.Tag parentTag = path[pathIdx];
                                String nodeTestName = step.name;
                                if (!"*".equals(nodeTestName) && !"node".equals(nodeTestName) &&
                                        !parentTag.getName().equals(nodeTestName)) {
                                    return false;
                                }
                                continue;

                            case SELF:
                                int contextIdx = pathIdx - 1;
                                if (contextIdx < 0 || contextIdx >= pathLength) {
                                    return false;
                                }
                                Xml.Tag currentTag = path[contextIdx];
                                String selfNodeTestName = step.name;
                                if (!"*".equals(selfNodeTestName) && !"node".equals(selfNodeTestName) &&
                                        !currentTag.getName().equals(selfNodeTestName)) {
                                    return false;
                                }
                                continue;

                            case CHILD:
                                // child:: is default, fall through to element handling
                                continue;

                            case OTHER:
                                // Unsupported axis
                                return false;
                        }
                        continue;

                    case ATTRIBUTE_STEP:
                        return matchAttributeStepCompiled(step);

                    case NODE_TYPE_TEST:
                        return matchNodeTypeTestCompiled(step, pathIdx);

                    case NODE_TEST:
                        // Element step - the most common case
                        break;
                }

                // NODE_TEST handling - element matching
                // Only compute position/size if predicates need it
                Xml.Tag currentTag = pathIdx < pathLength ? path[pathIdx] : null;
                int position = 1;
                int size = 1;
                if (step.needsPositionInfo() && currentTag != null && pathIdx > 0) {
                    Xml.Tag parentTag = path[pathIdx - 1];
                    String expectedName = step.name != null ? step.name : "*";
                    long posInfo = computePositionPacked(parentTag, currentTag, expectedName);
                    position = positionOf(posInfo);
                    size = sizeOf(posInfo);
                }

                // Handle descendant axis for element steps
                if (i > stepStartIdx && step.isDescendant) {
                    boolean found = false;
                    for (int searchIdx = pathIdx; searchIdx < pathLength; searchIdx++) {
                        Xml.Tag searchTag = path[searchIdx];
                        int searchPos = 1;
                        int searchSize = 1;
                        // Only compute position if predicates need it
                        if (step.needsPositionInfo() && searchIdx > 0) {
                            Xml.Tag searchParent = path[searchIdx - 1];
                            String expectedName = step.name != null ? step.name : "*";
                            long searchPosInfo = computePositionPacked(searchParent, searchTag, expectedName);
                            searchPos = positionOf(searchPosInfo);
                            searchSize = sizeOf(searchPosInfo);
                        }
                        if (matchCompiledStep(step, searchTag, searchPos, searchSize)) {
                            pathIdx = searchIdx + 1;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                } else {
                    // Regular element step
                    if (pathIdx >= pathLength) {
                        // Use pre-computed nextIsBacktrack flag
                        if (step.nextIsBacktrack()) {
                            if (pathIdx <= 0) {
                                return false;
                            }
                            Xml.Tag parent = path[pathIdx - 1];
                            String expectedChild = step.name != null ? step.name : "*";
                            if (!childExists(parent, expectedChild)) {
                                return false;
                            }
                            continue;
                        }
                        return false;
                    }
                    if (!matchCompiledStep(step, path[pathIdx], position, size)) {
                        // Use pre-computed nextIsBacktrack flag
                        if (step.nextIsBacktrack() && pathIdx > 0) {
                            Xml.Tag parent = path[pathIdx - 1];
                            String expectedChild = step.name != null ? step.name : "*";
                            if (childExists(parent, expectedChild)) {
                                didSiblingCheck = true;
                                continue;
                            }
                        }
                        return false;
                    }
                    pathIdx++;
                }
            }

            // For element paths, verify we've used the entire path
            if (!((stepFlags & FLAG_HAS_ATTRIBUTE_STEP) != 0)) {
                if (!((stepFlags & FLAG_HAS_ABBREVIATED_STEP) != 0)) {
                    if (!((stepFlags & FLAG_HAS_AXIS_STEP) != 0)) {
                        return pathIdx == pathLength;
                    }
                }
            }
            return true;
        }

        /**
         * Compute the position and size for a tag among its siblings with the same name.
         * Returns a packed long: high 32 bits = position, low 32 bits = size.
         * Use positionOf(result) and sizeOf(result) to extract values.
         */
        private long computePositionPacked(Xml.Tag parent, Xml.Tag currentTag, String expectedName) {
            int position = 1;
            int size = 0;

            List<? extends Content> contents = parent.getContent();
            if (contents != null) {
                for (int i = 0, n = contents.size(); i < n; i++) {
                    Content content = contents.get(i);
                    if (content instanceof Xml.Tag) {
                        Xml.Tag child = (Xml.Tag) content;
                        boolean matches = "*".equals(expectedName) || child.getName().equals(expectedName);
                        if (matches) {
                            size++;
                            if (child == currentTag) {
                                position = size;
                            }
                        }
                    }
                }
            }

            int finalSize = size == 0 ? 1 : size;
            return ((long) position << 32) | (finalSize & 0xFFFFFFFFL);
        }

        private static int positionOf(long packed) {
            return (int) (packed >>> 32);
        }

        private static int sizeOf(long packed) {
            return (int) packed;
        }

        /**
         * Match a single compiled step against a tag.
         * @param position 1-based position of this tag among matching siblings (use 1 if unknown)
         * @param size total count of matching siblings (use 1 if unknown)
         */
        private boolean matchCompiledStep(CompiledStep step, Xml.Tag tag, int position, int size) {
            // For NODE_TEST steps, check the element name
            String expectedName = step.name;
            if (expectedName == null) {
                return false;
            }

            // Check element name
            if (!"*".equals(expectedName) && !tag.getName().equals(expectedName)) {
                return false;
            }

            // Check predicates (null means no predicates - common case)
            if (step.predicates != null) {
                for (XPathParser.PredicateContext predicate : step.predicates) {
                    if (!evaluatePredicate(predicate, tag, position, size)) {
                        return false;
                    }
                }
            }

            return true;
        }

        /**
         * Match node type test using compiled step data.
         */
        private boolean matchNodeTypeTestCompiled(CompiledStep step, int pathIdx) {
            switch (step.nodeTypeTestType) {
                case TEXT:
                    // text() matches when cursor is on text content
                    // In OpenRewrite, text is represented as Xml.CharData
                    if (cursor.getValue() instanceof Xml.CharData) {
                        return true;
                    }
                    // Also match if we've consumed all element steps and the last element has text content
                    // pathIdx points to where we'd be after matching all element steps
                    if (pathIdx > 0 && pathIdx == pathLength) {
                        Xml.Tag lastTag = path[pathIdx - 1];
                        return lastTag.getValue().isPresent();
                    }
                    return false;

                case COMMENT:
                    // comment() matches when cursor is on a comment
                    return cursor.getValue() instanceof Xml.Comment;

                case NODE:
                    // node() matches any node type
                    return true;

                case PROCESSING_INSTRUCTION:
                    // processing-instruction() matches processing instructions
                    return cursor.getValue() instanceof Xml.ProcessingInstruction;

                default:
                    // Unknown node type test - not supported
                    return false;
            }
        }

        /**
         * Match attribute step using compiled step data.
         */
        private boolean matchAttributeStepCompiled(CompiledStep step) {
            if (!(cursor.getValue() instanceof Xml.Attribute)) {
                return false;
            }

            Xml.Attribute attribute = cursor.getValue();
            String attrName = step.name;

            // Check attribute name
            if (!"*".equals(attrName) && !attribute.getKeyAsString().equals(attrName)) {
                return false;
            }

            // Check predicates on the attribute (null means no predicates)
            if (step.predicates != null) {
                for (XPathParser.PredicateContext predicate : step.predicates) {
                    if (!evaluateAttributePredicate(predicate, attribute, 1, 1)) {
                        return false;
                    }
                }
            }

            return true;
        }

        /**
         * Evaluate predicate against attribute.
         */
        private boolean evaluateAttributePredicate(XPathParser.PredicateContext predicate, Xml.Attribute attribute,
                                                   int position, int size) {
            return evaluateOrExpr(predicate.predicateExpr().orExpr(), null, attribute, position, size);
        }

        /**
         * Evaluate a predicate against a tag.
         */
        private boolean evaluatePredicate(XPathParser.PredicateContext predicate, Xml.Tag tag, int position, int size) {
            return evaluateOrExpr(predicate.predicateExpr().orExpr(), tag, null, position, size);
        }

        /**
         * Evaluate OR expression.
         */
        private boolean evaluateOrExpr(XPathParser.OrExprContext orExpr, Xml.@Nullable Tag tag, Xml.@Nullable Attribute attribute,
                                       int position, int size) {
            List<XPathParser.AndExprContext> andExprs = orExpr.andExpr();
            for (XPathParser.AndExprContext andExpr : andExprs) {
                if (evaluateAndExpr(andExpr, tag, attribute, position, size)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Evaluate AND expression.
         */
        private boolean evaluateAndExpr(XPathParser.AndExprContext andExpr, Xml.@Nullable Tag tag, Xml.@Nullable Attribute attribute,
                                        int position, int size) {
            for (XPathParser.PrimaryExprContext primaryExpr : andExpr.primaryExpr()) {
                if (!evaluatePrimaryExpr(primaryExpr, tag, attribute, position, size)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Evaluate primary expression.
         * @param position 1-based position of the current node among matching siblings
         * @param size total count of matching siblings
         */
        private boolean evaluatePrimaryExpr(XPathParser.PrimaryExprContext primaryExpr,
                                            Xml.@Nullable Tag tag, Xml.@Nullable Attribute attribute,
                                            int position, int size) {
            XPathParser.PredicateValueContext predicateValue = primaryExpr.predicateValue();

            // Evaluate the predicate value
            Object value = evaluatePredicateValue(predicateValue, tag, attribute, position, size);

            // Check if there's a comparison
            if (primaryExpr.comparisonOp() != null && primaryExpr.comparand() != null) {
                Object comparand = evaluateComparand(primaryExpr.comparand());
                return evaluateComparison(value, primaryExpr.comparisonOp(), comparand);
            } else {
                // No comparison - treat as boolean or positional
                return toBoolOrPositional(value, position);
            }
        }

        /**
         * Evaluate a predicate value expression.
         */
        private Object evaluatePredicateValue(XPathParser.PredicateValueContext predicateValue,
                                              Xml.@Nullable Tag tag, Xml.@Nullable Attribute attribute,
                                              int position, int size) {
            if (predicateValue.functionCall() != null) {
                return evaluatePredicateFunctionCall(predicateValue.functionCall(), tag, attribute, position, size);
            } else if (predicateValue.attributeStep() != null) {
                return getAttributeValue(predicateValue.attributeStep(), tag);
            } else if (predicateValue.relativeLocationPath() != null) {
                return getRelativePathValue(predicateValue.relativeLocationPath(), tag);
            } else if (predicateValue.childElementTest() != null) {
                return getChildElementValue(predicateValue.childElementTest(), tag);
            } else if (predicateValue.NUMBER() != null) {
                String numStr = predicateValue.NUMBER().getText();
                if (numStr.contains(".")) {
                    return Double.parseDouble(numStr);
                } else {
                    return Integer.parseInt(numStr);
                }
            }
            return null;
        }

        /**
         * Convert value to boolean, treating numbers as positional predicates.
         * In XPath, [1] means position()=1, not a truthy check.
         */
        private boolean toBoolOrPositional(Object value, int position) {
            if (value instanceof Number) {
                // Numeric value is a positional predicate
                return ((Number) value).intValue() == position;
            }
            return toBool(value);
        }

        /**
         * Evaluate function call in predicate context and return its value.
         */
        private Object evaluatePredicateFunctionCall(XPathParser.FunctionCallContext funcCall,
                                                     Xml.@Nullable Tag tag, Xml.@Nullable Attribute attribute,
                                                     int position, int size) {
            String funcName;
            if (funcCall.LOCAL_NAME() != null) {
                funcName = "local-name";
            } else if (funcCall.NAMESPACE_URI() != null) {
                funcName = "namespace-uri";
            } else if (funcCall.NCNAME() != null) {
                funcName = funcCall.NCNAME().getText();
            } else {
                return null;
            }

            switch (funcName) {
                case "local-name":
                    if (attribute != null) {
                        Namespaced namespaced = new Namespaced(new Cursor(cursor.getParent(), attribute));
                        return namespaced.getLocalName().orElse("");
                    } else if (tag != null) {
                        Namespaced namespaced = new Namespaced(findCursorForTag(tag));
                        return namespaced.getLocalName().orElse("");
                    }
                    return "";
                case "namespace-uri":
                    if (attribute != null) {
                        Namespaced namespaced = new Namespaced(new Cursor(cursor.getParent(), attribute));
                        return namespaced.getNamespaceUri().orElse("");
                    } else if (tag != null) {
                        Namespaced namespaced = new Namespaced(findCursorForTag(tag));
                        return namespaced.getNamespaceUri().orElse("");
                    }
                    return "";
                case "text":
                    if (tag != null) {
                        return tag.getValue().orElse("");
                    }
                    return "";
                case "position":
                    return position;
                case "last":
                    return size;
                case "contains":
                case "starts-with":
                case "ends-with":
                case "string-length":
                case "not":
                    // These need arguments - evaluate them
                    List<Object> args = new ArrayList<>();
                    if (funcCall.functionArgs() != null) {
                        for (XPathParser.FunctionArgContext arg : funcCall.functionArgs().functionArg()) {
                            args.add(evaluateFunctionArg(arg));
                        }
                    }
                    return evaluateFunction(funcName, args);
                default:
                    return null;
            }
        }

        /**
         * Get attribute value for comparison.
         */
        private Object getAttributeValue(XPathParser.AttributeStepContext attrStep, Xml.@Nullable Tag tag) {
            if (tag == null) {
                return null;
            }
            String attrName = getAttributeStepName(attrStep);
            for (Xml.Attribute attr : tag.getAttributes()) {
                if ("*".equals(attrName) || attr.getKeyAsString().equals(attrName)) {
                    return attr.getValueAsString();
                }
            }
            return null;
        }

        /**
         * Get value from relative path for comparison.
         */
        private Object getRelativePathValue(XPathParser.RelativeLocationPathContext relPath, Xml.@Nullable Tag tag) {
            if (tag == null) {
                return null;
            }

            List<XPathParser.StepContext> steps = relPath.step();
            if (steps.isEmpty()) {
                return null;
            }

            // Check if last step is a node type test (like text())
            XPathParser.StepContext lastStep = steps.get(steps.size() - 1);
            boolean endsWithNodeTypeTest = lastStep.nodeTypeTest() != null;

            // Build the element path (without the final node type test if present)
            StringBuilder elementPath = new StringBuilder();
            int elementStepCount = endsWithNodeTypeTest ? steps.size() - 1 : steps.size();
            List<XPathParser.PathSeparatorContext> separators = relPath.pathSeparator();

            for (int i = 0; i < elementStepCount; i++) {
                if (i > 0 && i - 1 < separators.size()) {
                    elementPath.append(separators.get(i - 1).getText());
                }
                elementPath.append(steps.get(i).getText());
            }

            // Find matching elements using the element path
            Set<Xml.Tag> matchingTags;
            String pathStr = elementPath.toString();
            if (elementPath.length() == 0) {
                matchingTags = singleton(tag);
            } else if ("*".equals(pathStr)) {
                // Wildcard matches any direct child element
                matchingTags = new LinkedHashSet<>();
                List<? extends Content> contents = tag.getContent();
                if (contents != null) {
                    for (Content content : contents) {
                        if (content instanceof Xml.Tag) {
                            matchingTags.add((Xml.Tag) content);
                        }
                    }
                }
            } else {
                matchingTags = findChildTags(tag, pathStr);
            }

            if (matchingTags.isEmpty()) {
                return null;
            }

            Xml.Tag matchingTag = matchingTags.iterator().next();
            if (endsWithNodeTypeTest) {
                String nodeTypeName = lastStep.nodeTypeTest().NCNAME().getText();
                if ("text".equals(nodeTypeName)) {
                    return matchingTag.getValue().orElse("");
                }
                return null;
            } else {
                return matchingTag.getValue().orElse("");
            }
        }

        /**
         * Get child element value for comparison.
         */
        private Object getChildElementValue(XPathParser.ChildElementTestContext childTest, Xml.@Nullable Tag tag) {
            if (tag == null) {
                return null;
            }
            String childName = getChildElementTestName(childTest);
            for (Xml.Tag child : findDirectChildren(tag, childName)) {
                return child.getValue().orElse("");
            }
            return null;
        }

        /**
         * Find cursor for a tag in the path.
         */
        private Cursor findCursorForTag(Xml.Tag tag) {
            // Walk up the cursor path to find this tag
            for (Cursor c = cursor; c != null; c = c.getParent()) {
                if (c.getValue() == tag) {
                    return c;
                }
            }
            // If not found in cursor path, create a new cursor
            return new Cursor(cursor, tag);
        }

        /**
         * Remove quotes from string literal.
         */
        private String unquote(String s) {
            if (s.length() >= 2 && ((s.startsWith("'") && s.endsWith("'")) ||
                    (s.startsWith("\"") && s.endsWith("\"")))) {
                return s.substring(1, s.length() - 1);
            }
            return s;
        }

        /**
         * Get name from attribute step (handles QNAME, NCNAME, or WILDCARD).
         */
        private String getAttributeStepName(XPathParser.AttributeStepContext attrStep) {
            if (attrStep.QNAME() != null) {
                return attrStep.QNAME().getText();
            } else if (attrStep.NCNAME() != null) {
                return attrStep.NCNAME().getText();
            } else {
                return "*";
            }
        }

        /**
         * Get name from node test (handles QNAME, NCNAME, or WILDCARD).
         */
        private String getNodeTestName(XPathParser.NodeTestContext nodeTest) {
            if (nodeTest.QNAME() != null) {
                return nodeTest.QNAME().getText();
            } else if (nodeTest.NCNAME() != null) {
                return nodeTest.NCNAME().getText();
            } else {
                return "*";
            }
        }

        /**
         * Get name from child element test (handles QNAME, NCNAME, or WILDCARD).
         */
        private String getChildElementTestName(XPathParser.ChildElementTestContext childTest) {
            if (childTest.QNAME() != null) {
                return childTest.QNAME().getText();
            } else if (childTest.NCNAME() != null) {
                return childTest.NCNAME().getText();
            } else {
                return "*";
            }
        }

        /**
         * Find child tags matching a simple path expression.
         * Supports element names, wildcards (*), multi-step paths (foo/bar), and descendant axis (//).
         * Does NOT use FindTags to avoid circular dependency.
         */
        private Set<Xml.Tag> findChildTags(Xml.Tag startTag, String pathExpr) {
            Set<Xml.Tag> result = new LinkedHashSet<>();

            // Handle descendant-or-self axis (//)
            if (pathExpr.startsWith("//")) {
                String elementName = pathExpr.substring(2);
                // Remove any further path separators for now (simple case)
                if (elementName.contains("/")) {
                    elementName = elementName.substring(0, elementName.indexOf('/'));
                }
                findDescendants(startTag, elementName, result);
                return result;
            }

            // Handle paths with leading slash (absolute paths)
            boolean isAbsolute = pathExpr.startsWith("/");
            if (isAbsolute) {
                pathExpr = pathExpr.substring(1);
            }

            // Split path into steps
            String[] steps = pathExpr.split("/");
            if (steps.length == 0) {
                return result;
            }

            Set<Xml.Tag> currentMatches = new LinkedHashSet<>();

            // For absolute paths, the first step matches the startTag itself (the root)
            // For relative paths, the first step matches children of startTag
            if (isAbsolute) {
                // First step should match the startTag (root element)
                String firstStep = steps[0];
                if ("*".equals(firstStep) || startTag.getName().equals(firstStep)) {
                    if (steps.length == 1) {
                        currentMatches.add(startTag);
                    } else {
                        // Continue from children for remaining steps
                        currentMatches = findDirectChildren(startTag, steps[1]);
                        // Process remaining steps starting from index 2
                        for (int i = 2; i < steps.length; i++) {
                            Set<Xml.Tag> nextMatches = new LinkedHashSet<>();
                            for (Xml.Tag match : currentMatches) {
                                nextMatches.addAll(findDirectChildren(match, steps[i]));
                            }
                            currentMatches = nextMatches;
                        }
                    }
                }
            } else {
                // Relative path - start with children for the first step
                currentMatches = findDirectChildren(startTag, steps[0]);

                // Process remaining steps
                for (int i = 1; i < steps.length; i++) {
                    Set<Xml.Tag> nextMatches = new LinkedHashSet<>();
                    for (Xml.Tag match : currentMatches) {
                        nextMatches.addAll(findDirectChildren(match, steps[i]));
                    }
                    currentMatches = nextMatches;
                }
            }

            return currentMatches;
        }

        /**
         * Find all descendant tags matching the given element name.
         */
        private void findDescendants(Xml.Tag tag, String elementName, Set<Xml.Tag> result) {
            // Check if this tag matches
            if ("*".equals(elementName) || tag.getName().equals(elementName)) {
                result.add(tag);
            }
            // Recursively search children
            List<? extends Content> contents = tag.getContent();
            if (contents != null) {
                for (Content content : contents) {
                    if (content instanceof Xml.Tag) {
                        findDescendants((Xml.Tag) content, elementName, result);
                    }
                }
            }
        }

        /**
         * Check if a tag has a child with the given name.
         */
        private boolean childExists(Xml.Tag parent, String childName) {
            List<? extends Content> contents = parent.getContent();
            if (contents == null) {
                return false;
            }
            for (Content content : contents) {
                if (content instanceof Xml.Tag) {
                    Xml.Tag child = (Xml.Tag) content;
                    if ("*".equals(childName) || child.getName().equals(childName)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Find direct children of a tag matching the given element name or wildcard.
         */
        private Set<Xml.Tag> findDirectChildren(Xml.Tag parent, String elementName) {
            Set<Xml.Tag> result = new LinkedHashSet<>();
            List<? extends Content> contents = parent.getContent();
            if (contents == null) {
                return result;
            }

            for (Content content : contents) {
                if (content instanceof Xml.Tag) {
                    Xml.Tag child = (Xml.Tag) content;
                    if ("*".equals(elementName) || child.getName().equals(elementName)) {
                        result.add(child);
                    }
                }
            }
            return result;
        }
    }

    /**
     * Step types for compiled steps - avoids ANTLR tree navigation during matching.
     */
    private enum StepType {
        ABBREVIATED_DOT,      // .
        ABBREVIATED_DOTDOT,   // ..
        AXIS_STEP,            // parent::node(), self::element, child::*
        ATTRIBUTE_STEP,       // @attr, @*
        NODE_TYPE_TEST,       // text(), comment(), node(), processing-instruction()
        NODE_TEST             // element name or *
    }

    /**
     * Axis types for axis steps.
     */
    private enum AxisType {
        PARENT,
        SELF,
        CHILD,
        OTHER  // Unsupported axis
    }

    /**
     * Node type test types.
     */
    private enum NodeTypeTestType {
        TEXT,
        COMMENT,
        NODE,
        PROCESSING_INSTRUCTION,
        UNKNOWN
    }

    /**
     * Fully compiled step information - all data extracted from ANTLR tree.
     * No ANTLR context references needed during matching for simple steps.
     */
    private static class CompiledStep {
        final StepType type;
        final boolean isDescendant;

        // For NODE_TEST: element name (or "*" for wildcard)
        // For AXIS_STEP: node test name from axis (e.g., "node" from parent::node())
        // For ATTRIBUTE_STEP: attribute name (or "*" for @*)
        final @Nullable String name;

        // For AXIS_STEP only
        final AxisType axisType;

        // For NODE_TYPE_TEST only
        final NodeTypeTestType nodeTypeTestType;

        // Predicates - kept as ANTLR context for complex evaluation
        // null means no predicates (common case - avoids empty list allocation)
        final List<XPathParser.PredicateContext> predicates;

        // Step flags (bitmask)
        private static final int STEP_FLAG_NEEDS_POSITION = 1;
        private static final int STEP_FLAG_NEXT_IS_BACKTRACK = 1 << 1;

        int flags;

        private CompiledStep(StepType type, boolean isDescendant, @Nullable String name,
                            AxisType axisType, NodeTypeTestType nodeTypeTestType,
                            List<XPathParser.PredicateContext> predicates, int flags) {
            this.type = type;
            this.isDescendant = isDescendant;
            this.name = name;
            this.axisType = axisType;
            this.nodeTypeTestType = nodeTypeTestType;
            this.predicates = predicates;
            this.flags = flags;
        }

        boolean needsPositionInfo() {
            return (flags & STEP_FLAG_NEEDS_POSITION) != 0;
        }

        boolean nextIsBacktrack() {
            return (flags & STEP_FLAG_NEXT_IS_BACKTRACK) != 0;
        }

        void setNextIsBacktrack() {
            flags |= STEP_FLAG_NEXT_IS_BACKTRACK;
        }

        static CompiledStep fromStepContext(XPathParser.StepContext step, boolean isDescendant) {
            List<XPathParser.PredicateContext> predicates = step.predicate();
            // Use null for empty predicates to avoid allocation check during matching
            if (predicates.isEmpty()) {
                predicates = null;
            }

            // Compute flags
            int flags = predicatesNeedPosition(predicates) ? STEP_FLAG_NEEDS_POSITION : 0;

            // Abbreviated step: . or ..
            if (step.abbreviatedStep() != null) {
                if (step.abbreviatedStep().DOTDOT() != null) {
                    return new CompiledStep(StepType.ABBREVIATED_DOTDOT, isDescendant, null,
                            AxisType.OTHER, NodeTypeTestType.UNKNOWN, predicates, flags);
                } else {
                    return new CompiledStep(StepType.ABBREVIATED_DOT, isDescendant, null,
                            AxisType.OTHER, NodeTypeTestType.UNKNOWN, predicates, flags);
                }
            }

            // Axis step: parent::node(), self::element, etc.
            if (step.axisStep() != null) {
                XPathParser.AxisStepContext axisStep = step.axisStep();
                String axisName = axisStep.axisName().NCNAME().getText();
                String nodeTestName = getNodeTestNameStatic(axisStep.nodeTest());

                AxisType axisType;
                switch (axisName) {
                    case "parent":
                        axisType = AxisType.PARENT;
                        break;
                    case "self":
                        axisType = AxisType.SELF;
                        break;
                    case "child":
                        axisType = AxisType.CHILD;
                        break;
                    default:
                        axisType = AxisType.OTHER;
                }

                return new CompiledStep(StepType.AXIS_STEP, isDescendant, nodeTestName,
                        axisType, NodeTypeTestType.UNKNOWN, predicates, flags);
            }

            // Attribute step: @attr, @*
            if (step.attributeStep() != null) {
                String attrName = getAttributeStepNameStatic(step.attributeStep());
                return new CompiledStep(StepType.ATTRIBUTE_STEP, isDescendant, attrName,
                        AxisType.OTHER, NodeTypeTestType.UNKNOWN, predicates, flags);
            }

            // Node type test: text(), comment(), node(), processing-instruction()
            if (step.nodeTypeTest() != null) {
                String functionName = step.nodeTypeTest().NCNAME().getText();
                NodeTypeTestType testType;
                switch (functionName) {
                    case "text":
                        testType = NodeTypeTestType.TEXT;
                        break;
                    case "comment":
                        testType = NodeTypeTestType.COMMENT;
                        break;
                    case "node":
                        testType = NodeTypeTestType.NODE;
                        break;
                    case "processing-instruction":
                        testType = NodeTypeTestType.PROCESSING_INSTRUCTION;
                        break;
                    default:
                        testType = NodeTypeTestType.UNKNOWN;
                }
                return new CompiledStep(StepType.NODE_TYPE_TEST, isDescendant, null,
                        AxisType.OTHER, testType, predicates, flags);
            }

            // Node test: element name or *
            if (step.nodeTest() != null) {
                String nodeName = step.nodeTest().getText();
                return new CompiledStep(StepType.NODE_TEST, isDescendant, nodeName,
                        AxisType.OTHER, NodeTypeTestType.UNKNOWN, predicates, flags);
            }

            // Shouldn't reach here - return a non-matching step
            return new CompiledStep(StepType.NODE_TEST, isDescendant, null,
                    AxisType.OTHER, NodeTypeTestType.UNKNOWN, predicates, flags);
        }

        /**
         * Check if any predicate needs position/size info.
         * This includes: position(), last(), or numeric predicates like [1].
         */
        private static boolean predicatesNeedPosition(List<XPathParser.PredicateContext> predicates) {
            if (predicates == null || predicates.isEmpty()) {
                return false;
            }
            for (XPathParser.PredicateContext pred : predicates) {
                if (predicateNeedsPosition(pred.predicateExpr())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Recursively check if a predicate expression needs position info.
         */
        private static boolean predicateNeedsPosition(XPathParser.PredicateExprContext expr) {
            if (expr == null || expr.orExpr() == null) {
                return false;
            }
            return orExprNeedsPosition(expr.orExpr());
        }

        private static boolean orExprNeedsPosition(XPathParser.OrExprContext orExpr) {
            for (XPathParser.AndExprContext andExpr : orExpr.andExpr()) {
                if (andExprNeedsPosition(andExpr)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean andExprNeedsPosition(XPathParser.AndExprContext andExpr) {
            for (XPathParser.PrimaryExprContext primary : andExpr.primaryExpr()) {
                if (primaryExprNeedsPosition(primary)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean primaryExprNeedsPosition(XPathParser.PrimaryExprContext primary) {
            XPathParser.PredicateValueContext pv = primary.predicateValue();
            if (pv == null) {
                return false;
            }
            // Numeric predicate like [1], [2] - always needs position
            if (pv.NUMBER() != null) {
                return true;
            }
            // Function call - check for position() or last()
            if (pv.functionCall() != null) {
                XPathParser.FunctionCallContext fc = pv.functionCall();
                if (fc.NCNAME() != null) {
                    String funcName = fc.NCNAME().getText();
                    if ("position".equals(funcName) || "last".equals(funcName)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Check if this step is a backtrack step (.. or parent::).
         */
        boolean isBacktrack() {
            return type == StepType.ABBREVIATED_DOTDOT ||
                   (type == StepType.AXIS_STEP && axisType == AxisType.PARENT);
        }
    }

    /**
     * Static helper to get node test name without visitor instance.
     */
    private static String getNodeTestNameStatic(XPathParser.NodeTestContext nodeTest) {
        if (nodeTest == null) {
            return "*";
        }
        if (nodeTest.WILDCARD() != null) {
            return "*";
        }
        return nodeTest.getText();
    }

    /**
     * Static helper to get attribute step name without visitor instance.
     */
    private static String getAttributeStepNameStatic(XPathParser.AttributeStepContext attrStep) {
        if (attrStep.WILDCARD() != null) {
            return "*";
        }
        if (attrStep.QNAME() != null) {
            return attrStep.QNAME().getText();
        }
        if (attrStep.NCNAME() != null) {
            return attrStep.NCNAME().getText();
        }
        return "*";
    }

}
