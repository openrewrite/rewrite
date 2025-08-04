/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.hcl.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.UnaryOperator;

import static java.util.Collections.singletonList;

public class HclPrinter<P> extends HclVisitor<PrintOutputCapture<P>> {

    @Override
    public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
        p.append(space.getWhitespace());

        for (Comment comment : space.getComments()) {
            visitMarkers(comment.getMarkers(), p);
            switch (comment.getStyle()) {
                case LINE_SLASH:
                    p.append("//").append(comment.getText());
                    break;
                case LINE_HASH:
                    p.append("#").append(comment.getText());
                    break;
                case INLINE:
                    p.append("/*").append(comment.getText()).append("*/");
                    break;
            }
            p.append(comment.getSuffix());
        }
        return space;
    }

    protected void visitLeftPadded(@Nullable String prefix, @Nullable HclLeftPadded<? extends Hcl> leftPadded, HclLeftPadded.Location location, PrintOutputCapture<P> p) {
        if (leftPadded != null) {
            beforeSyntax(leftPadded.getBefore(), leftPadded.getMarkers(), location.getBeforeLocation(), p);
            if (prefix != null) {
                p.append(prefix);
            }
            visit(leftPadded.getElement(), p);
            afterSyntax(leftPadded.getMarkers(), p);
        }
    }

    protected void visitRightPadded(List<? extends HclRightPadded<? extends Hcl>> nodes, HclRightPadded.Location location, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            HclRightPadded<? extends Hcl> node = nodes.get(i);
            beforeSyntax(Space.EMPTY, node.getMarkers(), null, p);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), location.getAfterLocation(), p);
            if (i < nodes.size() - 1) {
                p.append(suffixBetween);
            }
            afterSyntax(node.getMarkers(), p);
        }
    }

    protected void visitContainer(String before, @Nullable HclContainer<? extends Hcl> container, HclContainer.Location location, String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        beforeSyntax(container.getBefore(), container.getMarkers(), location.getBeforeLocation(), p);
        p.append(before);
        visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
        p.append(after == null ? "" : after);
        afterSyntax(container.getMarkers(), p);
    }

    @Override
    public Hcl visitAttribute(Hcl.Attribute attribute, PrintOutputCapture<P> p) {
        beforeSyntax(attribute, Space.Location.ATTRIBUTE, p);
        visit(attribute.getName(), p);
        visitSpace(attribute.getPadding().getType().getBefore(), Space.Location.ATTRIBUTE_ASSIGNMENT, p);
        p.append(attribute.getType() == Hcl.Attribute.Type.Assignment ? "=" : ":");
        visit(attribute.getValue(), p);
        if (attribute.getComma() != null) {
            visitSpace(attribute.getComma().getPrefix(), Space.Location.OBJECT_VALUE_ATTRIBUTE_COMMA, p);
            p.append(",");
        }
        afterSyntax(attribute, p);
        return attribute;
    }

    @Override
    public Hcl visitAttributeAccess(Hcl.AttributeAccess attributeAccess, PrintOutputCapture<P> p) {
        beforeSyntax(attributeAccess, Space.Location.ATTRIBUTE_ACCESS, p);
        visit(attributeAccess.getAttribute(), p);
        visitLeftPadded(".", attributeAccess.getPadding().getName(), HclLeftPadded.Location.ATTRIBUTE_ACCESS_NAME, p);
        afterSyntax(attributeAccess, p);
        return attributeAccess;
    }

    @Override
    public Hcl visitBinary(Hcl.Binary binary, PrintOutputCapture<P> p) {
        beforeSyntax(binary, Space.Location.BINARY, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), Space.Location.BINARY_OPERATOR, p);
        switch (binary.getOperator()) {
            case Addition:
                p.append('+');
                break;
            case Subtraction:
                p.append('-');
                break;
            case Multiplication:
                p.append('*');
                break;
            case Division:
                p.append('/');
                break;
            case Modulo:
                p.append('%');
                break;
            case LessThan:
                p.append('<');
                break;
            case GreaterThan:
                p.append('>');
                break;
            case LessThanOrEqual:
                p.append("<=");
                break;
            case GreaterThanOrEqual:
                p.append(">=");
                break;
            case Equal:
                p.append("==");
                break;
            case NotEqual:
                p.append("!=");
                break;
            case Or:
                p.append("||");
                break;
            case And:
                p.append("&&");
                break;
        }
        visit(binary.getRight(), p);
        afterSyntax(binary, p);
        return binary;
    }

    @Override
    public Hcl visitBlock(Hcl.Block block, PrintOutputCapture<P> p) {
        beforeSyntax(block, Space.Location.BLOCK, p);
        visit(block.getType(), p);
        visit(block.getLabels(), p);
        visitSpace(block.getOpen(), Space.Location.BLOCK_OPEN, p);
        p.append('{');
        visit(block.getBody(), p);
        visitSpace(block.getEnd(), Space.Location.BLOCK_CLOSE, p);
        p.append('}');
        afterSyntax(block, p);
        return block;
    }

    @Override
    public Hcl visitConditional(Hcl.Conditional conditional, PrintOutputCapture<P> p) {
        beforeSyntax(conditional, Space.Location.CONDITIONAL, p);
        visit(conditional.getCondition(), p);
        visitLeftPadded("?", conditional.getPadding().getTruePart(), HclLeftPadded.Location.CONDITIONAL_TRUE, p);
        visitLeftPadded(":", conditional.getPadding().getFalsePart(), HclLeftPadded.Location.CONDITIONAL_FALSE, p);
        afterSyntax(conditional, p);
        return conditional;
    }

    @Override
    public Hcl visitConfigFile(Hcl.ConfigFile configFile, PrintOutputCapture<P> p) {
        beforeSyntax(configFile, Space.Location.CONFIG_FILE, p);
        visit(configFile.getBody(), p);
        visitSpace(configFile.getEof(), Space.Location.CONFIG_FILE_EOF, p);
        afterSyntax(configFile, p);
        return configFile;
    }

    @Override
    public Hcl visitForIntro(Hcl.ForIntro forIntro, PrintOutputCapture<P> p) {
        beforeSyntax(forIntro, Space.Location.FOR_INTRO, p);
        visitContainer("for", forIntro.getPadding().getVariables(), HclContainer.Location.FOR_VARIABLES,
                ",", "in", p);
        visit(forIntro.getIn(), p);
        afterSyntax(forIntro, p);
        return forIntro;
    }

    @Override
    public Hcl visitForObject(Hcl.ForObject forObject, PrintOutputCapture<P> p) {
        beforeSyntax(forObject, Space.Location.FOR_OBJECT, p);
        p.append("{");
        visit(forObject.getIntro(), p);
        visitLeftPadded(":", forObject.getPadding().getUpdateName(), HclLeftPadded.Location.FOR_UPDATE, p);
        visitLeftPadded("=>", forObject.getPadding().getUpdateValue(), HclLeftPadded.Location.FOR_UPDATE_VALUE, p);
        if (forObject.getEllipsis() != null) {
            visitSpace(forObject.getEllipsis().getPrefix(), Space.Location.FOR_UPDATE_VALUE_ELLIPSIS, p);
            p.append("...");
        }
        if (forObject.getPadding().getCondition() != null) {
            visitLeftPadded("if", forObject.getPadding().getCondition(), HclLeftPadded.Location.FOR_CONDITION, p);
        }
        visitSpace(forObject.getEnd(), Space.Location.FOR_OBJECT_SUFFIX, p);
        p.append("}");
        afterSyntax(forObject, p);
        return forObject;
    }

    @Override
    public Hcl visitForTuple(Hcl.ForTuple forTuple, PrintOutputCapture<P> p) {
        beforeSyntax(forTuple, Space.Location.FOR_TUPLE, p);
        p.append("[");
        visit(forTuple.getIntro(), p);
        visitLeftPadded(":", forTuple.getPadding().getUpdate(), HclLeftPadded.Location.FOR_UPDATE, p);
        if (forTuple.getPadding().getCondition() != null) {
            visitLeftPadded("if", forTuple.getPadding().getCondition(), HclLeftPadded.Location.FOR_CONDITION, p);
        }
        visitSpace(forTuple.getEnd(), Space.Location.FOR_TUPLE_SUFFIX, p);
        p.append("]");
        afterSyntax(forTuple, p);
        return forTuple;
    }

    @Override
    public Hcl visitFunctionCall(Hcl.FunctionCall functionCall, PrintOutputCapture<P> p) {
        beforeSyntax(functionCall, Space.Location.FUNCTION_CALL, p);
        visit(functionCall.getName(), p);
        visitContainer("(", functionCall.getPadding().getArguments(), HclContainer.Location.FUNCTION_CALL_ARGUMENTS,
                ",", ")", p);
        afterSyntax(functionCall, p);
        return functionCall;
    }

    @Override
    public Hcl visitHeredocTemplate(Hcl.HeredocTemplate heredocTemplate, PrintOutputCapture<P> p) {
        beforeSyntax(heredocTemplate, Space.Location.HEREDOC, p);
        p.append(heredocTemplate.getArrow());
        visit(heredocTemplate.getDelimiter(), p);
        visit(heredocTemplate.getExpressions(), p);
        visitSpace(heredocTemplate.getEnd(), Space.Location.HEREDOC_END, p);
        p.append(heredocTemplate.getDelimiter().getName());
        afterSyntax(heredocTemplate, p);
        return heredocTemplate;
    }

    @Override
    public Hcl visitIdentifier(Hcl.Identifier identifier, PrintOutputCapture<P> p) {
        beforeSyntax(identifier, Space.Location.IDENTIFIER, p);
        p.append(identifier.getName());
        afterSyntax(identifier, p);
        return identifier;
    }

    @Override
    public Hcl visitIndex(Hcl.Index index, PrintOutputCapture<P> p) {
        beforeSyntax(index, Space.Location.INDEX, p);
        visit(index.getIndexed(), p);
        visit(index.getPosition(), p);
        afterSyntax(index, p);
        return index;
    }

    @Override
    public Hcl visitIndexPosition(Hcl.Index.Position indexPosition, PrintOutputCapture<P> p) {
        beforeSyntax(indexPosition, Space.Location.INDEX_POSITION, p);
        p.append("[");
        visitMarkers(indexPosition.getMarkers(), p);
        visitRightPadded(indexPosition.getPadding().getPosition(), HclRightPadded.Location.INDEX_POSITION, p);
        p.append("]");
        afterSyntax(indexPosition, p);
        return indexPosition;
    }

    @Override
    public Hcl visitLegacyIndexAttribute(Hcl.LegacyIndexAttributeAccess laccess, PrintOutputCapture<P> p) {
        beforeSyntax(laccess, Space.Location.LEGACY_INDEX_ATTRIBUTE_ACCESS, p);
        visitRightPadded(
                singletonList(laccess.getPadding().getBase()),
                HclRightPadded.Location.LEGACY_INDEX_ATTRIBUTE_ACCESS_BASE, "", p);
        p.append(".");
        visitLiteral(laccess.getIndex(), p);
        afterSyntax(laccess, p);
        return laccess;
    }

    @Override
    public Hcl visitLiteral(Hcl.Literal literal, PrintOutputCapture<P> p) {
        beforeSyntax(literal, Space.Location.LITERAL, p);
        p.append(literal.getValueSource());
        afterSyntax(literal, p);
        return literal;
    }

    @Override
    public Hcl visitObjectValue(Hcl.ObjectValue objectValue, PrintOutputCapture<P> p) {
        beforeSyntax(objectValue, Space.Location.OBJECT_VALUE, p);
        visitContainer("{", objectValue.getPadding().getAttributes(), HclContainer.Location.OBJECT_VALUE_ATTRIBUTES,
                "", "}", p);
        afterSyntax(objectValue, p);
        return objectValue;
    }

    @Override
    public Hcl visitParentheses(Hcl.Parentheses parentheses, PrintOutputCapture<P> p) {
        beforeSyntax(parentheses, Space.Location.PARENTHETICAL_EXPRESSION, p);
        p.append('(');
        visitRightPadded(parentheses.getPadding().getExpression(), HclRightPadded.Location.PARENTHESES, p);
        p.append(')');
        afterSyntax(parentheses, p);
        return parentheses;
    }

    @Override
    public Hcl visitQuotedTemplate(Hcl.QuotedTemplate template, PrintOutputCapture<P> p) {
        beforeSyntax(template, Space.Location.QUOTED_TEMPLATE, p);
        p.append('"');
        visit(template.getExpressions(), p);
        p.append('"');
        afterSyntax(template, p);
        return template;
    }

    @Override
    public Hcl visitTemplateInterpolation(Hcl.TemplateInterpolation template, PrintOutputCapture<P> p) {
        beforeSyntax(template, Space.Location.TEMPLATE_INTERPOLATION, p);
        p.append("${");
        visit(template.getExpression(), p);
        p.append('}');
        afterSyntax(template, p);
        return template;
    }

    @Override
    public Hcl visitSplat(Hcl.Splat splat, PrintOutputCapture<P> p) {
        beforeSyntax(splat, Space.Location.ATTRIBUTE_ACCESS, p);
        visit(splat.getSelect(), p);
        visit(splat.getOperator(), p);
        afterSyntax(splat, p);
        return splat;
    }

    @Override
    public Hcl visitSplatOperator(Hcl.Splat.Operator splatOperator, PrintOutputCapture<P> p) {
        beforeSyntax(splatOperator, Space.Location.SPLAT_OPERATOR, p);
        if (splatOperator.getType() == Hcl.Splat.Operator.Type.Full) {
            p.append('[');
        } else {
            p.append('.');
        }
        visitSpace(splatOperator.getSplat().getElement().getPrefix(), Space.Location.SPLAT_OPERATOR_PREFIX, p);
        p.append('*');
        if (splatOperator.getType() == Hcl.Splat.Operator.Type.Full) {
            visitSpace(splatOperator.getSplat().getAfter(), Space.Location.SPLAT_OPERATOR_SUFFIX, p);
            p.append(']');
        }
        afterSyntax(splatOperator, p);
        return splatOperator;
    }

    @Override
    public Hcl visitTuple(Hcl.Tuple tuple, PrintOutputCapture<P> p) {
        beforeSyntax(tuple, Space.Location.FUNCTION_CALL, p);
        visitContainer("[", tuple.getPadding().getValues(), HclContainer.Location.TUPLE_VALUES,
                ",", "]", p);
        afterSyntax(tuple, p);
        return tuple;
    }

    @Override
    public Hcl visitUnary(Hcl.Unary unary, PrintOutputCapture<P> p) {
        beforeSyntax(unary, Space.Location.UNARY, p);
        switch (unary.getOperator()) {
            case Negative:
                p.append('-');
                break;
            case Not:
                p.append('!');
                break;
        }
        visit(unary.getExpression(), p);
        afterSyntax(unary, p);
        return unary;
    }

    @Override
    public Hcl visitVariableExpression(Hcl.VariableExpression variableExpression, PrintOutputCapture<P> p) {
        beforeSyntax(variableExpression, Space.Location.VARIABLE_EXPRESSION, p);
        visit(variableExpression.getName(), p);
        afterSyntax(variableExpression, p);
        return variableExpression;
    }

    private static final UnaryOperator<String> HCL_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    private void beforeSyntax(Hcl h, Space.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(h.getPrefix(), h.getMarkers(), loc, p);
    }

    private void beforeSyntax(Space prefix, Markers markers, Space.@Nullable Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), HCL_MARKER_WRAPPER));
        }
        if (loc != null) {
            visitSpace(prefix, loc, p);
        }
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), HCL_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(Hcl h, PrintOutputCapture<P> p) {
        afterSyntax(h.getMarkers(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), HCL_MARKER_WRAPPER));
        }
    }
}
