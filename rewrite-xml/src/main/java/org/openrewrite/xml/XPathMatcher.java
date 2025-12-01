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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        // Early reject: for absolute paths without any //, if cursor depth exceeds element steps, can't match
        // e.g., /a/b has 2 element steps, so cursor at depth 3+ can never match
        // But /a//b can match at any depth due to the // so we can't apply this optimization
        if (compiled.hasAbsolutePath() && !compiled.hasDescendant() && compiled.elementStepCount > 0) {
            int depth = 0;
            for (Cursor c = cursor; c != null; c = c.getParent()) {
                if (c.getValue() instanceof Xml.Tag) {
                    depth++;
                    if (depth > compiled.elementStepCount) {
                        return false;  // Too deep
                    }
                }
            }
        }

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
            if (lastStep.getType() == StepType.AXIS_STEP && !matchesElementName(lastStep.getName(), tag.getName())) {
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
        if (parentStep.type == StepType.AXIS_STEP && !matchesElementName(parentStep.name, currentTag.getName())) {
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
                    if (!matchesElementName(step.name, tag.getName())) {
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
                    if (!matchesElementName(step.name, tag.getName())) {
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
     */
    private boolean matchStepAgainstTag(CompiledStep step, Xml.Tag tag, Cursor cursor) {
        // Handle different step types
        switch (step.type) {
            case NODE_TEST:
                // Element name or wildcard
                if (!matchesName(step.name, tag.getName())) {
                    return false;
                }
                break;
            case ABBREVIATED_DOT:
                // . matches current - always true for tags
                break;
            case ABBREVIATED_DOTDOT:
                // .. is handled differently - this shouldn't be called directly
                return false;
            case NODE_TYPE_TEST:
                // text(), comment(), node() - these don't match tags
                if (step.nodeTypeTestType != NodeTypeTestType.NODE) {
                    return false;
                }
                break;
            default:
                // Other step types not supported in bottom-up yet
                return false;
        }

        // Check predicates if present
        if (step.predicates.length > 0) {
            return evaluatePredicates(step.predicates, tag, cursor);
        }

        return true;
    }

    /**
     * Evaluate predicates against a tag.
     * Uses compiled expressions - no ANTLR tree traversal needed.
     */
    private boolean evaluatePredicates(CompiledExpr[] predicates, Xml.Tag tag, Cursor cursor) {
        // Calculate position/size once by looking at parent's children
        int position = 1;
        int size = 1;
        Cursor parentCursor = cursor.getParent();
        if (parentCursor != null && parentCursor.getValue() instanceof Xml.Tag) {
            Xml.Tag parent = parentCursor.getValue();
            List<? extends Content> contents = parent.getContent();
            if (contents != null) {
                int count = 0;
                for (Content c : contents) {
                    if (c instanceof Xml.Tag) {
                        Xml.Tag child = (Xml.Tag) c;
                        if (child.getName().equals(tag.getName())) {
                            count++;
                            if (child == tag) {
                                position = count;
                            }
                        }
                    }
                }
                size = count > 0 ? count : 1;
            }
        }

        for (CompiledExpr predicate : predicates) {
            if (!evaluateExpr(predicate, tag, null, cursor, position, size)) {
                return false;
            }
        }
        return true;
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
                return tag != null && pathExists(tag, expr);

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
        String leftValue = resolveValue(expr.left, tag, attr, cursor, position, size);
        String rightValue = resolveValue(expr.right, tag, attr, cursor, position, size);

        if (leftValue == null || rightValue == null) {
            return false;
        }

        return compareValues(leftValue, rightValue, expr.op);
    }

    /**
     * Unified value resolution for both tag and attribute contexts.
     */
    private @Nullable String resolveValue(CompiledExpr expr, Xml.@Nullable Tag tag, Xml.@Nullable Attribute attr,
                                          Cursor cursor, int position, int size) {
        switch (expr.type) {
            case STRING:
                return expr.stringValue;

            case NUMERIC:
                return String.valueOf(expr.numericValue);

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

            case PATH:
                // Tag context only
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
                if (expr.args != null && expr.args.length >= 2) {
                    String str = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    String substr = resolveValue(expr.args[1], tag, attr, cursor, position, size);
                    return str != null && substr != null && str.contains(substr);
                }
                return false;

            case STARTS_WITH:
                if (expr.args != null && expr.args.length >= 2) {
                    String str = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    String prefix = resolveValue(expr.args[1], tag, attr, cursor, position, size);
                    return str != null && prefix != null && str.startsWith(prefix);
                }
                return false;

            case ENDS_WITH:
                if (expr.args != null && expr.args.length >= 2) {
                    String str = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    String suffix = resolveValue(expr.args[1], tag, attr, cursor, position, size);
                    return str != null && suffix != null && str.endsWith(suffix);
                }
                return false;

            case STRING_LENGTH:
                if (expr.args != null && expr.args.length > 0) {
                    String str = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    return str != null && !str.isEmpty();
                }
                return false;

            case COUNT:
                if (expr.args != null && expr.args.length > 0) {
                    CompiledExpr pathArg = expr.args[0];
                    if (pathArg.type == ExprType.ABSOLUTE_PATH && pathArg.stringValue != null) {
                        Xml.Tag root = getRootTag(cursor);
                        if (root != null) {
                            Set<Xml.Tag> matches = findTagsByPath(root, pathArg.stringValue);
                            return !matches.isEmpty();
                        }
                    }
                }
                return false;

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
     */
    private @Nullable String resolveFunctionValue(CompiledExpr expr, Xml.@Nullable Tag tag, Xml.@Nullable Attribute attr,
                                                  Cursor cursor, int position, int size) {
        if (expr.functionType == null) {
            return null;
        }

        switch (expr.functionType) {
            case POSITION:
                return String.valueOf(position);

            case LAST:
                return String.valueOf(size);

            case LOCAL_NAME: {
                // Handle arguments: local-name(..) means local-name of parent, local-name(.) means self
                Xml.Tag targetTag = tag;
                if (expr.args != null && expr.args.length > 0) {
                    CompiledExpr argExpr = expr.args[0];
                    if (argExpr.type == ExprType.PARENT) {
                        targetTag = getParentTag(cursor);
                    } else if (argExpr.type == ExprType.SELF) {
                        targetTag = tag;
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
                    String str = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    return str != null ? String.valueOf(str.length()) : "0";
                }
                return "0";

            case SUBSTRING_BEFORE:
                if (expr.args != null && expr.args.length >= 2) {
                    String str = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    String delim = resolveValue(expr.args[1], tag, attr, cursor, position, size);
                    String result = substringBefore(str, delim);
                    return result != null ? result : "";
                }
                return "";

            case SUBSTRING_AFTER:
                if (expr.args != null && expr.args.length >= 2) {
                    String str = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    String delim = resolveValue(expr.args[1], tag, attr, cursor, position, size);
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
                            return String.valueOf(matches.size());
                        }
                    }
                }
                return "0";

            case CONTAINS:
                if (expr.args != null && expr.args.length >= 2) {
                    String str = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    String substr = resolveValue(expr.args[1], tag, attr, cursor, position, size);
                    return String.valueOf(str != null && substr != null && str.contains(substr));
                }
                return "false";

            case STARTS_WITH:
                if (expr.args != null && expr.args.length >= 2) {
                    String str = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    String prefix = resolveValue(expr.args[1], tag, attr, cursor, position, size);
                    return String.valueOf(str != null && prefix != null && str.startsWith(prefix));
                }
                return "false";

            case ENDS_WITH:
                if (expr.args != null && expr.args.length >= 2) {
                    String str = resolveValue(expr.args[0], tag, attr, cursor, position, size);
                    String suffix = resolveValue(expr.args[1], tag, attr, cursor, position, size);
                    return String.valueOf(str != null && suffix != null && str.endsWith(suffix));
                }
                return "false";

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
     * Package-private so XPathMatcherVisitor can share this logic.
     */
    static boolean compareValues(String left, String right, @Nullable ComparisonOp op) {
        if (op == null) {
            return left.equals(right);
        }

        switch (op) {
            case EQ:
                return left.equals(right);
            case NE:
                return !left.equals(right);
            case LT:
            case LE:
            case GT:
            case GE:
                // Try numeric comparison
                try {
                    double leftNum = Double.parseDouble(left);
                    double rightNum = Double.parseDouble(right);
                    switch (op) {
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
                } catch (NumberFormatException e) {
                    // Fall back to string comparison
                    int cmp = left.compareTo(right);
                    switch (op) {
                        case LT:
                            return cmp < 0;
                        case LE:
                            return cmp <= 0;
                        case GT:
                            return cmp > 0;
                        case GE:
                            return cmp >= 0;
                        default:
                            return false;
                    }
                }
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

        // Find all matching nodes
        Set<Xml.Tag> allMatches = findTagsByPath(root, compiled.filterExpr.pathExpr);
        if (allMatches.isEmpty()) {
            return false;
        }

        // Convert to list for positional access
        List<Xml.Tag> matchList = new ArrayList<>(allMatches);
        int size = matchList.size();

        // Apply predicates to filter the result set
        List<Xml.Tag> filteredMatches = new ArrayList<>();
        for (int i = 0; i < matchList.size(); i++) {
            Xml.Tag tag = matchList.get(i);
            int position = i + 1; // 1-based
            boolean allPredicatesMatch = true;
            for (CompiledExpr predicate : compiled.filterExpr.predicates) {
                if (!evaluateFilterPredicate(predicate, tag, position, size)) {
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

        // Get current tag from cursor
        Xml.Tag currentTag = null;
        if (cursor.getValue() instanceof Xml.Tag) {
            currentTag = cursor.getValue();
        }
        if (currentTag == null) {
            return false;
        }

        // Check if there's a trailing path after the predicates
        if (compiled.filterExpr.trailingPath != null) {
            Set<Xml.Tag> trailingMatches = new LinkedHashSet<>();
            for (Xml.Tag filteredTag : filteredMatches) {
                if (compiled.filterExpr.trailingIsDescendant) {
                    findDescendants(filteredTag, compiled.filterExpr.trailingPath, trailingMatches);
                } else {
                    // Handle multi-segment paths like "configuration/source"
                    trailingMatches.addAll(findChildrenByPath(filteredTag, compiled.filterExpr.trailingPath));
                }
            }
            return trailingMatches.contains(currentTag);
        } else {
            return filteredMatches.contains(currentTag);
        }
    }

    /**
     * Evaluate a predicate for filter expressions.
     */
    @SuppressWarnings("DataFlowIssue")
    private boolean evaluateFilterPredicate(CompiledExpr expr, Xml.Tag tag, int position, int size) {
        switch (expr.type) {
            case NUMERIC:
                return position == expr.numericValue;

            case FUNCTION:
                if (expr.functionType == FunctionType.LAST) {
                    return position == size;
                }
                if (expr.functionType == FunctionType.POSITION) {
                    return position > 0;
                }
                return false;

            case COMPARISON:
                // For filter predicates, comparisons often involve position()/last()
                String leftValue = resolveFilterValue(expr.left, tag, position, size);
                String rightValue = resolveFilterValue(expr.right, tag, position, size);
                if (leftValue == null || rightValue == null) {
                    return false;
                }
                return compareValues(leftValue, rightValue, expr.op);

            default:
                return true; // Be permissive for unsupported expressions
        }
    }

    /**
     * Resolve a value for filter predicate evaluation.
     */
    @SuppressWarnings("ConstantValue")
    private @Nullable String resolveFilterValue(CompiledExpr expr, Xml.Tag tag, int position, int size) {
        if (expr == null) return null;

        switch (expr.type) {
            case STRING:
                return expr.stringValue;
            case NUMERIC:
                return String.valueOf(expr.numericValue);
            case FUNCTION:
                if (expr.functionType == FunctionType.POSITION) {
                    return String.valueOf(position);
                }
                if (expr.functionType == FunctionType.LAST) {
                    return String.valueOf(size);
                }
                if (expr.functionType == FunctionType.LOCAL_NAME) {
                    return localName(tag.getName());
                }
                return null;
            default:
                return null;
        }
    }

    // ==================== Tree Traversal Helpers ====================

    /**
     * Get the cursor at the root tag by walking up to the document.
     */
    private @Nullable Cursor getRootCursor(Cursor cursor) {
        Cursor c = cursor;
        while (c.getParent() != null && !(c.getParent().getValue() instanceof Xml.Document)) {
            c = c.getParent();
        }
        return c.getValue() instanceof Xml.Tag ? c : null;
    }

    /**
     * Get the root tag from a cursor by walking up to the document.
     */
    private Xml.@Nullable Tag getRootTag(Cursor cursor) {
        Cursor rootCursor = getRootCursor(cursor);
        return rootCursor != null ? (Xml.Tag) rootCursor.getValue() : null;
    }

    /**
     * Find tags matching a path expression starting from a root tag.
     * Supports absolute paths (/a/b), descendant paths (//a), and relative paths (a/b).
     */
    private Set<Xml.Tag> findTagsByPath(Xml.Tag startTag, String pathExpr) {
        Set<Xml.Tag> result = new LinkedHashSet<>();

        // Handle descendant-or-self axis (//)
        if (pathExpr.startsWith("//")) {
            String elementName = pathExpr.substring(2);
            if (elementName.contains("/")) {
                elementName = elementName.substring(0, elementName.indexOf('/'));
            }
            findDescendants(startTag, elementName, result);
            return result;
        }

        // Handle absolute path
        boolean isAbsolute = pathExpr.startsWith("/");
        if (isAbsolute) {
            pathExpr = pathExpr.substring(1);
        }

        String[] steps = pathExpr.split("/");
        if (steps.length == 0) {
            return result;
        }

        Set<Xml.Tag> currentMatches = new LinkedHashSet<>();

        if (isAbsolute) {
            // First step matches the root element itself
            String firstStep = steps[0];
            if ("*".equals(firstStep) || startTag.getName().equals(firstStep)) {
                if (steps.length == 1) {
                    currentMatches.add(startTag);
                } else {
                    currentMatches = findDirectChildren(startTag, steps[1]);
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
            // Relative path - start with children
            currentMatches = findDirectChildren(startTag, steps[0]);
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
        if ("*".equals(elementName) || tag.getName().equals(elementName)) {
            result.add(tag);
        }
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
     * Find direct children matching the given element name or wildcard.
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

    /**
     * Find children by a path that may contain multiple segments (e.g., "configuration/source").
     */
    private Set<Xml.Tag> findChildrenByPath(Xml.Tag parent, String path) {
        String[] steps = path.split("/");
        Set<Xml.Tag> currentMatches = findDirectChildren(parent, steps[0]);
        for (int i = 1; i < steps.length; i++) {
            Set<Xml.Tag> nextMatches = new LinkedHashSet<>();
            for (Xml.Tag match : currentMatches) {
                nextMatches.addAll(findDirectChildren(match, steps[i]));
            }
            currentMatches = nextMatches;
        }
        return currentMatches;
    }

    /**
     * Check if a name pattern matches an actual element name in an axis context.
     * Handles wildcards (*), node() tests, and null patterns.
     * Use this for element names where node() is a valid node type test.
     *
     * @param pattern    the XPath name pattern (may be null, "*", "node", or a specific name)
     * @param actualName the actual element name to match against
     * @return true if the pattern matches the actual name
     */
    private static boolean matchesElementName(@Nullable String pattern, String actualName) {
        return pattern == null || "*".equals(pattern) || "node".equals(pattern) || actualName.equals(pattern);
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
