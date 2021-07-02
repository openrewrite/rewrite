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

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.tree.*;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

public class HclPrinter<P> extends HclVisitor<P> {
    private static final String PRINTER_ACC_KEY = "printed";

    private final TreePrinter<P> treePrinter;

    public HclPrinter(TreePrinter<P> treePrinter) {
        this.treePrinter = treePrinter;
    }

    @NonNull
    protected StringBuilder getPrinter() {
        StringBuilder acc = getCursor().getRoot().getMessage(PRINTER_ACC_KEY);
        if (acc == null) {
            acc = new StringBuilder();
            getCursor().getRoot().putMessage(PRINTER_ACC_KEY, acc);
        }
        return acc;
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        StringBuilder acc = getPrinter();
        acc.append(space.getWhitespace());

        for (Comment comment : space.getComments()) {
            visitMarkers(comment.getMarkers(), p);
            switch (comment.getStyle()) {
                case LINE_SLASH:
                    acc.append("//").append(comment.getText());
                    break;
                case LINE_HASH:
                    acc.append("#").append(comment.getText());
                    break;
                case INLINE:
                    acc.append("/*").append(comment.getText()).append("*/");
                    break;
            }
            acc.append(comment.getSuffix());
        }
        return space;
    }

    protected void visitLeftPadded(@Nullable String prefix, @Nullable HclLeftPadded<? extends Hcl> leftPadded, HclLeftPadded.Location location, P p) {
        if (leftPadded != null) {
            StringBuilder acc = getPrinter();
            visitSpace(leftPadded.getBefore(), location.getBeforeLocation(), p);
            if (prefix != null) {
                acc.append(prefix);
            }
            visit(leftPadded.getElement(), p);
        }
    }

    protected void visitRightPadded(List<? extends HclRightPadded<? extends Hcl>> nodes, HclRightPadded.Location location, String suffixBetween, P p) {
        StringBuilder acc = getPrinter();
        for (int i = 0; i < nodes.size(); i++) {
            HclRightPadded<? extends Hcl> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), location.getAfterLocation(), p);
            if (i < nodes.size() - 1) {
                acc.append(suffixBetween);
            }
        }
    }

    protected void visitContainer(String before, @Nullable HclContainer<? extends Hcl> container, HclContainer.Location location, String suffixBetween, @Nullable String after, P p) {
        if (container == null) {
            return;
        }
        StringBuilder acc = getPrinter();
        visitSpace(container.getBefore(), location.getBeforeLocation(), p);
        acc.append(before);
        visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
        acc.append(after == null ? "" : after);
    }

    public String print(Hcl hcl, P p) {
        setCursor(new Cursor(null, "EPSILON"));
        visit(hcl, p);
        return getPrinter().toString();
    }

    @Override
    @Nullable
    public Hcl visit(@Nullable Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        StringBuilder printerAcc = getPrinter();
        treePrinter.doBefore(tree, printerAcc, p);
        tree = super.visit(tree, p);
        if (tree != null) {
            treePrinter.doAfter(tree, printerAcc, p);
        }
        return (Hcl) tree;
    }

    public void visit(@Nullable List<? extends Hcl> nodes, P p) {
        if (nodes != null) {
            for (Hcl node : nodes) {
                visit(node, p);
            }
        }
    }

    @Override
    public Hcl visitAttribute(Hcl.Attribute attribute, P p) {
        visitSpace(attribute.getPrefix(), Space.Location.ATTRIBUTE, p);
        visit(attribute.getName(), p);
        StringBuilder acc = getPrinter();
        visitSpace(attribute.getPadding().getType().getBefore(), Space.Location.ATTRIBUTE_ASSIGNMENT, p);
        acc.append(attribute.getType().equals(Hcl.Attribute.Type.Assignment) ? "=" : ":");
        visit(attribute.getValue(), p);
        return attribute;
    }

    @Override
    public Hcl visitAttributeAccess(Hcl.AttributeAccess attributeAccess, P p) {
        visitSpace(attributeAccess.getPrefix(), Space.Location.ATTRIBUTE_ACCESS, p);
        visit(attributeAccess.getAttribute(), p);
        visitLeftPadded(".", attributeAccess.getPadding().getName(), HclLeftPadded.Location.ATTRIBUTE_ACCESS_NAME, p);
        return attributeAccess;
    }

    @Override
    public Hcl visitBinary(Hcl.Binary binary, P p) {
        visitSpace(binary.getPrefix(), Space.Location.BINARY, p);
        visit(binary.getLeft(), p);
        StringBuilder acc = getPrinter();
        visitSpace(binary.getPadding().getOperator().getBefore(), Space.Location.BINARY_OPERATOR, p);
        switch(binary.getOperator()) {
            case Addition:
                acc.append('+');
                break;
            case Subtraction:
                acc.append('-');
                break;
            case Multiplication:
                acc.append('*');
                break;
            case Division:
                acc.append('/');
                break;
            case Modulo:
                acc.append('%');
                break;
            case LessThan:
                acc.append('<');
                break;
            case GreaterThan:
                acc.append('>');
                break;
            case LessThanOrEqual:
                acc.append("<=");
                break;
            case GreaterThanOrEqual:
                acc.append(">=");
                break;
            case Equal:
                acc.append("==");
                break;
            case NotEqual:
                acc.append("!=");
                break;
            case Or:
                acc.append("||");
                break;
            case And:
                acc.append("&&");
                break;
        }
        visit(binary.getRight(), p);
        return binary;
    }

    @Override
    public Hcl visitBlock(Hcl.Block block, P p) {
        visitSpace(block.getPrefix(), Space.Location.BLOCK, p);
        visit(block.getType(), p);
        visit(block.getLabels(), p);
        StringBuilder acc = getPrinter();
        visitSpace(block.getOpen(), Space.Location.BLOCK_OPEN, p);
        acc.append('{');
        visit(block.getBody(), p);
        visitSpace(block.getClose(), Space.Location.BLOCK_CLOSE, p);
        acc.append('}');
        return block;
    }

    @Override
    public Hcl visitConditional(Hcl.Conditional conditional, P p) {
        visitSpace(conditional.getPrefix(), Space.Location.CONDITIONAL, p);
        visit(conditional.getCondition(), p);
        visitLeftPadded("?", conditional.getPadding().getTruePart(), HclLeftPadded.Location.CONDITIONAL_TRUE, p);
        visitLeftPadded(":", conditional.getPadding().getFalsePart(), HclLeftPadded.Location.CONDITIONAL_FALSE, p);
        return conditional;
    }

    @Override
    public Hcl visitConfigFile(Hcl.ConfigFile configFile, P p) {
        visitSpace(configFile.getPrefix(), Space.Location.CONFIG_FILE, p);
        visit(configFile.getBody(), p);
        return configFile;
    }

    @Override
    public Hcl visitForIntro(Hcl.ForIntro forIntro, P p) {
        visitSpace(forIntro.getPrefix(), Space.Location.FOR_INTRO, p);
        visitContainer("for", forIntro.getPadding().getVariables(), HclContainer.Location.FOR_VARIABLES,
                ",", "in", p);
        visit(forIntro.getIn(), p);
        return forIntro;
    }

    @Override
    public Hcl visitForObject(Hcl.ForObject forObject, P p) {
        visitSpace(forObject.getPrefix(), Space.Location.FOR_OBJECT, p);
        StringBuilder acc = getPrinter();
        acc.append("{");
        visit(forObject.getIntro(), p);
        visitLeftPadded(":", forObject.getPadding().getUpdateName(), HclLeftPadded.Location.FOR_UPDATE, p);
        visitLeftPadded("=>", forObject.getPadding().getUpdateValue(), HclLeftPadded.Location.FOR_UPDATE_VALUE, p);
        if(forObject.getEllipsis() != null) {
            visitSpace(forObject.getEllipsis().getPrefix(), Space.Location.FOR_UPDATE_VALUE_ELLIPSIS, p);
            acc.append("...");
        }
        if (forObject.getPadding().getCondition() != null) {
            visitLeftPadded("if", forObject.getPadding().getCondition(), HclLeftPadded.Location.FOR_CONDITION, p);
        }
        visitSpace(forObject.getEnd(), Space.Location.FOR_OBJECT_SUFFIX, p);
        acc.append("}");
        return forObject;
    }

    @Override
    public Hcl visitForTuple(Hcl.ForTuple forTuple, P p) {
        visitSpace(forTuple.getPrefix(), Space.Location.FOR_TUPLE, p);
        StringBuilder acc = getPrinter();
        acc.append("[");
        visit(forTuple.getIntro(), p);
        visitLeftPadded(":", forTuple.getPadding().getUpdate(), HclLeftPadded.Location.FOR_UPDATE, p);
        if (forTuple.getPadding().getCondition() != null) {
            visitLeftPadded("if", forTuple.getPadding().getCondition(), HclLeftPadded.Location.FOR_CONDITION, p);
        }
        visitSpace(forTuple.getEnd(), Space.Location.FOR_TUPLE_SUFFIX, p);
        acc.append("]");
        return forTuple;
    }

    @Override
    public Hcl visitFunctionCall(Hcl.FunctionCall functionCall, P p) {
        visitSpace(functionCall.getPrefix(), Space.Location.FUNCTION_CALL, p);
        visit(functionCall.getName(), p);
        visitContainer("(", functionCall.getPadding().getArguments(), HclContainer.Location.FUNCTION_CALL_ARGUMENTS,
                ",", ")", p);
        return functionCall;
    }

    @Override
    public Hcl visitHeredocTemplate(Hcl.HeredocTemplate heredocTemplate, P p) {
        visitSpace(heredocTemplate.getPrefix(), Space.Location.HEREDOC, p);
        StringBuilder acc = getPrinter();
        acc.append(heredocTemplate.getArrow());
        visit(heredocTemplate.getDelimiter(), p);
        visit(heredocTemplate.getExpressions(), p);
        visitSpace(heredocTemplate.getEnd(), Space.Location.HEREDOC_END, p);
        acc.append(heredocTemplate.getDelimiter().getName());
        return heredocTemplate;
    }

    @Override
    public Hcl visitIdentifier(Hcl.Identifier identifier, P p) {
        visitSpace(identifier.getPrefix(), Space.Location.IDENTIFIER, p);
        StringBuilder acc = getPrinter();
        acc.append(identifier.getName());
        return identifier;
    }

    @Override
    public Hcl visitIndex(Hcl.Index index, P p) {
        visitSpace(index.getPrefix(), Space.Location.INDEX, p);
        visit(index.getIndexed(), p);
        visit(index.getPosition(), p);
        return index;
    }

    @Override
    public Hcl visitIndexPosition(Hcl.Index.Position indexPosition, P p) {
        visitSpace(indexPosition.getPrefix(), Space.Location.INDEX_POSITION, p);
        StringBuilder acc = getPrinter();
        acc.append("[");
        visitMarkers(indexPosition.getMarkers(), p);
        visitRightPadded(indexPosition.getPadding().getPosition(), HclRightPadded.Location.INDEX_POSITION, p);
        acc.append("]");
        return indexPosition;
    }

    @Override
    public Hcl visitLiteral(Hcl.Literal literal, P p) {
        visitSpace(literal.getPrefix(), Space.Location.LITERAL, p);
        StringBuilder acc = getPrinter();
        acc.append(literal.getValueSource());
        return literal;
    }

    @Override
    public Hcl visitObjectValue(Hcl.ObjectValue objectValue, P p) {
        visitSpace(objectValue.getPrefix(), Space.Location.OBJECT_VALUE, p);
        visitContainer("{", objectValue.getPadding().getAttributes(), HclContainer.Location.OBJECT_VALUE_ATTRIBUTES,
                ",", "}", p);
        return objectValue;
    }

    @Override
    public Hcl visitParentheses(Hcl.Parentheses parentheses, P p) {
        visitSpace(parentheses.getPrefix(), Space.Location.PARENTHETICAL_EXPRESSION, p);
        StringBuilder acc = getPrinter();
        acc.append('(');
        visitRightPadded(parentheses.getPadding().getExpression(), HclRightPadded.Location.PARENTHESES, p);
        acc.append(')');
        return parentheses;
    }

    @Override
    public Hcl visitQuotedTemplate(Hcl.QuotedTemplate template, P p) {
        visitSpace(template.getPrefix(), Space.Location.QUOTED_TEMPLATE, p);
        StringBuilder acc = getPrinter();
        acc.append('"');
        visit(template.getExpressions(), p);
        acc.append('"');
        return template;
    }

    @Override
    public Hcl visitTemplateInterpolation(Hcl.TemplateInterpolation template, P p) {
        visitSpace(template.getPrefix(), Space.Location.TEMPLATE_INTERPOLATION, p);
        StringBuilder acc = getPrinter();
        acc.append("${");
        visit(template.getExpression(), p);
        acc.append('}');
        return template;
    }

    @Override
    public Hcl visitSplat(Hcl.Splat splat, P p) {
        visitSpace(splat.getPrefix(), Space.Location.ATTRIBUTE_ACCESS, p);
        visit(splat.getSelect(), p);
        visit(splat.getOperator(), p);
        return splat;
    }

    @Override
    public Hcl visitSplatOperator(Hcl.Splat.Operator splatOperator, P p) {
        visitSpace(splatOperator.getPrefix(), Space.Location.SPLAT_OPERATOR, p);
        StringBuilder acc = getPrinter();
        if (splatOperator.getType().equals(Hcl.Splat.Operator.Type.Full)) {
            acc.append('[');
        } else {
            acc.append('.');
        }
        visitSpace(splatOperator.getSplat().getElement().getPrefix(), Space.Location.SPLAT_OPERATOR_PREFIX, p);
        acc.append('*');
        if (splatOperator.getType().equals(Hcl.Splat.Operator.Type.Full)) {
            visitSpace(splatOperator.getSplat().getAfter(), Space.Location.SPLAT_OPERATOR_SUFFIX, p);
            acc.append(']');
        }
        return splatOperator;
    }

    @Override
    public Hcl visitTuple(Hcl.Tuple tuple, P p) {
        visitSpace(tuple.getPrefix(), Space.Location.FUNCTION_CALL, p);
        visitContainer("[", tuple.getPadding().getValues(), HclContainer.Location.TUPLE_VALUES,
                ",", "]", p);
        return tuple;
    }

    @Override
    public Hcl visitUnary(Hcl.Unary unary, P p) {
        visitSpace(unary.getPrefix(), Space.Location.UNARY, p);
        StringBuilder acc = getPrinter();
        switch(unary.getOperator()) {
            case Negative:
                acc.append('-');
                break;
            case Not:
                acc.append('!');
                break;
        }
        visit(unary.getExpression(), p);
        return unary;
    }

    @Override
    public Hcl visitVariableExpression(Hcl.VariableExpression variableExpression, P p) {
        visitSpace(variableExpression.getPrefix(), Space.Location.VARIABLE_EXPRESSION, p);
        visit(variableExpression.getName(), p);
        return variableExpression;
    }
}
