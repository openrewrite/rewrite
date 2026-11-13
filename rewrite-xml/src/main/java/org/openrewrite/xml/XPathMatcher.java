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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.xml.XPathCompiler.*;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static org.openrewrite.xml.XPathCompiler.FLAG_ABSOLUTE_PATH;

/**
 * Supports a limited set of XPath expressions, specifically those documented on <a
 * href="https://www.w3schools.com/xml/xpath_syntax.asp">this page</a>.
 * Additionally, supports `local-name()` and `namespace-uri()` conditions, `and`/`or` operators, chained conditions,
 * and abbreviated syntax (`.` for self, `..` for parent within a path).
 * <p>
 * Used for checking whether a visitor's cursor meets a certain XPath expression.
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class XPathMatcher {

    private static final Pattern NUMBER = Pattern.compile("-?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)");

    private final String expression;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private volatile CompiledXPath compiled;

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
        // Ensure expression is parsed and steps are compiled
        CompiledXPath xpath = compile();

        // Path expressions use bottom-up matching (consecutive parent steps
        // are normalized to predicates at compile time)
        if (xpath.isPathExpression() && xpath.steps.length > 0) {
            return matchBottomUp(cursor);
        }

        // Boolean and filter expressions use specialized evaluation
        return matchTopDown(cursor);
    }

    private CompiledXPath compile() {
        CompiledXPath result = compiled;
        //noinspection ConstantValue
        if (result == null) {
            synchronized (this) {
                result = compiled;
                if (result == null) {
                    compiled = result = XPathCompiler.compile(expression);
                }
            }
        }
        return result;
    }

    /**
     * Bottom-up matching: work backwards through compiled steps from the cursor.
     * This avoids building a path array and fails fast on mismatches.
     */
    private boolean matchBottomUp(Cursor cursor) {
        CompiledStep[] steps = compiled.steps;

        // Get current element
        Object cursorValue = cursor.getValue();

        // Match last step against current cursor position
        CompiledStep lastStep = steps[steps.length - 1];

        // Handle attribute matching
        if (lastStep.getType() == StepType.ATTRIBUTE_STEP) {
            if (!(cursorValue instanceof Xml.Attribute)) {
                return false;
            }
            Xml.Attribute attr = (Xml.Attribute) cursorValue;
            if (!matchesName(lastStep.getName(), attr.getKeyAsString())) {
                return false;
            }
            // Check predicates on attribute
            if (lastStep.getPredicates().length > 0) {
                if (!evaluateAttributePredicatesBottomUp(lastStep.getPredicates(), attr, cursor)) {
                    return false;
                }
            }
            // For attributes, continue matching from parent tag
            Cursor parentCursor = cursor.getParent();
            if (parentCursor == null || !(parentCursor.getValue() instanceof Xml.Tag)) {
                return false;
            }
            return matchRemainingStepsBottomUp(parentCursor, steps.length - 2);
        }

        // Handle node type test (text(), comment(), etc.)
        if (lastStep.getType() == StepType.NODE_TYPE_TEST) {
            return matchNodeTypeTestBottomUp(lastStep, cursor, steps.length - 2);
        }

        // Handle parent axis (parent::node()) or abbreviated parent (..) as last step
        // This means the cursor should be at a parent position, and we need to verify
        // the step before this exists as a child
        if (lastStep.getType() == StepType.ABBREVIATED_DOTDOT ||
                (lastStep.getType() == StepType.AXIS_STEP && lastStep.getAxisType() == AxisType.PARENT)) {
            return matchParentStepAsLast(lastStep, cursor, steps.length - 2);
        }

        // Handle self axis (self::node() or .) as last step
        if (lastStep.getType() == StepType.ABBREVIATED_DOT ||
                (lastStep.getType() == StepType.AXIS_STEP && lastStep.getAxisType() == AxisType.SELF)) {
            // . or self:: means current position - cursor must be a tag matching the name test (if any)
            if (!(cursorValue instanceof Xml.Tag)) {
                return false;
            }
            Xml.Tag tag = (Xml.Tag) cursorValue;
            if (lastStep.getType() == StepType.AXIS_STEP && !matchesElementName(lastStep, tag.getName())) {
                return false;
            }
            // Continue matching from current position (don't go up)
            return matchRemainingStepsBottomUp(cursor, steps.length - 2);
        }

        // For element matching, cursor must be a tag
        if (!(cursorValue instanceof Xml.Tag)) {
            return false;
        }
        Xml.Tag currentTag = (Xml.Tag) cursorValue;

        // Check last step matches current tag
        if (!matchStepAgainstTag(lastStep, currentTag, cursor)) {
            return false;
        }

        // Match remaining steps going up the cursor chain
        Cursor parentCursor = getParentTagCursor(cursor);
        return matchRemainingStepsBottomUp(parentCursor, steps.length - 2);
    }

    /**
     * Handle parent step (.. or parent::) as the last step.
     * This means we're at a parent position, and we need to verify the child exists.
     */
    private boolean matchParentStepAsLast(CompiledStep parentStep, Cursor cursor, int prevStepIdx) {
        Object cursorValue = cursor.getValue();
        if (!(cursorValue instanceof Xml.Tag)) {
            return false;
        }
        Xml.Tag currentTag = (Xml.Tag) cursorValue;

        // Check node test name for parent:: axis
        if (parentStep.type == StepType.AXIS_STEP && !matchesElementName(parentStep, currentTag.getName())) {
            return false;
        }

        // If there's a previous step, verify it exists as a child
        if (prevStepIdx >= 0) {
            CompiledStep prevStep = compiled.steps[prevStepIdx];
            // The previous step should exist as a child of current position
            if (prevStep.type == StepType.NODE_TEST && prevStep.name != null) {
                if (!hasChildWithName(currentTag, prevStep.name)) {
                    return false;
                }
            }
            // Continue matching from current position, skipping the child verification step
            return matchRemainingStepsBottomUp(cursor, prevStepIdx - 1);
        }

        return true;
    }

    /**
     * Check if a tag has a direct child with the given name.
     */
    private boolean hasChildWithName(Xml.Tag parent, String childName) {
        return findChildTag(parent, childName) != null;
    }

    /**
     * Handle .. (parent step) in the middle of a path.
     * The step at prevStepIdx should be checked as a child existence test.
     */
    private boolean matchParentStepInMiddle(@Nullable Cursor cursor, int prevStepIdx) {
        if (cursor == null || !(cursor.getValue() instanceof Xml.Tag)) {
            return false;
        }
        Xml.Tag currentTag = cursor.getValue();

        if (prevStepIdx < 0) {
            // No more steps - verify we're at root
            if ((compiled.flags & FLAG_ABSOLUTE_PATH) != 0 && !compiled.steps[0].isDescendant) {
                Cursor parentCursor = getParentTagCursor(cursor);
                return parentCursor == null || !(parentCursor.getValue() instanceof Xml.Tag);
            }
            return true;
        }

        CompiledStep prevStep = compiled.steps[prevStepIdx];

        // The previous step should exist as a child (this is the "detour" we took before going up)
        if (prevStep.type == StepType.NODE_TEST && prevStep.name != null) {
            if (!hasChildWithName(currentTag, prevStep.name)) {
                return false;
            }
            // Skip the child verification step and continue matching
            return matchRemainingStepsBottomUp(cursor, prevStepIdx - 1);
        }

        // For other step types, just continue (shouldn't normally happen)
        return matchRemainingStepsBottomUp(cursor, prevStepIdx - 1);
    }

    /**
     * Evaluate attribute predicates in bottom-up context.
     * Uses compiled expressions - no ANTLR tree traversal needed.
     */
    private boolean evaluateAttributePredicatesBottomUp(CompiledExpr[] predicates,
                                                        Xml.Attribute attr, Cursor cursor) {
        for (CompiledExpr predicate : predicates) {
            if (!evaluateExpr(predicate, null, attr, cursor, 1, 1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Match node type test in bottom-up context.
     */
    private boolean matchNodeTypeTestBottomUp(CompiledStep step, Cursor cursor, int nextStepIdx) {
        Object cursorValue = cursor.getValue();

        switch (step.nodeTypeTestType) {
            case TEXT:
                // text() can match:
                // 1. When cursor is on Xml.CharData - continue from parent tag
                // 2. When cursor is on a tag that has text content - continue from same position
                //    (text() verifies content exists, doesn't change traversal level)
                if (cursorValue instanceof Xml.CharData) {
                    Cursor parentCursor = getParentTagCursor(cursor);
                    return matchRemainingStepsBottomUp(parentCursor, nextStepIdx);
                }
                if (cursorValue instanceof Xml.Tag) {
                    Xml.Tag tag = (Xml.Tag) cursorValue;
                    if (tag.getValue().isPresent()) {
                        // Tag has text content - continue from SAME position
                        return matchRemainingStepsBottomUp(cursor, nextStepIdx);
                    }
                }
                return false;

            case COMMENT:
                if (cursorValue instanceof Xml.Comment) {
                    Cursor parentCursor = getParentTagCursor(cursor);
                    return matchRemainingStepsBottomUp(parentCursor, nextStepIdx);
                }
                return false;

            case NODE:
                // node() matches any node
                Cursor parentCursor = getParentTagCursor(cursor);
                return matchRemainingStepsBottomUp(parentCursor, nextStepIdx);

            case PROCESSING_INSTRUCTION:
                if (cursorValue instanceof Xml.ProcessingInstruction) {
                    Cursor parentCursorPi = getParentTagCursor(cursor);
                    return matchRemainingStepsBottomUp(parentCursorPi, nextStepIdx);
                }
                return false;

            default:
                return false;
        }
    }

    /**
     * Recursively match remaining steps going up the cursor chain.
     *
     * @param cursor  Current position in the document (may be null if we've reached root)
     * @param stepIdx Index of the step to match (going backwards from end)
     * @return true if all remaining steps match
     */
    private boolean matchRemainingStepsBottomUp(@Nullable Cursor cursor, int stepIdx) {
        // All steps matched - verify root condition
        if (stepIdx < 0) {
            if ((compiled.flags & FLAG_ABSOLUTE_PATH) != 0) {
                // Absolute path (starts with /) - must have reached root (no more tag ancestors)
                return cursor == null || !(cursor.getValue() instanceof Xml.Tag);
            }
            // Relative path or descendant path (starts with //) - any position is fine
            return true;
        }

        CompiledStep step = compiled.steps[stepIdx];

        // Handle special step types that don't consume parent levels normally
        switch (step.type) {
            case ABBREVIATED_DOT:
                // . means "self" - don't move up, just continue matching from same position
                return matchRemainingStepsBottomUp(cursor, stepIdx - 1);

            case ABBREVIATED_DOTDOT:
                // .. means "parent" - in bottom-up, the step BEFORE .. should be checked
                // as a child existence test (not a direct match)
                return matchParentStepInMiddle(cursor, stepIdx - 1);

            case AXIS_STEP:
                return matchAxisStepBottomUp(step, cursor, stepIdx);

            case NODE_TYPE_TEST:
                // Node type test in the middle of a path
                return matchNodeTypeStepBottomUp(step, cursor, stepIdx);

            default:
                // Regular element step (NODE_TEST)
                break;
        }

        // Check how this step relates to the step we just matched (stepIdx + 1)
        // The step at stepIdx+1 tells us the relationship via isDescendant
        CompiledStep nextStep = compiled.steps[stepIdx + 1];

        if (nextStep.isDescendant) {
            // The step we just matched can be a descendant of current step
            // So current step can be ANY ancestor - scan upward with backtracking
            Cursor pos = cursor;
            while (pos != null && pos.getValue() instanceof Xml.Tag) {
                Xml.Tag tag = pos.getValue();
                if (matchStepAgainstTag(step, tag, pos)) {
                    // Found a candidate - try to match remaining prefix
                    Cursor nextParent = getParentTagCursor(pos);
                    if (matchRemainingStepsBottomUp(nextParent, stepIdx - 1)) {
                        return true;  // This candidate worked
                    }
                    // Continue scanning for another candidate (backtracking)
                }
                pos = getParentTagCursor(pos);
            }
            return false;  // No valid candidate found
        } else {
            // Direct parent relationship - current step must be at cursor exactly
            if (cursor == null || !(cursor.getValue() instanceof Xml.Tag)) {
                return false;
            }
            Xml.Tag tag = cursor.getValue();
            if (!matchStepAgainstTag(step, tag, cursor)) {
                return false;
            }
            Cursor nextParent = getParentTagCursor(cursor);
            return matchRemainingStepsBottomUp(nextParent, stepIdx - 1);
        }
    }

    /**
     * Match axis step in bottom-up context.
     */
    private boolean matchAxisStepBottomUp(CompiledStep step, @Nullable Cursor cursor, int stepIdx) {
        switch (step.axisType) {
            case PARENT:
                // parent::node() or parent::element - similar to ..
                // Check node test name first
                if (cursor != null && cursor.getValue() instanceof Xml.Tag) {
                    Xml.Tag tag = cursor.getValue();
                    if (!matchesElementName(step, tag.getName())) {
                        return false;
                    }
                }
                // The step before parent:: should be verified as a child
                return matchParentStepInMiddle(cursor, stepIdx - 1);

            case SELF:
                // self::node() or self::element - matches current position
                // In bottom-up, we need to check the next step's cursor position
                // Actually, self:: doesn't consume a level - it's like . with a name test
                if (cursor != null && cursor.getValue() instanceof Xml.Tag) {
                    Xml.Tag tag = cursor.getValue();
                    if (!matchesElementName(step, tag.getName())) {
                        return false;
                    }
                }
                return matchRemainingStepsBottomUp(cursor, stepIdx - 1);

            case CHILD:
                // child:: is the default - same as normal element step
                if (cursor == null || !(cursor.getValue() instanceof Xml.Tag)) {
                    return false;
                }
                Xml.Tag tag = cursor.getValue();
                if (!matchStepAgainstTag(step, tag, cursor)) {
                    return false;
                }
                return matchRemainingStepsBottomUp(getParentTagCursor(cursor), stepIdx - 1);

            default:
                // Unsupported axis
                return false;
        }
    }

    /**
     * Match node type test step in bottom-up context (not as last step).
     */
    private boolean matchNodeTypeStepBottomUp(CompiledStep step, @Nullable Cursor cursor, int stepIdx) {
        // Node type tests in the middle of a path need special handling
        switch (step.nodeTypeTestType) {
            case NODE:
                // node() matches anything - don't consume extra level
                return matchRemainingStepsBottomUp(cursor, stepIdx - 1);

            case TEXT:
            case COMMENT:
            case PROCESSING_INSTRUCTION:
                // These typically don't appear in the middle of element paths
                // Fall through to default handling
            default:
                return false;
        }
    }

    /**
     * Get the parent cursor that contains a tag, skipping non-tag nodes.
     */
    private @Nullable Cursor getParentTagCursor(@Nullable Cursor cursor) {
        if (cursor == null) return null;
        Cursor parent = cursor.getParent();
        while (parent != null && !(parent.getValue() instanceof Xml.Tag)) {
            if (parent.getValue() instanceof Xml.Document) {
                return null;
            }
            parent = parent.getParent();
        }
        return parent;
    }

    /**
     * Get the parent tag from the cursor, skipping non-tag nodes.
     */
    private Xml.@Nullable Tag getParentTag(Cursor cursor) {
        Cursor parentCursor = getParentTagCursor(cursor);
        if (parentCursor != null && parentCursor.getValue() instanceof Xml.Tag) {
            return parentCursor.getValue();
        }
        return null;
    }

    /**
     * Check if a compiled step matches a tag.
     * Uses precomputed strategy for optimal evaluation order.
     */
    private boolean matchStepAgainstTag(CompiledStep step, Xml.Tag tag, Cursor cursor) {
        // Strategy-based dispatch - each strategy makes assumptions about
        // name/predicate presence to skip unnecessary runtime checks
        switch (step.strategy) {
            case CompiledStep.STRATEGY_NAME_ONLY:
                // Most common: specific element name, no predicates
                // Single string comparison, no predicate check needed
                //noinspection DataFlowIssue
                return step.name.equals(tag.getName());

            case CompiledStep.STRATEGY_NAME_THEN_PRED:
                // Specific name with predicates - check name first (selective)
                //noinspection DataFlowIssue
                if (!step.name.equals(tag.getName())) {
                    return false;
                }
                return evaluatePredicates(step, tag, cursor);

            case CompiledStep.STRATEGY_WILDCARD:
                // Wildcard without predicates - always matches any tag
                return true;

            case CompiledStep.STRATEGY_PRED_ONLY:
                // Wildcard with predicates - skip name check (provides no selectivity)
                // Go directly to predicate evaluation
                return evaluatePredicates(step, tag, cursor);

            case CompiledStep.STRATEGY_DOT:
                // . (self) - always matches current tag
                return true;

            case CompiledStep.STRATEGY_NODE_TYPE:
                // node() type test - matches tags only if it's node()
                return step.nodeTypeTestType == NodeTypeTestType.NODE;

            case CompiledStep.STRATEGY_OTHER:
            default:
                // Fallback for complex/unsupported cases
                return matchStepAgainstTagGeneric(step, tag, cursor);
        }
    }

    /**
     * Generic fallback matcher for complex step types.
     */
    private boolean matchStepAgainstTagGeneric(CompiledStep step, Xml.Tag tag, Cursor cursor) {
        switch (step.type) {
            case NODE_TEST:
                if (!matchesName(step.name, tag.getName())) {
                    return false;
                }
                break;
            case ABBREVIATED_DOT:
                break;
            case NODE_TYPE_TEST:
                if (step.nodeTypeTestType != NodeTypeTestType.NODE) {
                    return false;
                }
                break;
            case ABBREVIATED_DOTDOT:
            default:
                return false;
        }

        if (step.predicates.length > 0) {
            return evaluatePredicates(step, tag, cursor);
        }
        return true;
    }

    /**
     * Evaluate predicates against a tag.
     * Uses compiled expressions - no ANTLR tree traversal needed.
     */
    private boolean evaluatePredicates(CompiledStep step, Xml.Tag tag, Cursor cursor) {
        if (!step.needsPositionInfo()) {
            for (CompiledExpr predicate : step.predicates) {
                if (!evaluateExpr(predicate, tag, null, cursor, 1, 1)) {
                    return false;
                }
            }
            return true;
        }
        Cursor parent = cursor.getParent();
        List<Cursor> candidates = new ArrayList<>();
        if (parent != null && parent.getValue() instanceof Xml.Tag) {
            for (Xml.Tag child : ((Xml.Tag) parent.getValue()).getChildren()) {
                if (step.type == StepType.AXIS_STEP ? matchesElementName(step, child.getName()) :
                        matchesName(step.name, child.getName())) {
                    candidates.add(new Cursor(parent, child));
                }
            }
        } else {
            candidates.add(cursor);
        }
        for (Cursor candidate : filterCandidates(candidates, step.predicates)) {
            if (candidate.getValue() == tag) {
                return true;
            }
        }
        return false;
    }

    // ==================== Compiled Expression Evaluation ====================

    /**
     * Unified expression evaluation for both tag and attribute contexts.
     * Pass the relevant context (tag or attr), with the other as null.
     * This is allocation-free - no context objects are created.
     *
     * @param expr     the compiled expression to evaluate
     * @param tag      the tag context (null for attribute-only evaluation)
     * @param attr     the attribute context (null for tag-only evaluation)
     * @param cursor   the cursor position
     * @param position the 1-based position among siblings
     * @param size     the total number of siblings
     * @return true if the expression evaluates to true
     */
    @SuppressWarnings("DataFlowIssue")
    private boolean evaluateExpr(CompiledExpr expr, Xml.@Nullable Tag tag, Xml.@Nullable Attribute attr,
                                 Cursor cursor, int position, int size) {
        switch (expr.type) {
            case NUMERIC:
                // Positional predicate: [1], [2], etc.
                return position == expr.numericValue;

            case AND:
                return evaluateExpr(expr.left, tag, attr, cursor, position, size) &&
                        evaluateExpr(expr.right, tag, attr, cursor, position, size);

            case OR:
                return evaluateExpr(expr.left, tag, attr, cursor, position, size) ||
                        evaluateExpr(expr.right, tag, attr, cursor, position, size);

            case COMPARISON:
                return evaluateComparison(expr, tag, attr, cursor, position, size);

            case FUNCTION:
                return evaluateFunction(expr, tag, attr, cursor, position, size);

            case CHILD:
                // Existence check: [childName] - tag context only
                return tag != null && hasChildElement(tag, expr.name);

            case PATH:
                // Path existence check: [a/b/c] - tag context only
                return tag != null && (expr.pathSteps != null ?
                        !selectSteps(singletonList(cursor), expr.pathSteps).isEmpty() : pathExists(tag, expr));

            case ATTRIBUTE:
                // Existence check: [@attr] - tag context only
                return tag != null && hasAttribute(tag, expr.name);

            case BOOLEAN:
                return expr.booleanValue;

            default:
                return false;
        }
    }

    /**
     * Unified comparison evaluation for both tag and attribute contexts.
     */
    @SuppressWarnings("DataFlowIssue")
    private boolean evaluateComparison(CompiledExpr expr, Xml.@Nullable Tag tag, Xml.@Nullable Attribute attr,
                                       Cursor cursor, int position, int size) {
        Object leftValue = resolveValue(expr.left, tag, attr, cursor, position, size);
        Object rightValue = resolveValue(expr.right, tag, attr, cursor, position, size);

        if (leftValue == null || rightValue == null) {
            return false;
        }

        return compareValues(leftValue, rightValue, expr.op);
    }

    /**
     * Unified value resolution for both tag and attribute contexts.
     * Returns the natural type: Integer for numbers, Boolean for predicates, String for strings.
     */
    private @Nullable Object resolveValue(CompiledExpr expr, Xml.@Nullable Tag tag, Xml.@Nullable Attribute attr,
                                          Cursor cursor, int position, int size) {
        switch (expr.type) {
            case STRING:
                return expr.stringValue;

            case NUMERIC:
                return expr.numericValue;

            case CHILD:
                // Tag context only
                return tag != null ? getChildElementValue(tag, expr.name) : null;

            case ATTRIBUTE:
                // In tag context, get attribute from tag
                // In attribute context, get value from current attribute or parent tag
                if (tag != null) {
                    return getAttributeValue(tag, expr.name);
                }
                if (attr != null) {
                    if (expr.name == null || "*".equals(expr.name)) {
                        return attr.getValueAsString();
                    }
                    // For named attributes, check parent tag
                    Cursor parentCursor = cursor.getParent();
                    if (parentCursor != null && parentCursor.getValue() instanceof Xml.Tag) {
                        return getAttributeValue(parentCursor.getValue(), expr.name);
                    }
                }
                return null;

            case SELF:
                return tag != null ? stringValue(tag) : attr != null ? attr.getValueAsString() : null;

            case PATH:
                if (expr.pathSteps != null) {
                    List<Cursor> nodes = selectSteps(singletonList(cursor), expr.pathSteps);
                    return nodes.isEmpty() ? null : stringValue(nodes.get(0).getValue());
                }
                return tag != null ? resolvePathValue(expr, tag) : null;

            case ABSOLUTE_PATH:
                // Resolve path from document root
                if (expr.stringValue != null) {
                    Xml.Tag root = getRootTag(cursor);
                    if (root != null) {
                        Set<Xml.Tag> pathMatches = findTagsByPath(root, expr.stringValue);
                        if (!pathMatches.isEmpty()) {
                            return pathMatches.iterator().next().getValue().orElse("");
                        }
                    }
                }
                return "";

            case FUNCTION:
                return resolveFunctionValue(expr, tag, attr, cursor, position, size);

            default:
                return null;
        }
    }

    /**
     * Unified function evaluation as boolean for both contexts.
     */
    private boolean evaluateFunction(CompiledExpr expr, Xml.@Nullable Tag tag, Xml.@Nullable Attribute attr,
                                     Cursor cursor, int position, int size) {
        if (expr.functionType == null) {
            return false;
        }

        switch (expr.functionType) {
            case POSITION:
                return position > 0;

            case LAST:
                return position == size;

            case NOT:
                if (expr.args != null && expr.args.length > 0) {
                    return !evaluateExpr(expr.args[0], tag, attr, cursor, position, size);
                }
                return false;

            case CONTAINS:
            case STARTS_WITH:
            case ENDS_WITH:
                // These functions return Boolean - delegate to resolveFunctionValue
                Object boolResult = resolveFunctionValue(expr, tag, attr, cursor, position, size);
                return Boolean.TRUE.equals(boolResult);

            case STRING_LENGTH:
                // In boolean context, string-length is truthy if > 0
                Object lenResult = resolveFunctionValue(expr, tag, attr, cursor, position, size);
                return lenResult instanceof Number && ((Number) lenResult).intValue() > 0;

            case COUNT:
                // In boolean context, count is truthy if > 0
                Object countResult = resolveFunctionValue(expr, tag, attr, cursor, position, size);
                return countResult instanceof Number && ((Number) countResult).intValue() > 0;

            case TEXT:
                // text() as existence check - tag context only
                return tag != null && tag.getValue().isPresent() && !tag.getValue().get().trim().isEmpty();

            case LOCAL_NAME:
            case NAMESPACE_URI:
                // These return strings, not booleans - but as existence check they're truthy
                return true;

            default:
                return false;
        }
    }

    /**
     * Unified function value resolution for both contexts.
     * Returns the natural type: Integer for numeric functions, Boolean for predicates, String for string functions.
     */
    private @Nullable Object resolveFunctionValue(CompiledExpr expr, Xml.@Nullable Tag tag, Xml.@Nullable Attribute attr,
                                                  Cursor cursor, int position, int size) {
        if (expr.functionType == null) {
            return null;
        }

        switch (expr.functionType) {
            case POSITION:
                return position;

            case LAST:
                return size;

            case LOCAL_NAME: {
                // Handle arguments: local-name(..) means local-name of parent, local-name(.) means self
                Xml.Tag targetTag = tag;
                if (expr.args != null && expr.args.length > 0) {
                    CompiledExpr argExpr = expr.args[0];
                    if (argExpr.type == ExprType.PARENT) {
                        targetTag = getParentTag(cursor);
                    }
                }
                if (targetTag != null) {
                    return localName(targetTag.getName());
                }
                if (attr != null && (expr.args == null || expr.args.length == 0)) {
                    return localName(attr.getKeyAsString());
                }
                return null;
            }

            case NAMESPACE_URI: {
                // Handle arguments: namespace-uri(..) means namespace-uri of parent, namespace-uri(.) means self
                Xml.Tag targetTag = tag;
                Cursor targetCursor = cursor;
                if (expr.args != null && expr.args.length > 0) {
                    CompiledExpr argExpr = expr.args[0];
                    if (argExpr.type == ExprType.PARENT) {
                        Cursor parentCursor = getParentTagCursor(cursor);
                        if (parentCursor != null && parentCursor.getValue() instanceof Xml.Tag) {
                            targetTag = parentCursor.getValue();
                            targetCursor = parentCursor;
                        } else {
                            return null;
                        }
                    }
                }
                if (targetTag != null) {
                    return resolveNamespaceUri(targetTag, targetCursor);
                }
                if (attr != null && (expr.args == null || expr.args.length == 0)) {
                    return resolveAttributeNamespaceUri(attr, cursor);
                }
                return null;
            }

            case TEXT:
                // Tag context only
                return tag != null ? tag.getValue().orElse("") : null;

            case STRING_LENGTH:
                if (expr.args != null && expr.args.length > 0) {
                    Object val = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    String str = functionStringValue(val);
                    return str != null ? str.length() : 0;
                }
                return 0;

            case SUBSTRING_BEFORE:
                if (expr.args != null && expr.args.length >= 2) {
                    Object val0 = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    Object val1 = resolveValue(expr.args[1], tag, attr, cursor, position, size);
                    String str = functionStringValue(val0);
                    String delim = functionStringValue(val1);
                    String result = substringBefore(str, delim);
                    return result != null ? result : "";
                }
                return "";

            case SUBSTRING_AFTER:
                if (expr.args != null && expr.args.length >= 2) {
                    Object val0 = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    Object val1 = resolveValue(expr.args[1], tag, attr, cursor, position, size);
                    String str = functionStringValue(val0);
                    String delim = functionStringValue(val1);
                    String result = substringAfter(str, delim);
                    return result != null ? result : "";
                }
                return "";

            case COUNT:
                if (expr.args != null && expr.args.length > 0) {
                    CompiledExpr pathArg = expr.args[0];
                    if (pathArg.type == ExprType.ABSOLUTE_PATH && pathArg.stringValue != null) {
                        Xml.Tag root = getRootTag(cursor);
                        if (root != null) {
                            Set<Xml.Tag> matches = findTagsByPath(root, pathArg.stringValue);
                            return matches.size();
                        }
                    }
                }
                return 0;

            case CONTAINS:
                if (expr.args != null && expr.args.length >= 2) {
                    Object val0 = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    Object val1 = resolveValue(expr.args[1], tag, attr, cursor, position, size);
                    String str = functionStringValue(val0);
                    String substr = functionStringValue(val1);
                    return str != null && substr != null && str.contains(substr);
                }
                return false;

            case STARTS_WITH:
                if (expr.args != null && expr.args.length >= 2) {
                    Object val0 = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    Object val1 = resolveValue(expr.args[1], tag, attr, cursor, position, size);
                    String str = functionStringValue(val0);
                    String prefix = functionStringValue(val1);
                    return str != null && prefix != null && str.startsWith(prefix);
                }
                return false;

            case ENDS_WITH:
                if (expr.args != null && expr.args.length >= 2) {
                    Object val0 = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    Object val1 = resolveValue(expr.args[1], tag, attr, cursor, position, size);
                    String str = functionStringValue(val0);
                    String suffix = functionStringValue(val1);
                    return str != null && suffix != null && str.endsWith(suffix);
                }
                return false;

            default:
                return null;
        }
    }

    /**
     * Check if a path exists starting from a tag.
     * Used for predicates like [a/b/c] which check existence of a descendant path.
     */
    private boolean pathExists(Xml.Tag tag, CompiledExpr pathExpr) {
        if (pathExpr.args == null || pathExpr.args.length == 0) {
            return true;  // Empty path always exists
        }

        // Navigate through each step in the path
        Xml.Tag current = tag;
        for (CompiledExpr step : pathExpr.args) {
            if (step.type != ExprType.CHILD) {
                return false;  // Only CHILD steps supported in path existence check
            }
            String childName = step.name;
            Xml.Tag child = findChildTag(current, childName);
            if (child == null) {
                return false;
            }
            current = child;
        }
        return true;
    }

    /**
     * Find a child tag with the given name.
     */
    private Xml.@Nullable Tag findChildTag(Xml.Tag parent, @Nullable String name) {
        List<? extends Content> contents = parent.getContent();
        if (contents == null) {
            return null;
        }
        for (Content c : contents) {
            if (c instanceof Xml.Tag) {
                Xml.Tag child = (Xml.Tag) c;
                if (name == null || "*".equals(name) || child.getName().equals(name)) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Resolve a PATH expression value by navigating child elements.
     */
    private @Nullable String resolvePathValue(CompiledExpr expr, Xml.Tag tag) {
        if (expr.args == null || expr.args.length == 0) {
            // No path steps - get text of current element
            if (expr.functionType == FunctionType.TEXT) {
                return tag.getValue().orElse("");
            }
            return null;
        }

        // Navigate through child elements following the path
        Xml.Tag current = tag;
        for (int i = 0; i < expr.args.length; i++) {
            CompiledExpr step = expr.args[i];
            if (step.type != ExprType.CHILD) {
                return null;
            }
            Xml.Tag child = findChildTag(current, step.name);
            if (child == null) {
                return null;
            }
            current = child;
        }

        // Apply terminal function if present
        if (expr.functionType == FunctionType.TEXT) {
            return current.getValue().orElse("");
        }

        // Default: return text value of target element
        return current.getValue().orElse("");
    }

    /**
     * Compare two values using the given comparison operator.
     * Handles type-aware comparison: Number vs Number uses numeric comparison,
     * Boolean vs Boolean uses boolean comparison, otherwise uses string comparison.
     * Package-private so XPathMatcherVisitor can share this logic.
     */
    static boolean compareValues(Object left, Object right, @Nullable ComparisonOp op) {
        if (op == null) {
            return left.equals(right);
        }

        if (left instanceof Number || right instanceof Number ||
                op == ComparisonOp.LT || op == ComparisonOp.LE || op == ComparisonOp.GT || op == ComparisonOp.GE) {
            double leftNum = numberValue(left);
            double rightNum = numberValue(right);
            switch (op) {
                case EQ:
                    return leftNum == rightNum;
                case NE:
                    return leftNum != rightNum;
                case LT:
                    return leftNum < rightNum;
                case LE:
                    return leftNum <= rightNum;
                case GT:
                    return leftNum > rightNum;
                case GE:
                    return leftNum >= rightNum;
                default:
                    return false;
            }
        }

        // If both are booleans, use boolean comparison
        if (left instanceof Boolean && right instanceof Boolean) {
            switch (op) {
                case EQ:
                    return left.equals(right);
                case NE:
                    return !left.equals(right);
                default:
                    return false; // relational operators don't apply to booleans
            }
        }

        // Convert to strings for comparison
        String leftStr = left.toString();
        String rightStr = right.toString();

        switch (op) {
            case EQ:
                return leftStr.equals(rightStr);
            case NE:
                return !leftStr.equals(rightStr);
            default:
                return false;
        }
    }

    // ==================== Static String Function Helpers ====================

    private static @Nullable String substringBefore(@Nullable String str, @Nullable String delim) {
        if (str == null || delim == null) {
            return null;
        }
        int idx = str.indexOf(delim);
        return idx >= 0 ? str.substring(0, idx) : "";
    }

    private static @Nullable String substringAfter(@Nullable String str, @Nullable String delim) {
        if (str == null || delim == null) {
            return null;
        }
        int idx = str.indexOf(delim);
        return idx >= 0 ? str.substring(idx + delim.length()) : "";
    }

    private static String localName(String name) {
        int colonIdx = name.indexOf(':');
        return colonIdx >= 0 ? name.substring(colonIdx + 1) : name;
    }

    // ==================== Instance Helper Methods ====================

    /**
     * Check if tag has a child element with the given name.
     */
    private boolean hasChildElement(Xml.Tag tag, @Nullable String name) {
        return findChildTag(tag, name) != null;
    }

    /**
     * Get the text value of a child element.
     */
    private @Nullable String getChildElementValue(Xml.Tag tag, @Nullable String name) {
        Xml.Tag child = findChildTag(tag, name);
        return child != null ? child.getValue().orElse("") : null;
    }

    /**
     * Check if tag has an attribute with the given name.
     */
    private boolean hasAttribute(Xml.Tag tag, @Nullable String name) {
        List<Xml.Attribute> attrs = tag.getAttributes();
        for (Xml.Attribute attr : attrs) {
            if (name == null || "*".equals(name) || attr.getKeyAsString().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the value of an attribute.
     */
    private @Nullable String getAttributeValue(Xml.Tag tag, @Nullable String name) {
        List<Xml.Attribute> attrs = tag.getAttributes();
        for (Xml.Attribute attr : attrs) {
            if (name == null || "*".equals(name) || attr.getKeyAsString().equals(name)) {
                return attr.getValueAsString();
            }
        }
        return null;
    }

    /**
     * Resolve namespace URI for a tag.
     */
    private @Nullable String resolveNamespaceUri(Xml.Tag tag, Cursor cursor) {
        String tagName = tag.getName();
        String prefix = "";
        int colonIdx = tagName.indexOf(':');
        if (colonIdx >= 0) {
            prefix = tagName.substring(0, colonIdx);
        }
        return findNamespaceUri(prefix, cursor);
    }

    /**
     * Resolve namespace URI for an attribute.
     */
    private @Nullable String resolveAttributeNamespaceUri(Xml.Attribute attr, Cursor cursor) {
        String attrName = attr.getKeyAsString();
        int colonIdx = attrName.indexOf(':');
        if (colonIdx >= 0) {
            String prefix = attrName.substring(0, colonIdx);
            return findNamespaceUri(prefix, cursor);
        }
        return "";
    }

    /**
     * Find namespace URI for a prefix by walking up the cursor.
     */
    private @Nullable String findNamespaceUri(String prefix, Cursor cursor) {
        String nsAttr = prefix.isEmpty() ? "xmlns" : "xmlns:" + prefix;
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            if (c.getValue() instanceof Xml.Tag) {
                Xml.Tag t = c.getValue();
                for (Xml.Attribute attr : t.getAttributes()) {
                    if (attr.getKeyAsString().equals(nsAttr)) {
                        return attr.getValueAsString();
                    }
                }
            }
        }
        return prefix.isEmpty() ? "" : null;
    }

    /**
     * Handle non-path expressions (boolean expressions, filter expressions).
     * Path expressions are handled by matchBottomUp.
     */
    private boolean matchTopDown(Cursor cursor) {
        switch (compiled.exprType) {
            case XPathCompiler.EXPR_BOOLEAN:
                return matchBooleanExpr(cursor);
            case XPathCompiler.EXPR_FILTER:
                return matchFilterExpr(cursor);
            default:
                // Path expressions should go through matchBottomUp
                return false;
        }
    }

    // ==================== Boolean Expression Matching ====================

    /**
     * Match a boolean expression like contains(/root/element, 'value').
     * <p>
     * For expressions with pure absolute paths (/foo/bar): cursor must be at the root element.
     * <p>
     * For expressions with descendant paths (//foo) or relative paths (foo/bar):
     * evaluated from cursor context, matches at any position where the expression is true.
     */
    @SuppressWarnings("DataFlowIssue")
    private boolean matchBooleanExpr(Cursor cursor) {
        if (compiled.booleanExpr == null) {
            return false;
        }

        // Cursor must be at a tag
        if (!(cursor.getValue() instanceof Xml.Tag)) {
            return false;
        }
        Xml.Tag currentTag = cursor.getValue();

        // Check if expression contains relative paths or descendant paths
        // Relative paths (foo/bar) and descendant paths (//foo) can match at any cursor position
        // Only pure absolute paths (/foo/bar) require cursor to be at root
        if (compiled.booleanExpr.hasRelativePath() || !compiled.booleanExpr.hasPureAbsolutePath()) {
            // Relative or descendant paths: evaluate from cursor context at any position
            return evaluateExpr(compiled.booleanExpr, currentTag, null, cursor, 1, 1);
        }

        // Pure absolute paths only: only match at root element
        // Check if cursor is at root (parent is Document or no parent tag)
        Cursor parentCursor = cursor.getParent();
        while (parentCursor != null && !(parentCursor.getValue() instanceof Xml.Tag) &&
               !(parentCursor.getValue() instanceof Xml.Document)) {
            parentCursor = parentCursor.getParent();
        }
        if (parentCursor == null || !(parentCursor.getValue() instanceof Xml.Document)) {
            return false; // Not at root element
        }

        // Evaluate the expression from current (root) context
        return evaluateExpr(compiled.booleanExpr, currentTag, null, cursor, 1, 1);
    }

    // ==================== Filter Expression Matching ====================

    /**
     * Match a filter expression like (/root/a)[1] or (/root/a)[last()]/child.
     */
    @SuppressWarnings("DataFlowIssue")
    private boolean matchFilterExpr(Cursor cursor) {
        if (compiled.filterExpr == null) {
            return false;
        }

        Xml.Tag root = getRootTag(cursor);
        if (root == null) {
            return false;
        }

        List<Cursor> nodes = findNodesByPath(root, compiled.filterExpr.pathExpr);
        nodes = filterCandidates(nodes, compiled.filterExpr.predicates);
        if (compiled.filterExpr.trailingPath != null) {
            CompiledStep[] steps = compiled.filterExpr.trailingPath;
            if (compiled.filterExpr.trailingIsDescendant) {
                List<Cursor> contexts = new ArrayList<>();
                for (Cursor node : nodes) {
                    collectDescendants(node, contexts);
                }
                nodes = contexts;
            }
            nodes = selectSteps(nodes, steps);
        }
        for (Cursor node : nodes) {
            if (node.getValue() == cursor.getValue()) {
                return true;
            }
        }
        return false;
    }

    // ==================== Tree Traversal Helpers ====================

    /**
     * Get the root tag from a cursor by walking up to the document.
     */
    private Xml.@Nullable Tag getRootTag(Cursor cursor) {
        Cursor c = cursor;
        while (c.getParent() != null && !(c.getParent().getValue() instanceof Xml.Document)) {
            c = c.getParent();
        }
        return c.getValue() instanceof Xml.Tag ? (Xml.Tag) c.getValue() : null;
    }

    /**
     * Find tags matching a path expression starting from a root tag.
     * Supports absolute paths (/a/b), descendant paths (//a), and relative paths (a/b).
     */
    private Set<Xml.Tag> findTagsByPath(Xml.Tag startTag, String pathExpr) {
        Set<Xml.Tag> tags = new LinkedHashSet<>();
        for (Cursor node : findNodesByPath(startTag, pathExpr)) {
            if (node.getValue() instanceof Xml.Tag) {
                tags.add(node.getValue());
            }
        }
        return tags;
    }

    private List<Cursor> findNodesByPath(Xml.Tag root, String path) {
        XPathMatcher matcher = new XPathMatcher(path);
        List<Cursor> candidates = new ArrayList<>();
        collectDescendants(new Cursor(null, root), candidates);
        List<Cursor> matches = new ArrayList<>();
        for (Cursor candidate : candidates) {
            if (matcher.matches(candidate)) {
                matches.add(candidate);
            }
            if (candidate.getValue() instanceof Xml.Tag) {
                for (Xml.Attribute attribute : ((Xml.Tag) candidate.getValue()).getAttributes()) {
                    Cursor attributeCursor = new Cursor(candidate, attribute);
                    if (matcher.matches(attributeCursor)) {
                        matches.add(attributeCursor);
                    }
                }
            }
        }
        return matches;
    }

    private void collectDescendants(Cursor cursor, List<Cursor> result) {
        result.add(cursor);
        if (cursor.getValue() instanceof Xml.Tag) {
            for (Xml.Tag child : ((Xml.Tag) cursor.getValue()).getChildren()) {
                collectDescendants(new Cursor(cursor, child), result);
            }
        }
    }

    private List<Cursor> selectSteps(List<Cursor> contexts, CompiledStep[] steps) {
        for (CompiledStep step : steps) {
            List<Cursor> next = new ArrayList<>();
            for (Cursor context : contexts) {
                List<Cursor> origins = new ArrayList<>();
                if (step.isDescendant) {
                    collectDescendants(context, origins);
                } else {
                    origins.add(context);
                }
                for (Cursor origin : origins) {
                    List<Cursor> candidates = new ArrayList<>();
                    Object value = origin.getValue();
                    if (step.type == StepType.ABBREVIATED_DOT ||
                            step.type == StepType.AXIS_STEP && step.axisType == AxisType.SELF) {
                        if (step.type == StepType.ABBREVIATED_DOT || matchesNodeTest(step, value)) {
                            candidates.add(origin);
                        }
                    } else if (step.type == StepType.ABBREVIATED_DOTDOT ||
                            step.type == StepType.AXIS_STEP && step.axisType == AxisType.PARENT) {
                        Cursor parent = getParentTagCursor(origin);
                        if (parent != null && (step.type == StepType.ABBREVIATED_DOTDOT ||
                                matchesElementName(step, ((Xml.Tag) parent.getValue()).getName()))) {
                            candidates.add(parent);
                        }
                    } else if (value instanceof Xml.Tag) {
                        Xml.Tag tag = (Xml.Tag) value;
                        if (step.type == StepType.ATTRIBUTE_STEP) {
                            for (Xml.Attribute attribute : tag.getAttributes()) {
                                if (matchesName(step.name, attribute.getKeyAsString())) {
                                    candidates.add(new Cursor(origin, attribute));
                                }
                            }
                        } else if (step.type == StepType.NODE_TYPE_TEST) {
                            if (tag.getContent() != null) {
                                for (Content content : tag.getContent()) {
                                    if (matchesNodeTest(step, content)) {
                                        candidates.add(new Cursor(origin, content));
                                    }
                                }
                            }
                        } else {
                            for (Xml.Tag child : tag.getChildren()) {
                                if (step.type == StepType.AXIS_STEP ? matchesElementName(step, child.getName()) :
                                        matchesName(step.name, child.getName())) {
                                    candidates.add(new Cursor(origin, child));
                                }
                            }
                        }
                    }
                    next.addAll(filterCandidates(candidates, step.predicates));
                }
            }
            contexts = next;
        }
        return contexts;
    }

    private List<Cursor> filterCandidates(List<Cursor> candidates, CompiledExpr[] predicates) {
        for (CompiledExpr predicate : predicates) {
            List<Cursor> filtered = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                Cursor candidate = candidates.get(i);
                Object value = candidate.getValue();
                if (evaluateExpr(predicate, value instanceof Xml.Tag ? (Xml.Tag) value : null,
                        value instanceof Xml.Attribute ? (Xml.Attribute) value : null,
                        candidate, i + 1, candidates.size())) {
                    filtered.add(candidate);
                }
            }
            candidates = filtered;
        }
        return candidates;
    }

    private static String stringValue(Object node) {
        if (node instanceof Xml.Attribute) {
            return ((Xml.Attribute) node).getValueAsString();
        }
        if (node instanceof Xml.CharData) {
            return ((Xml.CharData) node).getText();
        }
        StringBuilder value = new StringBuilder();
        if (node instanceof Xml.Tag && ((Xml.Tag) node).getContent() != null) {
            for (Content child : ((Xml.Tag) node).getContent()) {
                if (child instanceof Xml.Tag || child instanceof Xml.CharData) {
                    value.append(stringValue(child));
                }
            }
        }
        return value.toString();
    }

    private static boolean matchesNodeTest(CompiledStep step, Object value) {
        switch (step.nodeTypeTestType) {
            case NODE:
                return true;
            case TEXT:
                return value instanceof Xml.CharData;
            case COMMENT:
                return value instanceof Xml.Comment;
            case PROCESSING_INSTRUCTION:
                return value instanceof Xml.ProcessingInstruction;
            default:
                return value instanceof Xml.Tag && matchesName(step.name, ((Xml.Tag) value).getName());
        }
    }

    private static @Nullable String functionStringValue(@Nullable Object value) {
        if (value instanceof Double) {
            double number = (Double) value;
            if (Double.isFinite(number)) {
                return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
            }
        }
        return value == null ? null : value.toString();
    }

    private static double numberValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = value.toString().trim();
        if (!NUMBER.matcher(text).matches()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static boolean matchesElementName(CompiledStep step, String actualName) {
        return step.nodeTypeTestType == NodeTypeTestType.NODE ||
                step.nodeTypeTestType == NodeTypeTestType.UNKNOWN && matchesName(step.name, actualName);
    }

    /**
     * Check if a name pattern matches an actual name (without node() test).
     * Handles wildcards (*) and null patterns.
     * Use this for attribute names or simple element name tests.
     *
     * @param pattern    the XPath name pattern (may be null, "*", or a specific name)
     * @param actualName the actual name to match against
     * @return true if the pattern matches the actual name
     */
    private static boolean matchesName(@Nullable String pattern, String actualName) {
        return pattern == null || "*".equals(pattern) || actualName.equals(pattern);
    }
}
