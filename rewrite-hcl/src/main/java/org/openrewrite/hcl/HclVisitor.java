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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.hcl.format.AutoFormatVisitor;
import org.openrewrite.hcl.tree.*;
import org.openrewrite.internal.ListUtils;

import java.util.List;

@SuppressWarnings("unused")
public class HclVisitor<P> extends TreeVisitor<Hcl, P> {

    public <H extends Hcl> H autoFormat(H h, P p) {
        return autoFormat(h, p, getCursor());
    }

    public <H extends Hcl> H autoFormat(H h, P p, Cursor cursor) {
        return autoFormat(h, null, p, cursor);
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public <H extends Hcl> H autoFormat(H h, @Nullable Hcl stopAfter, P p, Cursor cursor) {
        return (H) new AutoFormatVisitor<>(stopAfter).visit(h, p, cursor);
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Hcl.ConfigFile;
    }

    @Override
    public String getLanguage() {
        return "HCL";
    }

    public Hcl visitAttribute(Hcl.Attribute attribute, P p) {
        Hcl.Attribute a = attribute;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ATTRIBUTE, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = (Hcl.Attribute) visitBodyContent(a, p);
        a = a.withName((Expression) visit(a.getName(), p));
        visitSpace(a.getPadding().getType().getBefore(), Space.Location.ATTRIBUTE_ASSIGNMENT, p);
        return a.withValue((Expression) visit(a.getValue(), p));
    }

    public Hcl visitAttributeAccess(Hcl.AttributeAccess attributeAccess, P p) {
        Hcl.AttributeAccess a = attributeAccess;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ATTRIBUTE_ACCESS, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof Hcl.AttributeAccess)) {
            return temp;
        } else {
            a = (Hcl.AttributeAccess) temp;
        }
        a = a.withAttribute((Expression) visit(a.getAttribute(), p));
        return a.getPadding().withName(visitLeftPadded(a.getPadding().getName(), HclLeftPadded.Location.ATTRIBUTE_ACCESS_NAME, p));
    }

    public Hcl visitLegacyIndexAttribute(Hcl.LegacyIndexAttributeAccess legacyIndexAttributeAccess, P p) {
        Hcl.LegacyIndexAttributeAccess li = legacyIndexAttributeAccess;
        li = li.withPrefix(visitSpace(li.getPrefix(), Space.Location.LEGACY_INDEX_ATTRIBUTE_ACCESS, p));
        li = li.withMarkers(visitMarkers(li.getMarkers(), p));
        Expression temp = (Expression) visitExpression(li, p);
        if (!(temp instanceof Hcl.LegacyIndexAttributeAccess)) {
            return temp;
        } else {
            li = (Hcl.LegacyIndexAttributeAccess) temp;
        }
        li = li.getPadding().withBase(
                visitRightPadded(li.getPadding().getBase(), HclRightPadded.Location.LEGACY_INDEX_ATTRIBUTE_ACCESS_BASE, p));
        return li.withIndex((Hcl.Literal) visitLiteral(li.getIndex(), p));
    }


    public Hcl visitBinary(Hcl.Binary binary, P p) {
        Hcl.Binary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BINARY, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof Hcl.Binary)) {
            return temp;
        } else {
            b = (Hcl.Binary) temp;
        }
        b = b.withLeft((Expression) visit(b.getLeft(), p));
        b = b.getPadding().withOperator(b.getPadding().getOperator().withBefore(visitSpace(b.getPadding().getOperator().getBefore(), Space.Location.BINARY_OPERATOR, p)));
        return b.withRight((Expression) visit(b.getRight(), p));
    }

    public Hcl visitBlock(Hcl.Block block, P p) {
        Hcl.Block b = block;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BLOCK, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitBodyContent(b, p);
        if (!(temp instanceof Hcl.Block)) {
            return temp;
        } else {
            b = (Hcl.Block) temp;
        }
        b = b.withType((Hcl.Identifier) visit(b.getType(), p));
        b = b.withLabels(ListUtils.map(b.getLabels(), l -> (Label) visit(l, p)));
        b = b.withOpen(visitSpace(b.getOpen(), Space.Location.BLOCK_OPEN, p));
        b = b.withBody(ListUtils.map(b.getBody(), bc -> (BodyContent) visit(bc, p)));
        return b.withEnd(visitSpace(b.getEnd(), Space.Location.BLOCK_CLOSE, p));
    }

    public Hcl visitBodyContent(BodyContent bodyContent, P p) {
        return bodyContent;
    }

    public Hcl visitConditional(Hcl.Conditional conditional, P p) {
        Hcl.Conditional c = conditional;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CONDITIONAL, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        Expression temp = (Expression) visitExpression(c, p);
        if (!(temp instanceof Hcl.Conditional)) {
            return temp;
        } else {
            c = (Hcl.Conditional) temp;
        }
        c = c.withCondition((Expression) visit(c.getCondition(), p));
        c = c.getPadding().withTruePart(visitLeftPadded(c.getPadding().getTruePart(), HclLeftPadded.Location.CONDITIONAL_TRUE, p));
        return c.getPadding().withFalsePart(visitLeftPadded(c.getPadding().getFalsePart(), HclLeftPadded.Location.CONDITIONAL_FALSE, p));
    }

    public Hcl visitConfigFile(Hcl.ConfigFile configFile, P p) {
        Hcl.ConfigFile c = configFile;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CONFIG_FILE, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withBody(ListUtils.map(c.getBody(), bc -> (BodyContent) visit(bc, p)));
        return c.withEof(visitSpace(c.getEof(), Space.Location.CONFIG_FILE_EOF, p));
    }

    public Hcl visitEmpty(Hcl.Empty empty, P p) {
        Hcl.Empty e = empty;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.EMPTY, p));
        return e.withMarkers(visitMarkers(e.getMarkers(), p));
    }

    public Hcl visitForIntro(Hcl.ForIntro forIntro, P p) {
        Hcl.ForIntro f = forIntro;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FOR_INTRO, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.getPadding().withVariables(visitContainer(f.getPadding().getVariables(), HclContainer.Location.FOR_VARIABLES, p));
        return f.withIn((Expression) visit(f.getIn(), p));
    }

    public Hcl visitForObject(Hcl.ForObject forObject, P p) {
        Hcl.ForObject f = forObject;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.OBJECT_VALUE, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Expression temp = (Expression) visitExpression(f, p);
        if (!(temp instanceof Hcl.ForObject)) {
            return temp;
        } else {
            f = (Hcl.ForObject) temp;
        }
        f = f.withIntro((Hcl.ForIntro) visit(f.getIntro(), p));
        f = f.getPadding().withUpdateName(visitLeftPadded(f.getPadding().getUpdateName(), HclLeftPadded.Location.FOR_UPDATE, p));
        f = f.getPadding().withUpdateValue(visitLeftPadded(f.getPadding().getUpdateValue(), HclLeftPadded.Location.FOR_UPDATE_VALUE, p));
        if (f.getPadding().getCondition() != null) {
            f = f.getPadding().withCondition(visitLeftPadded(f.getPadding().getCondition(), HclLeftPadded.Location.FOR_CONDITION, p));
        }
        visitSpace(f.getEnd(), Space.Location.FOR_TUPLE_SUFFIX, p);
        return f;
    }

    public Hcl visitForTuple(Hcl.ForTuple forTuple, P p) {
        Hcl.ForTuple f = forTuple;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FOR_TUPLE, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Expression temp = (Expression) visitExpression(f, p);
        if (!(temp instanceof Hcl.ForTuple)) {
            return temp;
        } else {
            f = (Hcl.ForTuple) temp;
        }
        f = f.withIntro((Hcl.ForIntro) visit(f.getIntro(), p));
        f = f.getPadding().withUpdate(visitLeftPadded(f.getPadding().getUpdate(), HclLeftPadded.Location.FOR_UPDATE, p));
        if (f.getPadding().getCondition() != null) {
            f = f.getPadding().withCondition(visitLeftPadded(f.getPadding().getCondition(), HclLeftPadded.Location.FOR_CONDITION, p));
        }
        visitSpace(f.getEnd(), Space.Location.FOR_TUPLE_SUFFIX, p);
        return f;
    }

    public Hcl visitFunctionCall(Hcl.FunctionCall functionCall, P p) {
        Hcl.FunctionCall f = functionCall;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FUNCTION_CALL, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Expression temp = (Expression) visitExpression(f, p);
        if (!(temp instanceof Hcl.FunctionCall)) {
            return temp;
        } else {
            f = (Hcl.FunctionCall) temp;
        }
        f = f.withName((Hcl.Identifier) visit(f.getName(), p));
        return f.getPadding().withArguments(visitContainer(f.getPadding().getArguments(), HclContainer.Location.FUNCTION_CALL_ARGUMENTS, p));
    }

    public Hcl visitHeredocTemplate(Hcl.HeredocTemplate heredocTemplate, P p) {
        Hcl.HeredocTemplate h = heredocTemplate;
        h = h.withPrefix(visitSpace(h.getPrefix(), Space.Location.HEREDOC, p));
        h = h.withMarkers(visitMarkers(h.getMarkers(), p));
        Expression temp = (Expression) visitExpression(h, p);
        if (!(temp instanceof Hcl.HeredocTemplate)) {
            return temp;
        } else {
            h = (Hcl.HeredocTemplate) temp;
        }
        h = h.withDelimiter((Hcl.Identifier) visit(h.getDelimiter(), p));
        h = h.withExpressions(ListUtils.map(h.getExpressions(), e -> (Expression) visit(e, p)));
        return h.withEnd(visitSpace(h.getEnd(), Space.Location.HEREDOC_END, p));
    }

    public Hcl visitIdentifier(Hcl.Identifier identifier, P p) {
        Hcl.Identifier i = identifier;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IDENTIFIER, p));
        return i.withMarkers(visitMarkers(i.getMarkers(), p));
    }

    public Hcl visitIndex(Hcl.Index index, P p) {
        Hcl.Index i = index;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.INDEX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withIndexed((Expression) visit(i.getIndexed(), p));
        return i.withPosition((Hcl.Index.Position) visit(i.getPosition(), p));
    }

    public Hcl visitIndexPosition(Hcl.Index.Position indexPosition, P p) {
        Hcl.Index.Position i = indexPosition;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.INDEX_POSITION, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        return i.getPadding().withPosition(visitRightPadded(i.getPadding().getPosition(), HclRightPadded.Location.INDEX_POSITION, p));
    }

    public Hcl visitLiteral(Hcl.Literal literal, P p) {
        Hcl.Literal l = literal;
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LITERAL, p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        Expression temp = (Expression) visitExpression(l, p);
        if (!(temp instanceof Hcl.Literal)) {
            return temp;
        } else {
            l = (Hcl.Literal) temp;
        }
        return l;
    }

    public Hcl visitObjectValue(Hcl.ObjectValue objectValue, P p) {
        Hcl.ObjectValue o = objectValue;
        o = o.withPrefix(visitSpace(o.getPrefix(), Space.Location.OBJECT_VALUE, p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        Expression temp = (Expression) visitExpression(o, p);
        if (!(temp instanceof Hcl.ObjectValue)) {
            return temp;
        } else {
            o = (Hcl.ObjectValue) temp;
        }
        return o.getPadding().withAttributes(visitContainer(o.getPadding().getAttributes(), HclContainer.Location.OBJECT_VALUE_ATTRIBUTES, p));
    }

    public Hcl visitParentheses(Hcl.Parentheses parentheses, P p) {
        Hcl.Parentheses pa = parentheses;
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), Space.Location.ONE_LINE_BLOCK, p));
        pa = pa.withMarkers(visitMarkers(pa.getMarkers(), p));
        Expression temp = (Expression) visitExpression(pa, p);
        if (!(temp instanceof Hcl.Parentheses)) {
            return temp;
        } else {
            pa = (Hcl.Parentheses) temp;
        }
        return pa.getPadding().withExpression(visitRightPadded(pa.getPadding().getExpression(), HclRightPadded.Location.PARENTHESES, p));
    }

    public Hcl visitQuotedTemplate(Hcl.QuotedTemplate template, P p) {
        Hcl.QuotedTemplate t = template;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.QUOTED_TEMPLATE, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof Hcl.QuotedTemplate)) {
            return temp;
        } else {
            t = (Hcl.QuotedTemplate) temp;
        }
        return t.withExpressions(ListUtils.map(t.getExpressions(), e -> (Expression) visit(e, p)));
    }

    public Hcl visitSplat(Hcl.Splat splat, P p) {
        Hcl.Splat s = splat;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.SPLAT, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Expression temp = (Expression) visitExpression(s, p);
        if (!(temp instanceof Hcl.Splat)) {
            return temp;
        } else {
            s = (Hcl.Splat) temp;
        }
        s = s.withSelect((Expression) visit(s.getSelect(), p));
        return s.withOperator((Hcl.Splat.Operator) visit(s.getOperator(), p));
    }

    public Hcl visitSplatOperator(Hcl.Splat.Operator splatOperator, P p) {
        Hcl.Splat.Operator s = splatOperator;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.SPLAT, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        return s.withSplat(visitRightPadded(s.getSplat(), HclRightPadded.Location.SPLAT_OPERATOR, p));
    }

    public Hcl visitTemplateInterpolation(Hcl.TemplateInterpolation template, P p) {
        Hcl.TemplateInterpolation t = template;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TEMPLATE_INTERPOLATION, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof Hcl.TemplateInterpolation)) {
            return temp;
        } else {
            t = (Hcl.TemplateInterpolation) temp;
        }
        return t.withExpression((Expression) visit(t.getExpression(), p));
    }

    public Hcl visitTuple(Hcl.Tuple tuple, P p) {
        Hcl.Tuple t = tuple;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TUPLE, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof Hcl.Tuple)) {
            return temp;
        } else {
            t = (Hcl.Tuple) temp;
        }
        return t.getPadding().withValues(visitContainer(t.getPadding().getValues(), HclContainer.Location.TUPLE_VALUES, p));
    }

    public Hcl visitUnary(Hcl.Unary unary, P p) {
        Hcl.Unary u = unary;
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.UNARY, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        Expression temp = (Expression) visitExpression(u, p);
        if (!(temp instanceof Hcl.Unary)) {
            return temp;
        } else {
            u = (Hcl.Unary) temp;
        }
        return u.withExpression((Expression) visit(u.getExpression(), p));
    }

    public Hcl visitVariableExpression(Hcl.VariableExpression variableExpression, P p) {
        Hcl.VariableExpression v = variableExpression;
        v = v.withPrefix(visitSpace(v.getPrefix(), Space.Location.TEMPLATE_INTERPOLATION, p));
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        Expression temp = (Expression) visitExpression(v, p);
        if (!(temp instanceof Hcl.VariableExpression)) {
            return temp;
        } else {
            v = (Hcl.VariableExpression) temp;
        }
        return v.withName((Hcl.Identifier) visit(v.getName(), p));
    }

    public Hcl visitExpression(Expression expression, P p) {
        return expression;
    }

    public <T> @Nullable HclLeftPadded<T> visitLeftPadded(HclLeftPadded<T> left, HclLeftPadded.Location loc, P p) {
        setCursor(new Cursor(getCursor(), left));

        Space before = visitSpace(left.getBefore(), loc.getBeforeLocation(), p);
        T t = left.getElement();

        if (t instanceof Hcl) {
            //noinspection unchecked
            t = visitAndCast((Hcl) left.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }

        return (before == left.getBefore() && t == left.getElement()) ? left : new HclLeftPadded<>(before, t, left.getMarkers());
    }

    public <T> @Nullable HclRightPadded<T> visitRightPadded(@Nullable HclRightPadded<T> right, HclRightPadded.Location loc, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        if (t instanceof Hcl) {
            //noinspection unchecked
            t = visitAndCast((Hcl) right.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }
        Space after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
        return (after == right.getAfter() && t == right.getElement()) ? right : new HclRightPadded<>(t, after, right.getMarkers());
    }

    public <H extends Hcl> HclContainer<H> visitContainer(HclContainer<H> container,
                                                          HclContainer.Location loc, P p) {
        setCursor(new Cursor(getCursor(), container));

        Space before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
        List<HclRightPadded<H>> js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));

        setCursor(getCursor().getParent());

        return js == container.getPadding().getElements() && before == container.getBefore() ?
                container :
                HclContainer.build(before, js, container.getMarkers());
    }

    public Space visitSpace(Space space, Space.Location loc, P p) {
        return space;
    }
}
