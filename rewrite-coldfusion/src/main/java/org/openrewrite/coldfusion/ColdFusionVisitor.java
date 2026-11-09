/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.coldfusion;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.coldfusion.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;

public class ColdFusionVisitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Cf.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "coldfusion";
    }

    public Space visitSpace(Space space, CfSpace.Location loc, P p) {
        return visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container,
                                                                  CfContainer.Location loc, P p) {
        return super.visitContainer(container, JContainer.Location.LANGUAGE_EXTENSION, p);
    }

    public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, CfLeftPadded.Location loc, P p) {
        return super.visitLeftPadded(left, JLeftPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public J visitCompilationUnit(Cf.CompilationUnit compilationUnit, P p) {
        Cf.CompilationUnit c = compilationUnit;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withStatements(ListUtils.map(c.getPadding().getStatements(), statement ->
                visitRightPadded(statement, JRightPadded.Location.LANGUAGE_EXTENSION, p)));
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
        return c.withType(TypeUtils.asFullyQualified(visitType(c.getType(), p)));
    }

    public J visitText(Cf.Text text, P p) {
        Cf.Text t = text;
        t = t.withPrefix(visitSpace(t.getPrefix(), CfSpace.Location.TEXT_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        return visitStatement(t, p);
    }

    public J visitTag(Cf.Tag tag, P p) {
        Cf.Tag t = tag;
        t = t.withPrefix(visitSpace(t.getPrefix(), CfSpace.Location.TAG_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Statement temp = (Statement) visitStatement(t, p);
        if (!(temp instanceof Cf.Tag)) {
            return temp;
        }
        t = (Cf.Tag) temp;
        t = t.withAttributes(ListUtils.map(t.getAttributes(), a -> (Cf.TagAttribute) visit(a, p)));
        t = t.withExpression(visit(t.getExpression(), p));
        t = t.withBeforeEnd(visitSpace(t.getBeforeEnd(), CfSpace.Location.TAG_BEFORE_END, p));
        t = t.withBody((J.Block) visit(t.getBody(), p));
        Cf.Tag.Closing closing = t.getClosing();
        if (closing != null) {
            t = t.withClosing(closing.withBeforeEnd(visitSpace(closing.getBeforeEnd(),
                    CfSpace.Location.TAG_CLOSING_BEFORE_END, p)));
        }
        return t;
    }

    public J visitTagAttribute(Cf.TagAttribute attribute, P p) {
        Cf.TagAttribute a = attribute;
        a = a.withPrefix(visitSpace(a.getPrefix(), CfSpace.Location.TAG_ATTRIBUTE_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withName((J.Identifier) visitNonNull(a.getName(), p));
        return a.getPadding().withValue(visitLeftPadded(a.getPadding().getValue(),
                CfLeftPadded.Location.TAG_ATTRIBUTE_VALUE, p));
    }

    public J visitInterpolation(Cf.Interpolation interpolation, P p) {
        Cf.Interpolation i = interpolation;
        i = i.withPrefix(visitSpace(i.getPrefix(), CfSpace.Location.INTERPOLATION_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        Expression temp = (Expression) visitExpression(i, p);
        if (!(temp instanceof Cf.Interpolation)) {
            return temp;
        }
        i = (Cf.Interpolation) temp;
        i = i.withTree((Expression) visitNonNull(i.getTree(), p));
        return i.withAfter(visitSpace(i.getAfter(), CfSpace.Location.INTERPOLATION_SUFFIX, p));
    }

    public J visitStringTemplate(Cf.StringTemplate stringTemplate, P p) {
        Cf.StringTemplate s = stringTemplate;
        s = s.withPrefix(visitSpace(s.getPrefix(), CfSpace.Location.STRING_TEMPLATE_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Expression temp = (Expression) visitExpression(s, p);
        if (!(temp instanceof Cf.StringTemplate)) {
            return temp;
        }
        s = (Cf.StringTemplate) temp;
        s = s.withStrings(ListUtils.map(s.getStrings(), str -> visit(str, p)));
        return s.withType(visitType(s.getType(), p));
    }

    public J visitBinary(Cf.Binary binary, P p) {
        Cf.Binary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), CfSpace.Location.BINARY_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof Cf.Binary)) {
            return temp;
        }
        b = (Cf.Binary) temp;
        b = b.withLeft((Expression) visitNonNull(b.getLeft(), p));
        b = b.getPadding().withOperator(visitLeftPadded(b.getPadding().getOperator(), CfLeftPadded.Location.BINARY_OPERATOR, p));
        b = b.withRight((Expression) visitNonNull(b.getRight(), p));
        return b.withType(visitType(b.getType(), p));
    }

    public J visitAssignmentOperation(Cf.AssignmentOperation assignOp, P p) {
        Cf.AssignmentOperation a = assignOp;
        a = a.withPrefix(visitSpace(a.getPrefix(), CfSpace.Location.ASSIGNMENT_OPERATION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Statement temp = (Statement) visitStatement(a, p);
        if (!(temp instanceof Cf.AssignmentOperation)) {
            return temp;
        }
        a = (Cf.AssignmentOperation) temp;
        Expression temp2 = (Expression) visitExpression(a, p);
        if (!(temp2 instanceof Cf.AssignmentOperation)) {
            return temp2;
        }
        a = (Cf.AssignmentOperation) temp2;
        a = a.withVariable((Expression) visitNonNull(a.getVariable(), p));
        a = a.getPadding().withOperator(visitLeftPadded(a.getPadding().getOperator(),
                CfLeftPadded.Location.ASSIGNMENT_OPERATION_OPERATOR, p));
        a = a.withAssignment((Expression) visitNonNull(a.getAssignment(), p));
        return a.withType(visitType(a.getType(), p));
    }

    public J visitStructLiteral(Cf.StructLiteral structLiteral, P p) {
        Cf.StructLiteral s = structLiteral;
        s = s.withPrefix(visitSpace(s.getPrefix(), CfSpace.Location.STRUCT_LITERAL_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Expression temp = (Expression) visitExpression(s, p);
        if (!(temp instanceof Cf.StructLiteral)) {
            return temp;
        }
        s = (Cf.StructLiteral) temp;
        s = s.getPadding().withEntries(visitContainer(s.getPadding().getEntries(), CfContainer.Location.STRUCT_LITERAL_ENTRIES, p));
        return s.withType(visitType(s.getType(), p));
    }

    public J visitKeyValue(Cf.KeyValue keyValue, P p) {
        Cf.KeyValue k = keyValue;
        k = k.withPrefix(visitSpace(k.getPrefix(), CfSpace.Location.KEY_VALUE_PREFIX, p));
        k = k.withMarkers(visitMarkers(k.getMarkers(), p));
        Expression temp = (Expression) visitExpression(k, p);
        if (!(temp instanceof Cf.KeyValue)) {
            return temp;
        }
        k = (Cf.KeyValue) temp;
        k = k.withKey((Expression) visitNonNull(k.getKey(), p));
        k = k.getPadding().withSeparator(visitLeftPadded(k.getPadding().getSeparator(), CfLeftPadded.Location.KEY_VALUE_SEPARATOR, p));
        return k.withValue((Expression) visitNonNull(k.getValue(), p));
    }

    public J visitScriptTag(Cf.ScriptTag scriptTag, P p) {
        Cf.ScriptTag s = scriptTag;
        s = s.withPrefix(visitSpace(s.getPrefix(), CfSpace.Location.SCRIPT_TAG_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Statement temp = (Statement) visitStatement(s, p);
        if (!(temp instanceof Cf.ScriptTag)) {
            return temp;
        }
        s = (Cf.ScriptTag) temp;
        s = s.withName((J.Identifier) visitNonNull(s.getName(), p));
        s = s.withAttributes(ListUtils.map(s.getAttributes(), a -> (Cf.TagAttribute) visit(a, p)));
        s = s.withExpression((Expression) visit(s.getExpression(), p));
        return s.withBody((J.Block) visit(s.getBody(), p));
    }

    public J visitTagCall(Cf.TagCall tagCall, P p) {
        Cf.TagCall t = tagCall;
        t = t.withPrefix(visitSpace(t.getPrefix(), CfSpace.Location.TAG_CALL_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Statement temp = (Statement) visitStatement(t, p);
        if (!(temp instanceof Cf.TagCall)) {
            return temp;
        }
        t = (Cf.TagCall) temp;
        J call = visit(t.getCall(), p);
        if (!(call instanceof J.MethodInvocation)) {
            //noinspection DataFlowIssue
            return call;
        }
        t = t.withCall((J.MethodInvocation) call);
        return t.withBody((J.Block) visitNonNull(t.getBody(), p));
    }

    public J visitClosure(Cf.Closure closure, P p) {
        Cf.Closure c = closure;
        c = c.withPrefix(visitSpace(c.getPrefix(), CfSpace.Location.CLOSURE_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        Expression temp = (Expression) visitExpression(c, p);
        if (!(temp instanceof Cf.Closure)) {
            return temp;
        }
        c = (Cf.Closure) temp;
        J lambda = visit(c.getLambda(), p);
        if (!(lambda instanceof J.Lambda)) {
            //noinspection DataFlowIssue
            return lambda;
        }
        c = c.withLambda((J.Lambda) lambda);
        return c.withAttributes(ListUtils.map(c.getAttributes(), a -> (J.Annotation) visit(a, p)));
    }

    public J visitTypeLiteral(Cf.TypeLiteral typeLiteral, P p) {
        Cf.TypeLiteral t = typeLiteral;
        t = t.withPrefix(visitSpace(t.getPrefix(), CfSpace.Location.TYPE_LITERAL_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof Cf.TypeLiteral)) {
            return temp;
        }
        t = (Cf.TypeLiteral) temp;
        t = t.withName((Expression) visitNonNull(t.getName(), p));
        return t.withType(visitType(t.getType(), p));
    }

    public J visitInvocation(Cf.Invocation invocation, P p) {
        Cf.Invocation i = invocation;
        i = i.withPrefix(visitSpace(i.getPrefix(), CfSpace.Location.INVOCATION_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        Statement temp = (Statement) visitStatement(i, p);
        if (!(temp instanceof Cf.Invocation)) {
            return temp;
        }
        i = (Cf.Invocation) temp;
        Expression temp2 = (Expression) visitExpression(i, p);
        if (!(temp2 instanceof Cf.Invocation)) {
            return temp2;
        }
        i = (Cf.Invocation) temp2;
        i = i.withTarget((Expression) visitNonNull(i.getTarget(), p));
        i = i.getPadding().withArguments(visitContainer(i.getPadding().getArguments(), CfContainer.Location.INVOCATION_ARGUMENTS, p));
        return i.withType(visitType(i.getType(), p));
    }

    public J visitExpressionStatement(Cf.ExpressionStatement statement, P p) {
        Cf.ExpressionStatement s = statement;
        J expression = visit(s.getExpression(), p);
        if (!(expression instanceof Expression)) {
            //noinspection DataFlowIssue
            return expression;
        }
        return s.withExpression((Expression) expression);
    }
}
