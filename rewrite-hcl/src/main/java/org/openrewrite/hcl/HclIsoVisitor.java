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
package org.openrewrite.hcl;

import org.openrewrite.hcl.tree.BodyContent;
import org.openrewrite.hcl.tree.Expression;
import org.openrewrite.hcl.tree.Hcl;

@SuppressWarnings("unused")
public class HclIsoVisitor<P> extends HclVisitor<P> {

    @Override
    public Hcl.Attribute visitAttribute(Hcl.Attribute attribute, P p) {
        return (Hcl.Attribute) super.visitAttribute(attribute, p);
    }

    @Override
    public Hcl.AttributeAccess visitAttributeAccess(Hcl.AttributeAccess attributeAccess, P p) {
        return (Hcl.AttributeAccess) super.visitAttributeAccess(attributeAccess, p);
    }

    @Override
    public Hcl.Binary visitBinary(Hcl.Binary binary, P p) {
        return (Hcl.Binary) super.visitBinary(binary, p);
    }

    @Override
    public Hcl.Block visitBlock(Hcl.Block block, P p) {
        return (Hcl.Block) super.visitBlock(block, p);
    }

    @Override
    public BodyContent visitBodyContent(BodyContent bodyContent, P p) {
        return (BodyContent) super.visitBodyContent(bodyContent, p);
    }

    @Override
    public Hcl.Conditional visitConditional(Hcl.Conditional conditional, P p) {
        return (Hcl.Conditional) super.visitConditional(conditional, p);
    }

    @Override
    public Hcl.ConfigFile visitConfigFile(Hcl.ConfigFile configFile, P p) {
        return (Hcl.ConfigFile) super.visitConfigFile(configFile, p);
    }

    @Override
    public Hcl.Empty visitEmpty(Hcl.Empty empty, P p) {
        return (Hcl.Empty) super.visitEmpty(empty, p);
    }

    @Override
    public Hcl.ForIntro visitForIntro(Hcl.ForIntro forIntro, P p) {
        return (Hcl.ForIntro) super.visitForIntro(forIntro, p);
    }

    @Override
    public Hcl.ForObject visitForObject(Hcl.ForObject forObject, P p) {
        return (Hcl.ForObject) super.visitForObject(forObject, p);
    }

    @Override
    public Hcl.ForTuple visitForTuple(Hcl.ForTuple forTuple, P p) {
        return (Hcl.ForTuple) super.visitForTuple(forTuple, p);
    }

    @Override
    public Hcl.FunctionCall visitFunctionCall(Hcl.FunctionCall functionCall, P p) {
        return (Hcl.FunctionCall) super.visitFunctionCall(functionCall, p);
    }

    @Override
    public Hcl.HeredocTemplate visitHeredocTemplate(Hcl.HeredocTemplate heredocTemplate, P p) {
        return (Hcl.HeredocTemplate) super.visitHeredocTemplate(heredocTemplate, p);
    }

    @Override
    public Hcl.Identifier visitIdentifier(Hcl.Identifier identifier, P p) {
        return (Hcl.Identifier) super.visitIdentifier(identifier, p);
    }

    @Override
    public Hcl.Index visitIndex(Hcl.Index index, P p) {
        return (Hcl.Index) super.visitIndex(index, p);
    }

    @Override
    public Hcl.Index.Position visitIndexPosition(Hcl.Index.Position indexPosition, P p) {
        return (Hcl.Index.Position) super.visitIndexPosition(indexPosition, p);
    }

    @Override
    public Hcl.LegacyIndexAttributeAccess visitLegacyIndexAttribute(Hcl.LegacyIndexAttributeAccess legacyIndexAttributeAccess, P p) {
        return (Hcl.LegacyIndexAttributeAccess) super.visitLegacyIndexAttribute(legacyIndexAttributeAccess, p);
    }

    @Override
    public Hcl.Literal visitLiteral(Hcl.Literal literal, P p) {
        return (Hcl.Literal) super.visitLiteral(literal, p);
    }

    @Override
    public Hcl.ObjectValue visitObjectValue(Hcl.ObjectValue objectValue, P p) {
        return (Hcl.ObjectValue) super.visitObjectValue(objectValue, p);
    }

    @Override
    public Hcl.Parentheses visitParentheses(Hcl.Parentheses parentheses, P p) {
        return (Hcl.Parentheses) super.visitParentheses(parentheses, p);
    }

    @Override
    public Hcl.QuotedTemplate visitQuotedTemplate(Hcl.QuotedTemplate template, P p) {
        return (Hcl.QuotedTemplate) super.visitQuotedTemplate(template, p);
    }

    @Override
    public Hcl.Splat visitSplat(Hcl.Splat splat, P p) {
        return (Hcl.Splat) super.visitSplat(splat, p);
    }

    @Override
    public Hcl.Splat.Operator visitSplatOperator(Hcl.Splat.Operator splatOperator, P p) {
        return (Hcl.Splat.Operator) super.visitSplatOperator(splatOperator, p);
    }

    @Override
    public Hcl.TemplateInterpolation visitTemplateInterpolation(Hcl.TemplateInterpolation template, P p) {
        return (Hcl.TemplateInterpolation) super.visitTemplateInterpolation(template, p);
    }

    @Override
    public Hcl.Tuple visitTuple(Hcl.Tuple tuple, P p) {
        return (Hcl.Tuple) super.visitTuple(tuple, p);
    }

    @Override
    public Hcl.Unary visitUnary(Hcl.Unary unary, P p) {
        return (Hcl.Unary) super.visitUnary(unary, p);
    }

    @Override
    public Hcl.VariableExpression visitVariableExpression(Hcl.VariableExpression variableExpression, P p) {
        return (Hcl.VariableExpression) super.visitVariableExpression(variableExpression, p);
    }

    @Override
    public Expression visitExpression(Expression expression, P p) {
        return (Expression) super.visitExpression(expression, p);
    }
}
