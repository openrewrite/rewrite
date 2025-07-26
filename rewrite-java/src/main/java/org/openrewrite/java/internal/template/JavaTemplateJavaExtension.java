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
package org.openrewrite.java.internal.template;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.Location.*;

/**
 * Not so much an extension as the FOUNDATION of templating for the Java family of languages. Nevertheless, the
 * base class gives us an opportunity to extract this long block of code from {@link JavaTemplate} itself and
 * make it a bit more readable.
 */
public class JavaTemplateJavaExtension extends JavaTemplateLanguageExtension {
    private static final J.Block EMPTY_BLOCK = new J.Block(randomId(), Space.EMPTY,
            Markers.EMPTY, new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY),
            emptyList(), Space.format(" "));

    public JavaTemplateJavaExtension(JavaTemplateParser templateParser, Substitutions substitutions,
                                     String substitutedTemplate, JavaCoordinates coordinates) {
        super(templateParser, substitutions, substitutedTemplate, coordinates);
    }

    @Override
    public TreeVisitor<? extends J, Integer> getMixin() {
        return new JavaVisitor<Integer>() {
            private boolean substituted;

            @Override
            public J visitAnnotation(J.Annotation annotation, Integer p) {
                if (loc == ANNOTATION_PREFIX && mode == JavaCoordinates.Mode.REPLACEMENT &&
                    isScope(annotation)) {
                    List<J.Annotation> gen = unsubstitute(templateParser.parseAnnotations(getCursor(), substitutedTemplate));
                    if (gen.isEmpty()) {
                        throw new IllegalStateException("Unable to parse annotation from template: \n" +
                                                        substitutedTemplate +
                                                        "\nUse JavaTemplate.Builder.doBeforeParseTemplate() to see what stub is being generated and include it in any bug report.");
                    }
                    return gen.get(0).withPrefix(annotation.getPrefix());
                } else if (loc == ANNOTATION_ARGUMENTS && mode == JavaCoordinates.Mode.REPLACEMENT &&
                           isScope(annotation)) {
                    List<J.Annotation> gen = unsubstitute(templateParser.parseAnnotations(getCursor(), "@Example(" + substitutedTemplate + ")"));
                    return annotation.withArguments(gen.get(0).getArguments());
                }

                return super.visitAnnotation(annotation, p);
            }

            @Override
            public J visitBlock(J.Block block, Integer p) {
                switch (loc) {
                    case BLOCK_END: {
                        if (isScope(block)) {
                            List<Statement> gen = unsubstitute(templateParser.parseBlockStatements(
                                    new Cursor(getCursor(), insertionPoint), Statement.class, substitutedTemplate,
                                    substitutions.getTypeVariables(), loc, mode));

                            if (coordinates.getComparator() != null) {
                                J.Block b = block;
                                for (Statement g : gen) {
                                    b = b.withStatements(
                                            ListUtils.insertInOrder(
                                                    block.getStatements(),
                                                    autoFormat(g, p, getCursor()),
                                                    getComparatorOrThrow()
                                            )
                                    );
                                }
                                return b;
                            }

                            return block.withStatements(
                                    ListUtils.concatAll(
                                            block.getStatements(),
                                            ListUtils.map(gen, (i, s) -> autoFormat(s, p, getCursor()))
                                    )
                            );
                        }
                        break;
                    }
                    case STATEMENT_PREFIX: {
                        return block.withStatements(ListUtils.flatMap(block.getStatements(), statement -> {
                            if (isScope(statement)) {
                                List<Statement> gen = unsubstitute(templateParser.parseBlockStatements(
                                        new Cursor(getCursor(), insertionPoint), Statement.class, substitutedTemplate,
                                        substitutions.getTypeVariables(), loc, mode));

                                Cursor parent = getCursor();
                                for (int i = 0; i < gen.size(); i++) {
                                    Statement s = gen.get(i);
                                    Statement formattedS = autoFormat(i == 0 ? s.withPrefix(statement.getPrefix().withComments(emptyList())) : s, p, parent);
                                    gen.set(i, formattedS);
                                }

                                switch (mode) {
                                    case REPLACEMENT:
                                        return gen;
                                    case BEFORE:
                                        return ListUtils.concat(gen, statement);
                                    case AFTER:
                                        return ListUtils.concat(statement, gen);
                                }
                            }
                            return statement;
                        }));
                    }
                }
                return super.visitBlock(block, p);
            }

            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                if (isScope(classDecl)) {
                    switch (loc) {
                        case ANNOTATIONS: {
                            List<J.Annotation> gen = unsubstitute(templateParser.parseAnnotations(getCursor(), substitutedTemplate));
                            J.ClassDeclaration c = classDecl;
                            if (mode == JavaCoordinates.Mode.REPLACEMENT) {
                                c = c.withLeadingAnnotations(gen);
                                if (c.getTypeParameters() != null) {
                                    c = c.withTypeParameters(ListUtils.map(c.getTypeParameters(), tp -> tp.withAnnotations(emptyList())));
                                }
                                c = c.withModifiers(ListUtils.map(c.getModifiers(), m -> m.withAnnotations(emptyList())));
                                c = c.getPadding().withKind(c.getPadding().getKind().withAnnotations(emptyList()));
                            } else {
                                for (J.Annotation a : gen) {
                                    c = c.withLeadingAnnotations(ListUtils.insertInOrder(c.getLeadingAnnotations(), a,
                                            getComparatorOrThrow()));
                                }
                            }
                            return autoFormat(c, c.getName(), p, getCursor().getParentOrThrow());
                        }
                        case EXTENDS: {
                            TypeTree anExtends = unsubstitute(templateParser.parseExtends(getCursor(), substitutedTemplate));
                            J.ClassDeclaration c = classDecl.withExtends(anExtends);

                            //noinspection ConstantConditions
                            return c.getPadding().withExtends(c.getPadding().getExtends().withBefore(Space.format(" ")));
                        }
                        case IMPLEMENTS: {
                            List<TypeTree> implementings = unsubstitute(templateParser.parseImplements(getCursor(), substitutedTemplate));
                            List<JavaType.FullyQualified> implementsTypes = implementings.stream()
                                    .map(TypedTree::getType)
                                    .map(TypeUtils::asFullyQualified)
                                    .filter(Objects::nonNull)
                                    .collect(toList());
                            J.ClassDeclaration c = classDecl;

                            if (mode == JavaCoordinates.Mode.REPLACEMENT) {
                                c = c.withImplements(implementings);
                                //noinspection ConstantConditions
                                c = c.getPadding().withImplements(c.getPadding().getImplements().withBefore(Space.EMPTY));
                            } else {
                                c = c.withImplements(ListUtils.concatAll(c.getImplements(), implementings));
                            }
                            if (c.getType() != null) {
                                c = c.withType(((JavaType.Class) c.getType()).withInterfaces(ListUtils.concatAll(c.getType().getInterfaces(), implementsTypes)));
                            }

                            //noinspection ConstantConditions
                            return autoFormat(c, c.getImplements().get(c.getImplements().size() - 1), p,
                                    getCursor().getParentOrThrow());
                        }
                        case TYPE_PARAMETERS: {
                            List<J.TypeParameter> typeParameters = unsubstitute(templateParser.parseTypeParameters(getCursor(), substitutedTemplate));
                            return classDecl.withTypeParameters(typeParameters);
                        }
                    }
                }
                return super.visitClassDeclaration(classDecl, p);
            }

            @Override
            public J visitExpression(Expression expression, Integer p) {
                if ((loc == EXPRESSION_PREFIX ||
                     loc == STATEMENT_PREFIX && expression instanceof Statement) &&
                    isScope(expression)) {
                    return autoFormat(unsubstitute(templateParser.parseExpression(
                                    getCursor(),
                                    substitutedTemplate,
                                    substitutions.getTypeVariables(),
                                    loc))
                            .withPrefix(expression.getPrefix()), p);
                }
                return expression;
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fa, Integer p) {
                if (loc == FIELD_ACCESS_PREFIX && isScope(fa)) {
                    return autoFormat(unsubstitute(templateParser.parseExpression(
                                    getCursor(),
                                    substitutedTemplate,
                                    substitutions.getTypeVariables(),
                                    loc))
                            .withPrefix(fa.getPrefix()), p);
                } else if (loc == STATEMENT_PREFIX && isScope(fa)) {
                    // NOTE: while `J.FieldAccess` inherits from `Statement` they can only ever be used as expressions
                    return autoFormat(unsubstitute(templateParser.parseExpression(
                                    getCursor(),
                                    substitutedTemplate,
                                    substitutions.getTypeVariables(),
                                    loc))
                            .withPrefix(fa.getPrefix()), p);
                }
                return super.visitFieldAccess(fa, p);
            }

            @Override
            public J visitIdentifier(J.Identifier ident, Integer p) {
                // ONLY for backwards compatibility, otherwise the same as expression replacement
                if (loc == IDENTIFIER_PREFIX && isScope(ident)) {
                    return autoFormat(unsubstitute(templateParser.parseExpression(
                                    getCursor(),
                                    substitutedTemplate,
                                    substitutions.getTypeVariables(),
                                    loc))
                            .withPrefix(ident.getPrefix()), p);
                }
                return super.visitIdentifier(ident, p);
            }

            @Override
            public J visitLambda(J.Lambda lambda, Integer p) {
                if (loc == LAMBDA_PARAMETERS_PREFIX && isScope(lambda.getParameters())) {
                    return lambda.withParameters(unsubstitute(templateParser.parseLambdaParameters(getCursor(), substitutedTemplate)));
                }
                return maybeReplaceStatement(lambda, J.class, 0);
            }

            @Override
            public J visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                if (isScope(method)) {
                    switch (loc) {
                        case ANNOTATIONS: {
                            List<J.Annotation> gen = unsubstitute(templateParser.parseAnnotations(getCursor(), substitutedTemplate));
                            J.MethodDeclaration m = method;
                            if (mode == JavaCoordinates.Mode.REPLACEMENT) {
                                m = method.withLeadingAnnotations(gen);
                                if (m.getTypeParameters() != null) {
                                    m = m.withTypeParameters(ListUtils.map(m.getTypeParameters(), tp -> tp.withAnnotations(emptyList())));
                                }
                                if (m.getReturnTypeExpression() instanceof J.AnnotatedType) {
                                    m = m.withReturnTypeExpression(((J.AnnotatedType) m.getReturnTypeExpression()).getTypeExpression());
                                }
                                m = m.withModifiers(ListUtils.map(m.getModifiers(), m2 -> m2.withAnnotations(emptyList())));
                                m = m.getAnnotations().withName(m.getAnnotations().getName().withAnnotations(emptyList()));
                            } else {
                                for (J.Annotation a : gen) {
                                    m = m.withLeadingAnnotations(ListUtils.insertInOrder(m.getLeadingAnnotations(), a,
                                            getComparatorOrThrow()));
                                }
                            }
                            return autoFormat(m, m.getName(), p,
                                    getCursor().getParentOrThrow());
                        }
                        case BLOCK_PREFIX: {
                            List<Statement> gen = unsubstitute(templateParser.parseBlockStatements(getCursor(), Statement.class,
                                    substitutedTemplate, substitutions.getTypeVariables(), loc, mode));
                            J.Block body = method.getBody();
                            if (body == null) {
                                body = EMPTY_BLOCK;
                            }
                            body = body.withStatements(gen);
                            return method.withBody(autoFormat(body, p, getCursor()));
                        }
                        case METHOD_DECLARATION_PARAMETERS: {
                            List<Statement> parameters = unsubstitute(templateParser.parseParameters(getCursor(), substitutedTemplate));

                            // Update the J.MethodDeclaration's type information to reflect its new parameter list
                            JavaType.Method type = method.getMethodType();
                            if (type != null) {
                                List<String> paramNames = new ArrayList<>(parameters.size());
                                List<JavaType> paramTypes = new ArrayList<>(parameters.size());
                                for (Statement parameter : parameters) {
                                    if (!(parameter instanceof J.VariableDeclarations)) {
                                        throw new IllegalArgumentException(
                                                "Only variable declarations may be part of a method declaration's parameter " +
                                                "list:" + parameter.print(getCursor()));
                                    }
                                    J.VariableDeclarations decl = (J.VariableDeclarations) parameter;
                                    if (decl.getVariables().size() != 1) {
                                        throw new IllegalArgumentException(
                                                "Multi-variable declarations may not be used in a method declaration's " +
                                                "parameter list: " + parameter.print(getCursor()));
                                    }
                                    J.VariableDeclarations.NamedVariable namedVariable = decl.getVariables().get(0);
                                    paramNames.add(namedVariable.getSimpleName());
                                    // Make a best-effort attempt to update the type information
                                    if (namedVariable.getType() == null && decl.getTypeExpression() instanceof J.Identifier) {
                                        // null if the type of the argument is a generic type parameter
                                        // Try to find an appropriate type from the method itself
                                        J.Identifier declTypeIdent = (J.Identifier) decl.getTypeExpression();
                                        String typeParameterName = declTypeIdent.getSimpleName();
                                        List<J.TypeParameter> typeParameters = (method.getTypeParameters() == null) ? emptyList() : method.getTypeParameters();
                                        for (J.TypeParameter typeParameter : typeParameters) {
                                            J.Identifier typeParamIdent = (J.Identifier) typeParameter.getName();
                                            if (typeParamIdent.getSimpleName().equals(typeParameterName)) {
                                                List<TypeTree> bounds = typeParameter.getBounds();
                                                JavaType.FullyQualified bound;
                                                if (bounds == null || bounds.isEmpty()) {
                                                    bound = JavaType.ShallowClass.build("java.lang.Object");
                                                } else {
                                                    bound = (JavaType.FullyQualified) bounds.get(0);
                                                }

                                                JavaType.GenericTypeVariable genericType = new JavaType.GenericTypeVariable(
                                                        null, typeParamIdent.getSimpleName(),
                                                        JavaType.GenericTypeVariable.Variance.COVARIANT,
                                                        singletonList(bound));

                                                paramTypes.add(genericType);
                                            }
                                        }
                                    } else {
                                        paramTypes.add(namedVariable.getType());
                                    }
                                }

                                type = type.withParameterNames(paramNames).withParameterTypes(paramTypes);
                            }

                            return method.withParameters(parameters).withMethodType(type).withName(method.getName().withType(type));
                        }
                        case THROWS: {
                            J.MethodDeclaration m = method.withThrows(unsubstitute(templateParser.parseThrows(getCursor(), substitutedTemplate)));

                            // Update method type information to reflect the new checked exceptions
                            JavaType.Method type = m.getMethodType();
                            if (type != null) {
                                List<JavaType> newThrows = new ArrayList<>();
                                List<NameTree> throws_ = (m.getThrows() == null) ? emptyList() : m.getThrows();
                                for (NameTree t : throws_) {
                                    J.Identifier exceptionIdent = (J.Identifier) t;
                                    newThrows.add(exceptionIdent.getType());
                                }
                                type = type.withThrownExceptions(newThrows);
                            }

                            //noinspection ConstantConditions
                            return m.getPadding().withThrows(m.getPadding().getThrows().withBefore(Space.format(" ")))
                                    .withMethodType(type).withName(method.getName().withType(type));
                        }
                        case TYPE_PARAMETERS: {
                            List<J.TypeParameter> typeParameters = unsubstitute(templateParser.parseTypeParameters(getCursor(), substitutedTemplate));
                            J.MethodDeclaration m = method.withTypeParameters(typeParameters);
                            if (m.getName().getType() != null) {
                                m = m.withName(method.getName().withType(m.getMethodType()));
                            }
                            return autoFormat(m, typeParameters.get(typeParameters.size() - 1), p,
                                    getCursor().getParentOrThrow());
                        }
                    }
                }
                return super.visitMethodDeclaration(method, p);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                if (getCursor().firstEnclosing(Javadoc.DocComment.class) != null) {
                    // We don't have support for changing method references in Javadoc comments (yet), so it's safer not to attempt any changes
                    return method;
                }
                if ((loc == METHOD_INVOCATION_ARGUMENTS || loc == METHOD_INVOCATION_NAME) && isScope(method)) {
                    J.MethodInvocation m;
                    if (loc == METHOD_INVOCATION_ARGUMENTS) {
                        m = unsubstitute(templateParser.parseMethodArguments(getCursor(), substitutedTemplate, substitutions.getTypeVariables(), loc));
                        m = autoFormat(m, 0);
                        m = method.withArguments(m.getArguments()).withMethodType(m.getMethodType());
                    } else {
                        m = unsubstitute(templateParser.parseMethod(getCursor(), substitutedTemplate, substitutions.getTypeVariables(), loc));
                        m = autoFormat(m, 0);
                        m = method.withName(m.getName()).withArguments(m.getArguments()).withMethodType(m.getMethodType());
                    }

                    // This will only happen if the template encountered non-fatal errors during parsing
                    // Make a best-effort attempt to recover by patching together a new Method type from the old one
                    // There are many ways this type could be not quite right, but leaving the type alone is likely to cause MethodMatcher false-positives
                    JavaType.Method mt = method.getMethodType();
                    if (m.getMethodType() == null && mt != null) {
                        List<JavaType> argTypes = m.getArguments().stream()
                                .map(Expression::getType)
                                .map(it -> {
                                    // Invoking a method with a string literal still means the invocation has the class type
                                    if (it == JavaType.Primitive.String) {
                                        return JavaType.ShallowClass.build("java.lang.String");
                                    }
                                    return it;
                                })
                                .collect(toList());
                        mt = mt.withParameterTypes(argTypes);
                        m = m.withMethodType(mt);
                    }
                    if (m.getName().getType() != null) {
                        m = m.withName(m.getName().withType(m.getType()));
                    }
                    return m;
                }
                return maybeReplaceStatement(method, J.class, 0);
            }

            @Override
            public J visitNewClass(J.NewClass newClass, Integer p) {
                if (isScope(newClass)) {
                    // allow a `J.NewClass` to also be replaced by an expression
                    return maybeReplaceStatement(newClass, J.class, p);
                }
                return super.visitNewClass(newClass, p);
            }

            @Override
            public J visitPackage(J.Package pkg, Integer integer) {
                if (loc == PACKAGE_PREFIX && isScope(pkg)) {
                    return pkg.withExpression(unsubstitute(templateParser.parsePackage(getCursor(), substitutedTemplate)));
                }
                return super.visitPackage(pkg, integer);
            }

            @Override
            public J visitStatement(Statement statement, Integer p) {
                return maybeReplaceStatement(statement, Statement.class, p);
            }

            private <J3 extends J> J3 maybeReplaceStatement(Statement statement, Class<J3> expected, Integer p) {
                if (loc == STATEMENT_PREFIX && isScope(statement)) {
                    if (mode == JavaCoordinates.Mode.REPLACEMENT) {
                        List<J3> gen = unsubstitute(templateParser.parseBlockStatements(getCursor(),
                                expected, substitutedTemplate, substitutions.getTypeVariables(), loc, mode));
                        if (gen.size() != 1) {
                            // for some languages with optional semicolons, templates may generate a statement
                            // and an empty, e.g. for a statement replacement in Groovy for the last statement
                            // of a method that has an implicit return
                            if (gen.size() == 2) {
                                if (gen.get(0) instanceof J.Empty) {
                                    return autoFormat(gen.get(1).withPrefix(statement.getPrefix()), p);
                                }
                                if (gen.get(1) instanceof J.Empty) {
                                    return autoFormat(gen.get(0).withPrefix(statement.getPrefix()), p);
                                }
                            }
                            throw new IllegalArgumentException("Expected a template that would generate exactly one " +
                                                               "statement to replace one statement, but generated " + gen.size() +
                                                               ". Template:\n" + substitutedTemplate + "\nSubstitutions:\n" + substitutions +
                                                               "\nStatement:\n" + statement);
                        }

                        return autoFormat(gen.get(0).withPrefix(statement.getPrefix()), p);
                    }
                    throw new IllegalArgumentException("Cannot insert a new statement before an existing statement and return both to a visit method that returns one statement.");
                }
                //noinspection unchecked
                return (J3) super.visitStatement(statement, p);
            }

            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer p) {
                if (isScope(multiVariable)) {
                    if (loc == ANNOTATIONS) {
                        J.VariableDeclarations v = multiVariable;
                        final List<J.Annotation> gen = unsubstitute(templateParser.parseAnnotations(getCursor(), substitutedTemplate));
                        if (mode == JavaCoordinates.Mode.REPLACEMENT) {
                            v = v.withLeadingAnnotations(gen);
                            if (v.getTypeExpression() instanceof J.AnnotatedType) {
                                v = v.withTypeExpression(((J.AnnotatedType) v.getTypeExpression()).getTypeExpression());
                            }
                            v = v.withModifiers(ListUtils.map(v.getModifiers(), m -> m.withAnnotations(emptyList())));
                        } else {
                            for (J.Annotation a : gen) {
                                v = v.withLeadingAnnotations(ListUtils.insertInOrder(v.getLeadingAnnotations(), a,
                                        getComparatorOrThrow()));
                            }
                        }
                        return autoFormat(v, v.getTypeExpression(), p,
                                getCursor().getParentOrThrow());
                    }
                }
                return super.visitVariableDeclarations(multiVariable, p);
            }

            private boolean isScope(J test) {
                return !substituted && test.isScope(insertionPoint);
            }

            private <J2 extends J> @Nullable J2 unsubstitute(J2 j) {
                try {
                    return substitutions.unsubstitute(j);
                } finally {
                    substituted = true;
                }
            }

            private <J2 extends J> List<J2> unsubstitute(List<J2> js) {
                try {
                    return substitutions.unsubstitute(js);
                } finally {
                    substituted = true;
                }
            }
        };
    }

    private <J2 extends J> Comparator<J2> getComparatorOrThrow() {
        return requireNonNull(coordinates.getComparator());
    }
}
