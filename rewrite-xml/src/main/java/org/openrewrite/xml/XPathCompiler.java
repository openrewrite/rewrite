/*
 * Copyright 2025 the original author or authors.
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.jspecify.annotations.Nullable;
import org.openrewrite.xml.internal.ThrowingErrorListener;
import org.openrewrite.xml.internal.grammar.XPathLexer;
import org.openrewrite.xml.internal.grammar.XPathParser;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Parses and compiles XPath expressions into an optimized representation
 * for efficient matching against XML cursor positions.
 */
final class XPathCompiler {

    private static final Cache<String, CompiledXPath> CACHE = Caffeine.newBuilder()
            .maximumSize(256)
            .build();

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
     * Results are cached to avoid repeated parsing of the same expression.
     *
     * @param expression the XPath expression to compile
     * @return the compiled XPath representation
     */
    public static CompiledXPath compile(String expression) {
        return requireNonNull(CACHE.get(expression, XPathCompiler::compileInternal));
    }

    private static CompiledXPath compileInternal(String expression) {
        ThrowingErrorListener errorListener = new ThrowingErrorListener(expression);
        XPathLexer lexer = new XPathLexer(CharStreams.fromString(expression));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        XPathParser parser = new XPathParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return compileXPathExpression(parser.xpathExpression());
    }

    /**
     * Compile an XPath expression from the parse tree.
     * Determines whether this is a path expression, boolean expression, or filter expression.
     */
    private static CompiledXPath compileXPathExpression(XPathParser.XpathExpressionContext ctx) {
        // xpathExpression : expr
        // expr : orExpr
        XPathParser.ExprContext exprCtx = ctx.expr();
        if (exprCtx == null || exprCtx.orExpr() == null) {
            return new CompiledXPath(EMPTY_STEPS, 0, EXPR_PATH, null, null);
        }

        // Try to extract a simple path expression
        XPathParser.OrExprContext orExpr = exprCtx.orExpr();

        // Check if this is a simple path (single andExpr with no OR)
        if (orExpr.andExpr().size() == 1) {
            XPathParser.AndExprContext andExpr = orExpr.andExpr(0);

            // Check if this is a simple path (single equalityExpr with no AND)
            if (andExpr.equalityExpr().size() == 1) {
                XPathParser.EqualityExprContext eqExpr = andExpr.equalityExpr(0);

                // Check if this is a simple path (single relationalExpr with no comparison)
                if (eqExpr.relationalExpr().size() == 1) {
                    XPathParser.RelationalExprContext relExpr = eqExpr.relationalExpr(0);

                    // Check if this is a simple path (single unaryExpr with no relational op)
                    if (relExpr.unaryExpr().size() == 1) {
                        XPathParser.PathExprContext pathExpr = relExpr.unaryExpr(0).unionExpr().pathExpr();

                        // If it's a locationPath, compile as a path expression
                        if (pathExpr.locationPath() != null) {
                            return compileLocationPath(pathExpr.locationPath());
                        }

                        // If it's a filter expression (function call, bracketed, or literal)
                        if (pathExpr.functionCallExpr() != null ||
                            pathExpr.bracketedExpr() != null ||
                            pathExpr.literalOrNumber() != null) {
                            return compileFilterExprAsXPath(pathExpr);
                        }
                    }
                }
            }
        }

        // Otherwise it's a boolean expression
        CompiledExpr boolExpr = compileOrExpr(orExpr);
        return new CompiledXPath(EMPTY_STEPS, 0, EXPR_BOOLEAN, boolExpr, null);
    }

    /**
     * Compile a location path (absolute or relative) into a path expression.
     */
    private static CompiledXPath compileLocationPath(XPathParser.LocationPathContext locationPath) {
        XPathParser.RelativeLocationPathContext relPath = null;
        int flags = 0;

        if (locationPath.absoluteLocationPath() != null) {
            XPathParser.AbsoluteLocationPathContext absCtx = locationPath.absoluteLocationPath();
            if (absCtx.DOUBLE_SLASH() != null) {
                flags |= FLAG_DESCENDANT_OR_SELF;
            } else {
                flags |= FLAG_ABSOLUTE_PATH;
            }
            relPath = absCtx.relativeLocationPath();
        } else if (locationPath.relativeLocationPath() != null) {
            relPath = locationPath.relativeLocationPath();
        }

        CompiledStep[] compiledSteps = EMPTY_STEPS;

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
                }

                // Set nextIsBacktrack for lookahead optimization
                if (i + 1 < compiledSteps.length && compiledSteps[i + 1].isBacktrack()) {
                    s.setNextIsBacktrack();
                }
            }
        }

        // Normalize mid-path parent steps (.. or parent::) to existence predicates
        compiledSteps = normalizeParentSteps(compiledSteps);

        // Recompute flags after normalization
        flags = recomputeFlags(compiledSteps, flags);

        return new CompiledXPath(compiledSteps, flags, EXPR_PATH, null, null);
    }

    /**
     * Compile a filter expression (parenthesized path with predicates) as XPath.
     */
    private static CompiledXPath compileFilterExprAsXPath(XPathParser.PathExprContext pathExpr) {
        // Check for bracketed expression like (/path/expr)[predicate]
        if (pathExpr.bracketedExpr() != null) {
            XPathParser.BracketedExprContext bracketed = pathExpr.bracketedExpr();

            // Get the inner expression from parentheses
            if (bracketed.LPAREN() != null && bracketed.expr() != null) {
                String innerPath = bracketed.expr().getText();

                // Compile predicates
                CompiledExpr[] predicates = compilePredicates(bracketed.predicate());

                // Check for trailing path
                String trailingPath = null;
                boolean trailingIsDescendant = false;
                if (pathExpr.pathSeparator() != null && pathExpr.relativeLocationPath() != null) {
                    trailingPath = pathExpr.relativeLocationPath().getText();
                    trailingIsDescendant = pathExpr.pathSeparator().DOUBLE_SLASH() != null;
                }

                CompiledFilterExpr compiled = new CompiledFilterExpr(innerPath, predicates, trailingPath, trailingIsDescendant);
                return new CompiledXPath(EMPTY_STEPS, 0, EXPR_FILTER, null, compiled);
            }
        }

        // Not a parenthesized expression - treat as boolean expression
        CompiledExpr boolExpr = compilePathExpr(pathExpr);
        return new CompiledXPath(EMPTY_STEPS, 0, EXPR_BOOLEAN, boolExpr, null);
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
        ArrayList<CompiledStep> result = new ArrayList<>();
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
     * Get name from name test (handles QNAME, NCNAME, or WILDCARD).
     */
    static String getNameTestName(XPathParser.@Nullable NameTestContext nameTest) {
        if (nameTest == null) {
            return "*";
        }
        if (nameTest.WILDCARD() != null) {
            return "*";
        }
        return nameTest.getText();
    }

    /**
     * Get name from node test (handles nameTest or nodeType()).
     */
    static String getNodeTestName(XPathParser.@Nullable NodeTestContext nodeTest) {
        if (nodeTest == null) {
            return "*";
        }
        if (nodeTest.nameTest() != null) {
            return getNameTestName(nodeTest.nameTest());
        }
        // nodeType LPAREN RPAREN - return the type name for node type tests
        if (nodeTest.nodeType() != null) {
            return nodeTest.nodeType().getText();
        }
        return "*";
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
        if (predicate.predicateExpr() == null || predicate.predicateExpr().expr() == null) {
            return CompiledExpr.unsupported("empty predicate");
        }
        return compileExpr(predicate.predicateExpr().expr());
    }

    /**
     * Compile an expression (top-level entry point for expression compilation).
     */
    static CompiledExpr compileExpr(XPathParser.ExprContext expr) {
        if (expr.orExpr() == null) {
            return CompiledExpr.unsupported("empty expression");
        }
        return compileOrExpr(expr.orExpr());
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
        List<XPathParser.EqualityExprContext> eqExprs = andExpr.equalityExpr();
        if (eqExprs.isEmpty()) {
            return CompiledExpr.unsupported("empty and expression");
        }
        CompiledExpr result = compileEqualityExpr(eqExprs.get(0));
        for (int i = 1; i < eqExprs.size(); i++) {
            result = CompiledExpr.and(result, compileEqualityExpr(eqExprs.get(i)));
        }
        return result;
    }

    /**
     * Compile an equality expression (supports: expr = expr, expr != expr)
     */
    static CompiledExpr compileEqualityExpr(XPathParser.EqualityExprContext eqExpr) {
        List<XPathParser.RelationalExprContext> relExprs = eqExpr.relationalExpr();
        if (relExprs.isEmpty()) {
            return CompiledExpr.unsupported("empty equality expression");
        }

        CompiledExpr result = compileRelationalExpr(relExprs.get(0));

        // Handle chained equality operators
        for (int i = 1; i < relExprs.size(); i++) {
            // Determine the operator - tokens between relational expressions
            // In the grammar: relationalExpr ((EQUALS | NOT_EQUALS) relationalExpr)*
            // We need to look at the i-1 operator token
            Token opToken = getEqualityOperator(eqExpr, i - 1);
            ComparisonOp op = (opToken != null && opToken.getType() == XPathParser.NOT_EQUALS)
                    ? ComparisonOp.NE : ComparisonOp.EQ;

            CompiledExpr right = compileRelationalExpr(relExprs.get(i));
            result = CompiledExpr.comparison(result, op, right);
        }

        return result;
    }

    /**
     * Get the equality operator token at the given index.
     */
    private static @Nullable Token getEqualityOperator(XPathParser.EqualityExprContext ctx, int index) {
        // EQUALS and NOT_EQUALS tokens appear between relational expressions
        int tokenIndex = 0;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof org.antlr.v4.runtime.tree.TerminalNode) {
                org.antlr.v4.runtime.tree.TerminalNode term = (org.antlr.v4.runtime.tree.TerminalNode) ctx.getChild(i);
                int type = term.getSymbol().getType();
                if (type == XPathParser.EQUALS || type == XPathParser.NOT_EQUALS) {
                    if (tokenIndex == index) {
                        return term.getSymbol();
                    }
                    tokenIndex++;
                }
            }
        }
        return null;
    }

    /**
     * Compile a relational expression (supports: expr < expr, expr > expr, etc.)
     */
    static CompiledExpr compileRelationalExpr(XPathParser.RelationalExprContext relExpr) {
        List<XPathParser.UnaryExprContext> unaryExprs = relExpr.unaryExpr();
        if (unaryExprs.isEmpty()) {
            return CompiledExpr.unsupported("empty relational expression");
        }

        CompiledExpr result = compileUnaryExpr(unaryExprs.get(0));

        // Handle chained relational operators
        for (int i = 1; i < unaryExprs.size(); i++) {
            Token opToken = getRelationalOperator(relExpr, i - 1);
            ComparisonOp op = ComparisonOp.EQ; // default
            if (opToken != null) {
                switch (opToken.getType()) {
                    case XPathParser.LT:
                        op = ComparisonOp.LT;
                        break;
                    case XPathParser.GT:
                        op = ComparisonOp.GT;
                        break;
                    case XPathParser.LTE:
                        op = ComparisonOp.LE;
                        break;
                    case XPathParser.GTE:
                        op = ComparisonOp.GE;
                        break;
                }
            }

            CompiledExpr right = compileUnaryExpr(unaryExprs.get(i));
            result = CompiledExpr.comparison(result, op, right);
        }

        return result;
    }

    /**
     * Get the relational operator token at the given index.
     */
    private static @Nullable Token getRelationalOperator(XPathParser.RelationalExprContext ctx, int index) {
        int tokenIndex = 0;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof org.antlr.v4.runtime.tree.TerminalNode) {
                org.antlr.v4.runtime.tree.TerminalNode term = (org.antlr.v4.runtime.tree.TerminalNode) ctx.getChild(i);
                int type = term.getSymbol().getType();
                if (type == XPathParser.LT || type == XPathParser.GT ||
                    type == XPathParser.LTE || type == XPathParser.GTE) {
                    if (tokenIndex == index) {
                        return term.getSymbol();
                    }
                    tokenIndex++;
                }
            }
        }
        return null;
    }

    /**
     * Compile a unary expression.
     */
    static CompiledExpr compileUnaryExpr(XPathParser.UnaryExprContext unaryExpr) {
        // unaryExpr : unionExpr (currently no unary minus support)
        return compileUnionExpr(unaryExpr.unionExpr());
    }

    /**
     * Compile a union expression.
     */
    static CompiledExpr compileUnionExpr(XPathParser.UnionExprContext unionExpr) {
        // unionExpr : pathExpr (currently no union | support)
        return compilePathExpr(unionExpr.pathExpr());
    }

    /**
     * Compile a path expression.
     */
    static CompiledExpr compilePathExpr(XPathParser.PathExprContext pathExpr) {
        // pathExpr : functionCallExpr (...)? | bracketedExpr (...)? | literalOrNumber (...)? | locationPath
        if (pathExpr.locationPath() != null) {
            return compileLocationPathAsExpr(pathExpr.locationPath());
        }

        // Handle filter expressions: functionCallExpr, bracketedExpr, or literalOrNumber
        CompiledExpr filterResult = null;

        if (pathExpr.functionCallExpr() != null) {
            filterResult = compileFunctionCallExpr(pathExpr.functionCallExpr());
        } else if (pathExpr.bracketedExpr() != null) {
            filterResult = compileBracketedExpr(pathExpr.bracketedExpr());
        } else if (pathExpr.literalOrNumber() != null) {
            filterResult = compileLiteralOrNumber(pathExpr.literalOrNumber());
        }

        if (filterResult != null) {
            // Handle trailing path: expr / relativeLocationPath
            if (pathExpr.pathSeparator() != null && pathExpr.relativeLocationPath() != null) {
                // This is a complex expression like func()[pred]/path
                // For now, return the filter result (trailing path handled at higher level)
                return filterResult;
            }
            return filterResult;
        }

        return CompiledExpr.unsupported("unknown path expression");
    }

    /**
     * Compile a location path as an expression (for use in predicates).
     */
    static CompiledExpr compileLocationPathAsExpr(XPathParser.LocationPathContext locationPath) {
        if (locationPath.absoluteLocationPath() != null) {
            return CompiledExpr.absolutePath(locationPath.getText());
        }
        if (locationPath.relativeLocationPath() != null) {
            return compileRelativePathAsExpr(locationPath.relativeLocationPath());
        }
        return CompiledExpr.unsupported("unknown location path");
    }

    /**
     * Compile a relative location path as an expression.
     */
    static CompiledExpr compileRelativePathAsExpr(XPathParser.RelativeLocationPathContext relPath) {
        List<XPathParser.StepContext> steps = relPath.step();
        if (steps.isEmpty()) {
            return CompiledExpr.unsupported("empty path");
        }

        // Check if last step is a node type test (like text())
        XPathParser.StepContext lastStep = steps.get(steps.size() - 1);
        FunctionType terminalFunction = null;
        int elementStepCount = steps.size();

        if (lastStep.nodeTest() != null && lastStep.nodeTest().nodeType() != null) {
            String funcName = lastStep.nodeTest().nodeType().getText();
            terminalFunction = getNodeTypeAsFunctionType(funcName);
            elementStepCount = steps.size() - 1;
        }

        // Single element step with no terminal function -> CHILD
        if (elementStepCount == 1 && terminalFunction == null) {
            XPathParser.StepContext step = steps.get(0);
            if (step.abbreviatedStep() != null) {
                if (step.abbreviatedStep().DOTDOT() != null) {
                    return CompiledExpr.parent();
                }
                if (step.abbreviatedStep().DOT() != null) {
                    return CompiledExpr.self();
                }
            }
            if (step.nodeTest() != null) {
                return CompiledExpr.child(getNodeTestName(step.nodeTest()));
            }
            if (step.attributeStep() != null) {
                return CompiledExpr.attribute(getAttributeStepName(step.attributeStep()));
            }
        }

        // Build array of child expressions for each element step
        CompiledExpr[] pathSteps = new CompiledExpr[elementStepCount];
        for (int i = 0; i < elementStepCount; i++) {
            XPathParser.StepContext step = steps.get(i);
            if (step.nodeTest() != null) {
                pathSteps[i] = CompiledExpr.child(getNodeTestName(step.nodeTest()));
            } else if (step.abbreviatedStep() != null) {
                if (step.abbreviatedStep().DOTDOT() != null) {
                    pathSteps[i] = CompiledExpr.parent();
                } else {
                    pathSteps[i] = CompiledExpr.self();
                }
            } else if (step.attributeStep() != null) {
                pathSteps[i] = CompiledExpr.attribute(getAttributeStepName(step.attributeStep()));
            } else {
                return CompiledExpr.unsupported("complex step in path");
            }
        }

        return CompiledExpr.path(pathSteps, terminalFunction);
    }

    /**
     * Map node type name to FunctionType.
     */
    private static @Nullable FunctionType getNodeTypeAsFunctionType(String name) {
        switch (name) {
            case "text":
                return FunctionType.TEXT;
            case "comment":
                return FunctionType.COMMENT;
            case "node":
                return FunctionType.NODE;
            case "processing-instruction":
                return FunctionType.PROCESSING_INSTRUCTION;
            default:
                return null;
        }
    }

    /**
     * Compile a function call expression: functionCall predicate*
     */
    static CompiledExpr compileFunctionCallExpr(XPathParser.FunctionCallExprContext fcExpr) {
        // predicates are handled at a higher level for now
        return compileFunctionCall(fcExpr.functionCall());
    }

    /**
     * Compile a bracketed expression: LPAREN expr RPAREN predicate*
     */
    static CompiledExpr compileBracketedExpr(XPathParser.BracketedExprContext bracketed) {
        if (bracketed.expr() != null) {
            return compileExpr(bracketed.expr());
        }
        return CompiledExpr.unsupported("empty bracketed expression");
    }

    /**
     * Compile a literal or number expression: literal predicate* | NUMBER predicate*
     */
    static CompiledExpr compileLiteralOrNumber(XPathParser.LiteralOrNumberContext literalOrNum) {
        if (literalOrNum.literal() != null) {
            return CompiledExpr.string(stripQuotes(literalOrNum.literal().getText()));
        }
        if (literalOrNum.NUMBER() != null) {
            try {
                int value = Integer.parseInt(literalOrNum.NUMBER().getText());
                return CompiledExpr.numeric(value);
            } catch (NumberFormatException e) {
                return CompiledExpr.unsupported("invalid number: " + literalOrNum.NUMBER().getText());
            }
        }
        return CompiledExpr.unsupported("unknown literal/number");
    }

    /**
     * Compile a function call.
     */
    static CompiledExpr compileFunctionCall(XPathParser.FunctionCallContext fc) {
        if (fc.functionName() == null) {
            return CompiledExpr.unsupported("unknown function");
        }

        String funcName = getFunctionNameText(fc.functionName());
        if (funcName == null) {
            return CompiledExpr.unsupported("unknown function");
        }
        FunctionType type = getFunctionType(funcName);

        // Compile arguments if present
        CompiledExpr[] args = EMPTY_PREDICATES;
        List<XPathParser.ArgumentContext> argCtxs = fc.argument();
        if (argCtxs != null && !argCtxs.isEmpty()) {
            args = new CompiledExpr[argCtxs.size()];
            for (int i = 0; i < argCtxs.size(); i++) {
                args[i] = compileArgument(argCtxs.get(i));
            }
        }

        return CompiledExpr.function(type, args);
    }

    /**
     * Compile a function argument: expr
     */
    static CompiledExpr compileArgument(XPathParser.ArgumentContext arg) {
        if (arg.expr() != null) {
            return compileExpr(arg.expr());
        }
        return CompiledExpr.unsupported("unknown argument type");
    }

    /**
     * Get the text of a function name from the parse tree.
     * Works for any token type in functionName rule (NCNAME, TEXT, COMMENT, NODE, PROCESSING_INSTRUCTION).
     */
    private static @Nullable String getFunctionNameText(XPathParser.FunctionNameContext fnCtx) {
        // getText() returns the text of whatever token matched the rule
        return fnCtx.getText();
    }

    /**
     * Map function name to FunctionType.
     */
    static FunctionType getFunctionType(String name) {
        switch (name) {
            case "position":
                return FunctionType.POSITION;
            case "last":
                return FunctionType.LAST;
            case "local-name":
                return FunctionType.LOCAL_NAME;
            case "namespace-uri":
                return FunctionType.NAMESPACE_URI;
            case "contains":
                return FunctionType.CONTAINS;
            case "starts-with":
                return FunctionType.STARTS_WITH;
            case "ends-with":
                return FunctionType.ENDS_WITH;
            case "string-length":
                return FunctionType.STRING_LENGTH;
            case "substring-before":
                return FunctionType.SUBSTRING_BEFORE;
            case "substring-after":
                return FunctionType.SUBSTRING_AFTER;
            case "count":
                return FunctionType.COUNT;
            case "text":
                return FunctionType.TEXT;
            case "not":
                return FunctionType.NOT;
            case "comment":
                return FunctionType.COMMENT;
            case "node":
                return FunctionType.NODE;
            case "processing-instruction":
                return FunctionType.PROCESSING_INSTRUCTION;
            default:
                throw new IllegalArgumentException("Unsupported XPath function: " + name + "()");
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
        final int exprType;

        // For EXPR_BOOLEAN: compiled boolean expression
        final @Nullable CompiledExpr booleanExpr;

        // For EXPR_FILTER: compiled filter expression
        final @Nullable CompiledFilterExpr filterExpr;

        CompiledXPath(CompiledStep[] steps,
                      int flags,
                      int exprType,
                      @Nullable CompiledExpr booleanExpr,
                      @Nullable CompiledFilterExpr filterExpr) {
            this.steps = steps;
            this.flags = flags;
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

        // Matching strategy - precomputed for optimal evaluation order
        // Strategies encode assumptions about name/predicate presence to skip runtime checks
        static final byte STRATEGY_NAME_ONLY = 0;           // Specific name, no predicates (most common)
        static final byte STRATEGY_NAME_THEN_PRED = 1;      // Specific name, then predicates
        static final byte STRATEGY_WILDCARD = 2;            // Wildcard name, no predicates (always matches)
        static final byte STRATEGY_PRED_ONLY = 3;           // Wildcard name, predicates only (name provides no selectivity)
        static final byte STRATEGY_DOT = 4;                 // . (self) - always matches current tag
        static final byte STRATEGY_NODE_TYPE = 5;           // node() type test
        static final byte STRATEGY_OTHER = 6;               // Fallback for complex cases

        final byte strategy;

        private CompiledStep(StepType type, boolean isDescendant, @Nullable String name,
                             AxisType axisType, NodeTypeTestType nodeTypeTestType,
                             CompiledExpr[] predicates, int flags, byte strategy) {
            this.type = type;
            this.isDescendant = isDescendant;
            this.name = name;
            this.axisType = axisType;
            this.nodeTypeTestType = nodeTypeTestType;
            this.predicates = predicates;
            this.flags = flags;
            this.strategy = strategy;
        }

        /**
         * Compute the optimal matching strategy based on step characteristics.
         */
        private static byte computeStrategy(StepType type, @Nullable String name, CompiledExpr[] predicates) {
            switch (type) {
                case NODE_TEST:
                    boolean isWildcard = "*".equals(name);
                    boolean hasPredicates = predicates.length > 0;
                    if (isWildcard) {
                        return hasPredicates ? STRATEGY_PRED_ONLY : STRATEGY_WILDCARD;
                    } else {
                        return hasPredicates ? STRATEGY_NAME_THEN_PRED : STRATEGY_NAME_ONLY;
                    }
                case ABBREVIATED_DOT:
                    return STRATEGY_DOT;
                case NODE_TYPE_TEST:
                    return STRATEGY_NODE_TYPE;
                default:
                    return STRATEGY_OTHER;
            }
        }

        public byte getStrategy() {
            return strategy;
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
                            AxisType.OTHER, NodeTypeTestType.UNKNOWN, predicates, flags,
                            computeStrategy(StepType.ABBREVIATED_DOTDOT, null, predicates));
                } else {
                    return new CompiledStep(StepType.ABBREVIATED_DOT, isDescendant, null,
                            AxisType.OTHER, NodeTypeTestType.UNKNOWN, predicates, flags,
                            computeStrategy(StepType.ABBREVIATED_DOT, null, predicates));
                }
            }

            // Attribute step: @attr, @*
            if (step.attributeStep() != null) {
                String attrName = getAttributeStepName(step.attributeStep());
                return new CompiledStep(StepType.ATTRIBUTE_STEP, isDescendant, attrName,
                        AxisType.OTHER, NodeTypeTestType.UNKNOWN, predicates, flags,
                        computeStrategy(StepType.ATTRIBUTE_STEP, attrName, predicates));
            }

            // Axis step with specifier: parent::node(), self::element, etc.
            // In the new grammar: step = axisSpecifier? nodeTest predicate*
            if (step.axisSpecifier() != null && step.nodeTest() != null) {
                XPathParser.AxisSpecifierContext axisSpec = step.axisSpecifier();
                String axisName = axisSpec.axisName().NCNAME().getText();
                String nodeTestName = getNodeTestName(step.nodeTest());

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
                        axisType, NodeTypeTestType.UNKNOWN, predicates, flags,
                        computeStrategy(StepType.AXIS_STEP, nodeTestName, predicates));
            }

            // Node test without axis: nameTest or nodeType()
            if (step.nodeTest() != null) {
                XPathParser.NodeTestContext nodeTest = step.nodeTest();

                // Check if it's a node type test: text(), comment(), node(), processing-instruction()
                if (nodeTest.nodeType() != null) {
                    String functionName = nodeTest.nodeType().getText();
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
                            AxisType.OTHER, testType, predicates, flags,
                            computeStrategy(StepType.NODE_TYPE_TEST, null, predicates));
                }

                // It's a name test: element name or *
                String nodeName = getNodeTestName(nodeTest);
                return new CompiledStep(StepType.NODE_TEST, isDescendant, nodeName,
                        AxisType.OTHER, NodeTypeTestType.UNKNOWN, predicates, flags,
                        computeStrategy(StepType.NODE_TEST, nodeName, predicates));
            }

            // Shouldn't reach here - return a non-matching step
            return new CompiledStep(StepType.NODE_TEST, isDescendant, null,
                    AxisType.OTHER, NodeTypeTestType.UNKNOWN, predicates, flags,
                    computeStrategy(StepType.NODE_TEST, null, predicates));
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
            // Recompute strategy since predicates changed
            return new CompiledStep(type, isDescendant, name, axisType, nodeTypeTestType, newPredicates, newFlags,
                    computeStrategy(type, name, newPredicates));
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
        COMMENT,            // comment()
        NODE,               // node()
        PROCESSING_INSTRUCTION  // processing-instruction()
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
        PARENT,         // .. (parent::node())
        SELF,           // . (self::node())
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

        public static CompiledExpr parent() {
            return new CompiledExpr(ExprType.PARENT, 0, null, null, null, null, null, null, null, false);
        }

        public static CompiledExpr self() {
            return new CompiledExpr(ExprType.SELF, 0, null, null, null, null, null, null, null, false);
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

        /**
         * Check if this expression contains any relative paths (CHILD or PATH types).
         * Relative paths should be evaluated from the cursor context, not from root.
         * ABSOLUTE_PATH expressions are not considered relative.
         */
        public boolean hasRelativePath() {
            switch (type) {
                case CHILD:
                case PATH:
                    return true;
                case COMPARISON:
                case AND:
                case OR:
                    return (left != null && left.hasRelativePath()) ||
                           (right != null && right.hasRelativePath());
                case FUNCTION:
                    if (args != null) {
                        for (CompiledExpr arg : args) {
                            if (arg.hasRelativePath()) {
                                return true;
                            }
                        }
                    }
                    return false;
                default:
                    return false;
            }
        }

        /**
         * Check if this expression contains only pure absolute paths (starting with / but not //).
         * Pure absolute paths like /foo/bar require cursor to be at root.
         * Descendant paths like //foo can match at any cursor position.
         * Returns true if the expression has at least one ABSOLUTE_PATH and all are pure absolute.
         */
        public boolean hasPureAbsolutePath() {
            switch (type) {
                case ABSOLUTE_PATH:
                    // Check if path starts with // (descendant) vs / (pure absolute)
                    return stringValue != null && stringValue.startsWith("/") && !stringValue.startsWith("//");
                case COMPARISON:
                case AND:
                case OR:
                    // Both sides must have pure absolute paths (if they have any paths)
                    boolean leftPure = left == null || !left.hasAnyAbsolutePath() || left.hasPureAbsolutePath();
                    boolean rightPure = right == null || !right.hasAnyAbsolutePath() || right.hasPureAbsolutePath();
                    boolean hasAny = (left != null && left.hasAnyAbsolutePath()) ||
                                     (right != null && right.hasAnyAbsolutePath());
                    return hasAny && leftPure && rightPure;
                case FUNCTION:
                    if (args != null) {
                        boolean anyAbsolute = false;
                        for (CompiledExpr arg : args) {
                            if (arg.hasAnyAbsolutePath()) {
                                anyAbsolute = true;
                                if (!arg.hasPureAbsolutePath()) {
                                    return false; // Has descendant path
                                }
                            }
                        }
                        return anyAbsolute;
                    }
                    return false;
                default:
                    return false;
            }
        }

        /**
         * Check if this expression contains any ABSOLUTE_PATH expressions.
         */
        private boolean hasAnyAbsolutePath() {
            switch (type) {
                case ABSOLUTE_PATH:
                    return true;
                case COMPARISON:
                case AND:
                case OR:
                    return (left != null && left.hasAnyAbsolutePath()) ||
                           (right != null && right.hasAnyAbsolutePath());
                case FUNCTION:
                    if (args != null) {
                        for (CompiledExpr arg : args) {
                            if (arg.hasAnyAbsolutePath()) {
                                return true;
                            }
                        }
                    }
                    return false;
                default:
                    return false;
            }
        }

        public ExprType getType() {
            return type;
        }
    }
}
