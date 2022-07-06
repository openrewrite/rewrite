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

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.tree.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;

import java.util.List;

public class HclPrinter<P> extends HclVisitor<PrintOutputCapture<P>> {

    @Override
    public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
        p.out.append(space.getWhitespace());

        for (Comment comment : space.getComments()) {
            visitMarkers(comment.getMarkers(), p);
            switch (comment.getStyle()) {
                case LINE_SLASH:
                    p.out.append("//").append(comment.getText());
                    break;
                case LINE_HASH:
                    p.out.append("#").append(comment.getText());
                    break;
                case INLINE:
                    p.out.append("/*").append(comment.getText()).append("*/");
                    break;
            }
            p.out.append(comment.getSuffix());
        }
        return space;
    }

    protected void visitLeftPadded(@Nullable String prefix, @Nullable HclLeftPadded<? extends Hcl> leftPadded, HclLeftPadded.Location location, PrintOutputCapture<P> p) {
        if (leftPadded != null) {
            visitSpace(leftPadded.getBefore(), location.getBeforeLocation(), p);
            if (prefix != null) {
                p.out.append(prefix);
            }
            visit(leftPadded.getElement(), p);
        }
    }

    protected void visitRightPadded(List<? extends HclRightPadded<? extends Hcl>> nodes, HclRightPadded.Location location, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            HclRightPadded<? extends Hcl> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), location.getAfterLocation(), p);
            if (i < nodes.size() - 1) {
                p.out.append(suffixBetween);
            }
        }
    }

    protected void visitContainer(String before, @Nullable HclContainer<? extends Hcl> container, HclContainer.Location location, String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        visitSpace(container.getBefore(), location.getBeforeLocation(), p);
        p.out.append(before);
        visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
        p.out.append(after == null ? "" : after);
    }

    @Override
    public Hcl visitAttribute(Hcl.Attribute attribute, PrintOutputCapture<P> p) {
        visitSpace(attribute.getPrefix(), Space.Location.ATTRIBUTE, p);
        visitMarkers(attribute.getMarkers(), p);
        visit(attribute.getName(), p);
        visitSpace(attribute.getPadding().getType().getBefore(), Space.Location.ATTRIBUTE_ASSIGNMENT, p);
        p.out.append(attribute.getType().equals(Hcl.Attribute.Type.Assignment) ? "=" : ":");
        visit(attribute.getValue(), p);
        return attribute;
    }

    @Override
    public Hcl visitAttributeAccess(Hcl.AttributeAccess attributeAccess, PrintOutputCapture<P> p) {
        visitSpace(attributeAccess.getPrefix(), Space.Location.ATTRIBUTE_ACCESS, p);
        visitMarkers(attributeAccess.getMarkers(), p);
        visit(attributeAccess.getAttribute(), p);
        visitLeftPadded(".", attributeAccess.getPadding().getName(), HclLeftPadded.Location.ATTRIBUTE_ACCESS_NAME, p);
        return attributeAccess;
    }

    @Override
    public Hcl visitBinary(Hcl.Binary binary, PrintOutputCapture<P> p) {
        visitSpace(binary.getPrefix(), Space.Location.BINARY, p);
        visitMarkers(binary.getMarkers(), p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), Space.Location.BINARY_OPERATOR, p);
        switch (binary.getOperator()) {
            case Addition:
                p.out.append('+');
                break;
            case Subtraction:
                p.out.append('-');
                break;
            case Multiplication:
                p.out.append('*');
                break;
            case Division:
                p.out.append('/');
                break;
            case Modulo:
                p.out.append('%');
                break;
            case LessThan:
                p.out.append('<');
                break;
            case GreaterThan:
                p.out.append('>');
                break;
            case LessThanOrEqual:
                p.out.append("<=");
                break;
            case GreaterThanOrEqual:
                p.out.append(">=");
                break;
            case Equal:
                p.out.append("==");
                break;
            case NotEqual:
                p.out.append("!=");
                break;
            case Or:
                p.out.append("||");
                break;
            case And:
                p.out.append("&&");
                break;
        }
        visit(binary.getRight(), p);
        return binary;
    }

    @Override
    public Hcl visitBlock(Hcl.Block block, PrintOutputCapture<P> p) {
        visitSpace(block.getPrefix(), Space.Location.BLOCK, p);
        visitMarkers(block.getMarkers(), p);
        visit(block.getType(), p);
        visit(block.getLabels(), p);
        visitSpace(block.getOpen(), Space.Location.BLOCK_OPEN, p);
        p.out.append('{');
        visit(block.getBody(), p);
        visitSpace(block.getEnd(), Space.Location.BLOCK_CLOSE, p);
        p.out.append('}');
        return block;
    }

    @Override
    public Hcl visitConditional(Hcl.Conditional conditional, PrintOutputCapture<P> p) {
        visitSpace(conditional.getPrefix(), Space.Location.CONDITIONAL, p);
        visitMarkers(conditional.getMarkers(), p);
        visit(conditional.getCondition(), p);
        visitLeftPadded("?", conditional.getPadding().getTruePart(), HclLeftPadded.Location.CONDITIONAL_TRUE, p);
        visitLeftPadded(":", conditional.getPadding().getFalsePart(), HclLeftPadded.Location.CONDITIONAL_FALSE, p);
        return conditional;
    }

    @Override
    public Hcl visitConfigFile(Hcl.ConfigFile configFile, PrintOutputCapture<P> p) {
        visitSpace(configFile.getPrefix(), Space.Location.CONFIG_FILE, p);
        visitMarkers(configFile.getMarkers(), p);
        visit(configFile.getBody(), p);
        visitSpace(configFile.getEof(), Space.Location.CONFIG_FILE_EOF, p);
        return configFile;
    }

    @Override
    public Hcl visitForIntro(Hcl.ForIntro forIntro, PrintOutputCapture<P> p) {
        visitSpace(forIntro.getPrefix(), Space.Location.FOR_INTRO, p);
        visitMarkers(forIntro.getMarkers(), p);
        visitContainer("for", forIntro.getPadding().getVariables(), HclContainer.Location.FOR_VARIABLES,
                ",", "in", p);
        visit(forIntro.getIn(), p);
        return forIntro;
    }

    @Override
    public Hcl visitForObject(Hcl.ForObject forObject, PrintOutputCapture<P> p) {
        visitSpace(forObject.getPrefix(), Space.Location.FOR_OBJECT, p);
        visitMarkers(forObject.getMarkers(), p);
        p.out.append("{");
        visit(forObject.getIntro(), p);
        visitLeftPadded(":", forObject.getPadding().getUpdateName(), HclLeftPadded.Location.FOR_UPDATE, p);
        visitLeftPadded("=>", forObject.getPadding().getUpdateValue(), HclLeftPadded.Location.FOR_UPDATE_VALUE, p);
        if (forObject.getEllipsis() != null) {
            visitSpace(forObject.getEllipsis().getPrefix(), Space.Location.FOR_UPDATE_VALUE_ELLIPSIS, p);
            p.out.append("...");
        }
        if (forObject.getPadding().getCondition() != null) {
            visitLeftPadded("if", forObject.getPadding().getCondition(), HclLeftPadded.Location.FOR_CONDITION, p);
        }
        visitSpace(forObject.getEnd(), Space.Location.FOR_OBJECT_SUFFIX, p);
        p.out.append("}");
        return forObject;
    }

    @Override
    public Hcl visitForTuple(Hcl.ForTuple forTuple, PrintOutputCapture<P> p) {
        visitSpace(forTuple.getPrefix(), Space.Location.FOR_TUPLE, p);
        visitMarkers(forTuple.getMarkers(), p);
        p.out.append("[");
        visit(forTuple.getIntro(), p);
        visitLeftPadded(":", forTuple.getPadding().getUpdate(), HclLeftPadded.Location.FOR_UPDATE, p);
        if (forTuple.getPadding().getCondition() != null) {
            visitLeftPadded("if", forTuple.getPadding().getCondition(), HclLeftPadded.Location.FOR_CONDITION, p);
        }
        visitSpace(forTuple.getEnd(), Space.Location.FOR_TUPLE_SUFFIX, p);
        p.out.append("]");
        return forTuple;
    }

    @Override
    public Hcl visitFunctionCall(Hcl.FunctionCall functionCall, PrintOutputCapture<P> p) {
        visitSpace(functionCall.getPrefix(), Space.Location.FUNCTION_CALL, p);
        visitMarkers(functionCall.getMarkers(), p);
        visit(functionCall.getName(), p);
        visitContainer("(", functionCall.getPadding().getArguments(), HclContainer.Location.FUNCTION_CALL_ARGUMENTS,
                ",", ")", p);
        return functionCall;
    }

    @Override
    public Hcl visitHeredocTemplate(Hcl.HeredocTemplate heredocTemplate, PrintOutputCapture<P> p) {
        visitSpace(heredocTemplate.getPrefix(), Space.Location.HEREDOC, p);
        visitMarkers(heredocTemplate.getMarkers(), p);
        p.out.append(heredocTemplate.getArrow());
        visit(heredocTemplate.getDelimiter(), p);
        visit(heredocTemplate.getExpressions(), p);
        visitSpace(heredocTemplate.getEnd(), Space.Location.HEREDOC_END, p);
        p.out.append(heredocTemplate.getDelimiter().getName());
        return heredocTemplate;
    }

    @Override
    public Hcl visitIdentifier(Hcl.Identifier identifier, PrintOutputCapture<P> p) {
        visitSpace(identifier.getPrefix(), Space.Location.IDENTIFIER, p);
        visitMarkers(identifier.getMarkers(), p);
        p.out.append(identifier.getName());
        return identifier;
    }

    @Override
    public Hcl visitIndex(Hcl.Index index, PrintOutputCapture<P> p) {
        visitSpace(index.getPrefix(), Space.Location.INDEX, p);
        visitMarkers(index.getMarkers(), p);
        visit(index.getIndexed(), p);
        visit(index.getPosition(), p);
        return index;
    }

    @Override
    public Hcl visitIndexPosition(Hcl.Index.Position indexPosition, PrintOutputCapture<P> p) {
        visitSpace(indexPosition.getPrefix(), Space.Location.INDEX_POSITION, p);
        visitMarkers(indexPosition.getMarkers(), p);
        p.out.append("[");
        visitMarkers(indexPosition.getMarkers(), p);
        visitRightPadded(indexPosition.getPadding().getPosition(), HclRightPadded.Location.INDEX_POSITION, p);
        p.out.append("]");
        return indexPosition;
    }

    @Override
    public Hcl visitLiteral(Hcl.Literal literal, PrintOutputCapture<P> p) {
        visitSpace(literal.getPrefix(), Space.Location.LITERAL, p);
        visitMarkers(literal.getMarkers(), p);
        p.out.append(literal.getValueSource());
        return literal;
    }

    @Override
    public Hcl visitObjectValue(Hcl.ObjectValue objectValue, PrintOutputCapture<P> p) {
        visitSpace(objectValue.getPrefix(), Space.Location.OBJECT_VALUE, p);
        visitMarkers(objectValue.getMarkers(), p);
        visitContainer("{", objectValue.getPadding().getAttributes(), HclContainer.Location.OBJECT_VALUE_ATTRIBUTES,
                ",", "}", p);
        return objectValue;
    }

    @Override
    public Hcl visitParentheses(Hcl.Parentheses parentheses, PrintOutputCapture<P> p) {
        visitSpace(parentheses.getPrefix(), Space.Location.PARENTHETICAL_EXPRESSION, p);
        visitMarkers(parentheses.getMarkers(), p);
        p.out.append('(');
        visitRightPadded(parentheses.getPadding().getExpression(), HclRightPadded.Location.PARENTHESES, p);
        p.out.append(')');
        return parentheses;
    }

    @Override
    public Hcl visitQuotedTemplate(Hcl.QuotedTemplate template, PrintOutputCapture<P> p) {
        visitSpace(template.getPrefix(), Space.Location.QUOTED_TEMPLATE, p);
        visitMarkers(template.getMarkers(), p);
        p.out.append('"');
        visit(template.getExpressions(), p);
        p.out.append('"');
        return template;
    }

    @Override
    public Hcl visitTemplateInterpolation(Hcl.TemplateInterpolation template, PrintOutputCapture<P> p) {
        visitSpace(template.getPrefix(), Space.Location.TEMPLATE_INTERPOLATION, p);
        visitMarkers(template.getMarkers(), p);
        p.out.append("${");
        visit(template.getExpression(), p);
        p.out.append('}');
        return template;
    }

    @Override
    public Hcl visitSplat(Hcl.Splat splat, PrintOutputCapture<P> p) {
        visitSpace(splat.getPrefix(), Space.Location.ATTRIBUTE_ACCESS, p);
        visitMarkers(splat.getMarkers(), p);
        visit(splat.getSelect(), p);
        visit(splat.getOperator(), p);
        return splat;
    }

    @Override
    public Hcl visitSplatOperator(Hcl.Splat.Operator splatOperator, PrintOutputCapture<P> p) {
        visitSpace(splatOperator.getPrefix(), Space.Location.SPLAT_OPERATOR, p);
        visitMarkers(splatOperator.getMarkers(), p);
        if (splatOperator.getType().equals(Hcl.Splat.Operator.Type.Full)) {
            p.out.append('[');
        } else {
            p.out.append('.');
        }
        visitSpace(splatOperator.getSplat().getElement().getPrefix(), Space.Location.SPLAT_OPERATOR_PREFIX, p);
        p.out.append('*');
        if (splatOperator.getType().equals(Hcl.Splat.Operator.Type.Full)) {
            visitSpace(splatOperator.getSplat().getAfter(), Space.Location.SPLAT_OPERATOR_SUFFIX, p);
            p.out.append(']');
        }
        return splatOperator;
    }

    @Override
    public Hcl visitTuple(Hcl.Tuple tuple, PrintOutputCapture<P> p) {
        visitSpace(tuple.getPrefix(), Space.Location.FUNCTION_CALL, p);
        visitMarkers(tuple.getMarkers(), p);
        visitContainer("[", tuple.getPadding().getValues(), HclContainer.Location.TUPLE_VALUES,
                ",", "]", p);
        return tuple;
    }

    @Override
    public Hcl visitUnary(Hcl.Unary unary, PrintOutputCapture<P> p) {
        visitSpace(unary.getPrefix(), Space.Location.UNARY, p);
        visitMarkers(unary.getMarkers(), p);
        switch (unary.getOperator()) {
            case Negative:
                p.out.append('-');
                break;
            case Not:
                p.out.append('!');
                break;
        }
        visit(unary.getExpression(), p);
        return unary;
    }

    @Override
    public Hcl visitVariableExpression(Hcl.VariableExpression variableExpression, PrintOutputCapture<P> p) {
        visitSpace(variableExpression.getPrefix(), Space.Location.VARIABLE_EXPRESSION, p);
        visitMarkers(variableExpression.getMarkers(), p);
        visit(variableExpression.getName(), p);
        return variableExpression;
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        if (marker instanceof SearchResult) {
            String description = ((SearchResult) marker).getDescription();
            p.out.append("/*~~")
                    .append(description == null ? "" : "(" + description + ")~~")
                    .append(">*/");
        }
        //noinspection unchecked
        return (M) marker;
    }
}
