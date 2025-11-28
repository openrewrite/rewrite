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

    private final String expression;
    private XPathParser.@Nullable XpathExpressionContext parsed;

    // Pre-compiled step information (computed lazily on first parse)
    private StepInfo @Nullable [] precompiledSteps;
    private boolean precompiledAbsolutePath;
    private boolean precompiledDescendantOrSelf;
    // Pre-computed step characteristics
    private boolean precompiledHasDescendant;
    private boolean precompiledHasAbbreviatedStep;
    private boolean precompiledHasAxisStep;
    private boolean precompiledHasAttributeStep;
    private boolean precompiledHasNodeTypeTest;
    private int precompiledElementSteps;

    public XPathMatcher(String expression) {
        this.expression = expression;
    }

    /**
     * Checks if the given XPath expression matches the provided cursor.
     *
     * @param cursor the cursor representing the XML document
     * @return true if the expression matches the cursor, false otherwise
     */
    // Reusable thread-local array to avoid allocation in matches()
    // Most XML documents have depth < 32
    private static final int MAX_CACHED_DEPTH = 32;
    private static final ThreadLocal<Xml.Tag[]> PATH_CACHE = ThreadLocal.withInitial(() -> new Xml.Tag[MAX_CACHED_DEPTH]);

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
        if (tagCount <= MAX_CACHED_DEPTH) {
            // Reverse in place within cached array, then use a view
            path = cached;
            for (int i = 0, j = tagCount - 1; i < j; i++, j--) {
                Xml.Tag tmp = path[i];
                path[i] = path[j];
                path[j] = tmp;
            }
        } else {
            // Deep document - fall back to allocation
            path = new Xml.Tag[tagCount];
            int idx = tagCount - 1;
            for (Cursor c = cursor; c != null; c = c.getParent()) {
                if (c.getValue() instanceof Xml.Tag) {
                    path[idx--] = c.getValue();
                }
            }
        }

        XPathParser.XpathExpressionContext ctx = parse();
        XPathMatcherVisitor visitor = new XPathMatcherVisitor(path, tagCount, cursor,
                precompiledSteps, precompiledAbsolutePath, precompiledDescendantOrSelf,
                precompiledHasDescendant, precompiledHasAbbreviatedStep, precompiledHasAxisStep,
                precompiledHasAttributeStep, precompiledHasNodeTypeTest, precompiledElementSteps);
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
        XPathParser.RelativeLocationPathContext relPath = null;
        precompiledAbsolutePath = false;
        precompiledDescendantOrSelf = false;

        if (ctx.absoluteLocationPath() != null) {
            XPathParser.AbsoluteLocationPathContext absCtx = ctx.absoluteLocationPath();
            if (absCtx.DOUBLE_SLASH() != null) {
                precompiledDescendantOrSelf = true;
            } else {
                precompiledAbsolutePath = true;
            }
            relPath = absCtx.relativeLocationPath();
        } else if (ctx.relativeLocationPath() != null) {
            relPath = ctx.relativeLocationPath();
        }
        // For filterExpr and booleanExpr, we don't precompile (more complex structure)

        if (relPath != null) {
            // Extract steps
            List<XPathParser.StepContext> stepCtxs = relPath.step();
            List<XPathParser.PathSeparatorContext> separators = relPath.pathSeparator();
            precompiledSteps = new StepInfo[stepCtxs.size()];

            for (int i = 0; i < stepCtxs.size(); i++) {
                boolean isDescendant = false;
                if (i > 0 && i - 1 < separators.size()) {
                    isDescendant = separators.get(i - 1).DOUBLE_SLASH() != null;
                }
                precompiledSteps[i] = new StepInfo(stepCtxs.get(i), isDescendant);
            }

            // Pre-compute step characteristics
            for (StepInfo s : precompiledSteps) {
                if (s.isDescendant) precompiledHasDescendant = true;
                XPathParser.StepContext step = s.step;
                if (step.abbreviatedStep() != null) precompiledHasAbbreviatedStep = true;
                if (step.axisStep() != null) precompiledHasAxisStep = true;
                if (step.attributeStep() != null) precompiledHasAttributeStep = true;
                if (step.nodeTypeTest() != null) precompiledHasNodeTypeTest = true;
                if (step.nodeTest() != null) precompiledElementSteps++;
            }
        }
    }

    /**
     * Visitor that evaluates XPath expressions against the cursor path.
     */
    private static class XPathMatcherVisitor extends XPathParserBaseVisitor<Boolean> {
        // Path array in root-first order: index 0 = root, index pathLength-1 = current tag
        private final Xml.Tag[] path;
        private final int pathLength;
        private final Cursor cursor;
        private Xml.@Nullable Tag rootTag;

        // Pre-compiled step information (may be null for complex expressions)
        private final StepInfo @Nullable [] precompiledSteps;
        private final boolean precompiledAbsolutePath;
        private final boolean precompiledDescendantOrSelf;
        private final boolean precompiledHasDescendant;
        private final boolean precompiledHasAbbreviatedStep;
        private final boolean precompiledHasAxisStep;
        private final boolean precompiledHasAttributeStep;
        private final boolean precompiledHasNodeTypeTest;
        private final int precompiledElementSteps;

        XPathMatcherVisitor(Xml.Tag[] path, int pathLength, Cursor cursor,
                           StepInfo @Nullable [] precompiledSteps,
                           boolean precompiledAbsolutePath,
                           boolean precompiledDescendantOrSelf,
                           boolean precompiledHasDescendant,
                           boolean precompiledHasAbbreviatedStep,
                           boolean precompiledHasAxisStep,
                           boolean precompiledHasAttributeStep,
                           boolean precompiledHasNodeTypeTest,
                           int precompiledElementSteps) {
            this.path = path;
            this.pathLength = pathLength;
            this.cursor = cursor;
            this.precompiledSteps = precompiledSteps;
            this.precompiledAbsolutePath = precompiledAbsolutePath;
            this.precompiledDescendantOrSelf = precompiledDescendantOrSelf;
            this.precompiledHasDescendant = precompiledHasDescendant;
            this.precompiledHasAbbreviatedStep = precompiledHasAbbreviatedStep;
            this.precompiledHasAxisStep = precompiledHasAxisStep;
            this.precompiledHasAttributeStep = precompiledHasAttributeStep;
            this.precompiledHasNodeTypeTest = precompiledHasNodeTypeTest;
            this.precompiledElementSteps = precompiledElementSteps;
        }

        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitXpathExpression(XPathParser.XpathExpressionContext ctx) {
            // Use precompiled steps for common path expressions (fast path)
            if (precompiledSteps != null) {
                if (precompiledDescendantOrSelf) {
                    return matchDescendantOrRelativePrecompiled(true);
                } else if (precompiledAbsolutePath) {
                    return matchAbsolutePrecompiled();
                } else {
                    // Relative path
                    return matchDescendantOrRelativePrecompiled(false);
                }
            }

            // Fall back to full visitor for complex expressions (booleanExpr, filterExpr)
            if (ctx.booleanExpr() != null) {
                return visitBooleanExpr(ctx.booleanExpr());
            } else if (ctx.filterExpr() != null) {
                return visitFilterExpr(ctx.filterExpr());
            } else if (ctx.absoluteLocationPath() != null) {
                return visitAbsoluteLocationPath(ctx.absoluteLocationPath());
            } else if (ctx.relativeLocationPath() != null) {
                return visitRelativeLocationPathFromStart(ctx.relativeLocationPath(), false, false);
            }
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

        @Override
        public Boolean visitAbsoluteLocationPath(XPathParser.AbsoluteLocationPathContext ctx) {
            boolean isDoubleSlash = ctx.DOUBLE_SLASH() != null;

            if (ctx.relativeLocationPath() == null) {
                // Just "/" - matches root
                return pathLength == 1;
            }

            return visitRelativeLocationPathFromStart(ctx.relativeLocationPath(), !isDoubleSlash, isDoubleSlash);
        }

        private Boolean visitRelativeLocationPathFromStart(XPathParser.RelativeLocationPathContext ctx,
                                                           boolean absolutePath, boolean descendantOrSelf) {
            List<StepInfo> steps = extractSteps(ctx);

            if (descendantOrSelf || !absolutePath) {
                // For // or relative paths, try to match from any position
                return matchDescendantOrRelative(steps, descendantOrSelf);
            } else {
                // For absolute paths starting with /, match from root
                return matchAbsolute(steps);
            }
        }

        /**
         * Extract step information from the relative location path.
         */
        private List<StepInfo> extractSteps(XPathParser.RelativeLocationPathContext ctx) {
            List<StepInfo> steps = new ArrayList<>();
            List<XPathParser.StepContext> stepCtxs = ctx.step();
            List<XPathParser.PathSeparatorContext> separators = ctx.pathSeparator();

            for (int i = 0; i < stepCtxs.size(); i++) {
                boolean isDescendant = false;
                if (i > 0 && i - 1 < separators.size()) {
                    isDescendant = separators.get(i - 1).DOUBLE_SLASH() != null;
                }
                steps.add(new StepInfo(stepCtxs.get(i), isDescendant));
            }
            return steps;
        }

        /**
         * Match absolute paths (starting with /).
         * Path is already in root-first order (index 0 = root).
         */
        private boolean matchAbsolute(List<StepInfo> steps) {
            // Check if we have special step types that affect path traversal
            // Use loops instead of streams for better performance
            boolean hasDescendant = false;
            boolean hasAbbreviatedStep = false;
            boolean hasAxisStep = false;
            boolean hasAttributeStep = false;
            boolean hasNodeTypeTest = false;
            int elementSteps = 0;

            for (int i = 0, n = steps.size(); i < n; i++) {
                StepInfo s = steps.get(i);
                if (s.isDescendant) hasDescendant = true;
                XPathParser.StepContext step = s.step;
                if (step.abbreviatedStep() != null) hasAbbreviatedStep = true;
                if (step.axisStep() != null) hasAxisStep = true;
                if (step.attributeStep() != null) hasAttributeStep = true;
                if (step.nodeTypeTest() != null) hasNodeTypeTest = true;
                if (step.nodeTest() != null) elementSteps++;
            }

            if (hasDescendant) {
                return matchWithDescendant(steps);
            }

            // If we have abbreviated steps (..) or axis steps (parent::) that can move backwards,
            // we can't do simple path length validation - just try to match
            if (hasAbbreviatedStep || hasAxisStep) {
                return matchStepsAgainstPath(steps, 0);
            }

            // For strict absolute paths, verify path length matches for element steps
            // (attribute step and node type tests don't add to path length)
            if (!hasAttributeStep && !hasNodeTypeTest && pathLength != elementSteps) {
                return false;
            }
            if ((hasAttributeStep || hasNodeTypeTest) && pathLength != elementSteps) {
                return false;
            }

            return matchStepsAgainstPath(steps, 0);
        }

        /**
         * Match paths that contain descendant-or-self axis (//).
         * Path is already in root-first order (index 0 = root).
         */
        private boolean matchWithDescendant(List<StepInfo> steps) {
            // Find where the // occurs
            int descendantIndex = -1;
            for (int i = 0; i < steps.size(); i++) {
                if (steps.get(i).isDescendant) {
                    descendantIndex = i;
                    break;
                }
            }

            if (descendantIndex == -1) {
                return matchStepsAgainstPath(steps, 0);
            }

            // Steps before the //
            List<StepInfo> beforeSteps = steps.subList(0, descendantIndex);
            // Steps from // onwards
            List<StepInfo> afterSteps = steps.subList(descendantIndex, steps.size());

            // Match the prefix
            if (!beforeSteps.isEmpty()) {
                for (int i = 0; i < beforeSteps.size(); i++) {
                    if (i >= pathLength) {
                        return false;
                    }
                    // Compute position for this step
                    Xml.Tag currentTag = path[i];
                    int position = 1;
                    int size = 1;
                    if (i > 0) {
                        Xml.Tag parentTag = path[i - 1];
                        StepInfo stepInfo = beforeSteps.get(i);
                        String expectedName = stepInfo.nodeTestName != null ? stepInfo.nodeTestName : "*";
                        PositionInfo posInfo = computePosition(parentTag, currentTag, expectedName);
                        position = posInfo.position;
                        size = posInfo.size;
                    }
                    if (!matchStep(beforeSteps.get(i), currentTag, position, size)) {
                        return false;
                    }
                }
            }

            // Try to match after steps from any position
            int startSearchAt = beforeSteps.size();
            for (int pathIdx = startSearchAt; pathIdx <= pathLength - countElementSteps(afterSteps); pathIdx++) {
                if (matchStepsAgainstPath(afterSteps, pathIdx)) {
                    return true;
                }
            }
            return false;
        }

        private int countElementSteps(List<StepInfo> steps) {
            int count = 0;
            for (int i = 0, n = steps.size(); i < n; i++) {
                if (steps.get(i).step.nodeTest() != null) count++;
            }
            return count;
        }

        /**
         * Match descendant-or-self (//) or relative paths.
         * Path is already in root-first order (index 0 = root).
         */
        private boolean matchDescendantOrRelative(List<StepInfo> steps, boolean isDescendantOrSelf) {
            // Check if there's a // in the middle of the expression (use loop instead of stream)
            boolean hasInternalDescendant = false;
            int elementSteps = 0;
            for (int i = 0, n = steps.size(); i < n; i++) {
                StepInfo s = steps.get(i);
                if (i > 0 && s.isDescendant) hasInternalDescendant = true;
                if (s.step.nodeTest() != null) elementSteps++;
            }

            if (hasInternalDescendant) {
                return matchWithDescendant(steps);
            }

            // For relative or // paths, try to match at the end of the path
            if (pathLength < elementSteps) {
                return false;
            }

            // Try matching from different starting positions
            int maxStartPos = pathLength - elementSteps;
            for (int startPos = maxStartPos; startPos >= 0; startPos--) {
                if (matchStepsAgainstPath(steps, startPos)) {
                    return true;
                }
            }
            return false;
        }

        // ==================== PRECOMPILED FAST PATH METHODS ====================
        // These use the precompiled step array and characteristics to avoid allocations

        /**
         * Fast path for absolute paths using precompiled steps.
         */
        private boolean matchAbsolutePrecompiled() {
            if (precompiledHasDescendant) {
                return matchWithDescendantPrecompiled();
            }

            if (precompiledHasAbbreviatedStep || precompiledHasAxisStep) {
                return matchStepsAgainstPathPrecompiled(0);
            }

            if (!precompiledHasAttributeStep && !precompiledHasNodeTypeTest && pathLength != precompiledElementSteps) {
                return false;
            }
            if ((precompiledHasAttributeStep || precompiledHasNodeTypeTest) && pathLength != precompiledElementSteps) {
                return false;
            }

            return matchStepsAgainstPathPrecompiled(0);
        }

        /**
         * Fast path for descendant/relative paths using precompiled steps.
         */
        private boolean matchDescendantOrRelativePrecompiled(boolean isDescendantOrSelf) {
            // Check for internal // (already computed during precompilation as hasDescendant for steps after first)
            boolean hasInternalDescendant = false;
            for (int i = 1; i < precompiledSteps.length; i++) {
                if (precompiledSteps[i].isDescendant) {
                    hasInternalDescendant = true;
                    break;
                }
            }

            if (hasInternalDescendant) {
                return matchWithDescendantPrecompiled();
            }

            if (pathLength < precompiledElementSteps) {
                return false;
            }

            int maxStartPos = pathLength - precompiledElementSteps;
            for (int startPos = maxStartPos; startPos >= 0; startPos--) {
                if (matchStepsAgainstPathPrecompiled(startPos)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Fast path for paths with descendant axis using precompiled steps.
         */
        private boolean matchWithDescendantPrecompiled() {
            // Find where the // occurs
            int descendantIndex = -1;
            for (int i = 0; i < precompiledSteps.length; i++) {
                if (precompiledSteps[i].isDescendant) {
                    descendantIndex = i;
                    break;
                }
            }

            if (descendantIndex == -1) {
                return matchStepsAgainstPathPrecompiled(0);
            }

            // Match the prefix (steps before //)
            for (int i = 0; i < descendantIndex; i++) {
                if (i >= pathLength) {
                    return false;
                }
                Xml.Tag currentTag = path[i];
                int position = 1;
                int size = 1;
                if (i > 0) {
                    Xml.Tag parentTag = path[i - 1];
                    StepInfo stepInfo = precompiledSteps[i];
                    String expectedName = stepInfo.nodeTestName != null ? stepInfo.nodeTestName : "*";
                    PositionInfo posInfo = computePosition(parentTag, currentTag, expectedName);
                    position = posInfo.position;
                    size = posInfo.size;
                }
                if (!matchStep(precompiledSteps[i], currentTag, position, size)) {
                    return false;
                }
            }

            // Count element steps in the suffix
            int afterElementSteps = 0;
            for (int i = descendantIndex; i < precompiledSteps.length; i++) {
                if (precompiledSteps[i].step.nodeTest() != null) afterElementSteps++;
            }

            // Try to match after steps from any position
            int startSearchAt = descendantIndex;
            for (int pathIdx = startSearchAt; pathIdx <= pathLength - afterElementSteps; pathIdx++) {
                if (matchStepsAgainstPathPrecompiledFrom(descendantIndex, pathIdx)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Fast path step matching using precompiled array.
         */
        private boolean matchStepsAgainstPathPrecompiled(int pathStartIdx) {
            return matchStepsAgainstPathPrecompiledFrom(0, pathStartIdx);
        }

        /**
         * Match steps from a given step index against path from a given path index.
         */
        private boolean matchStepsAgainstPathPrecompiledFrom(int stepStartIdx, int pathStartIdx) {
            int pathIdx = pathStartIdx;
            boolean didSiblingCheck = false;

            for (int i = stepStartIdx; i < precompiledSteps.length; i++) {
                StepInfo stepInfo = precompiledSteps[i];
                XPathParser.StepContext step = stepInfo.step;

                // Handle abbreviated step (. or ..)
                if (step.abbreviatedStep() != null) {
                    if (step.abbreviatedStep().DOTDOT() != null) {
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
                    }
                    continue;
                }

                // Handle axis step (parent::node(), etc.)
                if (step.axisStep() != null) {
                    String axisName = step.axisStep().axisName().NCNAME().getText();
                    String nodeTestName = getNodeTestName(step.axisStep().nodeTest());

                    if ("parent".equals(axisName)) {
                        if (didSiblingCheck) {
                            didSiblingCheck = false;
                            if (pathIdx > 0) {
                                Xml.Tag parentTag = path[pathIdx - 1];
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
                        if (!"*".equals(nodeTestName) && !"node".equals(nodeTestName) &&
                                !parentTag.getName().equals(nodeTestName)) {
                            return false;
                        }
                    } else if ("self".equals(axisName)) {
                        int contextIdx = pathIdx - 1;
                        if (contextIdx < 0 || contextIdx >= pathLength) {
                            return false;
                        }
                        Xml.Tag currentTag = path[contextIdx];
                        if (!"*".equals(nodeTestName) && !"node".equals(nodeTestName) &&
                                !currentTag.getName().equals(nodeTestName)) {
                            return false;
                        }
                    } else if (!"child".equals(axisName)) {
                        return false;
                    }
                    continue;
                }

                // Handle attribute step
                if (step.attributeStep() != null) {
                    return matchAttributeStep(step.attributeStep(), step.predicate());
                }

                // Handle node type test
                if (step.nodeTypeTest() != null) {
                    return matchNodeTypeTest(step.nodeTypeTest(), pathIdx);
                }

                // Compute position and size
                Xml.Tag currentTag = pathIdx < pathLength ? path[pathIdx] : null;
                int position = 1;
                int size = 1;
                if (currentTag != null && pathIdx > 0) {
                    Xml.Tag parentTag = path[pathIdx - 1];
                    String expectedName = stepInfo.nodeTestName != null ? stepInfo.nodeTestName : "*";
                    PositionInfo posInfo = computePosition(parentTag, currentTag, expectedName);
                    position = posInfo.position;
                    size = posInfo.size;
                }

                // Handle descendant axis for element steps
                if (i > stepStartIdx && stepInfo.isDescendant) {
                    boolean found = false;
                    for (int searchIdx = pathIdx; searchIdx < pathLength; searchIdx++) {
                        Xml.Tag searchTag = path[searchIdx];
                        int searchPos = 1;
                        int searchSize = 1;
                        if (searchIdx > 0) {
                            Xml.Tag searchParent = path[searchIdx - 1];
                            String expectedName = stepInfo.nodeTestName != null ? stepInfo.nodeTestName : "*";
                            PositionInfo posInfo = computePosition(searchParent, searchTag, expectedName);
                            searchPos = posInfo.position;
                            searchSize = posInfo.size;
                        }
                        if (matchStep(stepInfo, searchTag, searchPos, searchSize)) {
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
                        if (i + 1 < precompiledSteps.length) {
                            XPathParser.StepContext nextStep = precompiledSteps[i + 1].step;
                            boolean isBackTrack = (nextStep.abbreviatedStep() != null && nextStep.abbreviatedStep().DOTDOT() != null) ||
                                    (nextStep.axisStep() != null && "parent".equals(nextStep.axisStep().axisName().NCNAME().getText()));
                            if (isBackTrack) {
                                if (pathIdx <= 0) {
                                    return false;
                                }
                                Xml.Tag parent = path[pathIdx - 1];
                                String expectedChild = step.nodeTest() != null ? getNodeTestName(step.nodeTest()) : "*";
                                if (!childExists(parent, expectedChild)) {
                                    return false;
                                }
                                continue;
                            }
                        }
                        return false;
                    }
                    if (!matchStep(stepInfo, path[pathIdx], position, size)) {
                        if (i + 1 < precompiledSteps.length) {
                            XPathParser.StepContext nextStep = precompiledSteps[i + 1].step;
                            boolean isBackTrack = (nextStep.abbreviatedStep() != null && nextStep.abbreviatedStep().DOTDOT() != null) ||
                                    (nextStep.axisStep() != null && "parent".equals(nextStep.axisStep().axisName().NCNAME().getText()));
                            if (isBackTrack && pathIdx > 0) {
                                Xml.Tag parent = path[pathIdx - 1];
                                String expectedChild = step.nodeTest() != null ? getNodeTestName(step.nodeTest()) : "*";
                                if (childExists(parent, expectedChild)) {
                                    didSiblingCheck = true;
                                    continue;
                                }
                            }
                        }
                        return false;
                    }
                    pathIdx++;
                }
            }

            // For element paths, verify we've used the entire path
            if (!precompiledHasAttributeStep && !precompiledHasAbbreviatedStep && !precompiledHasAxisStep) {
                return pathIdx == pathLength;
            }
            return true;
        }

        /**
         * Match steps against path starting at the given position.
         */
        private boolean matchStepsAgainstPath(List<StepInfo> steps, int pathStartIdx) {
            int pathIdx = pathStartIdx;

            // Track whether we did a virtual sibling check (element exists but wasn't in cursor path)
            boolean didSiblingCheck = false;

            for (int i = 0; i < steps.size(); i++) {
                StepInfo stepInfo = steps.get(i);
                XPathParser.StepContext step = stepInfo.step;

                // Handle abbreviated step (. or ..)
                if (step.abbreviatedStep() != null) {
                    if (step.abbreviatedStep().DOTDOT() != null) {
                        // .. means parent - move backwards in the path
                        if (didSiblingCheck) {
                            // Previous step was a sibling check, so we're already conceptually at parent level
                            // The .. just confirms we want to stay at the current path position
                            didSiblingCheck = false;
                            // Don't decrement pathIdx
                        } else {
                            // If we've already gone past the cursor's current position, clamp
                            if (pathIdx > pathLength) {
                                pathIdx = pathLength;
                            }
                            if (pathIdx <= 0) {
                                return false; // No parent available
                            }
                            pathIdx--; // Move to parent
                        }
                    }
                    // . means self - stay at current position (no-op)
                    continue;
                }

                // Handle axis step (parent::node(), etc.)
                if (step.axisStep() != null) {
                    String axisName = step.axisStep().axisName().NCNAME().getText();
                    String nodeTestName = getNodeTestName(step.axisStep().nodeTest());

                    if ("parent".equals(axisName)) {
                        // parent axis - move backwards in the path
                        if (didSiblingCheck) {
                            // Previous step was a sibling check, so we're already at parent level
                            didSiblingCheck = false;
                            // Don't decrement pathIdx, just verify the parent matches the node test
                            if (pathIdx > 0) {
                                Xml.Tag parentTag = path[pathIdx - 1];
                                if (!"*".equals(nodeTestName) && !"node".equals(nodeTestName) &&
                                        !parentTag.getName().equals(nodeTestName)) {
                                    return false;
                                }
                            }
                            continue;
                        }
                        if (pathIdx <= 0) {
                            return false; // No parent available
                        }
                        pathIdx--; // Move to parent
                        // Verify the parent matches the node test
                        Xml.Tag parentTag = path[pathIdx];
                        if (!"*".equals(nodeTestName) && !"node".equals(nodeTestName) &&
                                !parentTag.getName().equals(nodeTestName)) {
                            return false;
                        }
                    } else if ("self".equals(axisName)) {
                        // self axis - verify current context node matches
                        // The current context is the node we just matched (pathIdx - 1)
                        int contextIdx = pathIdx - 1;
                        if (contextIdx < 0 || contextIdx >= pathLength) {
                            return false;
                        }
                        Xml.Tag currentTag = path[contextIdx];
                        if (!"*".equals(nodeTestName) && !"node".equals(nodeTestName) &&
                                !currentTag.getName().equals(nodeTestName)) {
                            return false;
                        }
                        // Stay at current position (don't advance)
                    } else if ("child".equals(axisName)) {
                        // child axis is the default, handled below with normal element matching
                        // Fall through to normal step handling
                    } else {
                        // Unsupported axis (ancestor, following, preceding, etc.)
                        // FIXME: Other axes not yet implemented
                        return false;
                    }
                    continue;
                }

                // Handle attribute step first (may or may not be descendant)
                if (step.attributeStep() != null) {
                    // For //@attr (descendant attribute), we just need to verify:
                    // 1. The cursor is on a matching attribute
                    // 2. We're somewhere within the matched path context
                    // For regular /@attr, we need to be at the end of the element path
                    if (stepInfo.isDescendant) {
                        // Descendant attribute - cursor just needs to be on matching attr
                        // and we need to be within the path (pathIdx doesn't need to equal path size)
                        return matchAttributeStep(step.attributeStep(), step.predicate());
                    }
                    // Non-descendant attribute - must be at end of element path
                    return matchAttributeStep(step.attributeStep(), step.predicate());
                }

                // Handle node type test (text(), comment(), node(), etc.)
                if (step.nodeTypeTest() != null) {
                    return matchNodeTypeTest(step.nodeTypeTest(), pathIdx);
                }

                // Compute position and size for positional predicates
                Xml.Tag currentTag = pathIdx < pathLength ? path[pathIdx] : null;
                int position = 1;
                int size = 1;
                if (currentTag != null && pathIdx > 0) {
                    Xml.Tag parentTag = path[pathIdx - 1];
                    String expectedName = stepInfo.nodeTestName != null ? stepInfo.nodeTestName : "*";
                    PositionInfo posInfo = computePosition(parentTag, currentTag, expectedName);
                    position = posInfo.position;
                    size = posInfo.size;
                }

                // Handle descendant axis for element steps
                if (i > 0 && stepInfo.isDescendant) {
                    // Find the next matching element
                    boolean found = false;
                    for (int searchIdx = pathIdx; searchIdx < pathLength; searchIdx++) {
                        Xml.Tag searchTag = path[searchIdx];
                        int searchPos = 1;
                        int searchSize = 1;
                        if (searchIdx > 0) {
                            Xml.Tag searchParent = path[searchIdx - 1];
                            String expectedName = stepInfo.nodeTestName != null ? stepInfo.nodeTestName : "*";
                            PositionInfo posInfo = computePosition(searchParent, searchTag, expectedName);
                            searchPos = posInfo.position;
                            searchSize = posInfo.size;
                        }
                        if (matchStep(stepInfo, searchTag, searchPos, searchSize)) {
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
                        // We've gone past the cursor position - check if next step moves back up
                        // This handles patterns like /a/b/c/.. (cursor at /a/b) or /a/b/c/../d (cursor at /a/b/d)
                        if (i + 1 < steps.size()) {
                            XPathParser.StepContext nextStep = steps.get(i + 1).step;
                            boolean isBackTrack = (nextStep.abbreviatedStep() != null && nextStep.abbreviatedStep().DOTDOT() != null) ||
                                    (nextStep.axisStep() != null && "parent".equals(nextStep.axisStep().axisName().NCNAME().getText()));

                            if (isBackTrack) {
                                // Pattern is going "up" after this step, so verify the element exists
                                // The parent is at path[pathIdx - 1]
                                if (pathIdx <= 0) {
                                    return false;
                                }
                                Xml.Tag parent = path[pathIdx - 1];
                                String expectedChild = step.nodeTest() != null ? getNodeTestName(step.nodeTest()) : "*";
                                if (!childExists(parent, expectedChild)) {
                                    return false;
                                }
                                // Don't advance pathIdx - the .. will handle moving back
                                // But we need to mark that we've "visited" this virtual position
                                // Actually, we stay at pathIdx because the .. will not change it
                                // (it would move back, but we're already at the right level)
                                continue;
                            }
                        }
                        return false;
                    }
                    if (!matchStep(stepInfo, path[pathIdx], position, size)) {
                        // Element doesn't match - but check if next step is .. or parent::
                        // In that case, we may be at a sibling position and need to verify the sibling exists
                        if (i + 1 < steps.size()) {
                            XPathParser.StepContext nextStep = steps.get(i + 1).step;
                            boolean isBackTrack = (nextStep.abbreviatedStep() != null && nextStep.abbreviatedStep().DOTDOT() != null) ||
                                    (nextStep.axisStep() != null && "parent".equals(nextStep.axisStep().axisName().NCNAME().getText()));

                            if (isBackTrack && pathIdx > 0) {
                                // Check if expected element exists as sibling (child of parent)
                                Xml.Tag parent = path[pathIdx - 1];
                                String expectedChild = step.nodeTest() != null ? getNodeTestName(step.nodeTest()) : "*";
                                if (childExists(parent, expectedChild)) {
                                    // Element exists as sibling - continue without advancing
                                    // Mark that we did a sibling check so the .. won't decrement pathIdx
                                    didSiblingCheck = true;
                                    continue;
                                }
                            }
                        }
                        return false;
                    }
                    pathIdx++;
                }
            }

            // For element paths, verify we've used the entire path
            // Use loops instead of streams for better performance
            boolean hasAttributeStep = false;
            boolean hasAbbreviatedStep = false;
            boolean hasAxisStep = false;
            for (int i = 0, n = steps.size(); i < n; i++) {
                XPathParser.StepContext s = steps.get(i).step;
                if (s.attributeStep() != null) { hasAttributeStep = true; break; }
                if (s.abbreviatedStep() != null) { hasAbbreviatedStep = true; break; }
                if (s.axisStep() != null) { hasAxisStep = true; break; }
            }
            if (!hasAttributeStep && !hasAbbreviatedStep && !hasAxisStep) {
                return pathIdx == pathLength;
            }
            // With abbreviated/axis steps, the path index may have moved around
            return true;
        }

        /**
         * Compute the position and size for a tag among its siblings with the same name.
         * Optimized to avoid ArrayList allocation - just count and track position.
         */
        private PositionInfo computePosition(Xml.Tag parent, Xml.Tag currentTag, String expectedName) {
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

            return new PositionInfo(position, size == 0 ? 1 : size);
        }

        /**
         * Match a single step against a tag.
         * @param position 1-based position of this tag among matching siblings (use 1 if unknown)
         * @param size total count of matching siblings (use 1 if unknown)
         */
        private boolean matchStep(StepInfo stepInfo, Xml.Tag tag, int position, int size) {
            // Use cached nodeTestName to avoid getText() allocation
            String expectedName = stepInfo.nodeTestName;
            if (expectedName == null) {
                return false;
            }

            // Check element name
            if (!"*".equals(expectedName) && !tag.getName().equals(expectedName)) {
                return false;
            }

            // Check predicates
            for (XPathParser.PredicateContext predicate : stepInfo.step.predicate()) {
                if (!evaluatePredicate(predicate, tag, position, size)) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Match node type test (text(), comment(), node(), etc.)
         */
        private boolean matchNodeTypeTest(XPathParser.NodeTypeTestContext nodeTypeTest, int pathIdx) {
            String functionName = nodeTypeTest.NCNAME().getText();

            switch (functionName) {
                case "text":
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

                case "comment":
                    // comment() matches when cursor is on a comment
                    return cursor.getValue() instanceof Xml.Comment;

                case "node":
                    // node() matches any node type
                    return true;

                case "processing-instruction":
                    // processing-instruction() matches processing instructions
                    return cursor.getValue() instanceof Xml.ProcessingInstruction;

                default:
                    // Unknown node type test - not supported
                    return false;
            }
        }

        /**
         * Match attribute step.
         */
        private boolean matchAttributeStep(XPathParser.AttributeStepContext attrStep,
                                           List<XPathParser.PredicateContext> predicates) {
            if (!(cursor.getValue() instanceof Xml.Attribute)) {
                return false;
            }

            Xml.Attribute attribute = cursor.getValue();
            String attrName = getAttributeStepName(attrStep);

            // Check attribute name
            if (!"*".equals(attrName) && !attribute.getKeyAsString().equals(attrName)) {
                return false;
            }

            // Check predicates on the attribute
            // Positional predicates on attributes are uncommon, use position=1, size=1
            for (XPathParser.PredicateContext predicate : predicates) {
                if (!evaluateAttributePredicate(predicate, attribute, 1, 1)) {
                    return false;
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
         * Evaluate function call (local-name(), namespace-uri(), text()).
         */
        private boolean evaluateFunctionCall(XPathParser.FunctionCallContext funcCall, String expectedValue,
                                             Xml.@Nullable Tag tag, Xml.@Nullable Attribute attribute) {
            // Get the function name - could be from dedicated tokens or NCNAME
            String funcName;
            if (funcCall.LOCAL_NAME() != null) {
                funcName = "local-name";
            } else if (funcCall.NAMESPACE_URI() != null) {
                funcName = "namespace-uri";
            } else if (funcCall.NCNAME() != null) {
                funcName = funcCall.NCNAME().getText();
            } else {
                return false;
            }

            switch (funcName) {
                case "local-name":
                    if (attribute != null) {
                        Namespaced namespaced = new Namespaced(new Cursor(cursor.getParent(), attribute));
                        return Objects.equals(namespaced.getLocalName().orElse(null), expectedValue);
                    } else if (tag != null) {
                        Namespaced namespaced = new Namespaced(findCursorForTag(tag));
                        return Objects.equals(namespaced.getLocalName().orElse(null), expectedValue);
                    }
                    break;
                case "namespace-uri":
                    if (attribute != null) {
                        Namespaced namespaced = new Namespaced(new Cursor(cursor.getParent(), attribute));
                        Optional<String> nsUri = namespaced.getNamespaceUri();
                        return nsUri.isPresent() && nsUri.get().equals(expectedValue);
                    } else if (tag != null) {
                        Namespaced namespaced = new Namespaced(findCursorForTag(tag));
                        Optional<String> nsUri = namespaced.getNamespaceUri();
                        return nsUri.isPresent() && nsUri.get().equals(expectedValue);
                    }
                    break;
                case "text":
                    if (tag != null) {
                        return tag.getValue().map(v -> v.equals(expectedValue)).orElse(false);
                    }
                    break;
                default:
                    // Unknown function - not supported
                    break;
            }
            return false;
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
         * Evaluate attribute test [@attr='value'] or [@*='value'].
         */
        private boolean evaluateAttributeTest(XPathParser.AttributeStepContext attrStep, String expectedValue,
                                              Xml.@Nullable Tag tag) {
            if (tag == null) {
                return false;
            }

            String attrName = getAttributeStepName(attrStep);

            for (Xml.Attribute attr : tag.getAttributes()) {
                if ("*".equals(attrName) || attr.getKeyAsString().equals(attrName)) {
                    if (attr.getValueAsString().equals(expectedValue)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Evaluate child element test [child='value'] or [*='value'].
         */
        private boolean evaluateChildElementTest(XPathParser.ChildElementTestContext childTest, String expectedValue,
                                                 Xml.@Nullable Tag tag) {
            if (tag == null) {
                return false;
            }

            String childName = getChildElementTestName(childTest);

            for (Xml.Tag child : findDirectChildren(tag, childName)) {
                if (child.getValue().map(v -> v.equals(expectedValue)).orElse(false)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Evaluate relative path test within predicate [bar/baz/text()='value'].
         * This handles path expressions that may end with a node type test like text().
         */
        private boolean evaluateRelativePathTest(XPathParser.RelativeLocationPathContext relPath, String expectedValue,
                                                 Xml.@Nullable Tag tag) {
            if (tag == null) {
                return false;
            }

            List<XPathParser.StepContext> steps = relPath.step();
            if (steps.isEmpty()) {
                return false;
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
            if (elementPath.length() > 0) {
                matchingTags = findChildTags(tag, elementPath.toString());
            } else {
                // No element path means the entire expression is just text() or similar
                matchingTags = singleton(tag);
            }

            // Check the expected value
            for (Xml.Tag matchingTag : matchingTags) {
                String actualValue;
                if (endsWithNodeTypeTest) {
                    String nodeTypeName = lastStep.nodeTypeTest().NCNAME().getText();
                    if ("text".equals(nodeTypeName)) {
                        actualValue = matchingTag.getValue().orElse("");
                    } else {
                        // Other node type tests not supported in comparison context
                        continue;
                    }
                } else {
                    // Direct element value comparison
                    actualValue = matchingTag.getValue().orElse("");
                }

                if (actualValue.equals(expectedValue)) {
                    return true;
                }
            }
            return false;
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
     * Helper class to hold step information including whether it follows //.
     * Caches getText() results to avoid repeated String allocations during matching.
     */
    private static class StepInfo {
        final XPathParser.StepContext step;
        final boolean isDescendant;
        // Cached getText() result for nodeTest - avoids allocation on every match
        final @Nullable String nodeTestName;

        StepInfo(XPathParser.StepContext step, boolean isDescendant) {
            this.step = step;
            this.isDescendant = isDescendant;
            // Cache the nodeTest name once during construction
            this.nodeTestName = step.nodeTest() != null ? step.nodeTest().getText() : null;
        }
    }

    /**
     * Helper class to hold position information for positional predicates.
     */
    private static class PositionInfo {
        final int position;
        final int size;

        PositionInfo(int position, int size) {
            this.position = position;
            this.size = size;
        }
    }
}
