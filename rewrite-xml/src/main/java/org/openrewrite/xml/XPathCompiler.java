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
import org.openrewrite.xml.internal.grammar.XPathLexer;
import org.openrewrite.xml.internal.grammar.XPathParser;

import java.util.List;

/**
 * Parses and compiles XPath expressions into an optimized representation
 * for efficient matching against XML cursor positions.
 */
final class XPathCompiler {

    // Step characteristic flags (bitmask)
    static final int FLAG_ABSOLUTE_PATH = 1;
    static final int FLAG_DESCENDANT_OR_SELF = 1 << 1;
    static final int FLAG_HAS_DESCENDANT = 1 << 2;
    static final int FLAG_HAS_ABBREVIATED_STEP = 1 << 3;
    static final int FLAG_HAS_AXIS_STEP = 1 << 4;
    static final int FLAG_HAS_ATTRIBUTE_STEP = 1 << 5;
    static final int FLAG_HAS_NODE_TYPE_TEST = 1 << 6;

    // Expression type constants
    static final int EXPR_PATH = 0;           // Simple path (compiled)
    static final int EXPR_BOOLEAN = 1;        // Boolean expression (compiled)
    static final int EXPR_FILTER = 2;         // Filter expression (compiled)

    // Empty steps array for non-path expressions
    private static final CompiledStep[] EMPTY_STEPS = new CompiledStep[0];

    private XPathCompiler() {
        // Utility class
    }

    /**
     * Compile an XPath expression into an optimized representation.
     *
     * @param expression the XPath expression to compile
     * @return the compiled XPath representation
     */
    public static CompiledXPath compile(String expression) {
        XPathLexer lexer = new XPathLexer(CharStreams.fromString(expression));
        XPathParser parser = new XPathParser(new CommonTokenStream(lexer));
        XPathParser.XpathExpressionContext ctx = parser.xpathExpression();
        return compileSteps(ctx);
    }

    /**
     * Pre-compile step information from parsed XPath context.
     */
    private static CompiledXPath compileSteps(XPathParser.XpathExpressionContext ctx) {
        // Determine expression type first
        if (ctx.booleanExpr() != null) {
            CompiledExpr boolExpr = compileBooleanExpr(ctx.booleanExpr());
            return new CompiledXPath(EMPTY_STEPS, 0, 0, EXPR_BOOLEAN, boolExpr, null);
        } else if (ctx.filterExpr() != null) {
            CompiledFilterExpr filterExpr = compileFilterExpr(ctx.filterExpr());
            return new CompiledXPath(EMPTY_STEPS, 0, 0, EXPR_FILTER, null, filterExpr);
        }

        // Otherwise it's a path expression
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

        CompiledStep[] compiledSteps = EMPTY_STEPS;
        int compiledElementSteps = 0;

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

        // Normalize mid-path parent steps (.. or parent::) to existence predicates
        compiledSteps = normalizeParentSteps(compiledSteps);

        // Recompute flags and element count after normalization
        flags = recomputeFlags(compiledSteps, flags);
        compiledElementSteps = countElementSteps(compiledSteps);

        return new CompiledXPath(compiledSteps, flags, compiledElementSteps, EXPR_PATH, null, null);
    }

    /**
     * Normalize mid-path parent steps (.. or parent::) into existence predicates.
     * <p>
     * Transforms patterns like:
     * - /a/b/c/../e → /a/b[c]/e (b must have child c)
     * - /a/b/c/d/../../e → /a/b[c/d]/e (b must have path c/d)
     * <p>
     * Leading parent steps (like ../foo) are left unchanged as they work naturally
     * in bottom-up matching.
     */
    private static CompiledStep[] normalizeParentSteps(CompiledStep[] steps) {
        if (steps.length == 0) {
            return steps;
        }

        // Quick check: does this path have any parent steps that aren't at the start?
        boolean hasNormalizableParent = false;
        for (int i = 1; i < steps.length; i++) {
            if (steps[i].isBacktrack() && !isLeadingParentStep(steps, i)) {
                hasNormalizableParent = true;
                break;
            }
        }
        if (!hasNormalizableParent) {
            return steps;
        }

        // Build normalized step list
        java.util.ArrayList<CompiledStep> result = new java.util.ArrayList<>();
        int i = 0;

        while (i < steps.length) {
            // Check if this is a parent step that should be normalized
            if (steps[i].isBacktrack() && !isLeadingParentStep(steps, i)) {
                // Count consecutive parent steps
                int parentCount = 0;
                int parentStart = i;
                while (i < steps.length && steps[i].isBacktrack()) {
                    parentCount++;
                    i++;
                }

                // The parentCount element steps before the parent sequence become a predicate
                // These steps are at positions: parentStart - parentCount to parentStart - 1
                int predicateStartIdx = parentStart - parentCount;

                if (predicateStartIdx < 0 || predicateStartIdx >= result.size()) {
                    // Not enough steps to consume - this is an edge case
                    // Just skip the parent steps (they'll be handled as leading parents)
                    continue;
                }

                // Extract the steps that become the predicate
                int stepsToConvert = Math.min(parentCount, result.size() - predicateStartIdx);
                CompiledStep[] predicateSteps = new CompiledStep[stepsToConvert];
                for (int j = 0; j < stepsToConvert; j++) {
                    predicateSteps[j] = result.remove(predicateStartIdx);
                }

                // Create the predicate expression
                CompiledExpr predicate = createPathPredicate(predicateSteps);

                // Attach predicate to the step now at predicateStartIdx - 1 (the anchor)
                if (predicateStartIdx > 0 && predicateStartIdx <= result.size()) {
                    int anchorIdx = predicateStartIdx - 1;
                    result.set(anchorIdx, result.get(anchorIdx).withAdditionalPredicate(predicate));
                }

                // Continue processing remaining steps after the parent sequence
            } else {
                result.add(steps[i]);
                i++;
            }
        }

        return result.toArray(new CompiledStep[0]);
    }

    /**
     * Check if a parent step at the given index is a "leading" parent step.
     * Leading parent steps are those at the start or only preceded by other parent steps.
     */
    private static boolean isLeadingParentStep(CompiledStep[] steps, int index) {
        for (int i = 0; i < index; i++) {
            if (!steps[i].isBacktrack()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create a predicate expression from a sequence of steps.
     * Single step becomes CHILD, multiple steps become PATH.
     */
    private static CompiledExpr createPathPredicate(CompiledStep[] steps) {
        if (steps.length == 1) {
            return CompiledExpr.child(steps[0].name);
        }

        // Multiple steps - create PATH expression
        CompiledExpr[] childExprs = new CompiledExpr[steps.length];
        for (int i = 0; i < steps.length; i++) {
            childExprs[i] = CompiledExpr.child(steps[i].name);
        }
        return CompiledExpr.path(childExprs, null);
    }

    /**
     * Recompute flags after normalization (parent steps may have been removed).
     */
    private static int recomputeFlags(CompiledStep[] steps, int originalFlags) {
        // Keep absolute/descendant-or-self flags from original
        int flags = originalFlags & (FLAG_ABSOLUTE_PATH | FLAG_DESCENDANT_OR_SELF);

        for (CompiledStep s : steps) {
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
            }
        }
        return flags;
    }

    /**
     * Count element steps (NODE_TEST type).
     */
    private static int countElementSteps(CompiledStep[] steps) {
        int count = 0;
        for (CompiledStep s : steps) {
            if (s.type == StepType.NODE_TEST) {
                count++;
            }
        }
        return count;
    }

    /**
     * Compile a top-level boolean expression: functionCall [comparisonOp comparand]
     */
    private static CompiledExpr compileBooleanExpr(XPathParser.BooleanExprContext ctx) {
        CompiledExpr funcExpr = compileFunctionCall(ctx.functionCall());

        if (ctx.comparisonOp() != null && ctx.comparand() != null) {
            ComparisonOp op = compileComparisonOp(ctx.comparisonOp());
            CompiledExpr comparand = compileComparand(ctx.comparand());
            return CompiledExpr.comparison(funcExpr, op, comparand);
        }

        return funcExpr;
    }

    /**
     * Compile a filter expression: (path)[predicate] [/trailing]
     */
    private static CompiledFilterExpr compileFilterExpr(XPathParser.FilterExprContext ctx) {
        // Get the path expression (absolute or relative) - first one is inside parentheses
        String pathExpr;
        if (ctx.absoluteLocationPath() != null) {
            pathExpr = ctx.absoluteLocationPath().getText();
        } else if (!ctx.relativeLocationPath().isEmpty()) {
            pathExpr = ctx.relativeLocationPath(0).getText();
        } else {
            pathExpr = "";
        }

        // Compile predicates
        CompiledExpr[] predicates = compilePredicates(ctx.predicate());

        // Check for trailing path
        String trailingPath = null;
        boolean trailingIsDescendant = false;
        if (ctx.pathSeparator() != null && ctx.relativeLocationPath().size() > 1) {
            trailingPath = ctx.relativeLocationPath(1).getText();
            trailingIsDescendant = ctx.pathSeparator().DOUBLE_SLASH() != null;
        }

        return new CompiledFilterExpr(pathExpr, predicates, trailingPath, trailingIsDescendant);
    }

    /**
     * Get name from node test (handles QNAME, NCNAME, or WILDCARD).
     */
    static String getNodeTestName(XPathParser.@Nullable NodeTestContext nodeTest) {
        if (nodeTest == null) {
            return "*";
        }
        if (nodeTest.WILDCARD() != null) {
            return "*";
        }
        return nodeTest.getText();
    }

    /**
     * Get name from attribute step (handles QNAME, NCNAME, or WILDCARD).
     */
    static String getAttributeStepName(XPathParser.AttributeStepContext attrStep) {
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

    /**
     * Get name from child element test (handles QNAME, NCNAME, or WILDCARD).
     */
    static String getChildElementTestName(XPathParser.ChildElementTestContext childTest) {
        if (childTest.QNAME() != null) {
            return childTest.QNAME().getText();
        }
        if (childTest.NCNAME() != null) {
            return childTest.NCNAME().getText();
        }
        return "*";
    }

    // ==================== Predicate Compilation ====================

    private static final CompiledExpr[] EMPTY_PREDICATES = new CompiledExpr[0];

    /**
     * Compile a list of predicates into CompiledExpr array.
     */
    static CompiledExpr[] compilePredicates(@Nullable List<XPathParser.PredicateContext> predicates) {
        if (predicates == null || predicates.isEmpty()) {
            return EMPTY_PREDICATES;
        }
        CompiledExpr[] result = new CompiledExpr[predicates.size()];
        for (int i = 0; i < predicates.size(); i++) {
            result[i] = compilePredicate(predicates.get(i));
        }
        return result;
    }

    /**
     * Compile a single predicate.
     */
    static CompiledExpr compilePredicate(XPathParser.PredicateContext predicate) {
        if (predicate.predicateExpr() == null || predicate.predicateExpr().orExpr() == null) {
            return CompiledExpr.unsupported("empty predicate");
        }
        return compileOrExpr(predicate.predicateExpr().orExpr());
    }

    /**
     * Compile an OR expression (supports: expr or expr or ...)
     */
    static CompiledExpr compileOrExpr(XPathParser.OrExprContext orExpr) {
        List<XPathParser.AndExprContext> andExprs = orExpr.andExpr();
        if (andExprs.isEmpty()) {
            return CompiledExpr.unsupported("empty or expression");
        }
        CompiledExpr result = compileAndExpr(andExprs.get(0));
        for (int i = 1; i < andExprs.size(); i++) {
            result = CompiledExpr.or(result, compileAndExpr(andExprs.get(i)));
        }
        return result;
    }

    /**
     * Compile an AND expression (supports: expr and expr and ...)
     */
    static CompiledExpr compileAndExpr(XPathParser.AndExprContext andExpr) {
        List<XPathParser.PrimaryExprContext> primaries = andExpr.primaryExpr();
        if (primaries.isEmpty()) {
            return CompiledExpr.unsupported("empty and expression");
        }
        CompiledExpr result = compilePrimaryExpr(primaries.get(0));
        for (int i = 1; i < primaries.size(); i++) {
            result = CompiledExpr.and(result, compilePrimaryExpr(primaries.get(i)));
        }
        return result;
    }

    /**
     * Compile a primary expression (predicateValue with optional comparison).
     */
    static CompiledExpr compilePrimaryExpr(XPathParser.PrimaryExprContext primary) {
        if (primary.predicateValue() == null) {
            return CompiledExpr.unsupported("missing predicate value");
        }

        CompiledExpr left = compilePredicateValue(primary.predicateValue());

        // Check for comparison
        if (primary.comparisonOp() != null && primary.comparand() != null) {
            ComparisonOp op = compileComparisonOp(primary.comparisonOp());
            CompiledExpr right = compileComparand(primary.comparand());
            return CompiledExpr.comparison(left, op, right);
        }

        return left;
    }

    /**
     * Compile a predicate value (function, attribute, child, number).
     */
    static CompiledExpr compilePredicateValue(XPathParser.PredicateValueContext pv) {
        // Numeric predicate [1], [2], etc.
        if (pv.NUMBER() != null) {
            try {
                int value = Integer.parseInt(pv.NUMBER().getText());
                return CompiledExpr.numeric(value);
            } catch (NumberFormatException e) {
                return CompiledExpr.unsupported("invalid number: " + pv.NUMBER().getText());
            }
        }

        // Function call: position(), last(), local-name(), contains(), etc.
        if (pv.functionCall() != null) {
            return compileFunctionCall(pv.functionCall());
        }

        // Attribute: @foo, @*
        if (pv.attributeStep() != null) {
            return CompiledExpr.attribute(getAttributeStepName(pv.attributeStep()));
        }

        // Child element test: foo, *
        if (pv.childElementTest() != null) {
            return CompiledExpr.child(getChildElementTestName(pv.childElementTest()));
        }

        // Relative path (e.g., bar/baz/text())
        if (pv.relativeLocationPath() != null) {
            return compileRelativePath(pv.relativeLocationPath());
        }

        return CompiledExpr.unsupported("unknown predicate value");
    }

    /**
     * Compile a relative path in a predicate (e.g., bar/baz/text()).
     */
    static CompiledExpr compileRelativePath(XPathParser.RelativeLocationPathContext relPath) {
        List<XPathParser.StepContext> steps = relPath.step();
        if (steps.isEmpty()) {
            return CompiledExpr.unsupported("empty path");
        }

        // Check if last step is a node type test (like text())
        XPathParser.StepContext lastStep = steps.get(steps.size() - 1);
        FunctionType terminalFunction = null;
        int elementStepCount = steps.size();

        if (lastStep.nodeTypeTest() != null) {
            String funcName = lastStep.nodeTypeTest().NCNAME().getText();
            terminalFunction = getFunctionType(funcName);
            elementStepCount = steps.size() - 1;
        }

        // Single element step with no terminal function -> CHILD
        if (elementStepCount == 1 && terminalFunction == null && steps.get(0).nodeTest() != null) {
            return CompiledExpr.child(steps.get(0).nodeTest().getText());
        }

        // Build array of child expressions for each element step
        CompiledExpr[] pathSteps = new CompiledExpr[elementStepCount];
        for (int i = 0; i < elementStepCount; i++) {
            XPathParser.StepContext step = steps.get(i);
            if (step.nodeTest() != null) {
                pathSteps[i] = CompiledExpr.child(step.nodeTest().getText());
            } else {
                // Can't handle complex steps in path
                return CompiledExpr.unsupported("complex step in path");
            }
        }

        return CompiledExpr.path(pathSteps, terminalFunction);
    }

    /**
     * Compile a function call.
     */
    static CompiledExpr compileFunctionCall(XPathParser.FunctionCallContext fc) {
        // Built-in function tokens
        if (fc.LOCAL_NAME() != null) {
            return CompiledExpr.function(FunctionType.LOCAL_NAME);
        }
        if (fc.NAMESPACE_URI() != null) {
            return CompiledExpr.function(FunctionType.NAMESPACE_URI);
        }

        // Named function (NCNAME)
        if (fc.NCNAME() != null) {
            String funcName = fc.NCNAME().getText();
            FunctionType type = getFunctionType(funcName);

            // Compile arguments if present
            CompiledExpr[] args = EMPTY_PREDICATES;
            if (fc.functionArgs() != null) {
                List<XPathParser.FunctionArgContext> argCtxs = fc.functionArgs().functionArg();
                args = new CompiledExpr[argCtxs.size()];
                for (int i = 0; i < argCtxs.size(); i++) {
                    args[i] = compileFunctionArg(argCtxs.get(i));
                }
            }

            return CompiledExpr.function(type, args);
        }

        return CompiledExpr.unsupported("unknown function");
    }

    /**
     * Compile a function argument.
     */
    static CompiledExpr compileFunctionArg(XPathParser.FunctionArgContext arg) {
        if (arg.stringLiteral() != null) {
            return CompiledExpr.string(stripQuotes(arg.stringLiteral().getText()));
        }
        if (arg.NUMBER() != null) {
            try {
                return CompiledExpr.numeric(Integer.parseInt(arg.NUMBER().getText()));
            } catch (NumberFormatException e) {
                return CompiledExpr.unsupported("invalid number");
            }
        }
        if (arg.functionCall() != null) {
            return compileFunctionCall(arg.functionCall());
        }
        // Path arguments (for contains(path, 'str'))
        if (arg.relativeLocationPath() != null) {
            List<XPathParser.StepContext> steps = arg.relativeLocationPath().step();
            if (steps.size() == 1 && steps.get(0).nodeTest() != null) {
                return CompiledExpr.child(steps.get(0).nodeTest().getText());
            }
            return CompiledExpr.unsupported("complex path argument");
        }
        if (arg.absoluteLocationPath() != null) {
            return CompiledExpr.absolutePath(arg.absoluteLocationPath().getText());
        }
        return CompiledExpr.unsupported("unknown argument type");
    }

    /**
     * Compile a comparand (right side of comparison).
     */
    static CompiledExpr compileComparand(XPathParser.ComparandContext comparand) {
        if (comparand.stringLiteral() != null) {
            return CompiledExpr.string(stripQuotes(comparand.stringLiteral().getText()));
        }
        if (comparand.NUMBER() != null) {
            try {
                return CompiledExpr.numeric(Integer.parseInt(comparand.NUMBER().getText()));
            } catch (NumberFormatException e) {
                return CompiledExpr.unsupported("invalid number");
            }
        }
        return CompiledExpr.unsupported("unknown comparand");
    }

    /**
     * Compile a comparison operator.
     */
    static ComparisonOp compileComparisonOp(XPathParser.ComparisonOpContext op) {
        if (op.EQUALS() != null) return ComparisonOp.EQ;
        if (op.NOT_EQUALS() != null) return ComparisonOp.NE;
        if (op.LT() != null) return ComparisonOp.LT;
        if (op.LTE() != null) return ComparisonOp.LE;
        if (op.GT() != null) return ComparisonOp.GT;
        if (op.GTE() != null) return ComparisonOp.GE;
        return ComparisonOp.EQ; // default
    }

    /**
     * Map function name to FunctionType.
     */
    static FunctionType getFunctionType(String name) {
        switch (name) {
            case "position": return FunctionType.POSITION;
            case "last": return FunctionType.LAST;
            case "local-name": return FunctionType.LOCAL_NAME;
            case "namespace-uri": return FunctionType.NAMESPACE_URI;
            case "contains": return FunctionType.CONTAINS;
            case "starts-with": return FunctionType.STARTS_WITH;
            case "ends-with": return FunctionType.ENDS_WITH;
            case "string-length": return FunctionType.STRING_LENGTH;
            case "substring-before": return FunctionType.SUBSTRING_BEFORE;
            case "substring-after": return FunctionType.SUBSTRING_AFTER;
            case "count": return FunctionType.COUNT;
            case "text": return FunctionType.TEXT;
            case "not": return FunctionType.NOT;
            default: return FunctionType.UNKNOWN;
        }
    }

    /**
     * Strip quotes from string literal.
     */
    static String stripQuotes(String s) {
        if (s.length() < 2) return s;
        char first = s.charAt(0);
        if ((first == '\'' || first == '"') && s.charAt(s.length() - 1) == first) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * Compiled XPath expression - holds all pre-compiled information.
     * No ANTLR references - fully compiled for efficient matching.
     */
    public static final class CompiledXPath {
        final CompiledStep[] steps;
        final int flags;
        final int elementStepCount;
        final int exprType;

        // For EXPR_BOOLEAN: compiled boolean expression
        final @Nullable CompiledExpr booleanExpr;

        // For EXPR_FILTER: compiled filter expression
        final @Nullable CompiledFilterExpr filterExpr;

        CompiledXPath(CompiledStep[] steps,
                      int flags,
                      int elementStepCount,
                      int exprType,
                      @Nullable CompiledExpr booleanExpr,
                      @Nullable CompiledFilterExpr filterExpr) {
            this.steps = steps;
            this.flags = flags;
            this.elementStepCount = elementStepCount;
            this.exprType = exprType;
            this.booleanExpr = booleanExpr;
            this.filterExpr = filterExpr;
        }

        public boolean isPathExpression() {
            return exprType == EXPR_PATH;
        }

        public boolean isBooleanExpression() {
            return exprType == EXPR_BOOLEAN;
        }

        public boolean isFilterExpression() {
            return exprType == EXPR_FILTER;
        }

        public boolean hasAbsolutePath() {
            return (flags & FLAG_ABSOLUTE_PATH) != 0;
        }

        public boolean hasDescendantOrSelf() {
            return (flags & FLAG_DESCENDANT_OR_SELF) != 0;
        }

        public boolean hasDescendant() {
            return (flags & FLAG_HAS_DESCENDANT) != 0;
        }
    }

    /**
     * Compiled filter expression: (/path/expr)[predicate]/trailing
     */
    public static final class CompiledFilterExpr {
        final String pathExpr;                    // The path inside parentheses
        final CompiledExpr[] predicates;          // Predicates to apply
        final @Nullable String trailingPath;      // Optional trailing path after predicates
        final boolean trailingIsDescendant;       // Is trailing path preceded by //?

        CompiledFilterExpr(String pathExpr, CompiledExpr[] predicates,
                           @Nullable String trailingPath, boolean trailingIsDescendant) {
            this.pathExpr = pathExpr;
            this.predicates = predicates;
            this.trailingPath = trailingPath;
            this.trailingIsDescendant = trailingIsDescendant;
        }
    }

    /**
     * Step types for compiled steps - avoids ANTLR tree navigation during matching.
     */
    public enum StepType {
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
    public enum AxisType {
        PARENT,
        SELF,
        CHILD,
        OTHER  // Unsupported axis
    }

    /**
     * Node type test types.
     */
    public enum NodeTypeTestType {
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
    public static final class CompiledStep {
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

        // Compiled predicates - no ANTLR references needed during matching
        final CompiledExpr[] predicates;

        // Step flags (bitmask)
        private static final int STEP_FLAG_NEEDS_POSITION = 1;
        private static final int STEP_FLAG_NEXT_IS_BACKTRACK = 1 << 1;

        int flags;

        private CompiledStep(StepType type, boolean isDescendant, @Nullable String name,
                             AxisType axisType, NodeTypeTestType nodeTypeTestType,
                             CompiledExpr[] predicates, int flags) {
            this.type = type;
            this.isDescendant = isDescendant;
            this.name = name;
            this.axisType = axisType;
            this.nodeTypeTestType = nodeTypeTestType;
            this.predicates = predicates;
            this.flags = flags;
        }

        public StepType getType() {
            return type;
        }

        public boolean isDescendant() {
            return isDescendant;
        }

        public @Nullable String getName() {
            return name;
        }

        public AxisType getAxisType() {
            return axisType;
        }

        public NodeTypeTestType getNodeTypeTestType() {
            return nodeTypeTestType;
        }

        public CompiledExpr[] getPredicates() {
            return predicates;
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
            // Compile predicates into expressions
            CompiledExpr[] predicates = compilePredicates(step.predicate());

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
                String nodeTestName = getNodeTestName(axisStep.nodeTest());

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
                String attrName = getAttributeStepName(step.attributeStep());
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
         * Check if any compiled predicate needs position/size info.
         */
        private static boolean predicatesNeedPosition(CompiledExpr[] predicates) {
            for (CompiledExpr pred : predicates) {
                if (pred.needsPosition()) {
                    return true;
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

        /**
         * Create a new step with an additional predicate appended.
         */
        CompiledStep withAdditionalPredicate(CompiledExpr predicate) {
            CompiledExpr[] newPredicates = new CompiledExpr[predicates.length + 1];
            System.arraycopy(predicates, 0, newPredicates, 0, predicates.length);
            newPredicates[predicates.length] = predicate;
            int newFlags = flags;
            if (predicate.needsPosition()) {
                newFlags |= STEP_FLAG_NEEDS_POSITION;
            }
            return new CompiledStep(type, isDescendant, name, axisType, nodeTypeTestType, newPredicates, newFlags);
        }
    }

    // ==================== Compiled Expression Types ====================

    /**
     * Comparison operators for predicate expressions.
     */
    public enum ComparisonOp {
        EQ,   // =
        NE,   // !=
        LT,   // <
        LE,   // <=
        GT,   // >
        GE    // >=
    }

    /**
     * Function call types we support.
     */
    public enum FunctionType {
        POSITION,           // position()
        LAST,               // last()
        LOCAL_NAME,         // local-name()
        NAMESPACE_URI,      // namespace-uri()
        CONTAINS,           // contains(str, substr)
        STARTS_WITH,        // starts-with(str, prefix)
        ENDS_WITH,          // ends-with(str, suffix)
        STRING_LENGTH,      // string-length(str)
        SUBSTRING_BEFORE,   // substring-before(str, delim)
        SUBSTRING_AFTER,    // substring-after(str, delim)
        COUNT,              // count(nodeset)
        TEXT,               // text()
        NOT,                // not(expr)
        UNKNOWN             // unsupported function
    }

    /**
     * Expression types for compiled predicate expressions.
     */
    public enum ExprType {
        NUMERIC,        // [1], [2], etc. - position check
        STRING,         // 'value'
        COMPARISON,     // left op right
        AND,            // expr1 and expr2
        OR,             // expr1 or expr2
        FUNCTION,       // position(), local-name(), contains(), etc.
        ATTRIBUTE,      // @name or @*
        CHILD,          // childName or * (in predicates)
        PATH,           // Multi-step relative path (e.g., bar/baz/text())
        ABSOLUTE_PATH,  // Absolute path like /root/element1
        BOOLEAN,        // true/false
        // UNSUPPORTED was removed - now throws at compile time
    }

    /**
     * Compiled predicate expression - fully compiled, no ANTLR references.
     * Uses a discriminated union pattern for Java 8 compatibility.
     */
    public static final class CompiledExpr {
        final ExprType type;

        // For NUMERIC
        final int numericValue;

        // For STRING
        final @Nullable String stringValue;

        // For COMPARISON
        final @Nullable CompiledExpr left;
        final @Nullable ComparisonOp op;
        final @Nullable CompiledExpr right;

        // For FUNCTION
        final @Nullable FunctionType functionType;
        final CompiledExpr @Nullable [] args;

        // For ATTRIBUTE, CHILD
        final @Nullable String name;

        // For BOOLEAN
        final boolean booleanValue;

        // Private constructor - use factory methods
        private CompiledExpr(ExprType type, int numericValue, @Nullable String stringValue,
                             @Nullable CompiledExpr left, @Nullable ComparisonOp op, @Nullable CompiledExpr right,
                             @Nullable FunctionType functionType, CompiledExpr @Nullable [] args,
                             @Nullable String name, boolean booleanValue) {
            this.type = type;
            this.numericValue = numericValue;
            this.stringValue = stringValue;
            this.left = left;
            this.op = op;
            this.right = right;
            this.functionType = functionType;
            this.args = args;
            this.name = name;
            this.booleanValue = booleanValue;
        }

        // Factory methods
        public static CompiledExpr numeric(int value) {
            return new CompiledExpr(ExprType.NUMERIC, value, null, null, null, null, null, null, null, false);
        }

        public static CompiledExpr string(String value) {
            return new CompiledExpr(ExprType.STRING, 0, value, null, null, null, null, null, null, false);
        }

        public static CompiledExpr comparison(CompiledExpr left, ComparisonOp op, CompiledExpr right) {
            return new CompiledExpr(ExprType.COMPARISON, 0, null, left, op, right, null, null, null, false);
        }

        public static CompiledExpr and(CompiledExpr left, CompiledExpr right) {
            return new CompiledExpr(ExprType.AND, 0, null, left, null, right, null, null, null, false);
        }

        public static CompiledExpr or(CompiledExpr left, CompiledExpr right) {
            return new CompiledExpr(ExprType.OR, 0, null, left, null, right, null, null, null, false);
        }

        public static CompiledExpr function(FunctionType type, CompiledExpr... args) {
            return new CompiledExpr(ExprType.FUNCTION, 0, null, null, null, null, type, args, null, false);
        }

        public static CompiledExpr attribute(@Nullable String name) {
            return new CompiledExpr(ExprType.ATTRIBUTE, 0, null, null, null, null, null, null, name, false);
        }

        public static CompiledExpr child(@Nullable String name) {
            return new CompiledExpr(ExprType.CHILD, 0, null, null, null, null, null, null, name, false);
        }

        /**
         * Create a PATH expression for multi-step relative paths.
         * @param steps Array of CHILD expressions representing each step
         * @param terminalFunction Optional function at the end (e.g., text())
         */
        public static CompiledExpr path(CompiledExpr[] steps, @Nullable FunctionType terminalFunction) {
            return new CompiledExpr(ExprType.PATH, 0, null, null, null, null, terminalFunction, steps, null, false);
        }

        public static CompiledExpr bool(boolean value) {
            return new CompiledExpr(ExprType.BOOLEAN, 0, null, null, null, null, null, null, null, value);
        }

        /**
         * Create an ABSOLUTE_PATH expression for absolute path arguments.
         * The path string is stored in stringValue.
         */
        public static CompiledExpr absolutePath(String pathExpr) {
            return new CompiledExpr(ExprType.ABSOLUTE_PATH, 0, pathExpr, null, null, null, null, null, null, false);
        }

        /**
         * Throws an exception for unsupported XPath constructs.
         * Called at compile time to fail fast with a descriptive message.
         */
        public static CompiledExpr unsupported(String description) {
            throw new UnsupportedOperationException("Unsupported XPath expression: " + description);
        }

        /**
         * Check if this expression needs position/size context to evaluate.
         */
        public boolean needsPosition() {
            switch (type) {
                case NUMERIC:
                    return true;
                case FUNCTION:
                    return functionType == FunctionType.POSITION || functionType == FunctionType.LAST;
                case COMPARISON:
                case AND:
                case OR:
                    return (left != null && left.needsPosition()) || (right != null && right.needsPosition());
                default:
                    return false;
            }
        }

        /**
         * Check if this is a wildcard attribute or child expression.
         */
        public boolean isWildcard() {
            return (type == ExprType.ATTRIBUTE || type == ExprType.CHILD) &&
                   (name == null || "*".equals(name));
        }

        public ExprType getType() {
            return type;
        }
    }
}
