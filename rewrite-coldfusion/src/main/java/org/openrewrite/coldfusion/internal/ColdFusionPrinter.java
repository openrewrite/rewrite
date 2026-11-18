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
package org.openrewrite.coldfusion.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.ColdFusionVisitor;
import org.openrewrite.coldfusion.marker.*;
import org.openrewrite.coldfusion.tree.*;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Semicolon;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

import static java.util.Collections.singletonList;

public class ColdFusionPrinter<P> extends ColdFusionVisitor<PrintOutputCapture<P>> {
    private static final UnaryOperator<String> MARKER_WRAPPER =
            out -> "<!---~~" + out + (out.isEmpty() ? "" : "~~") + ">--->";

    private final ColdFusionJavaPrinter delegate = new ColdFusionJavaPrinter();

    @Override
    public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
        if (!(tree instanceof Cf)) {
            Cursor delegateCursor = delegate.getCursor();
            try {
                return delegate.visit(tree, p, getCursor());
            } finally {
                delegate.setCursor(delegateCursor);
            }
        }
        return super.visit(tree, p);
    }

    @Override
    public J visitCompilationUnit(Cf.CompilationUnit compilationUnit, PrintOutputCapture<P> p) {
        if (compilationUnit.isCharsetBomMarked()) {
            // here rather than where any source file's printing restores it, which leaves it out of a file that is empty
            // or begins with a U+FEFF of its own
            p.append('﻿');
        }
        beforeSyntax(compilationUnit.getPrefix(), compilationUnit.getMarkers(), Space.Location.COMPILATION_UNIT_PREFIX, p);
        for (JRightPadded<Statement> statement : compilationUnit.getPadding().getStatements()) {
            visit(statement.getElement(), p);
            visitSpace(statement.getAfter(), Space.Location.LANGUAGE_EXTENSION, p);
            visitMarkers(statement.getMarkers(), p);
        }
        visitSpace(compilationUnit.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        afterSyntax(compilationUnit.getMarkers(), p);
        return compilationUnit;
    }

    @Override
    public J visitText(Cf.Text text, PrintOutputCapture<P> p) {
        beforeSyntax(text, CfSpace.Location.TEXT_PREFIX, p);
        p.append(text.getText());
        afterSyntax(text.getMarkers(), p);
        return text;
    }

    @Override
    public J visitTag(Cf.Tag tag, PrintOutputCapture<P> p) {
        beforeSyntax(tag, CfSpace.Location.TAG_PREFIX, p);
        p.append('<').append(tag.getName());
        for (Cf.TagAttribute attribute : tag.getAttributes()) {
            visit(attribute, p);
        }
        visit(tag.getExpression(), p);
        visitSpace(tag.getBeforeEnd(), CfSpace.Location.TAG_BEFORE_END, p);
        p.append(tag.isSelfClosing() ? "/>" : ">");
        visit(tag.getBody(), p);
        Cf.Tag.Closing closing = tag.getClosing();
        if (closing != null) {
            p.append("</").append(closing.getName());
            visitSpace(closing.getBeforeEnd(), CfSpace.Location.TAG_CLOSING_BEFORE_END, p);
            p.append('>');
        }
        afterSyntax(tag.getMarkers(), p);
        return tag;
    }

    @Override
    public J visitTagAttribute(Cf.TagAttribute attribute, PrintOutputCapture<P> p) {
        beforeSyntax(attribute, CfSpace.Location.TAG_ATTRIBUTE_PREFIX, p);
        visit(attribute.getName(), p);
        JLeftPadded<Expression> value = attribute.getPadding().getValue();
        if (value != null) {
            visitSpace(value.getBefore(), CfSpace.Location.TAG_ATTRIBUTE_VALUE, p);
            p.append('=');
            visit(value.getElement(), p);
        }
        afterSyntax(attribute.getMarkers(), p);
        return attribute;
    }

    @Override
    public J visitInterpolation(Cf.Interpolation interpolation, PrintOutputCapture<P> p) {
        beforeSyntax(interpolation, CfSpace.Location.INTERPOLATION_PREFIX, p);
        p.append('#');
        visit(interpolation.getTree(), p);
        visitSpace(interpolation.getAfter(), CfSpace.Location.INTERPOLATION_SUFFIX, p);
        p.append('#');
        afterSyntax(interpolation.getMarkers(), p);
        return interpolation;
    }

    @Override
    public J visitStringTemplate(Cf.StringTemplate stringTemplate, PrintOutputCapture<P> p) {
        beforeSyntax(stringTemplate, CfSpace.Location.STRING_TEMPLATE_PREFIX, p);
        p.append(stringTemplate.getDelimiter());
        for (J string : stringTemplate.getStrings()) {
            visit(string, p);
        }
        p.append(stringTemplate.getDelimiter());
        afterSyntax(stringTemplate.getMarkers(), p);
        return stringTemplate;
    }

    @Override
    public J visitBinary(Cf.Binary binary, PrintOutputCapture<P> p) {
        beforeSyntax(binary, CfSpace.Location.BINARY_PREFIX, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), CfSpace.Location.BINARY_OPERATOR, p);
        p.append(spelling(binary.getMarkers(), canonical(binary.getOperator())));
        visit(binary.getRight(), p);
        afterSyntax(binary.getMarkers(), p);
        return binary;
    }

    static String canonical(Cf.Binary.Type type) {
        switch (type) {
            case Concat:
                return "&";
            case Power:
                return "^";
            case IntegerDivision:
                return "\\";
            case Contains:
                return "CONTAINS";
            case DoesNotContain:
                return "DOES NOT CONTAIN";
            case Equivalence:
                return "EQV";
            case Implication:
                return "IMP";
            case Xor:
                return "XOR";
            case Elvis:
                return "?:";
            case Identical:
                return "===";
            case NotIdentical:
            default:
                return "!==";
        }
    }

    @Override
    public J visitAssignmentOperation(Cf.AssignmentOperation assignOp, PrintOutputCapture<P> p) {
        beforeSyntax(assignOp, CfSpace.Location.ASSIGNMENT_OPERATION_PREFIX, p);
        assignOp.getMarkers().findFirst(VarKeyword.class).ifPresent(var -> p.append(var.getKeyword()));
        visit(assignOp.getVariable(), p);
        visitSpace(assignOp.getPadding().getOperator().getBefore(), CfSpace.Location.ASSIGNMENT_OPERATION_OPERATOR, p);
        p.append("&=");
        visit(assignOp.getAssignment(), p);
        afterSyntax(assignOp.getMarkers(), p);
        return assignOp;
    }

    @Override
    public J visitStructLiteral(Cf.StructLiteral structLiteral, PrintOutputCapture<P> p) {
        beforeSyntax(structLiteral, CfSpace.Location.STRUCT_LITERAL_PREFIX, p);
        visitContainer(structLiteral.isOrdered() ? "[" : "{", structLiteral.getPadding().getEntries(),
                CfContainer.Location.STRUCT_LITERAL_ENTRIES, ",", structLiteral.isOrdered() ? "]" : "}", p);
        afterSyntax(structLiteral.getMarkers(), p);
        return structLiteral;
    }

    @Override
    public J visitKeyValue(Cf.KeyValue keyValue, PrintOutputCapture<P> p) {
        beforeSyntax(keyValue, CfSpace.Location.KEY_VALUE_PREFIX, p);
        visit(keyValue.getKey(), p);
        visitSpace(keyValue.getPadding().getSeparator().getBefore(), CfSpace.Location.KEY_VALUE_SEPARATOR, p);
        p.append(keyValue.getSeparator() == Cf.KeyValue.Separator.Colon ? ":" : "=");
        visit(keyValue.getValue(), p);
        afterSyntax(keyValue.getMarkers(), p);
        return keyValue;
    }

    @Override
    public J visitScriptTag(Cf.ScriptTag scriptTag, PrintOutputCapture<P> p) {
        beforeSyntax(scriptTag, CfSpace.Location.SCRIPT_TAG_PREFIX, p);
        visit(scriptTag.getName(), p);
        for (Cf.TagAttribute attribute : scriptTag.getAttributes()) {
            AttributeComma comma = attribute.getMarkers().findFirst(AttributeComma.class).orElse(null);
            if (comma != null) {
                visitSpace(comma.getBefore(), Space.Location.LANGUAGE_EXTENSION, p);
                p.append(',');
            }
            visit(attribute, p);
        }
        visit(scriptTag.getExpression(), p);
        visit(scriptTag.getBody(), p);
        afterSyntax(scriptTag.getMarkers(), p);
        return scriptTag;
    }

    @Override
    public J visitExpressionStatement(Cf.ExpressionStatement statement, PrintOutputCapture<P> p) {
        visit(statement.getExpression(), p);
        return statement;
    }

    @Override
    public J visitTagCall(Cf.TagCall tagCall, PrintOutputCapture<P> p) {
        beforeSyntax(tagCall, CfSpace.Location.TAG_CALL_PREFIX, p);
        visit(tagCall.getCall(), p);
        visit(tagCall.getBody(), p);
        afterSyntax(tagCall.getMarkers(), p);
        return tagCall;
    }

    @Override
    public J visitClosure(Cf.Closure closure, PrintOutputCapture<P> p) {
        beforeSyntax(closure, CfSpace.Location.CLOSURE_PREFIX, p);
        // the lambda prints the attributes between its parameters and its body, see ColdFusionJavaPrinter
        visit(closure.getLambda(), p);
        afterSyntax(closure.getMarkers(), p);
        return closure;
    }

    @Override
    public J visitTypeLiteral(Cf.TypeLiteral typeLiteral, PrintOutputCapture<P> p) {
        beforeSyntax(typeLiteral, CfSpace.Location.TYPE_LITERAL_PREFIX, p);
        visit(typeLiteral.getName(), p);
        afterSyntax(typeLiteral.getMarkers(), p);
        return typeLiteral;
    }

    @Override
    public J visitInvocation(Cf.Invocation invocation, PrintOutputCapture<P> p) {
        beforeSyntax(invocation, CfSpace.Location.INVOCATION_PREFIX, p);
        visit(invocation.getTarget(), p);
        visitContainer("(", invocation.getPadding().getArguments(), CfContainer.Location.INVOCATION_ARGUMENTS, ",", ")", p);
        afterSyntax(invocation.getMarkers(), p);
        return invocation;
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        if (marker instanceof Semicolon) {
            p.append(';');
        } else if (marker instanceof TrailingComma) {
            p.append(',');
            visitSpace(((TrailingComma) marker).getSuffix(), Space.Location.TRAILING_COMMA_SUFFIX, p);
        }
        //noinspection unchecked
        return (M) marker;
    }

    @Override
    public Space visitSpace(Space space, CfSpace.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, loc, p);
    }

    private void visitContainer(String before, JContainer<? extends J> container, CfContainer.Location location,
                                String suffixBetween, String after, PrintOutputCapture<P> p) {
        visitSpace(container.getBefore(), location.getBeforeLocation(), p);
        p.append(before);
        List<? extends JRightPadded<? extends J>> elements = container.getPadding().getElements();
        for (int i = 0; i < elements.size(); i++) {
            JRightPadded<? extends J> element = elements.get(i);
            visit(element.getElement(), p);
            visitSpace(element.getAfter(), location.getElementLocation().getAfterLocation(), p);
            visitMarkers(element.getMarkers(), p);
            if (i < elements.size() - 1 && !element.getMarkers().findFirst(OmitComma.class).isPresent()) {
                p.append(suffixBetween);
            }
        }
        p.append(after);
    }

    private void beforeSyntax(J j, CfSpace.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(j.getPrefix(), j.getMarkers(), loc, p);
    }

    private void beforeSyntax(Space prefix, Markers markers, CfSpace.Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
        visitSpace(prefix, loc, p);
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
    }

    private void beforeSyntax(Space prefix, Markers markers, Space.Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
        visitSpace(prefix, loc, p);
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
    }

    private static String spelling(Markers markers, String canonical) {
        OperatorSpelling spelling = markers.findFirst(OperatorSpelling.class).orElse(null);
        return spelling == null ? canonical : spelling.getText();
    }

    private static String keyword(Markers markers, String canonical) {
        KeywordSpelling spelling = markers.findFirst(KeywordSpelling.class).orElse(null);
        return spelling == null ? canonical : spelling.getText();
    }

    private class ColdFusionJavaPrinter extends JavaPrinter<P> {

        @Override
        public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
            if (tree instanceof Cf) {
                Cursor outerCursor = ColdFusionPrinter.this.getCursor();
                try {
                    return ColdFusionPrinter.this.visit(tree, p, getCursor());
                } finally {
                    ColdFusionPrinter.this.setCursor(outerCursor);
                }
            }
            return super.visit(tree, p);
        }

        /**
         * A semicolon is a {@link Semicolon} marker on the statement's padding, after the space in
         * front of it, since CFML lets most statements leave it off.
         */
        @Override
        protected void visitStatement(@Nullable JRightPadded<Statement> paddedStat, JRightPadded.Location location,
                                      PrintOutputCapture<P> p) {
            if (paddedStat != null) {
                visit(paddedStat.getElement(), p);
                visitSpace(paddedStat.getAfter(), location.getAfterLocation(), p);
                visitMarkers(paddedStat.getMarkers(), p);
            }
        }

        @Override
        public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
            if (marker instanceof Semicolon) {
                p.append(';');
            }
            return super.visitMarker(marker, p);
        }

        @Override
        public J visitModifier(J.Modifier modifier, PrintOutputCapture<P> p) {
            if (modifier.getKeyword() == null) {
                return super.visitModifier(modifier, p);
            }
            visit(modifier.getAnnotations(), p);
            beforeSyntax(modifier, Space.Location.MODIFIER_PREFIX, p);
            p.append(modifier.getKeyword());
            afterSyntax(modifier, p);
            return modifier;
        }

        @Override
        public J visitBinary(J.Binary binary, PrintOutputCapture<P> p) {
            OperatorSpelling spelling = binary.getMarkers().findFirst(OperatorSpelling.class).orElse(null);
            if (spelling == null) {
                return super.visitBinary(binary, p);
            }
            beforeSyntax(binary, Space.Location.BINARY_PREFIX, p);
            visit(binary.getLeft(), p);
            visitSpace(binary.getPadding().getOperator().getBefore(), Space.Location.BINARY_OPERATOR, p);
            p.append(spelling.getText());
            visit(binary.getRight(), p);
            afterSyntax(binary, p);
            return binary;
        }

        @Override
        public J visitUnary(J.Unary unary, PrintOutputCapture<P> p) {
            OperatorSpelling spelling = unary.getMarkers().findFirst(OperatorSpelling.class).orElse(null);
            if (spelling == null || unary.getOperator() != J.Unary.Type.Not) {
                return super.visitUnary(unary, p);
            }
            beforeSyntax(unary, Space.Location.UNARY_PREFIX, p);
            p.append(spelling.getText());
            visit(unary.getExpression(), p);
            afterSyntax(unary, p);
            return unary;
        }

        /**
         * A function. Its attributes are its annotations, which follow its parameters. A function written as a
         * tag prints its name and return type among them, and its parameters, the {@code <cfargument>} tags,
         * before its body.
         */
        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, PrintOutputCapture<P> p) {
            TagSyntax tag = method.getMarkers().findFirst(TagSyntax.class).orElse(null);
            AttributeClause returnTypeClause = clause(method.getMarkers(), "returntype");
            TypeTree returnType = method.getReturnTypeExpression();
            List<Held> held = new ArrayList<>();
            if (returnType != null && (tag != null || returnTypeClause != null)) {
                held.add(held(returnTypeClause, "returntype", () -> visit(returnType, p), p));
            }
            beforeSyntax(method, Space.Location.METHOD_DECLARATION_PREFIX, p);
            if (tag != null) {
                p.append('<').append(tag.getName());
                held.add(held(clause(method.getMarkers(), "name"), "name", () -> visit(method.getName(), p), p));
                visitAttributes(method.getLeadingAnnotations(), held, p);
                visitSpace(tag.getBeforeEnd(), Space.Location.LANGUAGE_EXTENSION, p);
                p.append(tag.isSelfClosing() ? "/>" : ">");
                for (JRightPadded<Statement> parameter : method.getPadding().getParameters().getPadding().getElements()) {
                    visit(parameter.getElement(), p);
                    visitSpace(parameter.getAfter(), Space.Location.METHOD_DECLARATION_PARAMETER_SUFFIX, p);
                }
                visit(method.getBody(), p);
                visitClosing(tag, p);
                afterSyntax(method, p);
                return method;
            }
            List<J.Modifier> modifiers = method.getModifiers();
            int returnTypePosition = method.getMarkers().findFirst(ReturnTypePosition.class)
                    .map(ReturnTypePosition::getPosition).orElse(modifiers.size());
            for (int i = 0; i < modifiers.size(); i++) {
                if (i == returnTypePosition && returnTypeClause == null) {
                    visit(returnType, p);
                }
                visitModifier(modifiers.get(i), p);
            }
            if (returnTypePosition >= modifiers.size() && returnTypeClause == null) {
                visit(returnType, p);
            }
            FunctionKeyword function = method.getMarkers().findFirst(FunctionKeyword.class).orElse(null);
            if (function != null) {
                visitSpace(function.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p);
                p.append(function.getKeyword());
            } else {
                p.append(method.getModifiers().isEmpty() && (returnType == null || returnTypeClause != null) ?
                        "function" : " function");
            }
            visit(method.getName(), p);
            JContainer<Statement> parameters = method.getPadding().getParameters();
            visitSpace(parameters.getBefore(), Space.Location.METHOD_DECLARATION_PARAMETERS, p);
            p.append('(');
            visitParameters(parameters.getPadding().getElements(), JRightPadded.Location.METHOD_DECLARATION_PARAMETER, p);
            p.append(')');
            visitAttributes(method.getLeadingAnnotations(), held, p);
            visit(method.getBody(), p);
            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitLambda(J.Lambda lambda, PrintOutputCapture<P> p) {
            beforeSyntax(lambda, Space.Location.LAMBDA_PREFIX, p);
            FunctionKeyword function = lambda.getMarkers().findFirst(FunctionKeyword.class).orElse(null);
            if (function != null) {
                visitSpace(function.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p);
                p.append(function.getKeyword());
            }
            J.Lambda.Parameters parameters = lambda.getParameters();
            visitSpace(parameters.getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p);
            visitMarkers(parameters.getMarkers(), p);
            if (parameters.isParenthesized()) {
                p.append('(');
                visitParameters(parameters.getPadding().getParameters(), JRightPadded.Location.LAMBDA_PARAM, p);
                p.append(')');
            } else {
                visitParameters(parameters.getPadding().getParameters(), JRightPadded.Location.LAMBDA_PARAM, p);
            }
            if (function == null) {
                visitSpace(lambda.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p);
                p.append("=>");
            }
            Object parent = getCursor().getParentTreeCursor().getValue();
            if (parent instanceof Cf.Closure) {
                for (J.Annotation attribute : ((Cf.Closure) parent).getAttributes()) {
                    visit(attribute, p);
                }
            }
            visit(lambda.getBody(), p);
            afterSyntax(lambda, p);
            return lambda;
        }

        /**
         * An attribute of a declaration, {@code output=false} or a bare {@code secured}.
         */
        @Override
        public J visitAnnotation(J.Annotation annotation, PrintOutputCapture<P> p) {
            beforeSyntax(annotation, Space.Location.ANNOTATION_PREFIX, p);
            visit(annotation.getAnnotationType(), p);
            JContainer<Expression> arguments = annotation.getPadding().getArguments();
            if (arguments != null) {
                visitSpace(arguments.getBefore(), Space.Location.ANNOTATION_ARGUMENTS, p);
                p.append('=');
                for (JRightPadded<Expression> argument : arguments.getPadding().getElements()) {
                    visit(argument.getElement(), p);
                    visitSpace(argument.getAfter(), Space.Location.ANNOTATION_ARGUMENT_SUFFIX, p);
                }
            }
            afterSyntax(annotation, p);
            return annotation;
        }

        /**
         * A variable, a parameter or a property. Its attributes are its annotations, which follow it, as in
         * {@code required string path hint="..."}, with any that name its name or its type among them. One written
         * as a tag prints them all in the tag.
         */
        @Override
        public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
            TagSyntax tag = multiVariable.getMarkers().findFirst(TagSyntax.class).orElse(null);
            AttributeClause typeClause = clause(multiVariable.getMarkers(), "type");
            AttributeClause nameClause = clause(multiVariable.getMarkers(), "name");
            TypeTree type = multiVariable.getTypeExpression();
            List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = multiVariable.getPadding().getVariables();
            List<Held> held = new ArrayList<>();
            beforeSyntax(multiVariable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
            if (tag != null) {
                p.append('<').append(tag.getName());
            } else {
                for (J.Modifier modifier : multiVariable.getModifiers()) {
                    visitModifier(modifier, p);
                }
            }
            if (type != null && (tag != null || typeClause != null)) {
                held.add(held(typeClause, "type", () -> visit(type, p), p));
            } else {
                visit(type, p);
            }
            if (tag != null || nameClause != null) {
                held.add(held(nameClause, "name",
                        () -> visitRightPadded(variables, JRightPadded.Location.NAMED_VARIABLE, ",", p), p));
            } else {
                visitRightPadded(variables, JRightPadded.Location.NAMED_VARIABLE, ",", p);
            }
            visitAttributes(multiVariable.getLeadingAnnotations(), held, p);
            if (tag != null) {
                visitSpace(tag.getBeforeEnd(), Space.Location.LANGUAGE_EXTENSION, p);
                p.append(tag.isSelfClosing() ? "/>" : ">");
                visitClosing(tag, p);
            }
            afterSyntax(multiVariable, p);
            return multiVariable;
        }

        /**
         * A part of a declaration written as an attribute, where among the declaration's attributes it prints.
         */
        private final class Held {
            final int position;
            final Runnable print;

            Held(int position, Runnable print) {
                this.position = position;
                this.print = print;
            }
        }

        private Held held(@Nullable AttributeClause clause, String name, Runnable value, PrintOutputCapture<P> p) {
            return new Held(clause == null ? Integer.MAX_VALUE : clause.getPosition(), () -> visitClause(clause, name, value, p));
        }

        /**
         * A declaration's annotations, with the parts it holds from its other attributes printed where they are
         * written among them. One a recipe added, which has no position, prints last.
         */
        private void visitAttributes(List<J.Annotation> annotations, List<Held> held, PrintOutputCapture<P> p) {
            List<Held> pending = new ArrayList<>(held);
            pending.sort(Comparator.comparingInt(h -> h.position));
            int next = 0;
            for (int position = 0; next < annotations.size() || !pending.isEmpty(); position++) {
                if (!pending.isEmpty() && (pending.get(0).position <= position || next == annotations.size())) {
                    pending.remove(0).print.run();
                } else {
                    visit(annotations.get(next++), p);
                }
            }
        }

        /**
         * An attribute that names a part of a declaration, {@code name="init"}, or one written for a part a recipe
         * added.
         */
        private void visitClause(@Nullable AttributeClause clause, String name, Runnable value, PrintOutputCapture<P> p) {
            visitSpace(clause == null ? Space.SINGLE_SPACE : clause.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p);
            p.append(clause == null ? name : clause.getName());
            visitSpace(clause == null ? Space.EMPTY : clause.getBeforeEquals(), Space.Location.LANGUAGE_EXTENSION, p);
            p.append('=');
            visitSpace(clause == null ? Space.EMPTY : clause.getAfterEquals(), Space.Location.LANGUAGE_EXTENSION, p);
            String quote = clause == null ? "\"" : clause.getQuote();
            p.append(quote);
            value.run();
            p.append(quote);
        }

        private @Nullable AttributeClause clause(Markers markers, String name) {
            for (Marker marker : markers.getMarkers()) {
                if (marker instanceof AttributeClause && ((AttributeClause) marker).isNamed(name)) {
                    return (AttributeClause) marker;
                }
            }
            return null;
        }

        private void visitClosing(TagSyntax tag, PrintOutputCapture<P> p) {
            Cf.Tag.Closing closing = tag.getClosing();
            if (closing != null) {
                p.append("</").append(closing.getName());
                visitSpace(closing.getBeforeEnd(), Space.Location.LANGUAGE_EXTENSION, p);
                p.append('>');
            }
        }

        /**
         * A component. It is named for its file, so a marker on its name prints around its keyword, or its tag's
         * name. Its attributes print in the order they are written, with its {@code extends} and {@code implements}
         * among its annotations.
         */
        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, PrintOutputCapture<P> p) {
            TagSyntax tag = classDecl.getMarkers().findFirst(TagSyntax.class).orElse(null);
            beforeSyntax(classDecl, Space.Location.CLASS_DECLARATION_PREFIX, p);
            if (tag != null) {
                p.append('<');
            }
            for (J.Modifier modifier : classDecl.getModifiers()) {
                visitModifier(modifier, p);
            }
            J.ClassDeclaration.Kind kind = classDecl.getPadding().getKind();
            boolean isInterface = kind.getType() == J.ClassDeclaration.Kind.Type.Interface;
            visitSpace(kind.getPrefix(), Space.Location.CLASS_KIND, p);
            beforeSyntax(Space.EMPTY, classDecl.getName().getMarkers(), null, p);
            p.append(tag != null ? tag.getName() : keyword(kind.getMarkers(), isInterface ? "interface" : "component"));
            afterSyntax(classDecl.getName().getMarkers(), p);

            List<Held> held = new ArrayList<>();
            JLeftPadded<TypeTree> extendings = classDecl.getPadding().getExtends();
            if (extendings != null) {
                held.add(new Held(position(extendings.getMarkers()), () -> visitExtends(extendings, p)));
            }
            JContainer<TypeTree> implementings = classDecl.getPadding().getImplements();
            if (implementings != null) {
                held.add(new Held(position(implementings.getMarkers()), () -> visitImplements(implementings, isInterface, p)));
            }
            visitAttributes(classDecl.getLeadingAnnotations(), held, p);
            if (tag != null) {
                visitSpace(tag.getBeforeEnd(), Space.Location.LANGUAGE_EXTENSION, p);
                p.append(tag.isSelfClosing() ? "/>" : ">");
            }
            visit(classDecl.getBody(), p);
            if (tag != null) {
                visitClosing(tag, p);
            }
            afterSyntax(classDecl, p);
            return classDecl;
        }

        private void visitExtends(JLeftPadded<TypeTree> extendings, PrintOutputCapture<P> p) {
            visitSupertypes(extendings.getBefore(), extendings.getMarkers(),
                    singletonList(new JRightPadded<>(extendings.getElement(), Space.EMPTY, Markers.EMPTY)), "extends", p);
        }

        private void visitImplements(JContainer<TypeTree> implementings, boolean isInterface, PrintOutputCapture<P> p) {
            visitSupertypes(implementings.getBefore(), implementings.getMarkers(), implementings.getPadding().getElements(),
                    isInterface ? "extends" : "implements", p);
        }

        /**
         * Where among a component's attributes its {@code extends} or {@code implements} is written, or last when
         * a recipe added it.
         */
        private int position(Markers markers) {
            return markers.findFirst(AttributeClause.class).map(AttributeClause::getPosition).orElse(Integer.MAX_VALUE);
        }

        /**
         * An {@code extends} or {@code implements} written as an attribute, {@code implements="IFoo, IBar"}.
         */
        private void visitSupertypes(Space before, Markers markers, List<JRightPadded<TypeTree>> names, String name,
                                     PrintOutputCapture<P> p) {
            AttributeClause clause = markers.findFirst(AttributeClause.class).orElse(null);
            String quote = clause == null ? "\"" : clause.getQuote();
            visitSpace(before, Space.Location.LANGUAGE_EXTENSION, p);
            p.append(clause == null ? name : clause.getName());
            visitSpace(clause == null ? Space.EMPTY : clause.getBeforeEquals(), Space.Location.LANGUAGE_EXTENSION, p);
            p.append('=');
            visitSpace(clause == null ? Space.EMPTY : clause.getAfterEquals(), Space.Location.LANGUAGE_EXTENSION, p);
            p.append(quote);
            for (int i = 0; i < names.size(); i++) {
                visit(names.get(i).getElement(), p);
                visitSpace(names.get(i).getAfter(), Space.Location.LANGUAGE_EXTENSION, p);
                if (i < names.size() - 1) {
                    p.append(',');
                }
            }
            p.append(quote);
        }

        @Override
        public J visitIf(J.If iff, PrintOutputCapture<P> p) {
            beforeSyntax(iff, Space.Location.IF_PREFIX, p);
            p.append(keyword(iff.getMarkers(), "if"));
            visit(iff.getIfCondition(), p);
            visitStatement(iff.getPadding().getThenPart(), JRightPadded.Location.IF_THEN, p);
            visit(iff.getElsePart(), p);
            afterSyntax(iff, p);
            return iff;
        }

        @Override
        public J visitElse(J.If.Else anElse, PrintOutputCapture<P> p) {
            beforeSyntax(anElse, Space.Location.ELSE_PREFIX, p);
            p.append(keyword(anElse.getMarkers(), "else"));
            visitStatement(anElse.getPadding().getBody(), JRightPadded.Location.IF_ELSE, p);
            afterSyntax(anElse, p);
            return anElse;
        }

        @Override
        public J visitForLoop(J.ForLoop forLoop, PrintOutputCapture<P> p) {
            beforeSyntax(forLoop, Space.Location.FOR_PREFIX, p);
            p.append(keyword(forLoop.getMarkers(), "for"));
            J.ForLoop.Control ctrl = forLoop.getControl();
            visitSpace(ctrl.getPrefix(), Space.Location.FOR_CONTROL_PREFIX, p);
            p.append('(');
            visitRightPadded(ctrl.getPadding().getInit(), JRightPadded.Location.FOR_INIT, ",", p);
            p.append(';');
            visitRightPadded(ctrl.getPadding().getCondition(), JRightPadded.Location.FOR_CONDITION, ";", p);
            visitRightPadded(ctrl.getPadding().getUpdate(), JRightPadded.Location.FOR_UPDATE, ",", p);
            p.append(')');
            visitStatement(forLoop.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p);
            afterSyntax(forLoop, p);
            return forLoop;
        }

        @Override
        public J visitForEachLoop(J.ForEachLoop forEachLoop, PrintOutputCapture<P> p) {
            beforeSyntax(forEachLoop, Space.Location.FOR_EACH_LOOP_PREFIX, p);
            p.append(keyword(forEachLoop.getMarkers(), "for"));
            J.ForEachLoop.Control ctrl = forEachLoop.getControl();
            visitSpace(ctrl.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, p);
            p.append('(');
            visitRightPadded(ctrl.getPadding().getVariable(), JRightPadded.Location.FOREACH_VARIABLE,
                    keyword(ctrl.getMarkers(), "in"), p);
            visitRightPadded(ctrl.getPadding().getIterable(), JRightPadded.Location.FOREACH_ITERABLE, "", p);
            p.append(')');
            visitStatement(forEachLoop.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p);
            afterSyntax(forEachLoop, p);
            return forEachLoop;
        }

        @Override
        public J visitWhileLoop(J.WhileLoop whileLoop, PrintOutputCapture<P> p) {
            beforeSyntax(whileLoop, Space.Location.WHILE_PREFIX, p);
            p.append(keyword(whileLoop.getMarkers(), "while"));
            visit(whileLoop.getCondition(), p);
            visitStatement(whileLoop.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, p);
            afterSyntax(whileLoop, p);
            return whileLoop;
        }

        @Override
        public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, PrintOutputCapture<P> p) {
            beforeSyntax(doWhileLoop, Space.Location.DO_WHILE_PREFIX, p);
            p.append(keyword(doWhileLoop.getMarkers(), "do"));
            visitStatement(doWhileLoop.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, p);
            JLeftPadded<J.ControlParentheses<Expression>> condition = doWhileLoop.getPadding().getWhileCondition();
            visitLeftPadded(keyword(condition.getMarkers(), "while"), condition, JLeftPadded.Location.WHILE_CONDITION, p);
            afterSyntax(doWhileLoop, p);
            return doWhileLoop;
        }

        @Override
        public J visitSwitch(J.Switch aSwitch, PrintOutputCapture<P> p) {
            beforeSyntax(aSwitch, Space.Location.SWITCH_PREFIX, p);
            p.append(keyword(aSwitch.getMarkers(), "switch"));
            visit(aSwitch.getSelector(), p);
            visit(aSwitch.getCases(), p);
            afterSyntax(aSwitch, p);
            return aSwitch;
        }

        @Override
        public J visitCase(J.Case aCase, PrintOutputCapture<P> p) {
            beforeSyntax(aCase, Space.Location.CASE_PREFIX, p);
            J label = aCase.getCaseLabels().get(0);
            if (!(label instanceof J.Identifier && "default".equalsIgnoreCase(((J.Identifier) label).getSimpleName()))) {
                p.append(keyword(aCase.getMarkers(), "case"));
            }
            visitContainer("", aCase.getPadding().getCaseLabels(), JContainer.Location.CASE_LABEL, ",", "", p);
            visitSpace(aCase.getPadding().getStatements().getBefore(), Space.Location.CASE, p);
            p.append(':');
            visitStatements(aCase.getPadding().getStatements().getPadding().getElements(), JRightPadded.Location.CASE, p);
            afterSyntax(aCase, p);
            return aCase;
        }

        @Override
        public J visitTry(J.Try tryable, PrintOutputCapture<P> p) {
            beforeSyntax(tryable, Space.Location.TRY_PREFIX, p);
            p.append(keyword(tryable.getMarkers(), "try"));
            visit(tryable.getBody(), p);
            visit(tryable.getCatches(), p);
            JLeftPadded<J.Block> finallyBlock = tryable.getPadding().getFinally();
            if (finallyBlock != null) {
                visitLeftPadded(keyword(finallyBlock.getMarkers(), "finally"), finallyBlock, JLeftPadded.Location.TRY_FINALLY, p);
            }
            afterSyntax(tryable, p);
            return tryable;
        }

        @Override
        public J visitCatch(J.Try.Catch aCatch, PrintOutputCapture<P> p) {
            beforeSyntax(aCatch, Space.Location.CATCH_PREFIX, p);
            p.append(keyword(aCatch.getMarkers(), "catch"));
            visit(aCatch.getParameter(), p);
            visit(aCatch.getBody(), p);
            afterSyntax(aCatch, p);
            return aCatch;
        }

        @Override
        public J visitThrow(J.Throw thrown, PrintOutputCapture<P> p) {
            beforeSyntax(thrown, Space.Location.THROW_PREFIX, p);
            p.append(keyword(thrown.getMarkers(), "throw"));
            visit(thrown.getException(), p);
            afterSyntax(thrown, p);
            return thrown;
        }

        @Override
        public J visitReturn(J.Return aReturn, PrintOutputCapture<P> p) {
            beforeSyntax(aReturn, Space.Location.RETURN_PREFIX, p);
            p.append(keyword(aReturn.getMarkers(), "return"));
            visit(aReturn.getExpression(), p);
            afterSyntax(aReturn, p);
            return aReturn;
        }

        @Override
        public J visitBreak(J.Break breakStatement, PrintOutputCapture<P> p) {
            beforeSyntax(breakStatement, Space.Location.BREAK_PREFIX, p);
            p.append(keyword(breakStatement.getMarkers(), "break"));
            visit(breakStatement.getLabel(), p);
            afterSyntax(breakStatement, p);
            return breakStatement;
        }

        @Override
        public J visitContinue(J.Continue continueStatement, PrintOutputCapture<P> p) {
            beforeSyntax(continueStatement, Space.Location.CONTINUE_PREFIX, p);
            p.append(keyword(continueStatement.getMarkers(), "continue"));
            visit(continueStatement.getLabel(), p);
            afterSyntax(continueStatement, p);
            return continueStatement;
        }

        @Override
        public J visitNewClass(J.NewClass newClass, PrintOutputCapture<P> p) {
            beforeSyntax(newClass, Space.Location.NEW_CLASS_PREFIX, p);
            visitSpace(newClass.getNew(), Space.Location.NEW_PREFIX, p);
            p.append(keyword(newClass.getMarkers(), "new"));
            visit(newClass.getClazz(), p);
            visitArguments(newClass.getPadding().getArguments(), JContainer.Location.NEW_CLASS_ARGUMENTS, p);
            afterSyntax(newClass, p);
            return newClass;
        }

        /**
         * A parameter list, where a parameter may be followed by the next on a new line with no comma.
         */
        private void visitParameters(List<? extends JRightPadded<? extends J>> parameters, JRightPadded.Location location,
                                     PrintOutputCapture<P> p) {
            for (int i = 0; i < parameters.size(); i++) {
                JRightPadded<? extends J> parameter = parameters.get(i);
                visit(parameter.getElement(), p);
                visitSpace(parameter.getAfter(), location.getAfterLocation(), p);
                visitMarkers(parameter.getMarkers(), p);
                if (i < parameters.size() - 1 && !parameter.getMarkers().findFirst(OmitComma.class).isPresent()) {
                    p.append(',');
                }
            }
        }

        /**
         * Call arguments, where a named argument may be followed by the next with no comma, as in a tag
         * written as a call, {@code cfparam(name="x" default="")}.
         */
        private void visitArguments(JContainer<Expression> arguments, JContainer.Location location, PrintOutputCapture<P> p) {
            if (arguments.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                return;
            }
            visitSpace(arguments.getBefore(), location.getBeforeLocation(), p);
            p.append('(');
            visitParameters(arguments.getPadding().getElements(), location.getElementLocation(), p);
            p.append(')');
        }

        /**
         * An array literal, {@code [1, 2, 3]}, which has no type written before it.
         */
        @Override
        public J visitNewArray(J.NewArray newArray, PrintOutputCapture<P> p) {
            JContainer<Expression> initializer = newArray.getPadding().getInitializer();
            if (newArray.getTypeExpression() != null || initializer == null) {
                return super.visitNewArray(newArray, p);
            }
            beforeSyntax(newArray, Space.Location.NEW_ARRAY_PREFIX, p);
            visitSpace(initializer.getBefore(), Space.Location.NEW_ARRAY_INITIALIZER, p);
            p.append('[');
            visitParameters(initializer.getPadding().getElements(), JRightPadded.Location.NEW_ARRAY_INITIALIZER, p);
            p.append(']');
            afterSyntax(newArray, p);
            return newArray;
        }

        @Override
        public J visitAssignmentOperation(J.AssignmentOperation assignOp, PrintOutputCapture<P> p) {
            VarKeyword var = assignOp.getMarkers().findFirst(VarKeyword.class).orElse(null);
            if (var == null) {
                return super.visitAssignmentOperation(assignOp, p);
            }
            beforeSyntax(assignOp, Space.Location.ASSIGNMENT_OPERATION_PREFIX, p);
            p.append(var.getKeyword());
            // the rest is printed by the superclass from the variable on, with no prefix of its own to repeat
            J.AssignmentOperation rest = assignOp.withPrefix(Space.EMPTY).withMarkers(Markers.EMPTY);
            super.visitAssignmentOperation(rest, p);
            afterSyntax(assignOp, p);
            return assignOp;
        }

        @Override
        public J visitAssignment(J.Assignment assignment, PrintOutputCapture<P> p) {
            VarKeyword var = assignment.getMarkers().findFirst(VarKeyword.class).orElse(null);
            if (var == null) {
                return super.visitAssignment(assignment, p);
            }
            beforeSyntax(assignment, Space.Location.ASSIGNMENT_PREFIX, p);
            p.append(var.getKeyword());
            visit(assignment.getVariable(), p);
            visitLeftPadded("=", assignment.getPadding().getAssignment(), JLeftPadded.Location.ASSIGNMENT, p);
            afterSyntax(assignment, p);
            return assignment;
        }

        @Override
        public J visitVariable(J.VariableDeclarations.NamedVariable variable, PrintOutputCapture<P> p) {
            VarKeyword var = variable.getMarkers().findFirst(VarKeyword.class).orElse(null);
            if (var == null) {
                return super.visitVariable(variable, p);
            }
            beforeSyntax(variable, Space.Location.VARIABLE_PREFIX, p);
            p.append(var.getKeyword());
            visit(variable.getName(), p);
            visitLeftPadded("=", variable.getPadding().getInitializer(), JLeftPadded.Location.VARIABLE_INITIALIZER, p);
            afterSyntax(variable, p);
            return variable;
        }

        private String memberSeparator(Markers markers) {
            if (markers.findFirst(SafeNavigation.class).isPresent()) {
                return "?.";
            }
            return markers.findFirst(DoubleColon.class).isPresent() ? "::" : ".";
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
            beforeSyntax(fieldAccess, Space.Location.FIELD_ACCESS_PREFIX, p);
            visit(fieldAccess.getTarget(), p);
            visitLeftPadded(memberSeparator(fieldAccess.getMarkers()), fieldAccess.getPadding().getName(),
                    JLeftPadded.Location.FIELD_ACCESS_NAME, p);
            afterSyntax(fieldAccess, p);
            return fieldAccess;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
            beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);
            visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT,
                    memberSeparator(method.getMarkers()), p);
            visit(method.getName(), p);
            visitArguments(method.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, p);
            afterSyntax(method, p);
            return method;
        }
    }
}
