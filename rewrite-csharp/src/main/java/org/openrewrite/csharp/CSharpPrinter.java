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
package org.openrewrite.csharp;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.csharp.marker.*;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.csharp.tree.Linq;
import org.openrewrite.java.marker.NullSafe;
import org.openrewrite.java.marker.OmitBraces;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Semicolon;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prints a C# LST back to source code. This is a port of the native {@code CSharpPrinter.cs}:
 * the two must produce identical output for the same tree, so a fix to one belongs in the other.
 */
public class CSharpPrinter<P> extends CSharpVisitor<PrintOutputCapture<P>> {

    private static final Pattern GHOST_COMMENT = Pattern.compile("//DIRECTIVE:(\\d+)\\r?\\n?");

    private static final UnaryOperator<String> CSHARP_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    protected void visitStatement(JRightPadded<Statement> paddedStat, PrintOutputCapture<P> p) {
        visit(paddedStat.getElement(), p);
        visitSpace(paddedStat.getAfter(), p);
        printStatementTerminator(paddedStat.getElement(), p);
    }

    protected void printStatementTerminator(Statement statement, PrintOutputCapture<P> p) {
        if (statement instanceof Cs.ExpressionStatement ||
            statement instanceof J.Return ||
            statement instanceof J.VariableDeclarations ||
            statement instanceof J.Empty ||
            statement instanceof J.Throw ||
            statement instanceof J.Break ||
            statement instanceof J.Continue ||
            statement instanceof Cs.GotoStatement ||
            statement instanceof Cs.DelegateDeclaration ||
            statement instanceof Cs.Yield ||
            statement instanceof Cs.ExternAlias ||
            statement instanceof Cs.UsingDirective ||
            statement instanceof J.DoWhileLoop ||
            statement instanceof J.Package) {
            p.append(';');
        } else if (statement instanceof J.ClassDeclaration) {
            J.ClassDeclaration classDecl = (J.ClassDeclaration) statement;
            // `record X;` has its semicolon printed in place of the body, `record C { };` has one after it
            if (!classDecl.getBody().getMarkers().findFirst(Semicolon.class).isPresent() &&
                classDecl.getMarkers().findFirst(Semicolon.class).isPresent()) {
                p.append(';');
            }
        } else if (statement instanceof Cs.EnumDeclaration) {
            if (statement.getMarkers().findFirst(Semicolon.class).isPresent()) {
                p.append(';');
            }
        } else if (statement instanceof Cs.PropertyDeclaration) {
            Cs.PropertyDeclaration property = (Cs.PropertyDeclaration) statement;
            if (property.getPadding().getExpressionBody() != null || property.getPadding().getInitializer() != null) {
                p.append(';');
            }
        } else if (statement instanceof Cs.AnnotatedStatement) {
            printStatementTerminator(((Cs.AnnotatedStatement) statement).getStatement(), p);
        }
    }

    @Override
    public J visitCompilationUnit(Cs.CompilationUnit compilationUnit, PrintOutputCapture<P> p) {
        beforeSyntax(compilationUnit, p);

        for (JRightPadded<Statement> externAlias : compilationUnit.getPadding().getExterns()) {
            visitStatement(externAlias, p);
        }
        for (JRightPadded<Statement> usingDirective : compilationUnit.getPadding().getUsings()) {
            visitStatement(usingDirective, p);
        }
        visit(compilationUnit.getAttributeLists(), p);
        for (JRightPadded<Statement> member : compilationUnit.getPadding().getMembers()) {
            visitStatement(member, p);
        }

        visitSpace(compilationUnit.getEof(), p);
        afterSyntax(compilationUnit, p);
        return compilationUnit;
    }

    @Override
    public J visitUsingDirective(Cs.UsingDirective usingDirective, PrintOutputCapture<P> p) {
        beforeSyntax(usingDirective, p);

        if (usingDirective.isGlobal()) {
            p.append("global");
            visitSpace(usingDirective.getPadding().getGlobal().getAfter(), p);
        }

        p.append("using");

        if (usingDirective.isStatic()) {
            visitSpace(usingDirective.getPadding().getStatic().getBefore(), p);
            p.append("static");
        }

        JLeftPadded<Boolean> unsafe = usingDirective.getPadding().getUnsafe();
        if (unsafe != null && unsafe.getElement()) {
            visitSpace(unsafe.getBefore(), p);
            p.append("unsafe");
        }

        JRightPadded<J.Identifier> alias = usingDirective.getPadding().getAlias();
        if (alias != null) {
            visit(alias.getElement(), p);
            visitSpace(alias.getAfter(), p);
            p.append('=');
        }

        visit(usingDirective.getNamespaceOrType(), p);
        afterSyntax(usingDirective, p);
        return usingDirective;
    }

    @Override
    public J visitPackage(J.Package pkg, PrintOutputCapture<P> p) {
        beforeSyntax(pkg, p);
        p.append("namespace");
        visit(pkg.getExpression(), p);
        afterSyntax(pkg, p);
        return pkg;
    }

    @Override
    public J visitNamespaceDeclaration(Cs.NamespaceDeclaration namespaceDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(namespaceDeclaration, p);
        p.append("namespace");
        JRightPadded<Expression> name = namespaceDeclaration.getPadding().getName();
        visit(name.getElement(), p);
        visitSpace(name.getAfter(), p);

        boolean fileScoped = name.getMarkers().findFirst(Semicolon.class).isPresent();
        p.append(fileScoped ? ';' : '{');

        for (JRightPadded<Statement> externAlias : namespaceDeclaration.getPadding().getExterns()) {
            visitStatement(externAlias, p);
        }
        for (JRightPadded<Statement> usingDirective : namespaceDeclaration.getPadding().getUsings()) {
            visitStatement(usingDirective, p);
        }
        for (JRightPadded<Statement> member : namespaceDeclaration.getPadding().getMembers()) {
            visitStatement(member, p);
        }

        if (!fileScoped) {
            visitSpace(namespaceDeclaration.getEnd(), p);
            p.append('}');
        }
        afterSyntax(namespaceDeclaration, p);
        return namespaceDeclaration;
    }

    @Override
    public J visitTupleType(Cs.TupleType tupleType, PrintOutputCapture<P> p) {
        beforeSyntax(tupleType, p);

        JContainer<Cs.TupleElement> elements = tupleType.getPadding().getElements();
        visitSpace(elements.getBefore(), p);
        p.append('(');

        List<JRightPadded<Cs.TupleElement>> padded = elements.getPadding().getElements();
        for (int i = 0; i < padded.size(); i++) {
            JRightPadded<Cs.TupleElement> element = padded.get(i);
            beforeSyntax(element.getElement(), p);
            visit(element.getElement().getType(), p);
            if (element.getElement().getName() != null) {
                visit(element.getElement().getName(), p);
            }
            afterSyntax(element.getElement(), p);
            visitSpace(element.getAfter(), p);
            if (i < padded.size() - 1) {
                p.append(',');
            }
        }

        p.append(')');
        afterSyntax(tupleType, p);
        return tupleType;
    }

    @Override
    public J visitTupleExpression(Cs.TupleExpression tupleExpression, PrintOutputCapture<P> p) {
        beforeSyntax(tupleExpression, p);
        visitContainer("(", tupleExpression.getPadding().getArguments(), ",", ")", p);
        afterSyntax(tupleExpression, p);
        return tupleExpression;
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
        beforeSyntax(fieldAccess, p);
        visit(fieldAccess.getTarget(), p);
        visitSpace(fieldAccess.getPadding().getName().getBefore(), p);

        NullSafe nullSafe = fieldAccess.getMarkers().findFirst(NullSafe.class).orElse(null);
        if (isPointerMemberAccess(fieldAccess.getTarget())) {
            p.append("->");
        } else if (nullSafe != null) {
            p.append('?');
            visitSpace(nullSafe.getDotPrefix(), p);
            p.append('.');
        } else {
            p.append('.');
        }

        visit(fieldAccess.getPadding().getName().getElement(), p);
        afterSyntax(fieldAccess, p);
        return fieldAccess;
    }

    private static boolean isPointerMemberAccess(Expression target) {
        return target instanceof Cs.PointerDereference &&
               target.getMarkers().findFirst(PointerMemberAccess.class).isPresent();
    }

    @Override
    public J visitMemberReference(J.MemberReference memberRef, PrintOutputCapture<P> p) {
        beforeSyntax(memberRef, p);
        visitRightPadded(memberRef.getPadding().getContaining(), ".", p);
        visit(memberRef.getPadding().getReference().getElement(), p);
        if (memberRef.getPadding().getTypeParameters() != null) {
            printTypeArguments(memberRef.getPadding().getTypeParameters(), p);
        }
        afterSyntax(memberRef, p);
        return memberRef;
    }

    @Override
    public J visitNullableType(J.NullableType nullableType, PrintOutputCapture<P> p) {
        beforeSyntax(nullableType, p);
        visitRightPadded(nullableType.getPadding().getTypeTree(), "?", p);
        afterSyntax(nullableType, p);
        return nullableType;
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, PrintOutputCapture<P> p) {
        beforeSyntax(type, p);
        visit(type.getClazz(), p);
        visitContainer("<", type.getPadding().getTypeParameters(), ",", ">", p);
        afterSyntax(type, p);
        return type;
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, PrintOutputCapture<P> p) {
        boolean multiDimensional = arrayAccess.getMarkers().findFirst(Cs.MultiDimensionalArray.class).isPresent();

        if (multiDimensional && arrayAccess.getIndexed() instanceof J.ArrayAccess) {
            printArrayAccessWithoutClosingBracket((J.ArrayAccess) arrayAccess.getIndexed(), p);
            beforeSyntax(arrayAccess, p);
            p.append(',');
            beforeSyntax(arrayAccess.getDimension(), p);
            visitRightPadded(arrayAccess.getDimension().getPadding().getIndex(), "]", p);
            afterSyntax(arrayAccess.getDimension(), p);
        } else {
            beforeSyntax(arrayAccess, p);
            visit(arrayAccess.getIndexed(), p);
            visitArrayDimension(arrayAccess.getDimension(), p);
        }
        afterSyntax(arrayAccess, p);
        return arrayAccess;
    }

    private void printArrayAccessWithoutClosingBracket(J.ArrayAccess arrayAccess, PrintOutputCapture<P> p) {
        boolean multiDimensional = arrayAccess.getMarkers().findFirst(Cs.MultiDimensionalArray.class).isPresent();
        JRightPadded<Expression> index = arrayAccess.getDimension().getPadding().getIndex();

        if (multiDimensional && arrayAccess.getIndexed() instanceof J.ArrayAccess) {
            printArrayAccessWithoutClosingBracket((J.ArrayAccess) arrayAccess.getIndexed(), p);
            beforeSyntax(arrayAccess, p);
            p.append(',');
            beforeSyntax(arrayAccess.getDimension(), p);
        } else {
            beforeSyntax(arrayAccess, p);
            visit(arrayAccess.getIndexed(), p);
            beforeSyntax(arrayAccess.getDimension(), p);
            NullSafe nullSafe = arrayAccess.getMarkers().findFirst(NullSafe.class).orElse(null);
            if (nullSafe != null) {
                p.append('?');
                visitSpace(nullSafe.getDotPrefix(), p);
            }
            p.append('[');
        }
        visit(index.getElement(), p);
        visitSpace(index.getAfter(), p);
        afterSyntax(arrayAccess.getDimension(), p);
        afterSyntax(arrayAccess, p);
    }

    @Override
    public J visitArrayDimension(J.ArrayDimension arrayDimension, PrintOutputCapture<P> p) {
        beforeSyntax(arrayDimension, p);
        // the null-safe marker lives on the enclosing array access
        Object parent = getCursor().getValue();
        NullSafe nullSafe = parent instanceof J.ArrayAccess ?
                ((J.ArrayAccess) parent).getMarkers().findFirst(NullSafe.class).orElse(null) :
                null;
        if (nullSafe != null) {
            p.append('?');
            visitSpace(nullSafe.getDotPrefix(), p);
        }
        p.append('[');
        visitRightPadded(arrayDimension.getPadding().getIndex(), "]", p);
        afterSyntax(arrayDimension, p);
        return arrayDimension;
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
        beforeSyntax(method, p);

        JRightPadded<Expression> select = method.getPadding().getSelect();
        if (select != null) {
            visit(select.getElement(), p);
            visitSpace(select.getAfter(), p);

            // a delegate invocation is sugar for `.Invoke()`, so neither the dot nor the name is printed
            if (!method.getMarkers().findFirst(Cs.DelegateInvocation.class).isPresent()) {
                NullSafe nullSafe = method.getMarkers().findFirst(NullSafe.class).orElse(null);
                if (isPointerMemberAccess(select.getElement())) {
                    p.append("->");
                } else if (nullSafe != null) {
                    p.append('?');
                    visitSpace(nullSafe.getDotPrefix(), p);
                    p.append('.');
                } else {
                    p.append('.');
                }
                visit(method.getName(), p);
            } else {
                visitMarkersOf(method.getName(), p);
            }
        } else {
            visit(method.getName(), p);
        }

        if (method.getPadding().getTypeParameters() != null) {
            printTypeArguments(method.getPadding().getTypeParameters(), p);
        }

        visitArguments(method.getPadding().getArguments(), p);
        afterSyntax(method, p);
        return method;
    }

    @Override
    public J visitNewClass(J.NewClass newClass, PrintOutputCapture<P> p) {
        beforeSyntax(newClass, p);

        visitRightPadded(newClass.getPadding().getEnclosing(), ".", p);

        p.append("new");
        visitSpace(newClass.getNew(), p);
        visit(newClass.getClazz(), p);

        JContainer<Expression> arguments = newClass.getPadding().getArguments();
        if (arguments.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
            for (Expression argument : arguments.getElements()) {
                visitMarkersOf(argument, p);
            }
        } else {
            visitArguments(arguments, p);
        }

        J.Block body = newClass.getBody();
        if (body != null) {
            List<Statement> statements = body.getStatements();
            if (statements.size() == 1 &&
                statements.get(0) instanceof Cs.ExpressionStatement &&
                ((Cs.ExpressionStatement) statements.get(0)).getExpression() instanceof Cs.InitializerExpression) {
                // an object or collection initializer
                beforeSyntax(body, p);
                visit(((Cs.ExpressionStatement) statements.get(0)).getExpression(), p);
                afterSyntax(body, p);
            } else {
                visitBlock(body, p);
            }
        }

        afterSyntax(newClass, p);
        return newClass;
    }

    @Override
    public J visitNewArray(J.NewArray newArray, PrintOutputCapture<P> p) {
        beforeSyntax(newArray, p);

        if (!(getCursor().getParentTreeCursor().getValue() instanceof Cs.StackAllocExpression)) {
            p.append("new");
        }

        visit(newArray.getTypeExpression(), p);

        List<J.ArrayDimension> dimensions = newArray.getDimensions();
        for (int i = 0; i < dimensions.size(); i++) {
            J.ArrayDimension dimension = dimensions.get(i);

            if (dimension.getMarkers().findFirst(MultiDimensionContinuation.class).isPresent()) {
                // within the same rank specifier
                p.append(',');
                beforeSyntax(dimension, p);
            } else {
                beforeSyntax(dimension, p);
                p.append('[');
            }

            JRightPadded<Expression> index = dimension.getPadding().getIndex();
            visit(index.getElement(), p);
            visitSpace(index.getAfter(), p);

            boolean nextIsContinuation = i + 1 < dimensions.size() &&
                                         dimensions.get(i + 1).getMarkers().findFirst(MultiDimensionContinuation.class).isPresent();
            if (!nextIsContinuation) {
                p.append(']');
            }
            afterSyntax(dimension, p);
        }

        JContainer<Expression> initializer = newArray.getPadding().getInitializer();
        if (initializer != null) {
            visitSpace(initializer.getBefore(), p);
            p.append('{');
            visitInitializerElements(initializer.getPadding().getElements(), p);
            p.append('}');
        }

        afterSyntax(newArray, p);
        return newArray;
    }

    /**
     * Empty braces hold a single {@link J.Empty} whose trailing space is the space between them.
     */
    private void visitInitializerElements(List<JRightPadded<Expression>> elements, PrintOutputCapture<P> p) {
        if (elements.size() == 1 && elements.get(0).getElement() instanceof J.Empty) {
            visit(elements.get(0).getElement(), p);
            visitSpace(elements.get(0).getAfter(), p);
        } else {
            visitRightPadded(elements, ",", p);
        }
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType, PrintOutputCapture<P> p) {
        beforeSyntax(arrayType, p);
        visit(arrayType.getElementType(), p);
        JLeftPadded<Space> dimension = arrayType.getDimension();
        if (dimension != null) {
            visitSpace(dimension.getBefore(), p);
            p.append('[');
            visitSpace(dimension.getElement(), p);
            p.append(']');
        }
        afterSyntax(arrayType, p);
        return arrayType;
    }

    @Override
    public J visitNamedExpression(Cs.NamedExpression namedExpression, PrintOutputCapture<P> p) {
        beforeSyntax(namedExpression, p);
        visitRightPadded(namedExpression.getPadding().getName(), ":", p);
        visit(namedExpression.getExpression(), p);
        afterSyntax(namedExpression, p);
        return namedExpression;
    }

    @Override
    public J visitRefExpression(Cs.RefExpression refExpression, PrintOutputCapture<P> p) {
        beforeSyntax(refExpression, p);
        switch (refExpression.getKind()) {
            case Out:
                p.append("out");
                break;
            case Ref:
                p.append("ref");
                break;
            case In:
                p.append("in");
                break;
        }
        visit(refExpression.getExpression(), p);
        afterSyntax(refExpression, p);
        return refExpression;
    }

    @Override
    public J visitDeclarationExpression(Cs.DeclarationExpression declarationExpression, PrintOutputCapture<P> p) {
        beforeSyntax(declarationExpression, p);
        visit(declarationExpression.getTypeExpression(), p);
        visit(declarationExpression.getVariables(), p);
        afterSyntax(declarationExpression, p);
        return declarationExpression;
    }

    @Override
    public J visitSingleVariableDesignation(Cs.SingleVariableDesignation singleVariableDesignation, PrintOutputCapture<P> p) {
        beforeSyntax(singleVariableDesignation, p);
        visit(singleVariableDesignation.getName(), p);
        afterSyntax(singleVariableDesignation, p);
        return singleVariableDesignation;
    }

    @Override
    public J visitParenthesizedVariableDesignation(Cs.ParenthesizedVariableDesignation parenthesizedVariableDesignation, PrintOutputCapture<P> p) {
        beforeSyntax(parenthesizedVariableDesignation, p);
        visitContainer("(", parenthesizedVariableDesignation.getPadding().getVariables(), ",", ")", p);
        afterSyntax(parenthesizedVariableDesignation, p);
        return parenthesizedVariableDesignation;
    }

    @Override
    public J visitDiscardVariableDesignation(Cs.DiscardVariableDesignation discardVariableDesignation, PrintOutputCapture<P> p) {
        beforeSyntax(discardVariableDesignation, p);
        visit(discardVariableDesignation.getDiscard(), p);
        afterSyntax(discardVariableDesignation, p);
        return discardVariableDesignation;
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParameter, PrintOutputCapture<P> p) {
        beforeSyntax(typeParameter, p);
        Cs.ConstrainedTypeParameter constrained = constrainedTypeParameter(typeParameter);
        if (constrained != null) {
            // the constrained type parameter repeats the name, and is the one to print it
            visitMarkersOf(typeParameter.getName(), p);
            beforeSyntax(Space.EMPTY, constrained.getMarkers(), p);
            printConstrainedTypeParameterDecl(constrained, p);
            afterSyntax(constrained.getMarkers(), p);
        } else {
            visit(typeParameter.getName(), p);
        }
        afterSyntax(typeParameter, p);
        return typeParameter;
    }

    /**
     * A type parameter that is only named by its constraints is not printed among the others.
     */
    private void visitImplicitTypeParameter(J.TypeParameter typeParameter, PrintOutputCapture<P> p) {
        visitMarkersOf(typeParameter, p);
        visitMarkersOf(typeParameter.getName(), p);
        Cs.ConstrainedTypeParameter constrained = constrainedTypeParameter(typeParameter);
        if (constrained != null) {
            visitMarkersOf(constrained, p);
            visitMarkersOf(constrained.getName(), p);
        }
    }

    /**
     * Attributes, variance and constraints of a type parameter are carried by its first bound.
     */
    private static Cs.@Nullable ConstrainedTypeParameter constrainedTypeParameter(J.TypeParameter typeParameter) {
        List<TypeTree> bounds = typeParameter.getBounds();
        return bounds != null && !bounds.isEmpty() && bounds.get(0) instanceof Cs.ConstrainedTypeParameter ?
                (Cs.ConstrainedTypeParameter) bounds.get(0) :
                null;
    }

    @Override
    public J visitConstrainedTypeParameter(Cs.ConstrainedTypeParameter constrainedTypeParameter, PrintOutputCapture<P> p) {
        beforeSyntax(constrainedTypeParameter, p);
        printConstrainedTypeParameterDecl(constrainedTypeParameter, p);
        printConstrainedTypeParameterConstraints(constrainedTypeParameter, p);
        afterSyntax(constrainedTypeParameter, p);
        return constrainedTypeParameter;
    }

    /**
     * The part of a constrained type parameter that appears between the angle brackets.
     */
    private void printConstrainedTypeParameterDecl(Cs.ConstrainedTypeParameter constrained, PrintOutputCapture<P> p) {
        visit(constrained.getAttributeLists(), p);

        JLeftPadded<Cs.ConstrainedTypeParameter.VarianceKind> variance = constrained.getPadding().getVariance();
        if (variance != null) {
            visitSpace(variance.getBefore(), p);
            p.append(variance.getElement() == Cs.ConstrainedTypeParameter.VarianceKind.In ? "in" : "out");
        }

        visit(constrained.getName(), p);
    }

    @Override
    public J visitTypeParameters(J.TypeParameters typeParameters, PrintOutputCapture<P> p) {
        printTypeParameterList(typeParameters.getPrefix(), typeParameters.getMarkers(), typeParameters.getPadding().getTypeParameters(), p);
        return typeParameters;
    }

    private void printTypeParameterList(Space before, Markers markers, List<JRightPadded<J.TypeParameter>> typeParameters, PrintOutputCapture<P> p) {
        beforeSyntax(before, markers, p);
        p.append('<');
        boolean needsComma = false;
        for (JRightPadded<J.TypeParameter> typeParameter : typeParameters) {
            JContainer<TypeTree> bounds = typeParameter.getElement().getPadding().getBounds();
            if (bounds != null && bounds.getMarkers().findFirst(ImplicitTypeParameters.class).isPresent()) {
                visitImplicitTypeParameter(typeParameter.getElement(), p);
                continue;
            }

            if (needsComma) {
                p.append(',');
            }

            visit(typeParameter.getElement(), p);

            visitSpace(typeParameter.getAfter(), p);
            needsComma = true;
        }
        p.append('>');
        afterSyntax(markers, p);
    }

    private void printTypeParameterConstraintsInSourceOrder(@Nullable List<JRightPadded<J.TypeParameter>> typeParameters, PrintOutputCapture<P> p) {
        if (typeParameters == null) {
            return;
        }

        List<Cs.ConstrainedTypeParameter> withWhere = new ArrayList<>();
        for (JRightPadded<J.TypeParameter> typeParameter : typeParameters) {
            Cs.ConstrainedTypeParameter constrained = constrainedTypeParameter(typeParameter.getElement());
            if (constrained != null && constrained.getPadding().getWhereConstraint() != null) {
                withWhere.add(constrained);
            }
        }

        withWhere.sort(Comparator.comparingInt(constrained -> constrained.getMarkers()
                .findFirst(WhereClauseOrder.class)
                .map(WhereClauseOrder::getOrder)
                .orElse(Integer.MAX_VALUE)));

        for (Cs.ConstrainedTypeParameter constrained : withWhere) {
            printConstrainedTypeParameterConstraints(constrained, p);
        }
    }

    /**
     * The {@code where T : constraint, constraint} part of a constrained type parameter.
     */
    private void printConstrainedTypeParameterConstraints(Cs.ConstrainedTypeParameter constrained, PrintOutputCapture<P> p) {
        JLeftPadded<J.Identifier> whereConstraint = constrained.getPadding().getWhereConstraint();
        if (whereConstraint == null) {
            return;
        }

        visitSpace(whereConstraint.getBefore(), p);
        p.append("where");
        visit(whereConstraint.getElement(), p);

        JContainer<Expression> constraints = constrained.getPadding().getConstraints();
        if (constraints != null) {
            visitSpace(constraints.getBefore(), p);
            p.append(':');
            List<JRightPadded<Expression>> elements = constraints.getPadding().getElements();
            for (int i = 0; i < elements.size(); i++) {
                visit(elements.get(i).getElement(), p);
                if (i < elements.size() - 1) {
                    visitSpace(elements.get(i).getAfter(), p);
                    p.append(',');
                }
            }
        }
    }

    @Override
    public J visitIsPattern(Cs.IsPattern isPattern, PrintOutputCapture<P> p) {
        beforeSyntax(isPattern, p);
        visit(isPattern.getExpression(), p);
        visitSpace(isPattern.getPadding().getPattern().getBefore(), p);
        p.append("is");
        visit(isPattern.getPadding().getPattern().getElement(), p);
        afterSyntax(isPattern, p);
        return isPattern;
    }

    @Override
    public J visitBinary(Cs.Binary binary, PrintOutputCapture<P> p) {
        beforeSyntax(binary, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), p);
        switch (binary.getOperator()) {
            case As:
                p.append("as");
                break;
            case NullCoalescing:
                p.append("??");
                break;
            case And:
                p.append("and");
                break;
            case Or:
                p.append("or");
                break;
        }
        visit(binary.getRight(), p);
        afterSyntax(binary, p);
        return binary;
    }

    @Override
    public J visitStatementExpression(Cs.StatementExpression statementExpression, PrintOutputCapture<P> p) {
        beforeSyntax(statementExpression, p);
        visit(statementExpression.getStatement(), p);
        afterSyntax(statementExpression, p);
        return statementExpression;
    }

    @Override
    public J visitSizeOf(Cs.SizeOf sizeOf, PrintOutputCapture<P> p) {
        beforeSyntax(sizeOf, p);
        p.append("sizeof");
        visit(sizeOf.getClazz(), p);
        afterSyntax(sizeOf, p);
        return sizeOf;
    }

    @Override
    public J visitTypeOf(Cs.TypeOf typeOf, PrintOutputCapture<P> p) {
        beforeSyntax(typeOf, p);
        p.append("typeof");
        visit(typeOf.getClazz(), p);
        afterSyntax(typeOf, p);
        return typeOf;
    }

    @Override
    public J visitUnsafeStatement(Cs.UnsafeStatement unsafeStatement, PrintOutputCapture<P> p) {
        beforeSyntax(unsafeStatement, p);
        p.append("unsafe");
        visit(unsafeStatement.getBlock(), p);
        afterSyntax(unsafeStatement, p);
        return unsafeStatement;
    }

    @Override
    public J visitPointerType(Cs.PointerType pointerType, PrintOutputCapture<P> p) {
        beforeSyntax(pointerType, p);
        visitRightPadded(pointerType.getPadding().getElementType(), "*", p);
        afterSyntax(pointerType, p);
        return pointerType;
    }

    @Override
    public J visitFixedStatement(Cs.FixedStatement fixedStatement, PrintOutputCapture<P> p) {
        beforeSyntax(fixedStatement, p);
        p.append("fixed");
        J.ControlParentheses<J.VariableDeclarations> declarations = fixedStatement.getDeclarations();
        beforeSyntax(declarations, p);
        p.append('(');
        visitVariableDeclarations(declarations.getTree(), p);
        visitSpace(declarations.getPadding().getTree().getAfter(), p);
        p.append(')');
        afterSyntax(declarations, p);
        visit(fixedStatement.getBlock(), p);
        afterSyntax(fixedStatement, p);
        return fixedStatement;
    }

    @Override
    public J visitExternAlias(Cs.ExternAlias externAlias, PrintOutputCapture<P> p) {
        beforeSyntax(externAlias, p);
        p.append("extern");
        visitSpace(externAlias.getPadding().getIdentifier().getBefore(), p);
        p.append("alias");
        visit(externAlias.getPadding().getIdentifier().getElement(), p);
        afterSyntax(externAlias, p);
        return externAlias;
    }

    @Override
    public J visitInitializerExpression(Cs.InitializerExpression initializerExpression, PrintOutputCapture<P> p) {
        beforeSyntax(initializerExpression, p);
        JContainer<Expression> expressions = initializerExpression.getPadding().getExpressions();
        visitSpace(expressions.getBefore(), p);
        p.append('{');
        visitInitializerElements(expressions.getPadding().getElements(), p);
        p.append('}');
        afterSyntax(initializerExpression, p);
        return initializerExpression;
    }

    @Override
    public J visitNullSafeExpression(Cs.NullSafeExpression nullSafeExpression, PrintOutputCapture<P> p) {
        beforeSyntax(nullSafeExpression, p);
        visitRightPadded(nullSafeExpression.getPadding().getExpression(), "!", p);
        afterSyntax(nullSafeExpression, p);
        return nullSafeExpression;
    }

    @Override
    public J visitDefaultExpression(Cs.DefaultExpression defaultExpression, PrintOutputCapture<P> p) {
        beforeSyntax(defaultExpression, p);
        p.append("default");
        JContainer<TypeTree> typeOperator = defaultExpression.getPadding().getTypeOperator();
        if (typeOperator != null) {
            visitSpace(typeOperator.getBefore(), p);
            p.append('(');
            for (JRightPadded<TypeTree> type : typeOperator.getPadding().getElements()) {
                visit(type.getElement(), p);
                visitSpace(type.getAfter(), p);
            }
            p.append(')');
        }
        afterSyntax(defaultExpression, p);
        return defaultExpression;
    }

    @Override
    public J visitRelationalPattern(Cs.RelationalPattern relationalPattern, PrintOutputCapture<P> p) {
        beforeSyntax(relationalPattern, p);
        visitSpace(relationalPattern.getPadding().getOperator().getBefore(), p);
        switch (relationalPattern.getOperator()) {
            case LessThan:
                p.append('<');
                break;
            case LessThanOrEqual:
                p.append("<=");
                break;
            case GreaterThan:
                p.append('>');
                break;
            case GreaterThanOrEqual:
                p.append(">=");
                break;
        }
        visit(relationalPattern.getValue(), p);
        afterSyntax(relationalPattern, p);
        return relationalPattern;
    }

    @Override
    public J visitPropertyPattern(Cs.PropertyPattern propertyPattern, PrintOutputCapture<P> p) {
        beforeSyntax(propertyPattern, p);
        visit(propertyPattern.getTypeQualifier(), p);

        JContainer<Expression> subpatterns = propertyPattern.getPadding().getSubpatterns();
        List<JRightPadded<Expression>> elements = subpatterns.getPadding().getElements();
        if (elements.size() == 1 && elements.get(0).getElement() instanceof J.Empty) {
            // `{ }`: the empty element's prefix is the space between the braces
            visitSpace(subpatterns.getBefore(), p);
            p.append('{');
            visit(elements.get(0).getElement(), p);
            p.append('}');
        } else {
            visitContainer("{", subpatterns, ",", "}", p);
        }

        visit(propertyPattern.getDesignation(), p);
        afterSyntax(propertyPattern, p);
        return propertyPattern;
    }

    @Override
    public J visitDeconstructionPattern(J.DeconstructionPattern deconstructionPattern, PrintOutputCapture<P> p) {
        beforeSyntax(deconstructionPattern, p);

        // a positional pattern without a type, like `(int x, int y)`, has an empty deconstructor
        visit(deconstructionPattern.getDeconstructor(), p);

        JContainer<J> nested = deconstructionPattern.getPadding().getNested();
        visitSpace(nested.getBefore(), p);
        p.append('(');
        visitDeclarations(nested.getPadding().getElements(), p);
        p.append(')');

        afterSyntax(deconstructionPattern, p);
        return deconstructionPattern;
    }

    @Override
    public J visitPropertyDeclaration(Cs.PropertyDeclaration propertyDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(propertyDeclaration, p);
        visitModifiers(propertyDeclaration.getModifiers(), p);
        visit(propertyDeclaration.getTypeExpression(), p);
        visitRightPadded(propertyDeclaration.getPadding().getInterfaceSpecifier(), ".", p);
        visit(propertyDeclaration.getName(), p);

        JLeftPadded<Expression> expressionBody = propertyDeclaration.getPadding().getExpressionBody();
        if (expressionBody != null) {
            visitSpace(expressionBody.getBefore(), p);
            p.append("=>");
            visit(expressionBody.getElement(), p);
        } else if (propertyDeclaration.getAccessors() != null) {
            visitBlock(propertyDeclaration.getAccessors(), p);
        }

        JLeftPadded<Expression> initializer = propertyDeclaration.getPadding().getInitializer();
        if (initializer != null) {
            visitSpace(initializer.getBefore(), p);
            p.append('=');
            visit(initializer.getElement(), p);
        }

        afterSyntax(propertyDeclaration, p);
        return propertyDeclaration;
    }

    @Override
    public J visitAccessorDeclaration(Cs.AccessorDeclaration accessorDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(accessorDeclaration, p);
        visit(accessorDeclaration.getAttributes(), p);
        visitModifiers(accessorDeclaration.getModifiers(), p);

        visitSpace(accessorDeclaration.getPadding().getKind().getBefore(), p);
        switch (accessorDeclaration.getKind()) {
            case Get:
                p.append("get");
                break;
            case Set:
                p.append("set");
                break;
            case Init:
                p.append("init");
                break;
            case Add:
                p.append("add");
                break;
            case Remove:
                p.append("remove");
                break;
        }

        JLeftPadded<Expression> expressionBody = accessorDeclaration.getPadding().getExpressionBody();
        if (expressionBody != null) {
            // an auto-implemented accessor has an empty body whose prefix is the space before `;`
            if (!(expressionBody.getElement() instanceof J.Empty)) {
                visitSpace(expressionBody.getBefore(), p);
                p.append("=>");
            }
            visit(expressionBody.getElement(), p);
            p.append(';');
        } else if (accessorDeclaration.getBody() != null) {
            visitBlock(accessorDeclaration.getBody(), p);
        } else {
            p.append(';');
        }

        afterSyntax(accessorDeclaration, p);
        return accessorDeclaration;
    }

    @Override
    public J visitAnnotatedStatement(Cs.AnnotatedStatement annotatedStatement, PrintOutputCapture<P> p) {
        beforeSyntax(annotatedStatement, p);
        visit(annotatedStatement.getAttributeLists(), p);
        visit(annotatedStatement.getStatement(), p);
        afterSyntax(annotatedStatement, p);
        return annotatedStatement;
    }

    @Override
    public J visitAttributeList(Cs.AttributeList attributeList, PrintOutputCapture<P> p) {
        beforeSyntax(attributeList, p);
        p.append('[');
        visitRightPadded(attributeList.getPadding().getTarget(), ":", p);

        List<JRightPadded<J.Annotation>> attributes = attributeList.getPadding().getAttributes();
        for (int i = 0; i < attributes.size(); i++) {
            visit(attributes.get(i).getElement(), p);
            visitSpace(attributes.get(i).getAfter(), p);
            if (i < attributes.size() - 1) {
                p.append(',');
            }
        }

        p.append(']');
        afterSyntax(attributeList, p);
        return attributeList;
    }

    @Override
    public J visitAnnotation(J.Annotation annotation, PrintOutputCapture<P> p) {
        beforeSyntax(annotation, p);
        visit(annotation.getAnnotationType(), p);
        if (annotation.getPadding().getArguments() != null) {
            visitArguments(annotation.getPadding().getArguments(), p);
        }
        afterSyntax(annotation, p);
        return annotation;
    }

    @Override
    public J visitBlock(J.Block block, PrintOutputCapture<P> p) {
        beforeSyntax(block, p);
        boolean omitBraces = block.getMarkers().findFirst(OmitBraces.class).isPresent();
        if (!omitBraces) {
            p.append('{');
        }

        for (JRightPadded<Statement> statement : block.getPadding().getStatements()) {
            // the primary constructor is printed as part of the class declaration header
            if (!isPrimaryConstructor(statement.getElement())) {
                visitStatement(statement, p);
            }
        }

        visitSpace(block.getEnd(), p);
        if (!omitBraces) {
            p.append('}');
        }
        afterSyntax(block, p);
        return block;
    }

    private static boolean isPrimaryConstructor(Statement statement) {
        return statement instanceof J.MethodDeclaration &&
               statement.getMarkers().findFirst(PrimaryConstructor.class).isPresent();
    }

    @Override
    public J visitClassDeclaration(J.ClassDeclaration classDecl, PrintOutputCapture<P> p) {
        beforeSyntax(classDecl, p);
        visitModifiers(classDecl.getModifiers(), p);

        visitSpace(classDecl.getPadding().getKind().getPrefix(), p);
        boolean struct = classDecl.getMarkers().findFirst(Struct.class).isPresent();
        boolean record = classDecl.getKind() == J.ClassDeclaration.Kind.Type.Record;
        if (record && struct) {
            p.append("record struct");
        } else if (record && classDecl.getMarkers().findFirst(RecordClass.class).isPresent()) {
            p.append("record class");
        } else if (struct) {
            p.append("struct");
        } else {
            switch (classDecl.getKind()) {
                case Interface:
                    p.append("interface");
                    break;
                case Record:
                    p.append("record");
                    break;
                case Enum:
                    p.append("enum");
                    break;
                case Annotation:
                    p.append("@interface");
                    break;
                case Value:
                    p.append("value");
                    break;
                default:
                    p.append("class");
            }
        }

        visit(classDecl.getName(), p);

        JContainer<J.TypeParameter> typeParameters = classDecl.getPadding().getTypeParameters();
        if (typeParameters != null && !typeParameters.getMarkers().findFirst(ImplicitTypeParameters.class).isPresent()) {
            printTypeParameterList(typeParameters.getBefore(), typeParameters.getMarkers(), typeParameters.getPadding().getElements(), p);
        } else if (typeParameters != null) {
            for (J.TypeParameter typeParameter : typeParameters.getElements()) {
                visitImplicitTypeParameter(typeParameter, p);
            }
        }

        for (Statement statement : classDecl.getBody().getStatements()) {
            if (isPrimaryConstructor(statement)) {
                J.MethodDeclaration constructor = (J.MethodDeclaration) statement;
                beforeSyntax(Space.EMPTY, constructor.getMarkers(), p);
                visitMarkersOf(constructor.getName(), p);
                JContainer<Statement> parameters = constructor.getPadding().getParameters();
                visitSpace(parameters.getBefore(), p);
                p.append('(');
                visitDeclarations(parameters.getPadding().getElements(), p);
                p.append(')');
                afterSyntax(constructor.getMarkers(), p);
                break;
            }
        }

        // the first base type is the extends clause, the rest follow each preceded by a comma
        JLeftPadded<TypeTree> extendings = classDecl.getPadding().getExtends();
        if (extendings != null) {
            visitSpace(extendings.getBefore(), p);
            p.append(':');
            visit(extendings.getElement(), p);
        }

        JContainer<TypeTree> implementings = classDecl.getPadding().getImplements();
        if (implementings != null) {
            visitSpace(implementings.getBefore(), p);
            for (JRightPadded<TypeTree> implementing : implementings.getPadding().getElements()) {
                p.append(',');
                visit(implementing.getElement(), p);
                visitSpace(implementing.getAfter(), p);
            }
        }

        printTypeParameterConstraintsInSourceOrder(typeParameters == null ? null : typeParameters.getPadding().getElements(), p);

        if (classDecl.getBody().getMarkers().findFirst(Semicolon.class).isPresent()) {
            beforeSyntax(classDecl.getBody(), p);
            p.append(';');
            afterSyntax(classDecl.getBody(), p);
        } else {
            visitBlock(classDecl.getBody(), p);
        }

        afterSyntax(classDecl, p);
        return classDecl;
    }

    @Override
    public J visitEnumValueSet(J.EnumValueSet enums, PrintOutputCapture<P> p) {
        beforeSyntax(enums, p);
        List<JRightPadded<J.EnumValue>> values = enums.getPadding().getEnums();
        for (int i = 0; i < values.size(); i++) {
            visit(values.get(i).getElement(), p);
            if (i < values.size() - 1) {
                visitSpace(values.get(i).getAfter(), p);
                p.append(',');
            }
        }
        afterSyntax(enums, p);
        return enums;
    }

    @Override
    public J visitEnumValue(J.EnumValue enumValue, PrintOutputCapture<P> p) {
        beforeSyntax(enumValue, p);
        visit(enumValue.getName(), p);
        visit(enumValue.getInitializer(), p);
        afterSyntax(enumValue, p);
        return enumValue;
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, PrintOutputCapture<P> p) {
        if (isPrimaryConstructor(method)) {
            return method;
        }

        beforeSyntax(method, p);
        printMethodDeclaration(method, null, p);
        afterSyntax(method, p);
        return method;
    }

    /**
     * @param interfaceSpecifier The interface of an explicit interface implementation, which goes
     *                           between the return type and the method name.
     */
    private void printMethodDeclaration(J.MethodDeclaration method, @Nullable JRightPadded<TypeTree> interfaceSpecifier,
                                        PrintOutputCapture<P> p) {
        visitModifiers(method.getModifiers(), p);
        visit(method.getReturnTypeExpression(), p);
        visitRightPadded(interfaceSpecifier, ".", p);

        visit(method.getName(), p);

        J.TypeParameters typeParameters = method.getPadding().getTypeParameters();
        if (typeParameters != null) {
            visit(typeParameters, p);
        }

        JContainer<Statement> parameters = method.getPadding().getParameters();
        visitSpace(parameters.getBefore(), p);
        p.append('(');
        List<JRightPadded<Statement>> elements = parameters.getPadding().getElements();
        for (int i = 0; i < elements.size(); i++) {
            JRightPadded<Statement> parameter = elements.get(i);
            if (parameter.getElement() instanceof J.Empty) {
                // the space between empty parentheses
                visit(parameter.getElement(), p);
                visitSpace(parameter.getAfter(), p);
            } else {
                visitDeclaration(parameter.getElement(), p);
                visitSpace(parameter.getAfter(), p);
                if (i < elements.size() - 1) {
                    p.append(',');
                }
            }
        }
        p.append(')');

        printTypeParameterConstraintsInSourceOrder(typeParameters == null ? null : typeParameters.getPadding().getTypeParameters(), p);

        // a constructor initializer, `: base(...)` or `: this(...)`
        JLeftPadded<Expression> defaultValue = method.getPadding().getDefaultValue();
        if (defaultValue != null) {
            visitSpace(defaultValue.getBefore(), p);
            p.append(':');
            visit(defaultValue.getElement(), p);
        }

        J.Block body = method.getBody();
        if (body == null) {
            return;
        }
        if (!printExpressionBody(method, body, p)) {
            if (body.getMarkers().findFirst(Semicolon.class).isPresent()) {
                beforeSyntax(body, p);
                p.append(';');
                afterSyntax(body, p);
            } else {
                visitBlock(body, p);
            }
        }
    }

    /**
     * An expression-bodied member holds its expression as the sole return statement of its body.
     */
    private boolean printExpressionBody(J member, J.Block body, PrintOutputCapture<P> p) {
        List<JRightPadded<Statement>> statements = body.getPadding().getStatements();
        if (!member.getMarkers().findFirst(ExpressionBodied.class).isPresent() ||
            statements.isEmpty() ||
            !(statements.get(0).getElement() instanceof J.Return)) {
            return false;
        }
        J.Return return_ = (J.Return) statements.get(0).getElement();
        beforeSyntax(body, p);
        p.append("=>");
        beforeSyntax(return_, p);
        visit(return_.getExpression(), p);
        afterSyntax(return_, p);
        visitSpace(statements.get(0).getAfter(), p);
        p.append(';');
        afterSyntax(body, p);
        return true;
    }

    @Override
    public J visitReturn(J.Return return_, PrintOutputCapture<P> p) {
        beforeSyntax(return_, p);
        p.append("return");
        visit(return_.getExpression(), p);
        afterSyntax(return_, p);
        return return_;
    }

    @Override
    public J visitIf(J.If iff, PrintOutputCapture<P> p) {
        beforeSyntax(iff, p);
        p.append("if");
        visitControlParentheses(iff.getIfCondition(), p);
        visitStatement(iff.getPadding().getThenPart(), p);

        J.If.Else elsePart = iff.getElsePart();
        if (elsePart != null) {
            beforeSyntax(elsePart, p);
            p.append("else");
            visitStatement(elsePart.getPadding().getBody(), p);
            afterSyntax(elsePart, p);
        }

        afterSyntax(iff, p);
        return iff;
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop, PrintOutputCapture<P> p) {
        beforeSyntax(whileLoop, p);
        p.append("while");
        visitControlParentheses(whileLoop.getCondition(), p);
        visitStatement(whileLoop.getPadding().getBody(), p);
        afterSyntax(whileLoop, p);
        return whileLoop;
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, PrintOutputCapture<P> p) {
        beforeSyntax(doWhileLoop, p);
        p.append("do");
        visitStatement(doWhileLoop.getPadding().getBody(), p);
        visitSpace(doWhileLoop.getPadding().getWhileCondition().getBefore(), p);
        p.append("while");
        visitControlParentheses(doWhileLoop.getWhileCondition(), p);
        afterSyntax(doWhileLoop, p);
        return doWhileLoop;
    }

    @Override
    public J visitLabel(J.Label label, PrintOutputCapture<P> p) {
        beforeSyntax(label, p);
        visitRightPadded(label.getPadding().getLabel(), ":", p);
        visit(label.getStatement(), p);
        printStatementTerminator(label.getStatement(), p);
        afterSyntax(label, p);
        return label;
    }

    @Override
    public J visitSynchronized(J.Synchronized synch, PrintOutputCapture<P> p) {
        beforeSyntax(synch, p);
        p.append("lock");
        visitControlParentheses(synch.getLock(), p);
        visit(synch.getBody(), p);
        afterSyntax(synch, p);
        return synch;
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forLoop, p);
        p.append("for");
        J.ForLoop.Control control = forLoop.getControl();
        beforeSyntax(control, p);
        p.append('(');

        List<JRightPadded<Statement>> init = control.getPadding().getInit();
        for (int i = 0; i < init.size(); i++) {
            visitForLoopControlStatement(init.get(i).getElement(), p);
            if (i < init.size() - 1) {
                visitSpace(init.get(i).getAfter(), p);
                p.append(',');
            }
        }

        p.append(';');

        JRightPadded<Expression> condition = control.getPadding().getCondition();
        visit(condition.getElement(), p);
        visitSpace(condition.getAfter(), p);
        p.append(';');

        List<JRightPadded<Statement>> update = control.getPadding().getUpdate();
        for (int i = 0; i < update.size(); i++) {
            Statement statement = update.get(i).getElement();
            if (statement instanceof J.Empty) {
                // only holds the space before the closing parenthesis
                visit(statement, p);
            } else {
                visitForLoopControlStatement(statement, p);
            }
            visitSpace(update.get(i).getAfter(), p);
            if (i < update.size() - 1) {
                p.append(',');
            }
        }

        p.append(')');
        afterSyntax(control, p);
        visitStatement(forLoop.getPadding().getBody(), p);
        afterSyntax(forLoop, p);
        return forLoop;
    }

    /**
     * The initializers and incrementors of a for loop are comma separated rather than terminated.
     */
    private void visitForLoopControlStatement(Statement statement, PrintOutputCapture<P> p) {
        if (statement instanceof Cs.ExpressionStatement) {
            visit(((Cs.ExpressionStatement) statement).getExpression(), p);
        } else {
            visitDeclaration(statement, p);
        }
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forEachLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forEachLoop, p);
        p.append("foreach");
        J.ForEachLoop.Control control = forEachLoop.getControl();
        beforeSyntax(control, p);
        p.append('(');

        visitDeclaration(control.getPadding().getVariable().getElement(), p);
        visitSpace(control.getPadding().getVariable().getAfter(), p);
        p.append("in");

        visitRightPadded(control.getPadding().getIterable(), ")", p);
        afterSyntax(control, p);
        visitStatement(forEachLoop.getPadding().getBody(), p);
        afterSyntax(forEachLoop, p);
        return forEachLoop;
    }

    @Override
    public J visitTry(J.Try tryable, PrintOutputCapture<P> p) {
        beforeSyntax(tryable, p);
        p.append("try");
        visitBlock(tryable.getBody(), p);

        for (J.Try.Catch catchClause : tryable.getCatches()) {
            visitCatchClause(catchClause, p);
        }

        JLeftPadded<J.Block> finally_ = tryable.getPadding().getFinally();
        if (finally_ != null) {
            visitSpace(finally_.getBefore(), p);
            p.append("finally");
            visitBlock(finally_.getElement(), p);
        }

        afterSyntax(tryable, p);
        return tryable;
    }

    @Override
    public J visitThrow(J.Throw thrown, PrintOutputCapture<P> p) {
        beforeSyntax(thrown, p);
        p.append("throw");
        // a rethrow has an empty exception
        visit(thrown.getException(), p);
        afterSyntax(thrown, p);
        return thrown;
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, PrintOutputCapture<P> p) {
        beforeSyntax(instanceOf, p);
        visitRightPadded(instanceOf.getPadding().getExpression(), "is", p);
        visit(instanceOf.getClazz(), p);
        visit(instanceOf.getPattern(), p);
        afterSyntax(instanceOf, p);
        return instanceOf;
    }

    @Override
    public J visitBreak(J.Break breakStatement, PrintOutputCapture<P> p) {
        beforeSyntax(breakStatement, p);
        p.append("break");
        afterSyntax(breakStatement, p);
        return breakStatement;
    }

    @Override
    public J visitContinue(J.Continue continueStatement, PrintOutputCapture<P> p) {
        beforeSyntax(continueStatement, p);
        p.append("continue");
        afterSyntax(continueStatement, p);
        return continueStatement;
    }

    @Override
    public J visitEmpty(J.Empty empty, PrintOutputCapture<P> p) {
        beforeSyntax(empty, p);
        afterSyntax(empty, p);
        return empty;
    }

    @Override
    public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, PrintOutputCapture<P> p) {
        JRightPadded<T> tree = controlParens.getPadding().getTree();
        if (tree.getElement() instanceof J.VariableDeclarations) {
            // catch and fixed print these parentheses themselves
            visit(tree.getElement(), p);
            return controlParens;
        }

        beforeSyntax(controlParens, p);
        boolean omitParens = !isTypeOperand(controlParens) &&
                             controlParens.getMarkers().findFirst(OmitParentheses.class).isPresent();
        if (!omitParens) {
            p.append('(');
        }
        visit(tree.getElement(), p);
        visitSpace(tree.getAfter(), p);
        if (!omitParens) {
            p.append(')');
        }
        afterSyntax(controlParens, p);
        return controlParens;
    }

    /**
     * The operand of {@code sizeof} and {@code typeof} is always parenthesized.
     */
    private boolean isTypeOperand(J.ControlParentheses<?> controlParens) {
        if (getCursor().getValue() != controlParens) {
            return false;
        }
        Object parent = getCursor().getParentTreeCursor().getValue();
        return parent instanceof Cs.SizeOf || parent instanceof Cs.TypeOf;
    }

    @Override
    public J visitLiteral(J.Literal literal, PrintOutputCapture<P> p) {
        beforeSyntax(literal, p);
        p.append(literal.getValueSource() != null ? literal.getValueSource() : sourceOf(literal));
        afterSyntax(literal, p);
        return literal;
    }

    /**
     * The C# source for the value of a literal that has none of its own.
     */
    private static String sourceOf(J.Literal literal) {
        Object value = literal.getValue();
        if (value == null) {
            return "null";
        }
        if (value instanceof String || value instanceof Character) {
            char quote = literal.getType() == JavaType.Primitive.Char ? '\'' : '"';
            return quote + escape(value.toString(), quote) + quote;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString() + "m";
        }
        if (value instanceof Number) {
            // a number reaches the native printer without its subtype, so the type of the literal decides
            Number number = (Number) value;
            JavaType.Primitive type = literal.getType();
            if (type == JavaType.Primitive.Float || type == null && value instanceof Float) {
                return Float.toString(number.floatValue()) + "f";
            }
            if (type == JavaType.Primitive.Double) {
                return Double.toString(number.doubleValue());
            }
            if (type == JavaType.Primitive.Long) {
                return number.longValue() + "L";
            }
        }
        return value.toString();
    }

    private static String escape(String text, char quote) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '\0':
                    escaped.append("\\0");
                    break;
                default:
                    if (c == quote) {
                        escaped.append('\\');
                    }
                    escaped.append(c);
            }
        }
        return escaped.toString();
    }

    @Override
    public J visitInterpolatedString(Cs.InterpolatedString interpolatedString, PrintOutputCapture<P> p) {
        beforeSyntax(interpolatedString, p);
        p.append(interpolatedString.getStart());
        visit(interpolatedString.getParts(), p);
        // for a raw string the end delimiter includes the newline and indentation before the closing quotes
        p.append(interpolatedString.getEnd());
        afterSyntax(interpolatedString, p);
        return interpolatedString;
    }

    @Override
    public J visitInterpolation(Cs.Interpolation interpolation, PrintOutputCapture<P> p) {
        // a raw string with multiple `$` delimits its interpolations with as many braces
        int braceCount = 1;
        Object parent = getCursor().getParentTreeCursor().getValue();
        if (parent instanceof Cs.InterpolatedString) {
            int dollarCount = 0;
            for (char c : ((Cs.InterpolatedString) parent).getStart().toCharArray()) {
                if (c == '$') {
                    dollarCount++;
                }
            }
            if (dollarCount > 1) {
                braceCount = dollarCount;
            }
        }

        // the prefix is the space inside the braces, so the markers go outside them
        beforeSyntax(Space.EMPTY, interpolation.getMarkers(), p);
        for (int i = 0; i < braceCount; i++) {
            p.append('{');
        }

        visitSpace(interpolation.getPrefix(), p);
        visit(interpolation.getExpression(), p);

        if (interpolation.getAlignment() != null) {
            visitSpace(interpolation.getAlignmentBefore(), p);
            p.append(',');
            visit(interpolation.getAlignment(), p);
        }

        if (interpolation.getFormat() != null) {
            visitSpace(interpolation.getFormatBefore(), p);
            p.append(':');
            visit(interpolation.getFormat(), p);
        }

        visitSpace(interpolation.getPadding().getExpression().getAfter(), p);

        for (int i = 0; i < braceCount; i++) {
            p.append('}');
        }
        afterSyntax(interpolation.getMarkers(), p);
        return interpolation;
    }

    @Override
    public J visitAwaitExpression(Cs.AwaitExpression awaitExpression, PrintOutputCapture<P> p) {
        beforeSyntax(awaitExpression, p);
        p.append("await");
        visit(awaitExpression.getExpression(), p);
        afterSyntax(awaitExpression, p);
        return awaitExpression;
    }

    @Override
    public J visitYield(Cs.Yield yield, PrintOutputCapture<P> p) {
        beforeSyntax(yield, p);
        p.append("yield");
        visit(yield.getReturnOrBreakKeyword(), p);
        visit(yield.getExpression(), p);
        afterSyntax(yield, p);
        return yield;
    }

    @Override
    public J visitIdentifier(J.Identifier ident, PrintOutputCapture<P> p) {
        beforeSyntax(ident, p);
        p.append(ident.getSimpleName());
        afterSyntax(ident, p);
        return ident;
    }

    @Override
    public J visitBinary(J.Binary binary, PrintOutputCapture<P> p) {
        beforeSyntax(binary, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), p);
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
            case BitAnd:
                p.append('&');
                break;
            case BitOr:
                p.append('|');
                break;
            case BitXor:
                p.append('^');
                break;
            case LeftShift:
                p.append("<<");
                break;
            case RightShift:
                p.append(">>");
                break;
            case UnsignedRightShift:
                p.append(">>>");
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
    public J visitTernary(J.Ternary ternary, PrintOutputCapture<P> p) {
        beforeSyntax(ternary, p);
        visit(ternary.getCondition(), p);

        JLeftPadded<Expression> falsePart = ternary.getPadding().getFalsePart();
        if (ternary.getMarkers().findFirst(Cs.NullCoalescing.class).isPresent()) {
            visitMarkersOf(ternary.getTruePart(), p);
            visitSpace(falsePart.getBefore(), p);
            p.append("??");
            visit(falsePart.getElement(), p);
        } else {
            visitSpace(ternary.getPadding().getTruePart().getBefore(), p);
            p.append('?');
            visit(ternary.getTruePart(), p);
            visitSpace(falsePart.getBefore(), p);
            p.append(':');
            visit(falsePart.getElement(), p);
        }

        afterSyntax(ternary, p);
        return ternary;
    }

    @Override
    public J visitSwitch(J.Switch switch_, PrintOutputCapture<P> p) {
        beforeSyntax(switch_, p);
        p.append("switch");
        visitControlParentheses(switch_.getSelector(), p);
        visitBlock(switch_.getCases(), p);
        afterSyntax(switch_, p);
        return switch_;
    }

    @Override
    public J visitCase(J.Case case_, PrintOutputCapture<P> p) {
        // a case belongs to its nearest switch, and only a switch expression drops the `case` keyword
        boolean inSwitchExpression = getCursor().getPathAsStream()
                .filter(v -> v instanceof J.Switch || v instanceof Cs.SwitchExpression)
                .findFirst()
                .orElse(null) instanceof Cs.SwitchExpression;

        beforeSyntax(case_, p);

        // unlike Java, a case has exactly one label
        List<JRightPadded<J>> labels = case_.getPadding().getCaseLabels().getPadding().getElements();
        if (!labels.isEmpty()) {
            J label = labels.get(0).getElement();

            if (inSwitchExpression) {
                visitDeclaration(label, p);
                visitSpace(labels.get(0).getAfter(), p);

                if (case_.getGuard() != null) {
                    p.append("when");
                    visit(case_.getGuard(), p);
                }

                JRightPadded<J> body = case_.getPadding().getBody();
                if (body != null) {
                    visitSpace(body.getAfter(), p);
                    p.append("=>");
                    visit(body.getElement(), p);
                }
            } else {
                if (label instanceof J.Identifier && "default".equals(((J.Identifier) label).getSimpleName())) {
                    visit(label, p);
                } else {
                    p.append("case");
                    visitDeclaration(label, p);
                }

                if (case_.getType() == J.Case.Type.Statement) {
                    visitSpace(labels.get(0).getAfter(), p);

                    if (case_.getGuard() != null) {
                        p.append("when");
                        visit(case_.getGuard(), p);
                    }

                    p.append(':');
                }
            }
        }

        if (!inSwitchExpression && case_.getType() == J.Case.Type.Statement) {
            for (JRightPadded<Statement> statement : case_.getPadding().getStatements().getPadding().getElements()) {
                visitStatement(statement, p);
            }
        }

        if (!inSwitchExpression && case_.getType() == J.Case.Type.Rule && case_.getPadding().getBody() != null) {
            p.append("=>");
            visit(case_.getBody(), p);
        }

        afterSyntax(case_, p);
        return case_;
    }

    @Override
    public J visitSwitchExpression(Cs.SwitchExpression switchExpression, PrintOutputCapture<P> p) {
        beforeSyntax(switchExpression, p);
        visitRightPadded(switchExpression.getPadding().getExpression(), "switch", p);
        visitContainer("{", switchExpression.getPadding().getArms(), ",", "}", p);
        afterSyntax(switchExpression, p);
        return switchExpression;
    }

    @Override
    public J visitSwitchExpressionArm(Cs.SwitchExpressionArm switchExpressionArm, PrintOutputCapture<P> p) {
        beforeSyntax(switchExpressionArm, p);
        visit(switchExpressionArm.getPattern(), p);

        JLeftPadded<Expression> whenExpression = switchExpressionArm.getPadding().getWhenExpression();
        if (whenExpression != null) {
            visitSpace(whenExpression.getBefore(), p);
            p.append("when");
            visit(whenExpression.getElement(), p);
        }

        visitSpace(switchExpressionArm.getPadding().getExpression().getBefore(), p);
        p.append("=>");
        visit(switchExpressionArm.getExpression(), p);
        afterSyntax(switchExpressionArm, p);
        return switchExpressionArm;
    }

    @Override
    public J visitConstantPattern(Cs.ConstantPattern constantPattern, PrintOutputCapture<P> p) {
        beforeSyntax(constantPattern, p);
        visit(constantPattern.getValue(), p);
        afterSyntax(constantPattern, p);
        return constantPattern;
    }

    @Override
    public J visitDiscardPattern(Cs.DiscardPattern discardPattern, PrintOutputCapture<P> p) {
        beforeSyntax(discardPattern, p);
        p.append('_');
        afterSyntax(discardPattern, p);
        return discardPattern;
    }

    @Override
    public J visitEnumDeclaration(Cs.EnumDeclaration enumDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(enumDeclaration, p);
        visit(enumDeclaration.getAttributeLists(), p);
        visitModifiers(enumDeclaration.getModifiers(), p);

        visitSpace(enumDeclaration.getPadding().getName().getBefore(), p);
        p.append("enum");
        visit(enumDeclaration.getName(), p);

        JLeftPadded<TypeTree> baseType = enumDeclaration.getPadding().getBaseType();
        if (baseType != null) {
            visitSpace(baseType.getBefore(), p);
            p.append(':');
            visit(baseType.getElement(), p);
        }

        visitContainer("{", enumDeclaration.getPadding().getMembers(), ",", "}", p);
        afterSyntax(enumDeclaration, p);
        return enumDeclaration;
    }

    @Override
    public J visitEnumMemberDeclaration(Cs.EnumMemberDeclaration enumMemberDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(enumMemberDeclaration, p);
        visit(enumMemberDeclaration.getAttributeLists(), p);
        visit(enumMemberDeclaration.getName(), p);
        JLeftPadded<Expression> initializer = enumMemberDeclaration.getPadding().getInitializer();
        if (initializer != null) {
            visitSpace(initializer.getBefore(), p);
            p.append('=');
            visit(initializer.getElement(), p);
        }
        afterSyntax(enumMemberDeclaration, p);
        return enumMemberDeclaration;
    }

    @Override
    public J visitCheckedExpression(Cs.CheckedExpression checkedExpression, PrintOutputCapture<P> p) {
        beforeSyntax(checkedExpression, p);
        visit(checkedExpression.getCheckedOrUncheckedKeyword(), p);
        visitControlParentheses(checkedExpression.getExpression(), p);
        afterSyntax(checkedExpression, p);
        return checkedExpression;
    }

    @Override
    public J visitCheckedStatement(Cs.CheckedStatement checkedStatement, PrintOutputCapture<P> p) {
        beforeSyntax(checkedStatement, p);
        visit(checkedStatement.getKeyword(), p);
        visit(checkedStatement.getBlock(), p);
        afterSyntax(checkedStatement, p);
        return checkedStatement;
    }

    @Override
    public J visitKeyword(Cs.Keyword keyword, PrintOutputCapture<P> p) {
        beforeSyntax(keyword, p);
        switch (keyword.getKind()) {
            case Ref:
                p.append("ref");
                break;
            case Out:
                p.append("out");
                break;
            case Await:
                p.append("await");
                break;
            case Base:
                p.append("base");
                break;
            case This:
                p.append("this");
                break;
            case Break:
                p.append("break");
                break;
            case Return:
                p.append("return");
                break;
            case Not:
                p.append("not");
                break;
            case Default:
                p.append("default");
                break;
            case Case:
                p.append("case");
                break;
            case Checked:
                p.append("checked");
                break;
            case Unchecked:
                p.append("unchecked");
                break;
            case Operator:
                p.append("operator");
                break;
        }
        afterSyntax(keyword, p);
        return keyword;
    }

    @Override
    public J visitRangeExpression(Cs.RangeExpression rangeExpression, PrintOutputCapture<P> p) {
        beforeSyntax(rangeExpression, p);
        JRightPadded<Expression> start = rangeExpression.getPadding().getStart();
        if (start != null) {
            visit(start.getElement(), p);
            visitSpace(start.getAfter(), p);
        }
        p.append("..");
        visit(rangeExpression.getEnd(), p);
        afterSyntax(rangeExpression, p);
        return rangeExpression;
    }

    @Override
    public J visitStackAllocExpression(Cs.StackAllocExpression stackAllocExpression, PrintOutputCapture<P> p) {
        beforeSyntax(stackAllocExpression, p);
        p.append("stackalloc");
        visit(stackAllocExpression.getExpression(), p);
        afterSyntax(stackAllocExpression, p);
        return stackAllocExpression;
    }

    @Override
    public J visitCollectionExpression(Cs.CollectionExpression collectionExpression, PrintOutputCapture<P> p) {
        beforeSyntax(collectionExpression, p);
        p.append('[');
        List<JRightPadded<Expression>> elements = collectionExpression.getPadding().getElements();
        for (int i = 0; i < elements.size(); i++) {
            JRightPadded<Expression> element = elements.get(i);
            if (element.getElement() instanceof J.Empty) {
                // holds the space inside an otherwise empty collection
                visit(element.getElement(), p);
                visitSpace(element.getAfter(), p);
            } else {
                visit(element.getElement(), p);
                visitSpace(element.getAfter(), p);
                if (i < elements.size() - 1) {
                    p.append(',');
                } else {
                    printTrailingComma(element.getMarkers(), p);
                }
            }
        }
        p.append(']');
        afterSyntax(collectionExpression, p);
        return collectionExpression;
    }

    @Override
    public J visitAllowsConstraintClause(Cs.AllowsConstraintClause allowsConstraintClause, PrintOutputCapture<P> p) {
        beforeSyntax(allowsConstraintClause, p);
        p.append("allows");
        visitContainer("", allowsConstraintClause.getPadding().getExpressions(), ",", "", p);
        afterSyntax(allowsConstraintClause, p);
        return allowsConstraintClause;
    }

    @Override
    public J visitRefStructConstraint(Cs.RefStructConstraint refStructConstraint, PrintOutputCapture<P> p) {
        beforeSyntax(refStructConstraint, p);
        p.append("ref struct");
        afterSyntax(refStructConstraint, p);
        return refStructConstraint;
    }

    @Override
    public J visitClassOrStructConstraint(Cs.ClassOrStructConstraint classOrStructConstraint, PrintOutputCapture<P> p) {
        beforeSyntax(classOrStructConstraint, p);
        p.append(classOrStructConstraint.getKind() == Cs.ClassOrStructConstraint.TypeKind.Class ? "class" : "struct");
        if (classOrStructConstraint.isNullable()) {
            p.append('?');
        }
        afterSyntax(classOrStructConstraint, p);
        return classOrStructConstraint;
    }

    @Override
    public J visitConstructorConstraint(Cs.ConstructorConstraint constructorConstraint, PrintOutputCapture<P> p) {
        beforeSyntax(constructorConstraint, p);
        p.append("new()");
        afterSyntax(constructorConstraint, p);
        return constructorConstraint;
    }

    @Override
    public J visitDefaultConstraint(Cs.DefaultConstraint defaultConstraint, PrintOutputCapture<P> p) {
        beforeSyntax(defaultConstraint, p);
        p.append("default");
        afterSyntax(defaultConstraint, p);
        return defaultConstraint;
    }

    @Override
    public J visitLambda(J.Lambda lambda, PrintOutputCapture<P> p) {
        boolean anonymousMethod = lambda.getMarkers().findFirst(AnonymousMethod.class).isPresent();

        beforeSyntax(lambda, p);

        if (anonymousMethod) {
            p.append("delegate");
        }

        J.Lambda.Parameters parameters = lambda.getParameters();
        List<JRightPadded<J>> elements = parameters.getPadding().getParameters();
        if (parameters.isParenthesized()) {
            // the prefix is the space before `(` of an anonymous method, but the space after it for a lambda
            beforeSyntax(anonymousMethod ? parameters.getPrefix() : Space.EMPTY, parameters.getMarkers(), p);
            p.append('(');
            if (!anonymousMethod) {
                visitSpace(parameters.getPrefix(), p);
            }
            visitDeclarations(elements, p);
            p.append(')');
            afterSyntax(parameters, p);
        } else if (!anonymousMethod) {
            beforeSyntax(parameters, p);
            for (JRightPadded<J> element : elements) {
                visitDeclaration(element.getElement(), p);
            }
            afterSyntax(parameters, p);
        } else {
            visitMarkersOf(parameters, p);
        }

        if (!anonymousMethod) {
            visitSpace(lambda.getArrow(), p);
            p.append("=>");
        }

        visit(lambda.getBody(), p);
        afterSyntax(lambda, p);
        return lambda;
    }

    @Override
    public J visitLambda(Cs.Lambda lambda, PrintOutputCapture<P> p) {
        beforeSyntax(lambda, p);
        visit(lambda.getAttributeLists(), p);
        visitModifiers(lambda.getModifiers(), p);
        visit(lambda.getReturnType(), p);
        visitLambda(lambda.getLambdaExpression(), p);
        afterSyntax(lambda, p);
        return lambda;
    }

    @Override
    public J visitAssignment(J.Assignment assignment, PrintOutputCapture<P> p) {
        beforeSyntax(assignment, p);
        visit(assignment.getVariable(), p);
        visitSpace(assignment.getPadding().getAssignment().getBefore(), p);
        p.append('=');
        visit(assignment.getAssignment(), p);
        afterSyntax(assignment, p);
        return assignment;
    }

    @Override
    public J visitAssignmentOperation(Cs.AssignmentOperation assignmentOperation, PrintOutputCapture<P> p) {
        beforeSyntax(assignmentOperation, p);
        visit(assignmentOperation.getVariable(), p);
        visitSpace(assignmentOperation.getPadding().getOperator().getBefore(), p);
        switch (assignmentOperation.getOperator()) {
            case Addition:
                p.append("+=");
                break;
            case Subtraction:
                p.append("-=");
                break;
            case Multiplication:
                p.append("*=");
                break;
            case Division:
                p.append("/=");
                break;
            case Modulo:
                p.append("%=");
                break;
            case BitAnd:
                p.append("&=");
                break;
            case BitOr:
                p.append("|=");
                break;
            case BitXor:
                p.append("^=");
                break;
            case LeftShift:
                p.append("<<=");
                break;
            case RightShift:
                p.append(">>=");
                break;
            case UnsignedRightShift:
                p.append(">>>=");
                break;
            case Coalesce:
            case NullCoalescing:
                p.append("??=");
                break;
        }
        visit(assignmentOperation.getAssignment(), p);
        afterSyntax(assignmentOperation, p);
        return assignmentOperation;
    }

    @Override
    public J visitUnary(J.Unary unary, PrintOutputCapture<P> p) {
        beforeSyntax(unary, p);
        switch (unary.getOperator()) {
            case PreIncrement:
                p.append("++");
                visit(unary.getExpression(), p);
                break;
            case PreDecrement:
                p.append("--");
                visit(unary.getExpression(), p);
                break;
            case PostIncrement:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), p);
                p.append("++");
                break;
            case PostDecrement:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), p);
                p.append("--");
                break;
            case Positive:
                p.append('+');
                visit(unary.getExpression(), p);
                break;
            case Negative:
                p.append('-');
                visit(unary.getExpression(), p);
                break;
            case Complement:
                p.append('~');
                visit(unary.getExpression(), p);
                break;
            case Not:
                p.append('!');
                visit(unary.getExpression(), p);
                break;
        }
        afterSyntax(unary, p);
        return unary;
    }

    @Override
    public J visitUnary(Cs.Unary unary, PrintOutputCapture<P> p) {
        beforeSyntax(unary, p);
        switch (unary.getOperator()) {
            case SuppressNullableWarning:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), p);
                p.append('!');
                break;
            case PointerType:
                p.append('*');
                visit(unary.getExpression(), p);
                break;
            case AddressOf:
                p.append('&');
                visit(unary.getExpression(), p);
                break;
            case Spread:
                p.append("..");
                visit(unary.getExpression(), p);
                break;
            case FromEnd:
                p.append('^');
                visit(unary.getExpression(), p);
                break;
            case Not:
                p.append("not");
                visit(unary.getExpression(), p);
                break;
        }
        afterSyntax(unary, p);
        return unary;
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, PrintOutputCapture<P> p) {
        beforeSyntax(parens, p);
        p.append('(');
        visitRightPadded(parens.getPadding().getTree(), ")", p);
        afterSyntax(parens, p);
        return parens;
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast, PrintOutputCapture<P> p) {
        beforeSyntax(typeCast, p);
        beforeSyntax(typeCast.getClazz(), p);
        p.append('(');
        visitRightPadded(typeCast.getClazz().getPadding().getTree(), ")", p);
        afterSyntax(typeCast.getClazz(), p);
        visit(typeCast.getExpression(), p);
        afterSyntax(typeCast, p);
        return typeCast;
    }

    @Override
    public J visitExpressionStatement(Cs.ExpressionStatement expressionStatement, PrintOutputCapture<P> p) {
        // the prefix and markers of an expression statement are those of its expression
        visit(expressionStatement.getExpression(), p);
        return expressionStatement;
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
        beforeSyntax(multiVariable, p);
        visitModifiers(multiVariable.getModifiers(), p);
        visit(multiVariable.getTypeExpression(), p);

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = multiVariable.getPadding().getVariables();
        for (int i = 0; i < variables.size(); i++) {
            visit(variables.get(i).getElement(), p);

            if (i < variables.size() - 1) {
                visitSpace(variables.get(i).getAfter(), p);
                p.append(',');
            }
        }

        afterSyntax(multiVariable, p);
        return multiVariable;
    }

    @Override
    public J visitVariable(J.VariableDeclarations.NamedVariable variable, PrintOutputCapture<P> p) {
        beforeSyntax(variable, p);
        visit(variable.getName(), p);

        JLeftPadded<Expression> initializer = variable.getPadding().getInitializer();
        if (initializer != null) {
            visitSpace(initializer.getBefore(), p);
            p.append('=');
            visit(initializer.getElement(), p);
        }

        afterSyntax(variable, p);
        return variable;
    }

    /**
     * Parameters, patterns and loop variables are variable declarations that are not statements.
     */
    private void visitDeclaration(J tree, PrintOutputCapture<P> p) {
        if (tree instanceof J.VariableDeclarations) {
            visitVariableDeclarations((J.VariableDeclarations) tree, p);
        } else {
            visit(tree, p);
        }
    }

    private void visitDeclarations(List<? extends JRightPadded<? extends J>> declarations, PrintOutputCapture<P> p) {
        for (int i = 0; i < declarations.size(); i++) {
            visitDeclaration(declarations.get(i).getElement(), p);
            visitSpace(declarations.get(i).getAfter(), p);
            if (i < declarations.size() - 1) {
                p.append(',');
            }
        }
    }

    private void printParameterList(String open, @Nullable JContainer<? extends J> container, String close, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        visitSpace(container.getBefore(), p);
        p.append(open);
        List<? extends JRightPadded<? extends J>> elements = container.getPadding().getElements();
        for (int i = 0; i < elements.size(); i++) {
            visitDeclaration(elements.get(i).getElement(), p);
            visitSpace(elements.get(i).getAfter(), p);
            if (i < elements.size() - 1) {
                p.append(',');
            }
        }
        p.append(close);
    }

    @Override
    public J visitPrimitive(J.Primitive primitive, PrintOutputCapture<P> p) {
        beforeSyntax(primitive, p);
        switch (primitive.getType()) {
            case Int:
                p.append("int");
                break;
            case Long:
                p.append("long");
                break;
            case Short:
                p.append("short");
                break;
            case Byte:
                p.append("byte");
                break;
            case Float:
                p.append("float");
                break;
            case Double:
                p.append("double");
                break;
            case Boolean:
                p.append("bool");
                break;
            case Char:
                p.append("char");
                break;
            case String:
                p.append("string");
                break;
            case Void:
                p.append("void");
                break;
            default:
                break;
        }
        afterSyntax(primitive, p);
        return primitive;
    }

    private void visitModifiers(List<J.Modifier> modifiers, PrintOutputCapture<P> p) {
        for (J.Modifier modifier : modifiers) {
            beforeSyntax(modifier, p);
            switch (modifier.getType()) {
                case Public:
                    p.append("public");
                    break;
                case Private:
                    p.append("private");
                    break;
                case Protected:
                    p.append("protected");
                    break;
                case Static:
                    p.append("static");
                    break;
                case Abstract:
                    p.append("abstract");
                    break;
                case Sealed:
                    p.append("sealed");
                    break;
                case Async:
                    p.append("async");
                    break;
                case Volatile:
                    p.append("volatile");
                    break;
                case LanguageExtension:
                    // every modifier that Java does not share with C#, such as `internal` or `readonly`
                    p.append(modifier.getKeyword());
                    break;
                default:
                    break;
            }
            afterSyntax(modifier, p);
        }
    }

    @Override
    public Space visitSpace(@Nullable Space space, Space.Location loc, PrintOutputCapture<P> p) {
        // only reached from inherited traversals, which print nothing in the native printer either
        return space;
    }

    protected void visitSpace(Space space, PrintOutputCapture<P> p) {
        p.append(space.getWhitespace());
        for (Comment comment : space.getComments()) {
            comment.printComment(getCursor(), p);
            p.append(comment.getSuffix());
        }
    }

    @Override
    public J visitConditionalDirective(Cs.ConditionalDirective conditionalDirective, PrintOutputCapture<P> p) {
        // Each branch is the whole file as compiled for one set of symbols, with a ghost comment
        // standing in for every directive line.
        List<Cs.CompilationUnit> branches = conditionalDirective.getBranches();
        String[] branchOutputs = new String[branches.size()];
        for (int i = 0; i < branches.size(); i++) {
            PrintOutputCapture<P> capture = new PrintOutputCapture<>(p.getContext(), p.getMarkerPrinter());
            visit(branches.get(i), capture);
            branchOutputs[i] = capture.getOut();
        }

        // Split each branch at its ghost comments, so that N directives leave N + 1 sections.
        List<List<String>> branchSections = new ArrayList<>(branchOutputs.length);
        List<List<Boolean>> branchTrailingNewlines = new ArrayList<>(branchOutputs.length);
        List<Integer> directiveOrder = null;

        for (String branchOutput : branchOutputs) {
            List<String> sections = new ArrayList<>();
            List<Boolean> trailingNewlines = new ArrayList<>();
            List<Integer> directiveIndices = new ArrayList<>();

            Matcher ghost = GHOST_COMMENT.matcher(branchOutput);
            int lastEnd = 0;
            while (ghost.find()) {
                sections.add(branchOutput.substring(lastEnd, ghost.start()));
                directiveIndices.add(Integer.parseInt(ghost.group(1)));
                trailingNewlines.add(ghost.group().endsWith("\n"));
                lastEnd = ghost.end();
            }
            sections.add(branchOutput.substring(lastEnd));

            branchSections.add(sections);
            branchTrailingNewlines.add(trailingNewlines);
            if (directiveOrder == null) {
                directiveOrder = directiveIndices;
            }
        }

        if (directiveOrder == null || directiveOrder.isEmpty()) {
            p.append(branchOutputs[0]);
            return conditionalDirective;
        }

        // Interleave the directive lines with the section of whichever branch is active after each one.
        Deque<Integer> activeBranches = new ArrayDeque<>();
        activeBranches.push(0);

        p.append(branchSections.get(0).get(0));

        for (int d = 0; d < directiveOrder.size(); d++) {
            Cs.DirectiveLine directive = conditionalDirective.getDirectiveLines().get(directiveOrder.get(d));

            p.append(directive.getText());
            if (branchTrailingNewlines.get(0).get(d)) {
                p.append('\n');
            }

            // A recipe can remove the node whose prefix carried the ghost comment of an #if,
            // leaving its #elif, #else or #endif with nothing to pop.
            switch (directive.getKind()) {
                case If:
                    activeBranches.push(directive.getActiveBranchIndex());
                    break;
                case Elif:
                case Else:
                    activeBranches.poll();
                    activeBranches.push(directive.getActiveBranchIndex());
                    break;
                case Endif:
                    activeBranches.poll();
                    break;
            }

            // no branch activates a directive whose index is negative, so the primary one is used
            int activeBranch = activeBranches.isEmpty() ? 0 : Math.max(activeBranches.peek(), 0);
            if (activeBranch < branchSections.size() && d + 1 < branchSections.get(activeBranch).size()) {
                p.append(branchSections.get(activeBranch).get(d + 1));
            }
        }

        return conditionalDirective;
    }

    @Override
    public J visitPragmaWarningDirective(Cs.PragmaWarningDirective pragmaWarningDirective, PrintOutputCapture<P> p) {
        beforeSyntax(pragmaWarningDirective, p);
        p.append("#pragma");
        visitSpace(orSingleSpace(pragmaWarningDirective.getKeywordSpacing()), p);
        p.append("warning");
        visitSpace(orSingleSpace(pragmaWarningDirective.getActionSpacing()), p);
        p.append(pragmaWarningDirective.getAction() == Cs.PragmaWarningDirective.PragmaWarningAction.Disable ? "disable" : "restore");
        visitRightPadded(pragmaWarningDirective.getPadding().getWarningCodes(), ",", p);
        afterSyntax(pragmaWarningDirective, p);
        return pragmaWarningDirective;
    }

    @Override
    public J visitPragmaChecksumDirective(Cs.PragmaChecksumDirective pragmaChecksumDirective, PrintOutputCapture<P> p) {
        beforeSyntax(pragmaChecksumDirective, p);
        p.append("#pragma");
        visitSpace(orSingleSpace(pragmaChecksumDirective.getKeywordSpacing()), p);
        p.append("checksum");
        p.append(pragmaChecksumDirective.getArguments());
        afterSyntax(pragmaChecksumDirective, p);
        return pragmaChecksumDirective;
    }

    /**
     * Keeps a directive valid when its LST was built without the spacing between its keywords.
     */
    private static Space orSingleSpace(Space space) {
        return space.getWhitespace().isEmpty() && space.getComments().isEmpty() ? Space.SINGLE_SPACE : space;
    }

    @Override
    public J visitNullableDirective(Cs.NullableDirective nullableDirective, PrintOutputCapture<P> p) {
        beforeSyntax(nullableDirective, p);
        p.append('#');
        p.append(nullableDirective.getHashSpacing());
        p.append("nullable");
        p.append(nullableDirective.getKeywordSpacing());
        p.append(nullableDirective.getSetting().name().toLowerCase(Locale.ROOT));
        if (nullableDirective.getTarget() != null) {
            p.append(' ');
            p.append(nullableDirective.getTarget().name().toLowerCase(Locale.ROOT));
        }
        p.append(nullableDirective.getTrailingComment());
        afterSyntax(nullableDirective, p);
        return nullableDirective;
    }

    @Override
    public J visitRegionDirective(Cs.RegionDirective regionDirective, PrintOutputCapture<P> p) {
        beforeSyntax(regionDirective, p);
        p.append('#');
        p.append(regionDirective.getHashSpacing());
        p.append("region");
        p.append(regionDirective.getName());
        afterSyntax(regionDirective, p);
        return regionDirective;
    }

    @Override
    public J visitEndRegionDirective(Cs.EndRegionDirective endRegionDirective, PrintOutputCapture<P> p) {
        beforeSyntax(endRegionDirective, p);
        p.append('#');
        p.append(endRegionDirective.getHashSpacing());
        p.append("endregion");
        p.append(endRegionDirective.getName());
        afterSyntax(endRegionDirective, p);
        return endRegionDirective;
    }

    @Override
    public J visitDefineDirective(Cs.DefineDirective defineDirective, PrintOutputCapture<P> p) {
        beforeSyntax(defineDirective, p);
        p.append("#define");
        visit(defineDirective.getSymbol(), p);
        afterSyntax(defineDirective, p);
        return defineDirective;
    }

    @Override
    public J visitUndefDirective(Cs.UndefDirective undefDirective, PrintOutputCapture<P> p) {
        beforeSyntax(undefDirective, p);
        p.append("#undef");
        visit(undefDirective.getSymbol(), p);
        afterSyntax(undefDirective, p);
        return undefDirective;
    }

    @Override
    public J visitErrorDirective(Cs.ErrorDirective errorDirective, PrintOutputCapture<P> p) {
        beforeSyntax(errorDirective, p);
        p.append("#error");
        p.append(' ');
        p.append(errorDirective.getMessage());
        afterSyntax(errorDirective, p);
        return errorDirective;
    }

    @Override
    public J visitWarningDirective(Cs.WarningDirective warningDirective, PrintOutputCapture<P> p) {
        beforeSyntax(warningDirective, p);
        p.append("#warning");
        p.append(' ');
        p.append(warningDirective.getMessage());
        afterSyntax(warningDirective, p);
        return warningDirective;
    }

    @Override
    public J visitLineDirective(Cs.LineDirective lineDirective, PrintOutputCapture<P> p) {
        beforeSyntax(lineDirective, p);
        p.append("#line");
        switch (lineDirective.getKind()) {
            case Hidden:
                p.append(" hidden");
                break;
            case Default:
                p.append(" default");
                break;
            case Numeric:
                visit(lineDirective.getLine(), p);
                break;
        }
        visit(lineDirective.getFile(), p);
        afterSyntax(lineDirective, p);
        return lineDirective;
    }

    protected void beforeSyntax(J j, PrintOutputCapture<P> p) {
        beforeSyntax(j.getPrefix(), j.getMarkers(), p);
    }

    protected void beforeSyntax(Space prefix, Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), CSHARP_MARKER_WRAPPER));
        }
        visitSpace(prefix, p);
        printTrailingComma(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), CSHARP_MARKER_WRAPPER));
        }
    }

    /**
     * A tree with no text of its own still has its markers printed.
     */
    private void visitMarkersOf(J tree, PrintOutputCapture<P> p) {
        beforeSyntax(Space.EMPTY, tree.getMarkers(), p);
        afterSyntax(tree.getMarkers(), p);
    }

    protected void afterSyntax(J j, PrintOutputCapture<P> p) {
        afterSyntax(j.getMarkers(), p);
    }

    protected void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), CSHARP_MARKER_WRAPPER));
        }
    }

    private void printTrailingComma(Markers markers, PrintOutputCapture<P> p) {
        TrailingComma trailingComma = markers.findFirst(TrailingComma.class).orElse(null);
        if (trailingComma != null) {
            p.append(',');
            visitSpace(trailingComma.getSuffix(), p);
        }
    }

    private void printTypeArguments(JContainer<Expression> typeArguments, PrintOutputCapture<P> p) {
        visitSpace(typeArguments.getBefore(), p);
        p.append('<');
        List<JRightPadded<Expression>> elements = typeArguments.getPadding().getElements();
        for (int i = 0; i < elements.size(); i++) {
            visit(elements.get(i).getElement(), p);
            visitSpace(elements.get(i).getAfter(), p);
            if (i < elements.size() - 1) {
                p.append(',');
            }
        }
        p.append('>');
    }

    /**
     * An empty argument list holds a single {@link J.Empty} whose trailing space is the space
     * between the parentheses.
     */
    private void visitArguments(JContainer<Expression> arguments, PrintOutputCapture<P> p) {
        visitSpace(arguments.getBefore(), p);
        p.append('(');
        List<JRightPadded<Expression>> elements = arguments.getPadding().getElements();
        for (int i = 0; i < elements.size(); i++) {
            JRightPadded<Expression> argument = elements.get(i);
            if (argument.getElement() instanceof J.Empty) {
                visit(argument.getElement(), p);
                visitSpace(argument.getAfter(), p);
            } else {
                visit(argument.getElement(), p);
                visitSpace(argument.getAfter(), p);
                if (i < elements.size() - 1) {
                    p.append(',');
                }
            }
        }
        p.append(')');
    }

    protected void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), p);
            printTrailingComma(node.getMarkers(), p);
            if (i < nodes.size() - 1) {
                p.append(suffixBetween);
            }
        }
    }

    protected void visitRightPadded(@Nullable JRightPadded<? extends J> rightPadded, String suffix, PrintOutputCapture<P> p) {
        if (rightPadded != null) {
            visit(rightPadded.getElement(), p);
            visitSpace(rightPadded.getAfter(), p);
            p.append(suffix);
        }
    }

    protected void visitContainer(String before, @Nullable JContainer<? extends J> container, String suffixBetween,
                                  String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        visitSpace(container.getBefore(), p);
        p.append(before);
        visitRightPadded(container.getPadding().getElements(), suffixBetween, p);
        p.append(after);
    }

    @Override
    public J visitGotoStatement(Cs.GotoStatement gotoStatement, PrintOutputCapture<P> p) {
        beforeSyntax(gotoStatement, p);
        p.append("goto");

        Cs.Keyword caseOrDefault = gotoStatement.getCaseOrDefaultKeyword();
        if (caseOrDefault != null) {
            beforeSyntax(caseOrDefault, p);
            p.append(caseOrDefault.getKind() == Cs.Keyword.KeywordKind.Case ? "case" : "default");
            afterSyntax(caseOrDefault, p);
        }

        visit(gotoStatement.getTarget(), p);
        afterSyntax(gotoStatement, p);
        return gotoStatement;
    }

    @Override
    public J visitUsingStatement(Cs.UsingStatement usingStatement, PrintOutputCapture<P> p) {
        beforeSyntax(usingStatement, p);
        p.append("using");
        beforeSyntax(usingStatement.getExpression(), p);
        p.append('(');
        JRightPadded<Expression> resource = usingStatement.getExpression().getPadding().getTree();
        visitDeclaration(resource.getElement(), p);
        visitSpace(resource.getAfter(), p);
        p.append(')');
        afterSyntax(usingStatement.getExpression(), p);
        visit(usingStatement.getStatement(), p);
        afterSyntax(usingStatement, p);
        return usingStatement;
    }

    @Override
    public J visitImplicitElementAccess(Cs.ImplicitElementAccess implicitElementAccess, PrintOutputCapture<P> p) {
        beforeSyntax(implicitElementAccess, p);
        visitContainer("[", implicitElementAccess.getPadding().getArgumentList(), ",", "]", p);
        afterSyntax(implicitElementAccess, p);
        return implicitElementAccess;
    }

    @Override
    public J visitSlicePattern(Cs.SlicePattern slicePattern, PrintOutputCapture<P> p) {
        beforeSyntax(slicePattern, p);
        p.append("..");
        afterSyntax(slicePattern, p);
        return slicePattern;
    }

    @Override
    public J visitListPattern(Cs.ListPattern listPattern, PrintOutputCapture<P> p) {
        beforeSyntax(listPattern, p);
        visitContainer("[", listPattern.getPadding().getPatterns(), ",", "]", p);
        visit(listPattern.getDesignation(), p);
        afterSyntax(listPattern, p);
        return listPattern;
    }

    @Override
    public J visitAliasQualifiedName(Cs.AliasQualifiedName aliasQualifiedName, PrintOutputCapture<P> p) {
        beforeSyntax(aliasQualifiedName, p);
        visitRightPadded(aliasQualifiedName.getPadding().getAlias(), "::", p);
        visit(aliasQualifiedName.getName(), p);
        afterSyntax(aliasQualifiedName, p);
        return aliasQualifiedName;
    }

    @Override
    public J visitRefType(Cs.RefType refType, PrintOutputCapture<P> p) {
        beforeSyntax(refType, p);
        p.append("ref");
        if (refType.getReadonlyKeyword() != null) {
            beforeSyntax(refType.getReadonlyKeyword(), p);
            p.append("readonly");
            afterSyntax(refType.getReadonlyKeyword(), p);
        }
        visit(refType.getTypeIdentifier(), p);
        afterSyntax(refType, p);
        return refType;
    }

    @Override
    public J visitAnonymousObjectCreationExpression(Cs.AnonymousObjectCreationExpression anonymousObject, PrintOutputCapture<P> p) {
        beforeSyntax(anonymousObject, p);
        p.append("new");
        JContainer<Expression> initializers = anonymousObject.getPadding().getInitializers();
        visitSpace(initializers.getBefore(), p);
        p.append('{');
        visitInitializerElements(initializers.getPadding().getElements(), p);
        p.append('}');
        afterSyntax(anonymousObject, p);
        return anonymousObject;
    }

    @Override
    public J visitWithExpression(Cs.WithExpression withExpression, PrintOutputCapture<P> p) {
        beforeSyntax(withExpression, p);
        visit(withExpression.getExpression(), p);
        visitSpace(withExpression.getPadding().getInitializer().getBefore(), p);
        p.append("with");
        visit(withExpression.getInitializer(), p);
        afterSyntax(withExpression, p);
        return withExpression;
    }

    @Override
    public J visitSpreadExpression(Cs.SpreadExpression spreadExpression, PrintOutputCapture<P> p) {
        beforeSyntax(spreadExpression, p);
        p.append("..");
        visit(spreadExpression.getExpression(), p);
        afterSyntax(spreadExpression, p);
        return spreadExpression;
    }

    @Override
    public J visitFunctionPointerType(Cs.FunctionPointerType functionPointerType, PrintOutputCapture<P> p) {
        beforeSyntax(functionPointerType, p);
        p.append("delegate*");

        JLeftPadded<Cs.FunctionPointerType.CallingConvention> callingConvention = functionPointerType.getPadding().getCallingConvention();
        if (callingConvention != null) {
            visitSpace(callingConvention.getBefore(), p);
            p.append(callingConvention.getElement() == Cs.FunctionPointerType.CallingConvention.Managed ? "managed" : "unmanaged");
        }

        visitContainer("[", functionPointerType.getPadding().getUnmanagedCallingConventionTypes(), ",", "]", p);
        visitContainer("<", functionPointerType.getPadding().getParameterTypes(), ",", ">", p);
        afterSyntax(functionPointerType, p);
        return functionPointerType;
    }

    @Override
    public J visitTypeWithArguments(Cs.TypeWithArguments typeWithArguments, PrintOutputCapture<P> p) {
        beforeSyntax(typeWithArguments, p);
        visit(typeWithArguments.getTypeExpression(), p);
        visitContainer("(", typeWithArguments.getPadding().getArguments(), ",", ")", p);
        afterSyntax(typeWithArguments, p);
        return typeWithArguments;
    }

    private void visitCatchClause(J.Try.Catch catchClause, PrintOutputCapture<P> p) {
        beforeSyntax(catchClause, p);
        p.append("catch");

        J.VariableDeclarations declaration = catchClause.getParameter().getTree();
        J.VariableDeclarations.NamedVariable variable = declaration.getVariables().isEmpty() ?
                null :
                declaration.getVariables().get(0);

        // a catch without a declaration has no parentheses, but its markers are still printed
        boolean declared = declaration.getTypeExpression() != null;
        beforeSyntax(catchClause.getParameter(), p);
        if (declared) {
            p.append('(');
        }
        beforeSyntax(declaration, p);
        visit(declaration.getTypeExpression(), p);
        if (variable != null) {
            // an unnamed variable only holds the exception filter
            beforeSyntax(variable, p);
            visit(variable.getName(), p);
            afterSyntax(variable, p);
        }
        afterSyntax(declaration, p);
        if (declared) {
            visitSpace(catchClause.getParameter().getPadding().getTree().getAfter(), p);
            p.append(')');
        }
        afterSyntax(catchClause.getParameter(), p);

        if (variable != null && variable.getInitializer() instanceof Cs.WhenClause) {
            visitSpace(variable.getPadding().getInitializer().getBefore(), p);
            p.append("when");
            visit(variable.getInitializer(), p);
        }

        visitBlock(catchClause.getBody(), p);
        afterSyntax(catchClause, p);
    }

    @Override
    public J visitWhenClause(Cs.WhenClause whenClause, PrintOutputCapture<P> p) {
        beforeSyntax(whenClause, p);
        beforeSyntax(whenClause.getCondition(), p);
        p.append('(');
        visitRightPadded(whenClause.getCondition().getPadding().getTree(), ")", p);
        afterSyntax(whenClause.getCondition(), p);
        afterSyntax(whenClause, p);
        return whenClause;
    }

    @Override
    public J visitExplicitInterfaceMember(Cs.ExplicitInterfaceMember explicitInterfaceMember, PrintOutputCapture<P> p) {
        beforeSyntax(explicitInterfaceMember, p);
        J.MethodDeclaration method = explicitInterfaceMember.getMethodDeclaration();
        beforeSyntax(method, p);
        printMethodDeclaration(method, explicitInterfaceMember.getPadding().getInterfaceSpecifier(), p);
        afterSyntax(method, p);
        afterSyntax(explicitInterfaceMember, p);
        return explicitInterfaceMember;
    }

    @Override
    public J visitDelegateDeclaration(Cs.DelegateDeclaration delegateDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(delegateDeclaration, p);
        visit(delegateDeclaration.getAttributes(), p);
        visitModifiers(delegateDeclaration.getModifiers(), p);

        visitSpace(delegateDeclaration.getPadding().getReturnType().getBefore(), p);
        p.append("delegate");
        visit(delegateDeclaration.getReturnType(), p);
        visit(delegateDeclaration.getIdentifier(), p);

        JContainer<J.TypeParameter> typeParameters = delegateDeclaration.getPadding().getTypeParameters();
        visitContainer("<", typeParameters, ",", ">", p);
        printParameterList("(", delegateDeclaration.getPadding().getParameters(), ")", p);
        printTypeParameterConstraintsInSourceOrder(typeParameters == null ? null : typeParameters.getPadding().getElements(), p);

        afterSyntax(delegateDeclaration, p);
        return delegateDeclaration;
    }

    @Override
    public J visitEventDeclaration(Cs.EventDeclaration eventDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(eventDeclaration, p);
        visit(eventDeclaration.getAttributeLists(), p);
        visitModifiers(eventDeclaration.getModifiers(), p);

        visitSpace(eventDeclaration.getPadding().getTypeExpression().getBefore(), p);
        p.append("event");
        visit(eventDeclaration.getTypeExpression(), p);
        visitRightPadded(eventDeclaration.getPadding().getInterfaceSpecifier(), ".", p);
        visit(eventDeclaration.getName(), p);
        visitContainer("{", eventDeclaration.getPadding().getAccessors(), "", "}", p);

        afterSyntax(eventDeclaration, p);
        return eventDeclaration;
    }

    @Override
    public J visitIndexerDeclaration(Cs.IndexerDeclaration indexerDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(indexerDeclaration, p);
        visitModifiers(indexerDeclaration.getModifiers(), p);
        visit(indexerDeclaration.getTypeExpression(), p);
        visitRightPadded(indexerDeclaration.getPadding().getExplicitInterfaceSpecifier(), ".", p);
        visit(indexerDeclaration.getIndexer(), p);
        printParameterList("[", indexerDeclaration.getPadding().getParameters(), "]", p);

        JLeftPadded<Expression> expressionBody = indexerDeclaration.getPadding().getExpressionBody();
        if (expressionBody != null) {
            visitSpace(expressionBody.getBefore(), p);
            p.append("=>");
            visit(expressionBody.getElement(), p);
            p.append(';');
        } else if (indexerDeclaration.getAccessors() != null) {
            visitBlock(indexerDeclaration.getAccessors(), p);
        }

        afterSyntax(indexerDeclaration, p);
        return indexerDeclaration;
    }

    @Override
    public J visitOperatorDeclaration(Cs.OperatorDeclaration operatorDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(operatorDeclaration, p);
        visit(operatorDeclaration.getAttributeLists(), p);
        visitModifiers(operatorDeclaration.getModifiers(), p);
        visit(operatorDeclaration.getReturnType(), p);
        visitRightPadded(operatorDeclaration.getPadding().getExplicitInterfaceSpecifier(), ".", p);

        beforeSyntax(operatorDeclaration.getOperatorKeyword(), p);
        p.append("operator");
        afterSyntax(operatorDeclaration.getOperatorKeyword(), p);

        if (operatorDeclaration.getCheckedKeyword() != null) {
            beforeSyntax(operatorDeclaration.getCheckedKeyword(), p);
            p.append("checked");
            afterSyntax(operatorDeclaration.getCheckedKeyword(), p);
        }

        visitSpace(operatorDeclaration.getPadding().getOperatorToken().getBefore(), p);
        switch (operatorDeclaration.getPadding().getOperatorToken().getElement()) {
            case Plus:
                p.append('+');
                break;
            case Minus:
                p.append('-');
                break;
            case Bang:
                p.append('!');
                break;
            case Tilde:
                p.append('~');
                break;
            case PlusPlus:
                p.append("++");
                break;
            case MinusMinus:
                p.append("--");
                break;
            case Star:
                p.append('*');
                break;
            case Division:
                p.append('/');
                break;
            case Percent:
                p.append('%');
                break;
            case LeftShift:
                p.append("<<");
                break;
            case RightShift:
                p.append(">>");
                break;
            case UnsignedRightShift:
                p.append(">>>");
                break;
            case LessThan:
                p.append('<');
                break;
            case GreaterThan:
                p.append('>');
                break;
            case LessThanEquals:
                p.append("<=");
                break;
            case GreaterThanEquals:
                p.append(">=");
                break;
            case Equals:
                p.append("==");
                break;
            case NotEquals:
                p.append("!=");
                break;
            case Ampersand:
                p.append('&');
                break;
            case Bar:
                p.append('|');
                break;
            case Caret:
                p.append('^');
                break;
            case True:
                p.append("true");
                break;
            case False:
                p.append("false");
                break;
        }

        printParameterList("(", operatorDeclaration.getPadding().getParameters(), ")", p);

        if (!printExpressionBody(operatorDeclaration, operatorDeclaration.getBody(), p)) {
            visitBlock(operatorDeclaration.getBody(), p);
        }

        afterSyntax(operatorDeclaration, p);
        return operatorDeclaration;
    }

    @Override
    public J visitConversionOperatorDeclaration(Cs.ConversionOperatorDeclaration conversionOperatorDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(conversionOperatorDeclaration, p);
        visitModifiers(conversionOperatorDeclaration.getModifiers(), p);

        visitSpace(conversionOperatorDeclaration.getPadding().getKind().getBefore(), p);
        p.append(conversionOperatorDeclaration.getKind() == Cs.ConversionOperatorDeclaration.ExplicitImplicit.Implicit ? "implicit" : "explicit");

        visitRightPadded(conversionOperatorDeclaration.getPadding().getInterfaceSpecifier(), ".", p);

        visitSpace(conversionOperatorDeclaration.getPadding().getReturnType().getBefore(), p);
        p.append("operator");
        visit(conversionOperatorDeclaration.getReturnType(), p);

        printParameterList("(", conversionOperatorDeclaration.getPadding().getParameters(), ")", p);

        JLeftPadded<Expression> expressionBody = conversionOperatorDeclaration.getPadding().getExpressionBody();
        if (expressionBody != null) {
            visitSpace(expressionBody.getBefore(), p);
            p.append("=>");
            visit(expressionBody.getElement(), p);
            p.append(';');
        } else if (conversionOperatorDeclaration.getBody() != null) {
            visitBlock(conversionOperatorDeclaration.getBody(), p);
        }

        afterSyntax(conversionOperatorDeclaration, p);
        return conversionOperatorDeclaration;
    }

    @Override
    public J visitForEachVariableLoop(Cs.ForEachVariableLoop forEachVariableLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forEachVariableLoop, p);
        p.append("foreach");
        Cs.ForEachVariableLoop.Control control = forEachVariableLoop.getControlElement();
        beforeSyntax(control, p);
        p.append('(');
        visitRightPadded(control.getPadding().getVariable(), "in", p);
        visitRightPadded(control.getPadding().getIterable(), ")", p);
        afterSyntax(control, p);
        visitStatement(forEachVariableLoop.getPadding().getBody(), p);
        afterSyntax(forEachVariableLoop, p);
        return forEachVariableLoop;
    }

    @Override
    public J visitPointerDereference(Cs.PointerDereference pointerDereference, PrintOutputCapture<P> p) {
        beforeSyntax(pointerDereference, p);
        // as the target of `->` the dereference is part of that operator
        if (!pointerDereference.getMarkers().findFirst(PointerMemberAccess.class).isPresent()) {
            p.append('*');
        }
        visit(pointerDereference.getExpression(), p);
        afterSyntax(pointerDereference, p);
        return pointerDereference;
    }

    @Override
    public J visitPointerFieldAccess(Cs.PointerFieldAccess pointerFieldAccess, PrintOutputCapture<P> p) {
        beforeSyntax(pointerFieldAccess, p);
        visit(pointerFieldAccess.getTarget(), p);
        visitSpace(pointerFieldAccess.getPadding().getName().getBefore(), p);
        p.append("->");
        visit(pointerFieldAccess.getName(), p);
        afterSyntax(pointerFieldAccess, p);
        return pointerFieldAccess;
    }

    @Override
    public J visitQueryExpression(Linq.QueryExpression queryExpression, PrintOutputCapture<P> p) {
        beforeSyntax(queryExpression, p);
        visit(queryExpression.getFromClause(), p);
        visit(queryExpression.getBody(), p);
        afterSyntax(queryExpression, p);
        return queryExpression;
    }

    @Override
    public J visitQueryBody(Linq.QueryBody queryBody, PrintOutputCapture<P> p) {
        beforeSyntax(queryBody, p);
        visit(queryBody.getClauses(), p);
        visit(queryBody.getSelectOrGroup(), p);
        visit(queryBody.getContinuation(), p);
        afterSyntax(queryBody, p);
        return queryBody;
    }

    @Override
    public J visitFromClause(Linq.FromClause fromClause, PrintOutputCapture<P> p) {
        beforeSyntax(fromClause, p);
        p.append("from");
        visit(fromClause.getTypeIdentifier(), p);
        visitRightPadded(fromClause.getPadding().getIdentifier(), "in", p);
        visit(fromClause.getExpression(), p);
        afterSyntax(fromClause, p);
        return fromClause;
    }

    @Override
    public J visitLetClause(Linq.LetClause letClause, PrintOutputCapture<P> p) {
        beforeSyntax(letClause, p);
        p.append("let");
        visitRightPadded(letClause.getPadding().getIdentifier(), "=", p);
        visit(letClause.getExpression(), p);
        afterSyntax(letClause, p);
        return letClause;
    }

    @Override
    public J visitJoinClause(Linq.JoinClause joinClause, PrintOutputCapture<P> p) {
        beforeSyntax(joinClause, p);
        p.append("join");
        visitRightPadded(joinClause.getPadding().getIdentifier(), "in", p);
        visitRightPadded(joinClause.getPadding().getInExpression(), "on", p);
        visitRightPadded(joinClause.getPadding().getLeftExpression(), "equals", p);
        visit(joinClause.getRightExpression(), p);
        JLeftPadded<Linq.JoinIntoClause> into = joinClause.getPadding().getInto();
        if (into != null) {
            visitSpace(into.getBefore(), p);
            visit(into.getElement(), p);
        }
        afterSyntax(joinClause, p);
        return joinClause;
    }

    @Override
    public J visitJoinIntoClause(Linq.JoinIntoClause joinIntoClause, PrintOutputCapture<P> p) {
        beforeSyntax(joinIntoClause, p);
        p.append("into");
        visit(joinIntoClause.getIdentifier(), p);
        afterSyntax(joinIntoClause, p);
        return joinIntoClause;
    }

    @Override
    public J visitWhereClause(Linq.WhereClause whereClause, PrintOutputCapture<P> p) {
        beforeSyntax(whereClause, p);
        p.append("where");
        visit(whereClause.getCondition(), p);
        afterSyntax(whereClause, p);
        return whereClause;
    }

    @Override
    public J visitOrderByClause(Linq.OrderByClause orderByClause, PrintOutputCapture<P> p) {
        beforeSyntax(orderByClause, p);
        p.append("orderby");
        visitRightPadded(orderByClause.getPadding().getOrderings(), ",", p);
        afterSyntax(orderByClause, p);
        return orderByClause;
    }

    @Override
    public J visitOrdering(Linq.Ordering ordering, PrintOutputCapture<P> p) {
        beforeSyntax(ordering, p);
        visit(ordering.getExpression(), p);
        if (ordering.getDirection() != null) {
            visitSpace(ordering.getPadding().getExpression().getAfter(), p);
            p.append(ordering.getDirection() == Linq.Ordering.DirectionKind.Ascending ? "ascending" : "descending");
        }
        afterSyntax(ordering, p);
        return ordering;
    }

    @Override
    public J visitSelectClause(Linq.SelectClause selectClause, PrintOutputCapture<P> p) {
        beforeSyntax(selectClause, p);
        p.append("select");
        visit(selectClause.getExpression(), p);
        afterSyntax(selectClause, p);
        return selectClause;
    }

    @Override
    public J visitGroupClause(Linq.GroupClause groupClause, PrintOutputCapture<P> p) {
        beforeSyntax(groupClause, p);
        p.append("group");
        visitRightPadded(groupClause.getPadding().getGroupExpression(), "by", p);
        visit(groupClause.getKey(), p);
        afterSyntax(groupClause, p);
        return groupClause;
    }

    @Override
    public J visitQueryContinuation(Linq.QueryContinuation queryContinuation, PrintOutputCapture<P> p) {
        beforeSyntax(queryContinuation, p);
        p.append("into");
        visit(queryContinuation.getIdentifier(), p);
        visit(queryContinuation.getBody(), p);
        afterSyntax(queryContinuation, p);
        return queryContinuation;
    }
}
