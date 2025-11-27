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
import org.openrewrite.xml.search.FindTags;
import org.openrewrite.xml.trait.Namespaced;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.reverse;

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
        List<Xml.Tag> path = new ArrayList<>();
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            if (c.getValue() instanceof Xml.Tag) {
                path.add(c.getValue());
            }
        }

        XPathParser.XpathExpressionContext ctx = parse();
        XPathMatcherVisitor visitor = new XPathMatcherVisitor(path, cursor);
        return visitor.visit(ctx);
    }

    private XPathParser.XpathExpressionContext parse() {
        if (parsed == null) {
            XPathLexer lexer = new XPathLexer(CharStreams.fromString(expression));
            XPathParser parser = new XPathParser(new CommonTokenStream(lexer));
            parsed = parser.xpathExpression();
        }
        return parsed;
    }

    /**
     * Visitor that evaluates XPath expressions against the cursor path.
     */
    private static class XPathMatcherVisitor extends XPathParserBaseVisitor<Boolean> {
        private final List<Xml.Tag> path;
        private final Cursor cursor;
        private Xml.@Nullable Tag rootTag;

        XPathMatcherVisitor(List<Xml.Tag> path, Cursor cursor) {
            this.path = path;
            this.cursor = cursor;
        }

        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitXpathExpression(XPathParser.XpathExpressionContext ctx) {
            if (ctx.booleanExpr() != null) {
                return visitBooleanExpr(ctx.booleanExpr());
            } else if (ctx.absoluteLocationPath() != null) {
                return visitAbsoluteLocationPath(ctx.absoluteLocationPath());
            } else if (ctx.relativeLocationPath() != null) {
                return visitRelativeLocationPathFromStart(ctx.relativeLocationPath(), false, false);
            }
            return false;
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
            if (result && path.size() == 1) {
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
            } else if (funcCall.QNAME() != null) {
                funcName = funcCall.QNAME().getText();
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

            // Use FindTags to locate the element at the path
            if (absPath != null || relPath != null) {
                String pathStr = absPath != null ? absPath.getText() : relPath.getText();
                // FindTags expects an XPath expression, so we can pass it directly
                java.util.Set<Xml.Tag> found = FindTags.find(root, pathStr);
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
        private boolean evaluateComparison(Object left, XPathParser.ComparisonOpContext op, Object right) {
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
                return path.size() == 1;
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
         */
        private boolean matchAbsolute(List<StepInfo> steps) {
            // Reverse path so root is first
            List<Xml.Tag> orderedPath = new ArrayList<>(path);
            reverse(orderedPath);

            // Count how many steps match elements (not attributes or node type tests)
            long elementSteps = steps.stream()
                    .filter(s -> s.step.nodeTest() != null)
                    .count();

            // Check if we have a descendant-or-self axis (//) anywhere
            boolean hasDescendant = steps.stream().anyMatch(s -> s.isDescendant);

            if (hasDescendant) {
                return matchWithDescendant(steps, orderedPath);
            }

            // Check for special step types that don't add to element count
            boolean hasAttributeStep = steps.stream().anyMatch(s -> s.step.attributeStep() != null);
            boolean hasNodeTypeTest = steps.stream().anyMatch(s -> s.step.nodeTypeTest() != null);

            // For strict absolute paths, verify path length matches for element steps
            // (attribute step and node type tests don't add to path length)
            if (!hasAttributeStep && !hasNodeTypeTest && orderedPath.size() != elementSteps) {
                return false;
            }
            if ((hasAttributeStep || hasNodeTypeTest) && orderedPath.size() != elementSteps) {
                return false;
            }

            return matchStepsAgainstPath(steps, orderedPath, 0);
        }

        /**
         * Match paths that contain descendant-or-self axis (//).
         */
        private boolean matchWithDescendant(List<StepInfo> steps, List<Xml.Tag> orderedPath) {
            // Find where the // occurs
            int descendantIndex = -1;
            for (int i = 0; i < steps.size(); i++) {
                if (steps.get(i).isDescendant) {
                    descendantIndex = i;
                    break;
                }
            }

            if (descendantIndex == -1) {
                return matchStepsAgainstPath(steps, orderedPath, 0);
            }

            // Steps before the //
            List<StepInfo> beforeSteps = steps.subList(0, descendantIndex);
            // Steps from // onwards
            List<StepInfo> afterSteps = steps.subList(descendantIndex, steps.size());

            // Match the prefix
            if (!beforeSteps.isEmpty()) {
                for (int i = 0; i < beforeSteps.size(); i++) {
                    if (i >= orderedPath.size()) {
                        return false;
                    }
                    if (!matchStep(beforeSteps.get(i), orderedPath.get(i))) {
                        return false;
                    }
                }
            }

            // Try to match after steps from any position
            int startSearchAt = beforeSteps.size();
            for (int pathIdx = startSearchAt; pathIdx <= orderedPath.size() - countElementSteps(afterSteps); pathIdx++) {
                if (matchStepsAgainstPath(afterSteps, orderedPath, pathIdx)) {
                    return true;
                }
            }
            return false;
        }

        private int countElementSteps(List<StepInfo> steps) {
            return (int) steps.stream().filter(s -> s.step.nodeTest() != null).count();
        }

        /**
         * Match descendant-or-self (//) or relative paths.
         */
        private boolean matchDescendantOrRelative(List<StepInfo> steps, boolean isDescendantOrSelf) {
            // Reverse path so root is first
            List<Xml.Tag> orderedPath = new ArrayList<>(path);
            reverse(orderedPath);

            // Check if there's a // in the middle of the expression
            boolean hasInternalDescendant = steps.stream().skip(1).anyMatch(s -> s.isDescendant);

            if (hasInternalDescendant) {
                return matchWithDescendant(steps, orderedPath);
            }

            // Count element steps
            long elementSteps = steps.stream().filter(s -> s.step.nodeTest() != null).count();

            // For relative or // paths, try to match at the end of the path
            if (orderedPath.size() < elementSteps) {
                return false;
            }

            // Try matching from different starting positions
            int maxStartPos = orderedPath.size() - (int) elementSteps;
            for (int startPos = maxStartPos; startPos >= 0; startPos--) {
                if (matchStepsAgainstPath(steps, orderedPath, startPos)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Match steps against path starting at the given position.
         */
        private boolean matchStepsAgainstPath(List<StepInfo> steps, List<Xml.Tag> orderedPath, int pathStartIdx) {
            int pathIdx = pathStartIdx;

            for (int i = 0; i < steps.size(); i++) {
                StepInfo stepInfo = steps.get(i);
                XPathParser.StepContext step = stepInfo.step;

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
                    return matchNodeTypeTest(step.nodeTypeTest(), pathIdx, orderedPath);
                }

                // Handle descendant axis for element steps
                if (i > 0 && stepInfo.isDescendant) {
                    // Find the next matching element
                    boolean found = false;
                    for (int searchIdx = pathIdx; searchIdx < orderedPath.size(); searchIdx++) {
                        if (matchStep(stepInfo, orderedPath.get(searchIdx))) {
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
                    if (pathIdx >= orderedPath.size()) {
                        return false;
                    }
                    if (!matchStep(stepInfo, orderedPath.get(pathIdx))) {
                        return false;
                    }
                    pathIdx++;
                }
            }

            // For element paths, verify we've used the entire path
            boolean hasAttributeStep = steps.stream().anyMatch(s -> s.step.attributeStep() != null);
            if (!hasAttributeStep) {
                return pathIdx == orderedPath.size();
            }
            return true;
        }

        /**
         * Match a single step against a tag.
         */
        private boolean matchStep(StepInfo stepInfo, Xml.Tag tag) {
            XPathParser.StepContext step = stepInfo.step;
            XPathParser.NodeTestContext nodeTest = step.nodeTest();

            if (nodeTest == null) {
                return false;
            }

            // Check element name
            String expectedName = nodeTest.getText();
            if (!"*".equals(expectedName) && !tag.getName().equals(expectedName)) {
                return false;
            }

            // Check predicates
            for (XPathParser.PredicateContext predicate : step.predicate()) {
                if (!evaluatePredicate(predicate, tag)) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Match node type test (text(), comment(), node(), etc.)
         */
        private boolean matchNodeTypeTest(XPathParser.NodeTypeTestContext nodeTypeTest,
                                          int pathIdx, List<Xml.Tag> orderedPath) {
            String functionName = nodeTypeTest.QNAME().getText();

            switch (functionName) {
                case "text":
                    // text() matches when cursor is on text content
                    // In OpenRewrite, text is represented as Xml.CharData
                    if (cursor.getValue() instanceof Xml.CharData) {
                        return true;
                    }
                    // Also match if we've consumed all element steps and the last element has text content
                    // pathIdx points to where we'd be after matching all element steps
                    if (pathIdx > 0 && pathIdx == orderedPath.size()) {
                        Xml.Tag lastTag = orderedPath.get(pathIdx - 1);
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
            String attrName = attrStep.QNAME() != null ? attrStep.QNAME().getText() : "*";

            // Check attribute name
            if (!"*".equals(attrName) && !attribute.getKeyAsString().equals(attrName)) {
                return false;
            }

            // Check predicates on the attribute
            for (XPathParser.PredicateContext predicate : predicates) {
                if (!evaluateAttributePredicate(predicate, attribute)) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Evaluate predicate against attribute.
         */
        private boolean evaluateAttributePredicate(XPathParser.PredicateContext predicate, Xml.Attribute attribute) {
            return evaluateOrExpr(predicate.predicateExpr().orExpr(), null, attribute);
        }

        /**
         * Evaluate a predicate against a tag.
         */
        private boolean evaluatePredicate(XPathParser.PredicateContext predicate, Xml.Tag tag) {
            return evaluateOrExpr(predicate.predicateExpr().orExpr(), tag, null);
        }

        /**
         * Evaluate OR expression.
         */
        private boolean evaluateOrExpr(XPathParser.OrExprContext orExpr, Xml.@Nullable Tag tag, Xml.@Nullable Attribute attribute) {
            List<XPathParser.AndExprContext> andExprs = orExpr.andExpr();
            for (XPathParser.AndExprContext andExpr : andExprs) {
                if (evaluateAndExpr(andExpr, tag, attribute)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Evaluate AND expression.
         */
        private boolean evaluateAndExpr(XPathParser.AndExprContext andExpr, Xml.@Nullable Tag tag, Xml.@Nullable Attribute attribute) {
            for (XPathParser.PrimaryExprContext primaryExpr : andExpr.primaryExpr()) {
                if (!evaluatePrimaryExpr(primaryExpr, tag, attribute)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Evaluate primary expression.
         */
        private boolean evaluatePrimaryExpr(XPathParser.PrimaryExprContext primaryExpr,
                                            Xml.@Nullable Tag tag, Xml.@Nullable Attribute attribute) {
            String expectedValue = unquote(primaryExpr.stringLiteral().STRING_LITERAL().getText());

            if (primaryExpr.functionCall() != null) {
                return evaluateFunctionCall(primaryExpr.functionCall(), expectedValue, tag, attribute);
            } else if (primaryExpr.attributeStep() != null) {
                return evaluateAttributeTest(primaryExpr.attributeStep(), expectedValue, tag);
            } else if (primaryExpr.relativeLocationPath() != null) {
                return evaluateRelativePathTest(primaryExpr.relativeLocationPath(), expectedValue, tag);
            } else if (primaryExpr.childElementTest() != null) {
                return evaluateChildElementTest(primaryExpr.childElementTest(), expectedValue, tag);
            }
            return false;
        }

        /**
         * Evaluate function call (local-name(), namespace-uri(), text()).
         */
        private boolean evaluateFunctionCall(XPathParser.FunctionCallContext funcCall, String expectedValue,
                                             Xml.@Nullable Tag tag, Xml.@Nullable Attribute attribute) {
            // Get the function name - could be from dedicated tokens or QNAME
            String funcName;
            if (funcCall.LOCAL_NAME() != null) {
                funcName = "local-name";
            } else if (funcCall.NAMESPACE_URI() != null) {
                funcName = "namespace-uri";
            } else if (funcCall.QNAME() != null) {
                funcName = funcCall.QNAME().getText();
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

            String attrName = attrStep.QNAME() != null ? attrStep.QNAME().getText() : "*";

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

            String childName = childTest.QNAME() != null ? childTest.QNAME().getText() : "*";

            for (Xml.Tag child : FindTags.find(tag, childName)) {
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
            java.util.Set<Xml.Tag> matchingTags;
            if (elementPath.length() > 0) {
                matchingTags = FindTags.find(tag, elementPath.toString());
            } else {
                // No element path means the entire expression is just text() or similar
                matchingTags = java.util.Collections.singleton(tag);
            }

            // Check the expected value
            for (Xml.Tag matchingTag : matchingTags) {
                String actualValue;
                if (endsWithNodeTypeTest) {
                    String nodeTypeName = lastStep.nodeTypeTest().QNAME().getText();
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
    }

    /**
     * Helper class to hold step information including whether it follows //.
     */
    private static class StepInfo {
        final XPathParser.StepContext step;
        final boolean isDescendant;

        StepInfo(XPathParser.StepContext step, boolean isDescendant) {
            this.step = step;
            this.isDescendant = isDescendant;
        }
    }
}
