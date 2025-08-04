/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.kotlin.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Quoted;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.marker.*;
import org.openrewrite.kotlin.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
public class KotlinPrinter<P> extends KotlinVisitor<PrintOutputCapture<P>> {
    private final KotlinJavaPrinter<P> delegate;

    public KotlinPrinter() {
        delegate = delegate();
    }

    protected KotlinJavaPrinter<P> delegate() {
        return new KotlinJavaPrinter<>(this);
    }

    @Override
    public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
        if (!(tree instanceof K)) {
            // re-route printing to the java printer
            return delegate.visit(tree, p);
        } else {
            return super.visit(tree, p);
        }
    }

    @Override
    public J visitCompilationUnit(K.CompilationUnit sourceFile, PrintOutputCapture<P> p) {
        if (sourceFile.getShebang() != null) {
            p.append(sourceFile.getShebang());
        }

        beforeSyntax(sourceFile, Space.Location.COMPILATION_UNIT_PREFIX, p);

        visit(sourceFile.getAnnotations(), p);

        JRightPadded<J.Package> pkg = sourceFile.getPadding().getPackageDeclaration();
        if (pkg != null) {
            visitRightPadded(pkg, p);
        }

        for (JRightPadded<J.Import> import_ : sourceFile.getPadding().getImports()) {
            visitRightPadded(import_, p);
        }

        for (JRightPadded<Statement> statement : sourceFile.getPadding().getStatements()) {
            visitRightPadded(statement, p);
        }

        visitSpace(sourceFile.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        afterSyntax(sourceFile, p);
        return sourceFile;
    }

    @Override
    public J visitAnnotatedExpression(K.AnnotatedExpression annotatedExpression, PrintOutputCapture<P> p) {
        visit(annotatedExpression.getAnnotations(), p);
        visit(annotatedExpression.getExpression(), p);
        afterSyntax(annotatedExpression, p);
        return annotatedExpression;
    }

    @Override
    public J visitBinary(K.Binary binary, PrintOutputCapture<P> p) {
        beforeSyntax(binary, Space.Location.BINARY_PREFIX, p);
        String keyword = "";
        switch (binary.getOperator()) {
            case Contains:
                keyword = "in";
                break;
            case Elvis:
                keyword = "?:";
                break;
            case NotContains:
                keyword = "!in";
                break;
            case IdentityEquals:
                keyword = "===";
                break;
            case IdentityNotEquals:
                keyword = "!==";
                break;
            case RangeTo:
                keyword = "..";
                break;
            case RangeUntil:
                keyword = "..<";
                break;
        }
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), KSpace.Location.BINARY_OPERATOR, p);
        p.append(keyword);

        visit(binary.getRight(), p);

        visitSpace(binary.getAfter(), KSpace.Location.BINARY_SUFFIX, p);
        afterSyntax(binary, p);
        return binary;
    }

    @Override
    public J visitClassDeclaration(K.ClassDeclaration classDeclaration, PrintOutputCapture<P> p) {
        return delegate.visitClassDeclaration0(classDeclaration.getClassDeclaration(), classDeclaration.getTypeConstraints(), p);
    }

    @Override
    public J visitConstructor(K.Constructor constructor, PrintOutputCapture<P> p) {
        J.MethodDeclaration method = constructor.getMethodDeclaration();

        beforeSyntax(method, Space.Location.METHOD_DECLARATION_PREFIX, p);
        visit(method.getLeadingAnnotations(), p);
        for (J.Modifier m : method.getModifiers()) {
            delegate.visitModifier(m, p);
        }
        JContainer<Statement> params = method.getPadding().getParameters();
        beforeSyntax(params.getBefore(), params.getMarkers(), JContainer.Location.METHOD_DECLARATION_PARAMETERS.getBeforeLocation(), p);
        p.append("(");
        List<JRightPadded<Statement>> elements = params.getPadding().getElements();
        for (int i = 0; i < elements.size(); i++) {
            delegate.printMethodParameters(p, i, elements);
        }
        afterSyntax(params.getMarkers(), p);
        p.append(")");

        visitSpace(constructor.getPadding().getInvocation().getBefore(), KSpace.Location.CONSTRUCTOR_COLON, p);
        p.append(':');
        visit(constructor.getInvocation(), p);
        afterSyntax(constructor, p);

        visit(method.getBody(), p);

        return constructor;
    }

    @Override
    public J visitConstructorInvocation(K.ConstructorInvocation constructorInvocation, PrintOutputCapture<P> p) {
        beforeSyntax(constructorInvocation, KSpace.Location.CONSTRUCTOR_INVOCATION_PREFIX, p);
        visit(constructorInvocation.getTypeTree(), p);
        delegate.visitArgumentsContainer(constructorInvocation.getPadding().getArguments(), Space.Location.METHOD_INVOCATION_ARGUMENTS, p);
        afterSyntax(constructorInvocation, p);
        return constructorInvocation;
    }

    @Override
    public J visitDelegatedSuperType(K.DelegatedSuperType delegatedSuperType, PrintOutputCapture<P> p) {
        visit(delegatedSuperType.getTypeTree(), p);
        visitSpace(delegatedSuperType.getBy(), KSpace.Location.DELEGATED_SUPER_TYPE_BY, p);
        p.append("by");
        visit(delegatedSuperType.getDelegate(), p);
        afterSyntax(delegatedSuperType, p);
        return delegatedSuperType;
    }

    @Override
    public J visitDestructuringDeclaration(K.DestructuringDeclaration destructuringDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(destructuringDeclaration, KSpace.Location.DESTRUCTURING_DECLARATION_PREFIX, p);
        visit(destructuringDeclaration.getInitializer().getLeadingAnnotations(), p);
        for (J.Modifier m : destructuringDeclaration.getInitializer().getModifiers()) {
            delegate.visitModifier(m, p);
            if (m.getType() == J.Modifier.Type.Final) {
                p.append("val");
            }
        }
        visitSpace(destructuringDeclaration.getPadding().getDestructAssignments().getBefore(), Space.Location.LANGUAGE_EXTENSION, p);
        p.append("(");

        List<JRightPadded<Statement>> elements = destructuringDeclaration.getPadding().getDestructAssignments().getPadding().getElements();
        for (int i = 0; i < elements.size(); i++) {
            JRightPadded<Statement> element = elements.get(i);
            visit(element.getElement(), p);
            visitSpace(element.getAfter(), Space.Location.LANGUAGE_EXTENSION, p);
            visitMarkers(element.getMarkers(), p);
            p.append(i == elements.size() - 1 ? ")" : ",");
        }

        if (!destructuringDeclaration.getInitializer().getVariables().isEmpty() &&
            destructuringDeclaration.getInitializer().getVariables().get(0).getPadding().getInitializer() != null) {
            visitSpace(Objects.requireNonNull(destructuringDeclaration.getInitializer().getVariables().get(0).getPadding()
                    .getInitializer()).getBefore(), Space.Location.LANGUAGE_EXTENSION, p);
            p.append("=");
            visit(Objects.requireNonNull(destructuringDeclaration.getInitializer().getVariables().get(0).getPadding().getInitializer()).getElement(), p);
        }
        afterSyntax(destructuringDeclaration, p);
        return destructuringDeclaration;
    }

    @Override
    public J visitFunctionType(K.FunctionType functionType, PrintOutputCapture<P> p) {
        beforeSyntax(functionType, KSpace.Location.FUNCTION_TYPE_PREFIX, p);

        visit(functionType.getLeadingAnnotations(), p);
        for (J.Modifier modifier : functionType.getModifiers()) {
            delegate.visitModifier(modifier, p);
        }

        if (functionType.getReceiver() != null) {
            visitRightPadded(functionType.getReceiver(), p);
            p.append(".");
        }
        delegate.visitContainer("(", functionType.getPadding().getParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ")", p);
        visitSpace(functionType.getArrow() != null ? functionType.getArrow() : Space.SINGLE_SPACE, KSpace.Location.FUNCTION_TYPE_ARROW_PREFIX, p);
        p.append("->");

        visitRightPadded(functionType.getReturnType(), p);
        afterSyntax(functionType, p);
        return functionType;
    }

    @Override
    public J visitFunctionTypeParameter(K.FunctionType.Parameter parameter, PrintOutputCapture<P> p) {
        if (parameter.getName() != null) {
            visit(parameter.getName(), p);
            parameter.getMarkers().findFirst(TypeReferencePrefix.class).ifPresent(tref -> visitSpace(tref.getPrefix(), KSpace.Location.TYPE_REFERENCE_PREFIX, p));
            p.append(":");
        }
        visit(parameter.getParameterType(), p);
        return parameter;
    }

    @Override
    public J visitReturn(K.Return kReturn, PrintOutputCapture<P> p) {
        // backwards compatibility: leave this in until `K.KReturn#annotations` has been deleted
        // visit(kReturn.getAnnotations(), p);
        J.Return jReturn = kReturn.getExpression();
        if (kReturn.getLabel() != null) {
            beforeSyntax(jReturn, Space.Location.RETURN_PREFIX, p);
            p.append("return");
            p.append("@");
            visit(kReturn.getLabel(), p);
            if (jReturn.getExpression() != null) {
                visit(jReturn.getExpression(), p);
            }
            afterSyntax(jReturn, p);
        } else {
            visit(kReturn.getExpression(), p);
        }
        return kReturn;
    }

    @Override
    public J visitStringTemplate(K.StringTemplate stringTemplate, PrintOutputCapture<P> p) {
        beforeSyntax(stringTemplate, KSpace.Location.STRING_TEMPLATE_PREFIX, p);

        String delimiter = stringTemplate.getDelimiter();
        p.append(delimiter);

        visit(stringTemplate.getStrings(), p);
        p.append(delimiter);

        afterSyntax(stringTemplate, p);
        return stringTemplate;
    }

    @Override
    public J visitThis(K.This aThis, PrintOutputCapture<P> p) {
        beforeSyntax(aThis, KSpace.Location.THIS_PREFIX, p);

        p.append("this");
        if (aThis.getLabel() != null) {
            p.append("@");
            visit(aThis.getLabel(), p);
        }

        afterSyntax(aThis, p);
        return aThis;
    }

    @Override
    public J visitStringTemplateExpression(K.StringTemplate.Expression expression, PrintOutputCapture<P> p) {
        beforeSyntax(expression, KSpace.Location.STRING_TEMPLATE_PREFIX, p);
        if (expression.isEnclosedInBraces()) {
            p.append("${");
        } else {
            p.append("$");
        }
        visit(expression.getTree(), p);
        if (expression.isEnclosedInBraces()) {
            visitSpace(expression.getAfter(), KSpace.Location.STRING_TEMPLATE_SUFFIX, p);
            p.append('}');
        }
        afterSyntax(expression, p);
        return expression;
    }

    @Override
    public J visitListLiteral(K.ListLiteral listLiteral, PrintOutputCapture<P> p) {
        beforeSyntax(listLiteral, KSpace.Location.LIST_LITERAL_PREFIX, p);
        visitContainer("[", listLiteral.getPadding().getElements(), KContainer.Location.LIST_LITERAL_ELEMENTS,
                "]", p);
        afterSyntax(listLiteral, p);
        return listLiteral;
    }

    @Override
    public J visitMethodDeclaration(K.MethodDeclaration methodDeclaration, PrintOutputCapture<P> p) {
        return delegate.visitMethodDeclaration0(methodDeclaration.getMethodDeclaration(), methodDeclaration.getTypeConstraints(), p);
    }

    @Override
    public J visitParenthesizedTypeTree(J.ParenthesizedTypeTree parTree, PrintOutputCapture<P> p) {
        visitSpace(parTree.getPrefix(), Space.Location.PARENTHESES_PREFIX, p);
        visitParentheses(parTree.getParenthesizedType(), p);
        return parTree;
    }

    @Override
    public J visitProperty(K.Property property, PrintOutputCapture<P> p) {
        beforeSyntax(property, KSpace.Location.PROPERTY_PREFIX, p);

        J.VariableDeclarations vd = property.getVariableDeclarations();
        visit(vd.getLeadingAnnotations(), p);
        for (J.Modifier m : vd.getModifiers()) {
            delegate.visitModifier(m, p);
            if (m.getType() == J.Modifier.Type.Final) {
                p.append("val");
            }
        }

        delegate.visitContainer("<", property.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);

        Extension extension = vd.getMarkers().findFirst(Extension.class).orElse(null);
        if (extension != null) {
            if (property.getReceiver() != null) {
                visitRightPadded(property.getPadding().getReceiver(), p);
                p.append(".");
            }
        }

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> rpvs = vd.getPadding().getVariables();
        if (!rpvs.isEmpty()) {
            JRightPadded<J.VariableDeclarations.NamedVariable> rpv = vd.getPadding().getVariables().get(0);
            J.VariableDeclarations.NamedVariable nv = rpv.getElement();
            beforeSyntax(nv, Space.Location.VARIABLE_PREFIX, p);
            visit(nv.getName(), p);
            visitSpace(rpv.getAfter(), Space.Location.TYPE_PARAMETERS, p);

            if (vd.getTypeExpression() != null) {
                vd.getMarkers().findFirst(TypeReferencePrefix.class).ifPresent(tref -> visitSpace(tref.getPrefix(), KSpace.Location.TYPE_REFERENCE_PREFIX, p));
                p.append(":");
                visit(vd.getTypeExpression(), p);
            }

            if (nv.getInitializer() != null) {
                String equals = getEqualsText(vd);

                visitSpace(Objects.requireNonNull(nv.getPadding().getInitializer()).getBefore(), Space.Location.VARIABLE_INITIALIZER, p);
                p.append(equals);
            }
            visit(nv.getInitializer(), p);
        }

        if (property.getTypeConstraints() != null) {
            delegate.visitContainer("where", property.getTypeConstraints().getPadding().getConstraints(), JContainer.Location.TYPE_PARAMETERS, ",", "", p);
        }

        visitSpace(property.getPadding().getVariableDeclarations().getAfter(), Space.Location.VARIABLE_INITIALIZER, p);
        if (property.getPadding().getVariableDeclarations().getMarkers().findFirst(Semicolon.class).isPresent()) {
            p.append(";");
        }

        visitContainer(property.getAccessors(), p);
        afterSyntax(property, p);
        return property;
    }

    @Override
    public J visitSpreadArgument(K.SpreadArgument spreadArgument, PrintOutputCapture<P> p) {
        beforeSyntax(spreadArgument, KSpace.Location.SPREAD_ARGUMENT_PREFIX, p);
        p.append("*");
        visit(spreadArgument.getExpression(), p);

        afterSyntax(spreadArgument, p);
        return spreadArgument;
    }

    @Override
    public J visitUnary(K.Unary unary, PrintOutputCapture<P> p) {
        beforeSyntax(unary, Space.Location.UNARY_PREFIX, p);
        switch (unary.getOperator()) {
            case NotNull:
            default:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                p.append("!!");
                break;
        }
        afterSyntax(unary, p);
        return unary;
    }

    @Override
    public J visitAnnotationType(K.AnnotationType annotationType, PrintOutputCapture<P> p) {
        beforeSyntax(annotationType, Space.Location.ANNOTATION_PREFIX, p);
        visitRightPadded(annotationType.getPadding().getUseSite(), p);
        p.append(":");
        visit(annotationType.getCallee(), p);
        return annotationType;
    }

    @Override
    public J visitMultiAnnotationType(K.MultiAnnotationType multiAnnotationType, PrintOutputCapture<P> p) {
        beforeSyntax(multiAnnotationType, Space.Location.ANNOTATION_PREFIX, p);
        visitRightPadded(multiAnnotationType.getPadding().getUseSite(), p);

        if (!(multiAnnotationType.getUseSite() instanceof J.Empty)) {
            p.append(":");
        }

        delegate.visitContainer("[", multiAnnotationType.getAnnotations(), JContainer.Location.TYPE_PARAMETERS, "", "]", p);
        return multiAnnotationType;
    }

    @Override
    public J visitTypeAlias(K.TypeAlias typeAlias, PrintOutputCapture<P> p) {
        beforeSyntax(typeAlias, KSpace.Location.TYPE_ALIAS_PREFIX, p);
        visit(typeAlias.getLeadingAnnotations(), p);
        for (J.Modifier m : typeAlias.getModifiers()) {
            delegate.visitModifier(m, p);
        }
        visit(typeAlias.getName(), p);
        delegate.visitContainer("<", typeAlias.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        visitLeftPadded("=", typeAlias.getPadding().getInitializer(), KLeftPadded.Location.TYPE_ALIAS_INITIALIZER, p);
        afterSyntax(typeAlias, p);
        return typeAlias;
    }

    @Override
    public J visitWhen(K.When when, PrintOutputCapture<P> p) {
        beforeSyntax(when, KSpace.Location.WHEN_PREFIX, p);
        p.append("when");
        visit(when.getSelector(), p);
        visit(when.getBranches(), p);

        afterSyntax(when, p);
        return when;
    }

    @Override
    public J visitWhenBranch(K.WhenBranch whenBranch, PrintOutputCapture<P> p) {
        beforeSyntax(whenBranch, KSpace.Location.WHEN_BRANCH_PREFIX, p);
        visitContainer("", whenBranch.getPadding().getExpressions(), KContainer.Location.WHEN_BRANCH_EXPRESSION, "->", p);
        visit(whenBranch.getBody(), p);
        afterSyntax(whenBranch, p);
        return whenBranch;
    }

    public static class KotlinJavaPrinter<P> extends JavaPrinter<P> {
        KotlinPrinter<P> kotlinPrinter;

        public KotlinJavaPrinter(KotlinPrinter<P> kp) {
            kotlinPrinter = kp;
        }

        @Override
        public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
            if (tree instanceof K) {
                // re-route printing back up to groovy
                return kotlinPrinter.visit(tree, p);
            } else {
                return super.visit(tree, p);
            }
        }

        @Override
        public J visitAnnotation(J.Annotation annotation, PrintOutputCapture<P> p) {
            beforeSyntax(annotation, Space.Location.ANNOTATION_PREFIX, p);
            // Modifier is used for backwards compatibility.
            boolean isKModifier = annotation.getMarkers().findFirst(Modifier.class).isPresent() || annotation.getMarkers().findFirst(AnnotationConstructor.class).isPresent();
            if (!isKModifier) {
                p.append("@");
            }

            visit(annotation.getAnnotationType(), p);

            String beforeArgs = "(";
            String afterArgs = ")";
            String delimiter = ",";

            visitContainer(beforeArgs, annotation.getPadding().getArguments(), JContainer.Location.ANNOTATION_ARGUMENTS, delimiter, afterArgs, p);
            afterSyntax(annotation, p);
            return annotation;
        }

        @Override
        public J visitBinary(J.Binary binary, PrintOutputCapture<P> p) {
            String keyword = "";
            switch (binary.getOperator()) {
                case Addition:
                    keyword = "+";
                    break;
                case Subtraction:
                    keyword = "-";
                    break;
                case Multiplication:
                    keyword = "*";
                    break;
                case Division:
                    keyword = "/";
                    break;
                case Modulo:
                    keyword = "%";
                    break;
                case LessThan:
                    keyword = "<";
                    break;
                case GreaterThan:
                    keyword = ">";
                    break;
                case LessThanOrEqual:
                    keyword = "<=";
                    break;
                case GreaterThanOrEqual:
                    keyword = ">=";
                    break;
                case Equal:
                    keyword = "==";
                    break;
                case NotEqual:
                    keyword = "!=";
                    break;
                case BitAnd:
                    keyword = "and";
                    break;
                case BitOr:
                    keyword = "or";
                    break;
                case BitXor:
                    keyword = "xor";
                    break;
                case LeftShift:
                    keyword = "shl";
                    break;
                case RightShift:
                    keyword = "shr";
                    break;
                case UnsignedRightShift:
                    keyword = "ushr";
                    break;
                case Or:
                    keyword = (binary.getMarkers().findFirst(LogicalComma.class).isPresent()) ? "," : "||";
                    break;
                case And:
                    keyword = "&&";
                    break;
            }
            beforeSyntax(binary, Space.Location.BINARY_PREFIX, p);
            visit(binary.getLeft(), p);
            visitSpace(binary.getPadding().getOperator().getBefore(), Space.Location.BINARY_OPERATOR, p);
            p.append(keyword);
            visit(binary.getRight(), p);
            afterSyntax(binary, p);
            return binary;
        }

        @Override
        public J visitBlock(J.Block block, PrintOutputCapture<P> p) {
            beforeSyntax(block, Space.Location.BLOCK_PREFIX, p);

            if (block.isStatic()) {
                p.append("init");
                visitRightPadded(block.getPadding().getStatic(), JRightPadded.Location.STATIC_INIT, p);
            }

            boolean singleExpressionBlock = block.getMarkers().findFirst(SingleExpressionBlock.class).isPresent();
            if (singleExpressionBlock) {
                p.append("=");
            }

            boolean omitBraces = block.getMarkers().findFirst(OmitBraces.class).isPresent();
            if (!omitBraces) {
                p.append("{");
            }
            visitStatements(block.getPadding().getStatements(), JRightPadded.Location.BLOCK_STATEMENT, p);
            visitSpace(block.getEnd(), Space.Location.BLOCK_END, p);
            if (!omitBraces) {
                p.append("}");
            }
            afterSyntax(block, p);
            return block;
        }

        @Override
        public J visitBreak(J.Break breakStatement, PrintOutputCapture<P> p) {
            beforeSyntax(breakStatement, Space.Location.BREAK_PREFIX, p);
            p.append("break");
            if (breakStatement.getLabel() != null) {
                p.append("@");
            }
            visit(breakStatement.getLabel(), p);
            afterSyntax(breakStatement, p);
            return breakStatement;
        }

        @Override
        public void visitContainer(String before, @Nullable JContainer<? extends J> container, JContainer.Location location, String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
            super.visitContainer(before, container, location, suffixBetween, after, p);
        }

        @Override
        public <J2 extends J> JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JContainer.Location loc, PrintOutputCapture<P> pPrintOutputCapture) {
            return super.visitContainer(container, loc, pPrintOutputCapture);
        }

        @Override
        protected void visitLeftPadded(@Nullable String prefix, @Nullable JLeftPadded<? extends J> leftPadded, JLeftPadded.Location location, PrintOutputCapture<P> p) {
            super.visitLeftPadded(prefix, leftPadded, location, p);
        }

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, PrintOutputCapture<P> p) {
            return visitClassDeclaration0(classDecl, null, p);
        }

        private J.ClassDeclaration visitClassDeclaration0(J.ClassDeclaration classDecl, K.@Nullable TypeConstraints typeConstraints, PrintOutputCapture<P> p) {
            beforeSyntax(classDecl, Space.Location.CLASS_DECLARATION_PREFIX, p);
            visit(classDecl.getLeadingAnnotations(), p);
            for (J.Modifier m : classDecl.getModifiers()) {
                visitModifier(m, p);
            }

            String kind = getClassKind(classDecl);
            visit(classDecl.getPadding().getKind().getAnnotations(), p);
            visitSpace(classDecl.getPadding().getKind().getPrefix(), Space.Location.CLASS_KIND, p);

            KObject KObject = classDecl.getMarkers().findFirst(KObject.class).orElse(null);
            if (KObject != null) {
                p.append("object");
                if (!classDecl.getName().getMarkers().findFirst(Implicit.class).isPresent()) {
                    visit(classDecl.getName(), p);
                }
            } else {
                p.append(kind);
                visit(classDecl.getName(), p);
            }

            visitContainer("<", classDecl.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);

            if (classDecl.getMarkers().findFirst(PrimaryConstructor.class).isPresent()) {
                for (Statement statement : classDecl.getBody().getStatements()) {
                    if (statement instanceof J.MethodDeclaration &&
                        statement.getMarkers().findFirst(PrimaryConstructor.class).isPresent() &&
                        !statement.getMarkers().findFirst(Implicit.class).isPresent()) {
                        J.MethodDeclaration method = (J.MethodDeclaration) statement;
                        beforeSyntax(method, Space.Location.METHOD_DECLARATION_PREFIX, p);
                        visit(method.getLeadingAnnotations(), p);
                        for (J.Modifier modifier : method.getModifiers()) {
                            visitModifier(modifier, p);
                        }
                        JContainer<Statement> params = method.getPadding().getParameters();
                        beforeSyntax(params.getBefore(), params.getMarkers(), JContainer.Location.METHOD_DECLARATION_PARAMETERS.getBeforeLocation(), p);
                        p.append("(");
                        List<JRightPadded<Statement>> elements = params.getPadding().getElements();
                        for (int i = 0; i < elements.size(); i++) {
                            printMethodParameters(p, i, elements);
                        }
                        afterSyntax(params.getMarkers(), p);
                        p.append(")");
                        afterSyntax(method, p);
                        break;
                    }
                }
            }

            if (classDecl.getImplements() != null) {
                JContainer<TypeTree> container = classDecl.getPadding().getImplements();
                beforeSyntax(Objects.requireNonNull(container).getBefore(), container.getMarkers(), JContainer.Location.IMPLEMENTS.getBeforeLocation(), p);
                p.append(":");
                List<? extends JRightPadded<? extends J>> nodes = container.getPadding().getElements();
                for (int i = 0; i < nodes.size(); i++) {
                    JRightPadded<? extends J> node = nodes.get(i);
                    J element = node.getElement();
                    visit(element, p);
                    visitSpace(node.getAfter(), JContainer.Location.IMPLEMENTS.getElementLocation().getAfterLocation(), p);
                    visitMarkers(node.getMarkers(), p);
                    if (i < nodes.size() - 1) {
                        p.append(",");
                    }
                }
                afterSyntax(container.getMarkers(), p);
            }

            if (typeConstraints != null) {
                visitContainer("where", typeConstraints.getPadding().getConstraints(), JContainer.Location.TYPE_PARAMETERS, ",", "", p);
            }

            if (!classDecl.getBody().getMarkers().findFirst(OmitBraces.class).isPresent()) {
                visit(classDecl.getBody(), p);
            }
            afterSyntax(classDecl, p);
            return classDecl;
        }

        @Override
        public J visitContinue(J.Continue continueStatement, PrintOutputCapture<P> p) {
            beforeSyntax(continueStatement, Space.Location.CONTINUE_PREFIX, p);
            p.append("continue");
            if (continueStatement.getLabel() != null) {
                p.append("@");
            }
            visit(continueStatement.getLabel(), p);
            afterSyntax(continueStatement, p);
            return continueStatement;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
            beforeSyntax(fieldAccess, Space.Location.FIELD_ACCESS_PREFIX, p);
            visit(fieldAccess.getTarget(), p);
            String prefix = fieldAccess.getMarkers().findFirst(IsNullSafe.class).isPresent() ? "?." : ".";
            visitLeftPadded(prefix, fieldAccess.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p);
            afterSyntax(fieldAccess, p);
            return fieldAccess;
        }

        @Override
        public J visitForEachLoop(J.ForEachLoop forEachLoop, PrintOutputCapture<P> p) {
            beforeSyntax(forEachLoop, Space.Location.FOR_EACH_LOOP_PREFIX, p);
            p.append("for");
            J.ForEachLoop.Control ctrl = forEachLoop.getControl();
            visitSpace(ctrl.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, p);
            p.append('(');
            visitRightPadded(ctrl.getPadding().getVariable(), JRightPadded.Location.FOREACH_VARIABLE, "in", p);
            visitRightPadded(ctrl.getPadding().getIterable(), JRightPadded.Location.FOREACH_ITERABLE, "", p);
            p.append(')');
            visitStatement(forEachLoop.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p);
            afterSyntax(forEachLoop, p);
            return forEachLoop;
        }

        @Override
        public J visitIdentifier(J.Identifier ident, PrintOutputCapture<P> p) {
            if (ident.getMarkers().findFirst(Implicit.class).isPresent()) {
                return ident;
            }

            visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
            visit(ident.getAnnotations(), p);

            beforeSyntax(ident, Space.Location.IDENTIFIER_PREFIX, p);
            boolean isQuoted = ident.getMarkers().findFirst(Quoted.class).isPresent();
            if (isQuoted) {
                p.append("`");
            }
            p.append(ident.getSimpleName());
            if (isQuoted) {
                p.append("`");
            }
            afterSyntax(ident, p);
            return ident;
        }

        @Override
        public J visitImport(J.Import import_, PrintOutputCapture<P> p) {
            beforeSyntax(import_, Space.Location.IMPORT_PREFIX, p);
            p.append("import");
            if (import_.getQualid().getTarget() instanceof J.Empty) {
                visit(import_.getQualid().getName(), p);
            } else {
                visit(import_.getQualid(), p);
            }

            JLeftPadded<J.Identifier> alias = import_.getPadding().getAlias();
            if (alias != null) {
                visitSpace(alias.getBefore(), Space.Location.IMPORT_ALIAS_PREFIX, p);
                p.append("as");
                visit(alias.getElement(), p);
            }
            afterSyntax(import_, p);
            return import_;
        }

        @Override
        public J visitInstanceOf(J.InstanceOf instanceOf, PrintOutputCapture<P> p) {
            beforeSyntax(instanceOf, Space.Location.INSTANCEOF_PREFIX, p);
            String suffix = instanceOf.getMarkers().findFirst(NotIs.class).isPresent() ? "!is" : "is";
            visitRightPadded(instanceOf.getPadding().getExpr(), JRightPadded.Location.INSTANCEOF, suffix, p);
            visit(instanceOf.getClazz(), p);
            visit(instanceOf.getPattern(), p);
            afterSyntax(instanceOf, p);
            return instanceOf;
        }

        @Override
        public J visitLabel(J.Label label, PrintOutputCapture<P> p) {
            beforeSyntax(label, Space.Location.LABEL_PREFIX, p);
            visitRightPadded(label.getPadding().getLabel(), JRightPadded.Location.LABEL, "@", p);
            visit(label.getStatement(), p);
            afterSyntax(label, p);
            return label;
        }

        @Override
        public J visitLambda(J.Lambda lambda, PrintOutputCapture<P> p) {
            beforeSyntax(lambda, Space.Location.LAMBDA_PREFIX, p);

            if (lambda.getMarkers().findFirst(AnonymousFunction.class).isPresent()) {
                p.append("fun");
                visitLambdaParameters(lambda.getParameters(), p);
                visitBlock((J.Block) lambda.getBody(), p);
            } else {
                boolean omitBraces = lambda.getMarkers().findFirst(OmitBraces.class).isPresent();
                if (!omitBraces) {
                    p.append('{');
                }

                visitSpace(lambda.getParameters().getPrefix(), Space.Location.LAMBDA_PARAMETER, p);
                visitLambdaParameters(lambda.getParameters(), p);
                if (!lambda.getParameters().getParameters().isEmpty()) {
                    visitSpace(lambda.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p);
                    p.append("->");
                }
                visit(lambda.getBody(), p);
                if (!omitBraces) {
                    p.append('}');
                }
            }

            afterSyntax(lambda, p);
            return lambda;
        }

        @Override
        public J visitLambdaParameters(J.Lambda.Parameters parameters, PrintOutputCapture<P> p) {
            visitMarkers(parameters.getMarkers(), p);
            if (parameters.isParenthesized()) {
                visitSpace(parameters.getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p);
                p.append('(');
                visitRightPadded(parameters.getPadding().getParameters(), JRightPadded.Location.LAMBDA_PARAM, ",", p);
                p.append(')');
            } else {
                List<JRightPadded<J>> params = parameters.getPadding().getParameters();
                for (int i = 0; i < params.size(); i++) {
                    JRightPadded<J> param = params.get(i);
                    if (param.getElement() instanceof J.Lambda.Parameters) {
                        visitLambdaParameters((J.Lambda.Parameters) param.getElement(), p);
                        visitSpace(param.getAfter(), JRightPadded.Location.LAMBDA_PARAM.getAfterLocation(), p);
                    } else {
                        visit(param.getElement(), p);
                        visitSpace(param.getAfter(), JRightPadded.Location.LAMBDA_PARAM.getAfterLocation(), p);
                        visitMarkers(param.getMarkers(), p);
                        if (i < params.size() - 1) {
                            p.append(',');
                        }
                    }
                }
            }
            return parameters;
        }

        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, PrintOutputCapture<P> p) {
            return visitMethodDeclaration0(method, null, p);
        }

        private J.MethodDeclaration visitMethodDeclaration0(J.MethodDeclaration method, K.@Nullable TypeConstraints typeConstraints, PrintOutputCapture<P> p) {
            // Do not print generated methods.
            for (Marker marker : method.getMarkers().getMarkers()) {
                if (marker instanceof Implicit || marker instanceof PrimaryConstructor) {
                    return method;
                }
            }

            beforeSyntax(method, Space.Location.METHOD_DECLARATION_PREFIX, p);
            visit(method.getLeadingAnnotations(), p);
            for (J.Modifier m : method.getModifiers()) {
                visitModifier(m, p);
            }

            J.TypeParameters typeParameters = method.getAnnotations().getTypeParameters();
            if (typeParameters != null) {
                visit(typeParameters.getAnnotations(), p);
                visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, p);
                visitMarkers(typeParameters.getMarkers(), p);
                p.append("<");
                visitRightPadded(typeParameters.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", p);
                p.append(">");
            }

            boolean hasReceiverType = method.getMarkers().findFirst(Extension.class).isPresent();
            if (hasReceiverType) {
                J.VariableDeclarations infixReceiver = (J.VariableDeclarations) method.getParameters().get(0);
                JRightPadded<J.VariableDeclarations.NamedVariable> receiver = infixReceiver.getPadding().getVariables().get(0);
                visitRightPadded(receiver, JRightPadded.Location.NAMED_VARIABLE, ".", p);
            }

            if (!method.getName().getMarkers().findFirst(Implicit.class).isPresent()) {
                visit(method.getAnnotations().getName().getAnnotations(), p);
                visit(method.getName(), p);
            }

            JContainer<Statement> params = method.getPadding().getParameters();
            beforeSyntax(params.getBefore(), params.getMarkers(), JContainer.Location.METHOD_DECLARATION_PARAMETERS.getBeforeLocation(), p);
            if (!params.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                p.append("(");
            }
            int i = hasReceiverType ? 1 : 0;
            List<JRightPadded<Statement>> elements = params.getPadding().getElements();
            for (; i < elements.size(); i++) {
                printMethodParameters(p, i, elements);
            }
            afterSyntax(params.getMarkers(), p);
            if (!params.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                p.append(")");
            }

            if (method.getReturnTypeExpression() != null) {
                method.getMarkers().findFirst(TypeReferencePrefix.class).ifPresent(typeReferencePrefix ->
                        kotlinPrinter.visitSpace(typeReferencePrefix.getPrefix(), KSpace.Location.TYPE_REFERENCE_PREFIX, p));
                p.append(":");
                visit(method.getReturnTypeExpression(), p);
            }

            if (typeConstraints != null) {
                visitContainer("where", typeConstraints.getPadding().getConstraints(), JContainer.Location.TYPE_PARAMETERS, ",", "", p);
            }

            visit(method.getBody(), p);
            afterSyntax(method, p);
            return method;
        }

        private void printMethodParameters(PrintOutputCapture<P> p, int i, List<JRightPadded<Statement>> elements) {
            JRightPadded<Statement> element = elements.get(i);
            if (element.getElement().getMarkers().findFirst(Implicit.class).isPresent()) {
                return;
            }

            // inlined modified logic `JavaPrinter#visitRightPadded(JRightPadded, JRightPadded.Location, String, PrintOutputCapture)`
            // as that method would end up printing markers before the element and there is currently no way to differentiate
            // before and after markers
            String suffix = i == elements.size() - 1 ? "" : ",";
            visit(((JRightPadded<? extends J>) element).getElement(), p);
            visitSpace(element.getAfter(), JContainer.Location.METHOD_DECLARATION_PARAMETERS.getElementLocation().getAfterLocation(), p);
            visitMarkers(element.getMarkers(), p);
            p.append(suffix);
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
            boolean indexedAccess = method.getMarkers().findFirst(IndexedAccess.class).isPresent();

            beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);

            visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, p);
            if (method.getSelect() != null && !method.getMarkers().findFirst(Extension.class).isPresent() && !indexedAccess) {
                if (method.getMarkers().findFirst(IsNullSafe.class).isPresent()) {
                    p.append("?");
                }

                if (!"<empty>".equals(method.getName().getSimpleName()) &&
                    !method.getName().getMarkers().findFirst(Implicit.class).isPresent()) {
                    p.append(".");
                }
            }

            if (!indexedAccess) {
                visit(method.getName(), p);
                visitContainer("<", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
            }

            visitArgumentsContainer(method.getPadding().getArguments(), Space.Location.METHOD_INVOCATION_ARGUMENTS, p);

            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitNullableType(J.NullableType nt, PrintOutputCapture<P> p) {
            visit(nt.getAnnotations(), p);
            beforeSyntax(nt, Space.Location.NULLABLE_TYPE_PREFIX, p);
            visit(nt.getTypeTree(), p);
            visitSpace(nt.getPadding().getTypeTree().getAfter(), Space.Location.NULLABLE_TYPE_SUFFIX, p);
            p.append("?");
            afterSyntax(nt, p);
            return nt;
        }

        private void visitArgumentsContainer(JContainer<Expression> argContainer, Space.Location argsLocation, PrintOutputCapture<P> p) {
            visitSpace(argContainer.getBefore(), argsLocation, p);
            List<JRightPadded<Expression>> args = argContainer.getPadding().getElements();
            boolean omitParensOnMethod = argContainer.getMarkers().findFirst(OmitParentheses.class).isPresent();
            boolean indexedAccess = argContainer.getMarkers().findFirst(IndexedAccess.class).isPresent();

            int argCount = args.size();
            boolean isTrailingLambda = !args.isEmpty() && args.get(argCount - 1).getElement().getMarkers().findFirst(TrailingLambdaArgument.class).isPresent();

            if (!omitParensOnMethod) {
                p.append(indexedAccess ? '[' : '(');
            }

            for (int i = 0; i < argCount; i++) {
                JRightPadded<Expression> arg = args.get(i);

                // Print trailing lambda.
                if (i == argCount - 1 && isTrailingLambda) {
                    visitSpace(arg.getAfter(), JRightPadded.Location.METHOD_INVOCATION_ARGUMENT.getAfterLocation(), p);
                    if (!omitParensOnMethod) {
                        p.append(indexedAccess ? ']' : ')');
                    }
                    visit(arg.getElement(), p);
                    break;
                }

                if (i > 0 && omitParensOnMethod && !args.get(0).getElement().getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                    p.append(indexedAccess ? ']' : ')');
                } else if (i > 0) {
                    p.append(',');
                }

                visitRightPadded(arg, JRightPadded.Location.METHOD_INVOCATION_ARGUMENT, p);
            }

            if (!omitParensOnMethod && !isTrailingLambda) {
                p.append(indexedAccess ? ']' : ')');
            }
        }

        @Override
        public J visitNewClass(J.NewClass newClass, PrintOutputCapture<P> p) {
            beforeSyntax(newClass, Space.Location.NEW_CLASS_PREFIX, p);

            KObject kObject = newClass.getMarkers().findFirst(KObject.class).orElse(null);
            if (kObject != null) {
                p.append("object");
                // kotlinPrinter.visitSpace(kObject.getPrefix(), KSpace.Location.OBJECT_PREFIX, p);
            }

            newClass.getMarkers().findFirst(TypeReferencePrefix.class).ifPresent(typeReferencePrefix ->
                    kotlinPrinter.visitSpace(typeReferencePrefix.getPrefix(), KSpace.Location.TYPE_REFERENCE_PREFIX, p));

            if (kObject != null && newClass.getClazz() != null) {
                p.append(":");
            }

            visitRightPadded(newClass.getPadding().getEnclosing(), JRightPadded.Location.NEW_CLASS_ENCLOSING, ".", p);
            visitSpace(newClass.getNew(), Space.Location.NEW_PREFIX, p);
            visit(newClass.getClazz(), p);

            visitArgumentsContainer(newClass.getPadding().getArguments(), Space.Location.NEW_CLASS_ARGUMENTS, p);

            visit(newClass.getBody(), p);
            afterSyntax(newClass, p);
            return newClass;
        }

        @Override
        public J visitReturn(J.Return return_, PrintOutputCapture<P> p) {
            if (return_.getMarkers().findFirst(ImplicitReturn.class).isPresent()) {
                visitSpace(return_.getPrefix(), Space.Location.RETURN_PREFIX, p);
                visitMarkers(return_.getMarkers(), p);
                visit(return_.getExpression(), p);
                afterSyntax(return_, p);
                return return_;
            }
            return super.visitReturn(return_, p);
        }

        @Override
        public J visitTernary(J.Ternary ternary, PrintOutputCapture<P> p) {
            beforeSyntax(ternary, Space.Location.TERNARY_PREFIX, p);
            visitLeftPadded("", ternary.getPadding().getTruePart(), JLeftPadded.Location.TERNARY_TRUE, p);
            visitLeftPadded("?:", ternary.getPadding().getFalsePart(), JLeftPadded.Location.TERNARY_FALSE, p);
            afterSyntax(ternary, p);
            return ternary;
        }

        @Override
        public J visitTypeCast(J.TypeCast typeCast, PrintOutputCapture<P> p) {
            beforeSyntax(typeCast, Space.Location.TYPE_CAST_PREFIX, p);
            visit(typeCast.getExpression(), p);

            J.ControlParentheses<TypeTree> controlParens = typeCast.getClazz();
            beforeSyntax(controlParens, Space.Location.CONTROL_PARENTHESES_PREFIX, p);

            String as = typeCast.getMarkers().findFirst(IsNullSafe.class).isPresent() ? "as?" : "as";
            p.append(as);

            visit(controlParens.getTree(), p);
            afterSyntax(typeCast, p);
            return typeCast;
        }

        @Override
        public J visitTypeParameter(J.TypeParameter typeParam, PrintOutputCapture<P> p) {
            beforeSyntax(typeParam, Space.Location.TYPE_PARAMETERS_PREFIX, p);
            visit(typeParam.getAnnotations(), p);
            //can be null for old parse results
            //noinspection ConstantValue
            if (typeParam.getModifiers() != null) {
                for (J.Modifier m : typeParam.getModifiers()) {
                    visitModifier(m, p);
                }
            }

            String delimiter = "";
            visit(typeParam.getName(), p);
            if (typeParam.getBounds() != null && !typeParam.getBounds().isEmpty()) {
                Optional<TypeReferencePrefix> maybeTypeReferencePrefix = typeParam.getMarkers().findFirst(TypeReferencePrefix.class);
                if (maybeTypeReferencePrefix.isPresent()) {
                    delimiter = ":";
                    kotlinPrinter.visitSpace(maybeTypeReferencePrefix.get().getPrefix(), KSpace.Location.TYPE_REFERENCE_PREFIX, p);
                }
            }
            visitContainer(delimiter, typeParam.getPadding().getBounds(), JContainer.Location.TYPE_BOUNDS, "&", "", p);
            afterSyntax(typeParam, p);
            return typeParam;
        }

        @Override
        public J visitUnary(J.Unary unary, PrintOutputCapture<P> p) {
            if (unary.getOperator() == J.Unary.Type.Not && unary.getExpression() instanceof K.Binary && ((K.Binary) unary.getExpression()).getOperator() == K.Binary.Type.NotContains) {
                // This is a special case for the `!in` operator.
                // The `!` is a unary operator, but the `in` is a binary operator.
                // The `!` is printed as part of the binary operator.
                beforeSyntax(unary, Space.Location.UNARY_PREFIX, p);
                visit(unary.getExpression(), p);
                afterSyntax(unary, p);
                return unary;
            }
            return super.visitUnary(unary, p);
        }

        @Override
        public J visitWildcard(J.Wildcard wildcard, PrintOutputCapture<P> p) {
            beforeSyntax(wildcard, Space.Location.WILDCARD_PREFIX, p);
            if (wildcard.getPadding().getBound() != null) {
                p.append(wildcard.getPadding().getBound().getElement() == J.Wildcard.Bound.Super ? "in" : "out");
            }
            if (wildcard.getBoundedType() == null) {
                p.append('*');
            } else {
                visit(wildcard.getBoundedType(), p);
            }
            afterSyntax(wildcard, p);
            return wildcard;
        }

        @Override
        public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
            beforeSyntax(multiVariable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);

            visit(multiVariable.getLeadingAnnotations(), p);
            boolean isTypeAlias = false;
            for (J.Modifier m : multiVariable.getModifiers()) {
                visitModifier(m, p);
                if (m.getType() == J.Modifier.Type.Final) {
                    p.append("val");
                }

                if (m.getType() == J.Modifier.Type.LanguageExtension && m.getKeyword() != null && "typealias".equals(m.getKeyword())) {
                    isTypeAlias = true;
                }
            }

            boolean containsTypeReceiver = multiVariable.getMarkers().findFirst(Extension.class).isPresent();
            List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = multiVariable.getPadding().getVariables();
            // V1: Covers and unique case in `mapForLoop` of the KotlinParserVisitor caused by how the FirElement represents for loops.
            for (int i = 0; i < variables.size(); i++) {
                JRightPadded<J.VariableDeclarations.NamedVariable> variable = variables.get(i);
                beforeSyntax(variable.getElement(), Space.Location.VARIABLE_PREFIX, p);
                if (variables.size() > 1 && !containsTypeReceiver && i == 0) {
                    p.append("(");
                }

                visit(variable.getElement().getName(), p);
                visitSpace(variable.getAfter(), Space.Location.VARIABLE_INITIALIZER, p);

                if (multiVariable.getTypeExpression() != null) {
                    if (!isTypeAlias) {
                        p.append(":");
                    }
                    visit(multiVariable.getTypeExpression(), p);
                }

                if (variable.getElement().getPadding().getInitializer() != null) {
                    visitSpace(variable.getElement().getPadding().getInitializer().getBefore(), Space.Location.VARIABLE_INITIALIZER, p);
                }

                if (variable.getElement().getInitializer() != null) {
                    String equals = getEqualsText(multiVariable);
                    p.append(equals);
                }

                visit(variable.getElement().getInitializer(), p);

                if (i < variables.size() - 1) {
                    p.append(",");
                } else if (variables.size() > 1 && !containsTypeReceiver) {
                    p.append(")");
                }


                variable.getMarkers().findFirst(Semicolon.class).ifPresent(m -> visitMarker(m, p));
            }

            afterSyntax(multiVariable, p);
            return multiVariable;
        }

        @Override
        public J visitVariable(J.VariableDeclarations.NamedVariable variable, PrintOutputCapture<P> p) {
            beforeSyntax(variable, Space.Location.VARIABLE_PREFIX, p);
            boolean isTypeReceiver = variable.getMarkers().findFirst(Extension.class).isPresent();
            if (!isTypeReceiver) {
                visit(variable.getName(), p);
            }
            visitLeftPadded(isTypeReceiver ? "" : "=", variable.getPadding().getInitializer(), JLeftPadded.Location.VARIABLE_INITIALIZER, p);
            afterSyntax(variable, p);
            return variable;
        }

        protected void visitStatement(@Nullable JRightPadded<Statement> paddedStat, JRightPadded.Location location, PrintOutputCapture<P> p) {
            if (paddedStat != null) {
                Statement element = paddedStat.getElement();
                if (element.getMarkers().findFirst(Implicit.class).isPresent()) {
                    return;
                }
                visit(element, p);
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


        /**
         * Does not print the final modifier, as it is not supported in Kotlin.
         */
        @Override
        public J visitModifier(J.Modifier mod, PrintOutputCapture<P> p) {
            visit(mod.getAnnotations(), p);
            String keyword = "";
            switch (mod.getType()) {
                case Default:
                    keyword = "default";
                    break;
                case Public:
                    keyword = "public";
                    break;
                case Protected:
                    keyword = "protected";
                    break;
                case Private:
                    keyword = "private";
                    break;
                case Abstract:
                    keyword = "abstract";
                    break;
                case Static:
                    keyword = "static";
                    break;
                case Native:
                    keyword = "native";
                    break;
                case NonSealed:
                    keyword = "non-sealed";
                    break;
                case Sealed:
                    keyword = "sealed";
                    break;
                case Strictfp:
                    keyword = "strictfp";
                    break;
                case Synchronized:
                    keyword = "synchronized";
                    break;
                case Transient:
                    keyword = "transient";
                    break;
                case Volatile:
                    keyword = "volatile";
                    break;
                case LanguageExtension:
                    keyword = mod.getKeyword();
                    break;
            }
            beforeSyntax(mod, Space.Location.MODIFIER_PREFIX, p);
            p.append(keyword);
            afterSyntax(mod, p);
            return mod;
        }

        @Override
        protected void afterSyntax(J j, PrintOutputCapture<P> p) {
            super.afterSyntax(j, p);
        }
    }

    private static String getClassKind(J.ClassDeclaration classDecl) {
        String kind;
        if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Class || classDecl.getKind() == J.ClassDeclaration.Kind.Type.Enum || classDecl.getKind() == J.ClassDeclaration.Kind.Type.Annotation) {
            kind = "class";
        } else if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Interface) {
            kind = "interface";
        } else {
            throw new UnsupportedOperationException("Class kind is not supported: " + classDecl.getKind());
        }
        return kind;
    }

    @Override
    public Space visitSpace(Space space, KSpace.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, loc, p);
    }

    protected void visitContainer(String before, @Nullable JContainer<? extends J> container, KContainer.Location location,
                                  @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        visitSpace(container.getBefore(), location.getBeforeLocation(), p);
        p.append(before);
        visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), p);
        p.append(after == null ? "" : after);
    }

    @SuppressWarnings("SameParameterValue")
    protected void visitLeftPadded(@Nullable String prefix, @Nullable JLeftPadded<? extends J> leftPadded, KLeftPadded.Location location, PrintOutputCapture<P> p) {
        if (leftPadded != null) {
            beforeSyntax(leftPadded.getBefore(), leftPadded.getMarkers(), location.getBeforeLocation(), p);
            if (prefix != null) {
                p.append(prefix);
            }
            visit(leftPadded.getElement(), p);
            afterSyntax(leftPadded.getMarkers(), p);
        }
    }

    protected void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, KRightPadded.Location location, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), location.getAfterLocation(), p);
            visitMarkers(node.getMarkers(), p);
            if (i < nodes.size() - 1) {
                p.append(",");
            }
        }
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        return delegate.visitMarker(marker, p);
    }

    private static final UnaryOperator<String> JAVA_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    private void beforeSyntax(K k, KSpace.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(k.getPrefix(), k.getMarkers(), loc, p);
    }

    @SuppressWarnings("SameParameterValue")
    private void beforeSyntax(J j, Space.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(j.getPrefix(), j.getMarkers(), loc, p);
    }

    private void beforeSyntax(Space prefix, Markers markers, KSpace.@Nullable Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
        if (loc != null) {
            visitSpace(prefix, loc, p);
        }
        KotlinPrinter.this.visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }

    private void beforeSyntax(K k, Space.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(k.getPrefix(), k.getMarkers(), loc, p);
    }

    private void beforeSyntax(Space prefix, Markers markers, Space.@Nullable Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
        if (loc != null) {
            delegate.visitSpace(prefix, loc, p);
        }
        KotlinPrinter.this.visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(J j, PrintOutputCapture<P> p) {
        afterSyntax(j.getMarkers(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }

    private static String getEqualsText(J.VariableDeclarations vd) {
        String equals = "=";
        for (Marker marker : vd.getMarkers().getMarkers()) {
            if (marker instanceof By) {
                equals = "by";
                break;
            } else if (marker instanceof OmitEquals) {
                equals = "";
                break;
            }
        }
        return equals;
    }
}
