/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.template.JavaTemplateParser;
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.tree.Space.Location;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.Location.*;

public class JavaTemplate {
    private static final J.Block EMPTY_BLOCK = new J.Block(randomId(), Space.EMPTY,
            Markers.EMPTY, new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY),
            Collections.emptyList(), Space.format(" "));

    private final Supplier<Cursor> parentScopeGetter;
    private final String code;
    private final int parameterCount;
    private final Consumer<String> onAfterVariableSubstitution;
    private final JavaTemplateParser templateParser;

    private JavaTemplate(Supplier<Cursor> parentScopeGetter, Supplier<JavaParser> parser, String code, Set<String> imports,
                         Consumer<String> onAfterVariableSubstitution, Consumer<String> onBeforeParseTemplate) {
        this.parentScopeGetter = parentScopeGetter;
        this.code = code;
        this.onAfterVariableSubstitution = onAfterVariableSubstitution;
        this.parameterCount = StringUtils.countOccurrences(code, "#{");
        this.templateParser = new JavaTemplateParser(parser, onAfterVariableSubstitution, onBeforeParseTemplate, imports);
    }

    @SuppressWarnings("unchecked")
    public <J2 extends J> J2 withTemplate(Tree changing, JavaCoordinates coordinates, Object[] parameters) {
        if (parameters.length != parameterCount) {
            throw new IllegalArgumentException("This template requires " + parameterCount + " parameters.");
        }

        Substitutions substitutions = new Substitutions(code, parameters);
        String substitutedTemplate = substitutions.substitute();
        onAfterVariableSubstitution.accept(substitutedTemplate);

        Tree insertionPoint = coordinates.getTree();
        Location loc = coordinates.getSpaceLocation();
        JavaCoordinates.Mode mode = coordinates.getMode();

        AtomicReference<Cursor> parentCursorRef = new AtomicReference<>();

        // Find the parent cursor of the CHANGING element, which may not be the same as the cursor of
        // the method using the template. For example, using a template on a class declaration body
        // inside visitClassDeclaration. The body is the changing element, but the current cursor
        // is at the class declaration.
        //
        //      J visitClassDeclaration(J.ClassDeclaration c, Integer p) {
        //            c.getBody().withTemplate(template, c.getBody().coordinates.lastStatement());
        //      }
        new JavaIsoVisitor<Integer>() {
            @Nullable
            @Override
            public J visit(@Nullable Tree tree, Integer integer) {
                if (tree != null && tree.isScope(changing)) {
                    parentCursorRef.set(getCursor());
                    return (J) tree;
                }
                return super.visit(tree, integer);
            }
        }.visit(parentScopeGetter.get().getValue(), 0, parentScopeGetter.get().getParentOrThrow());

        Cursor parentCursor = parentCursorRef.get();

        //noinspection ConstantConditions
        return (J2) new JavaVisitor<Integer>() {
            @Override
            public J visitAnnotation(J.Annotation annotation, Integer integer) {
                if (loc.equals(ANNOTATION_PREFIX) && mode.equals(JavaCoordinates.Mode.REPLACEMENT) &&
                        annotation.isScope(insertionPoint)) {
                    List<J.Annotation> gen = substitutions.unsubstitute(templateParser.parseAnnotations(getCursor(), substitutedTemplate));
                    return gen.get(0).withPrefix(annotation.getPrefix());
                }

                return super.visitAnnotation(annotation, integer);
            }

            @Override
            public J visitBlock(J.Block block, Integer p) {
                switch (loc) {
                    case BLOCK_END: {
                        if(block.isScope(insertionPoint)) {
                            List<Statement> gen = substitutions.unsubstitute(templateParser.parseBlockStatements(
                                    new Cursor(getCursor(), insertionPoint),
                                    substitutedTemplate, loc));
                            return block.withStatements(
                                    ListUtils.concatAll(
                                            block.getStatements(),
                                            ListUtils.map(gen, (i, s) -> autoFormat(i == 0 ? s.withPrefix(Space.format("\n")) : s, p, getCursor()))
                                    )
                            );
                        }
                    }
                    case STATEMENT_PREFIX: {
                        return block.withStatements(ListUtils.flatMap(block.getStatements(), statement -> {
                            if (statement.isScope(insertionPoint)) {
                                List<Statement> gen = substitutions.unsubstitute(templateParser.parseBlockStatements(
                                        new Cursor(getCursor(), insertionPoint),
                                        substitutedTemplate, loc));

                                Cursor parent = getCursor();
                                for (int i = 0; i < gen.size(); i++) {
                                    Statement s = gen.get(i);
                                    Statement formattedS = autoFormat(i == 0 ? s.withPrefix(statement.getPrefix()) : s, p, parent);
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
                if (classDecl.isScope(insertionPoint)) {
                    switch (loc) {
                        case ANNOTATIONS: {
                            List<J.Annotation> gen = substitutions.unsubstitute(templateParser.parseAnnotations(getCursor(), substitutedTemplate));
                            J.ClassDeclaration c = classDecl;
                            if (mode.equals(JavaCoordinates.Mode.REPLACEMENT)) {
                                c = c.withLeadingAnnotations(gen);
                                c = c.withTypeParameters(ListUtils.map(c.getTypeParameters(), tp -> tp.withAnnotations(emptyList())));
                                c = c.withModifiers(ListUtils.map(c.getModifiers(), m -> m.withAnnotations(emptyList())));
                                c = c.getAnnotations().withKind(c.getAnnotations().getKind().withAnnotations(emptyList()));
                            } else {
                                for (J.Annotation a : gen) {
                                    c = c.withLeadingAnnotations(ListUtils.insertInOrder(c.getLeadingAnnotations(), a,
                                            coordinates.getComparator()));
                                }
                            }
                            return autoFormat(c, c.getLeadingAnnotations().get(c.getLeadingAnnotations().size() - 1), p,
                                    getCursor());
                        }
                        case EXTENDS: {
                            TypeTree anExtends = substitutions.unsubstitute(templateParser.parseExtends(substitutedTemplate));
                            J.ClassDeclaration c = classDecl.withExtends(anExtends);

                            //noinspection ConstantConditions
                            c = c.getPadding().withExtends(c.getPadding().getExtends().withBefore(Space.format(" ")));
                            return c;
                        }
                        case IMPLEMENTS: {
                            List<TypeTree> implementings = substitutions.unsubstitute(templateParser.parseImplements(substitutedTemplate));
                            J.ClassDeclaration c = classDecl.withImplements(implementings);

                            //noinspection ConstantConditions
                            c = c.getPadding().withImplements(c.getPadding().getImplements().withBefore(Space.format(" ")));
                            return c;
                        }
                        case TYPE_PARAMETERS: {
                            List<J.TypeParameter> typeParameters = substitutions.unsubstitute(templateParser.parseTypeParameters(substitutedTemplate));
                            return classDecl.withTypeParameters(typeParameters);
                        }
                    }
                }
                return super.visitClassDeclaration(classDecl, p);
            }

            @Override
            public J visitLambda(J.Lambda lambda, Integer p) {
                if (loc.equals(LAMBDA_PARAMETERS_PREFIX) && lambda.getParameters().isScope(insertionPoint)) {
                    return lambda.withParameters(substitutions.unsubstitute(templateParser.parseLambdaParameters(substitutedTemplate)));
                }
                if(loc.equals(STATEMENT_PREFIX) && lambda.isScope(insertionPoint) && mode.equals(JavaCoordinates.Mode.REPLACEMENT)) {
                    J gen = substitutions.unsubstitute(templateParser.parseExpression(substitutedTemplate));
                    return autoFormat(gen.withPrefix(lambda.getPrefix()), p, getCursor().getParentOrThrow());
                }
                return super.visitLambda(lambda, p);
            }

            @Override
            public J visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                if (method.isScope(insertionPoint)) {
                    switch (loc) {
                        case ANNOTATIONS: {
                            List<J.Annotation> gen = substitutions.unsubstitute(templateParser.parseAnnotations(getCursor(), substitutedTemplate));
                            J.MethodDeclaration m = method;
                            if (mode.equals(JavaCoordinates.Mode.REPLACEMENT)) {
                                m = method.withLeadingAnnotations(gen);
                                if (!m.getTypeParameters().isEmpty()) {
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
                                            coordinates.getComparator()));
                                }
                            }
                            return autoFormat(m, m.getLeadingAnnotations().get(m.getLeadingAnnotations().size() - 1), p,
                                    getCursor().getParentOrThrow());
                        }
                        case BLOCK_PREFIX: {
                            List<Statement> gen = substitutions.unsubstitute(templateParser.parseBlockStatements(getCursor(), substitutedTemplate, loc));
                            J.Block body = method.getBody();
                            if (body == null) {
                                body = EMPTY_BLOCK;
                            }
                            body = body.withStatements(gen);
                            return method.withBody(autoFormat(body, p, getCursor()));
                        }
                        case METHOD_DECLARATION_PARAMETERS: {
                            List<Statement> parameters = substitutions.unsubstitute(templateParser.parseParameters(substitutedTemplate));

                            // Update the J.MethodDeclaration's type information to reflect its new parameter list
                            JavaType.Method type = method.getType();
                            if(type != null) {
                                List<String> paramNames = new ArrayList<>();
                                List<JavaType> paramTypes = new ArrayList<>();
                                for(Statement parameter : parameters) {
                                    if(!(parameter instanceof J.VariableDeclarations)) {
                                        throw new IllegalArgumentException(
                                                "Only variable declarations may be part of a method declaration's parameter " +
                                                        "list:" + parameter.print());
                                    }
                                    J.VariableDeclarations decl = (J.VariableDeclarations) parameter;
                                    if(decl.getVariables().size() != 1) {
                                        throw new IllegalArgumentException(
                                                "Multi-variable declarations may not be used in a method declaration's " +
                                                        "parameter list: " + parameter.print());
                                    }
                                    J.VariableDeclarations.NamedVariable namedVariable = decl.getVariables().get(0);
                                    paramNames.add(namedVariable.getSimpleName());
                                    // Make a best-effort attempt to update the type information
                                    if(namedVariable.getType() == null && decl.getTypeExpression() instanceof J.Identifier) {
                                        // null if the type of the argument is a generic type parameter
                                        // Try to find an appropriate type from the method itself
                                        J.Identifier declTypeIdent = (J.Identifier) decl.getTypeExpression();
                                        String typeParameterName = declTypeIdent.getSimpleName();
                                        for(J.TypeParameter typeParameter : method.getTypeParameters()) {
                                            J.Identifier typeParamIdent = (J.Identifier) typeParameter.getName();
                                            if(typeParamIdent.getSimpleName().equals(typeParameterName)) {
                                                List<TypeTree> bounds = typeParameter.getBounds();
                                                JavaType.FullyQualified bound;
                                                if(bounds.size() == 0) {
                                                    bound = JavaType.Class.OBJECT;
                                                } else {
                                                    bound = (JavaType.FullyQualified) bounds.get(0);
                                                }
                                                JavaType.GenericTypeVariable genericType = new JavaType.GenericTypeVariable(
                                                        typeParamIdent.getSimpleName(),
                                                        bound);

                                                paramTypes.add(genericType);
                                            }
                                        }
                                    } else {
                                        paramTypes.add(namedVariable.getType());
                                    }
                                }
                                type = JavaType.Method.build(type.getFlags(), type.getDeclaringType(), type.getName(),
                                        type.getGenericSignature().withParamTypes(paramTypes),
                                        type.getResolvedSignature().withParamTypes(paramTypes),
                                        paramNames, type.getThrownExceptions());
                            }

                            return method.withParameters(parameters)
                                    .withType(type);
                        }
                        case THROWS: {
                            J.MethodDeclaration m = method.withThrows(substitutions.unsubstitute(templateParser.parseThrows(substitutedTemplate)));

                            // Update method type information to reflect the new checked exceptions
                            JavaType.Method type = m.getType();
                            if(type != null) {
                                List<JavaType.FullyQualified> newThrows = new ArrayList<>();
                                for(NameTree t : m.getThrows()) {
                                    J.Identifier exceptionIdent = (J.Identifier) t;
                                    newThrows.add((JavaType.FullyQualified) exceptionIdent.getType());
                                }
                                type = JavaType.Method.build(type.getFlags(), type.getDeclaringType(), type.getName(),
                                        type.getGenericSignature(), type.getResolvedSignature(), type.getParamNames(), newThrows);
                            }

                            //noinspection ConstantConditions
                            m = m.getPadding().withThrows(m.getPadding().getThrows().withBefore(Space.format(" ")))
                                    .withType(type);
                            return m;
                        }
                        case TYPE_PARAMETERS: {
                            List<J.TypeParameter> typeParameters = substitutions.unsubstitute(templateParser.parseTypeParameters(substitutedTemplate));
                            J.MethodDeclaration m = method.withTypeParameters(typeParameters);
                            return autoFormat(m, typeParameters.get(typeParameters.size() - 1), p,
                                    getCursor().getParentOrThrow());
                        }
                    }
                }
                return super.visitMethodDeclaration(method, p);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                if (loc.equals(METHOD_INVOCATION_ARGUMENTS) && method.isScope(insertionPoint)) {
                    List<Expression> args = substitutions.unsubstitute(templateParser.parseMethodArguments(getCursor(), substitutedTemplate, loc));
                    J.MethodInvocation m = method.withArguments(args);
                    m = autoFormat(m, 0, getCursor().getParentOrThrow());
                    return m;
                }
                if(loc.equals(STATEMENT_PREFIX) && method.isScope(insertionPoint) && mode.equals(JavaCoordinates.Mode.REPLACEMENT)) {
                    J gen = substitutions.unsubstitute(templateParser.parseExpression(substitutedTemplate));
                    return autoFormat(gen.withPrefix(method.getPrefix()), integer, getCursor().getParentOrThrow());
                }
                return super.visitMethodInvocation(method, integer);
            }

            @Override
            public J visitPackage(J.Package pkg, Integer integer) {
                if (loc.equals(PACKAGE_PREFIX) && pkg.isScope(insertionPoint)) {
                    return pkg.withExpression(substitutions.unsubstitute(templateParser.parsePackage(substitutedTemplate)));
                }
                return super.visitPackage(pkg, integer);
            }

            @Override
            public J visitStatement(Statement statement, Integer p) {
                if (loc.equals(STATEMENT_PREFIX) && statement.isScope(insertionPoint)) {
                    if (mode.equals(JavaCoordinates.Mode.REPLACEMENT)) {
                        List<Statement> gen = substitutions.unsubstitute(templateParser.parseBlockStatements(getCursor(), substitutedTemplate, loc));
                        if (gen.size() != 1) {
                            throw new IllegalArgumentException("Expected a template that would generate exactly one " +
                                    "statement to replace one statement, but generated " + gen.size());
                        }
                        return autoFormat(gen.get(0).withPrefix(statement.getPrefix()), p, getCursor().getParentOrThrow());
                    }
                    throw new IllegalArgumentException("Cannot insert a new statement before an existing statement and return both to a visit method that returns one statement.");
                }
                return super.visitStatement(statement, p);
            }

            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer p) {
                if (multiVariable.isScope(insertionPoint)) {
                    if (loc == ANNOTATIONS) {
                        J.VariableDeclarations v = multiVariable.withLeadingAnnotations(substitutions.unsubstitute(templateParser.parseAnnotations(getCursor(), substitutedTemplate)));
                        if (v.getTypeExpression() instanceof J.AnnotatedType) {
                            v = v.withTypeExpression(((J.AnnotatedType) v.getTypeExpression()).getTypeExpression());
                        }
                        v = v.withModifiers(ListUtils.map(v.getModifiers(), m -> m.withAnnotations(emptyList())));
                        return autoFormat(v, v.getLeadingAnnotations().get(v.getLeadingAnnotations().size() - 1), p,
                                getCursor().getParentOrThrow());
                    }
                }
                return super.visitVariableDeclarations(multiVariable, p);
            }
        }.visit(changing, 0, parentCursor);
    }

    public static Builder builder(Supplier<Cursor> parentScope, String code) {
        return new Builder(parentScope, code);
    }

    public static class Builder {
        private final Supplier<Cursor> parentScope;
        private final String code;
        private final Set<String> imports = new HashSet<>();

        private Supplier<JavaParser> javaParser = () -> JavaParser.fromJavaVersion().build();

        private Consumer<String> onAfterVariableSubstitution = s -> {
        };
        private Consumer<String> onBeforeParseTemplate = s -> {
        };

        Builder(Supplier<Cursor> parentScope, String code) {
            this.parentScope = parentScope;
            this.code = code.trim();
        }

        public Builder imports(String... fullyQualifiedTypeNames) {
            for (String typeName : fullyQualifiedTypeNames) {
                if (typeName.startsWith("import ") || typeName.startsWith("static ")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include an \"import \" or \"static \" prefix");
                } else if (typeName.endsWith(";") || typeName.endsWith("\n")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include a suffixed terminator");
                }
                this.imports.add("import " + typeName + ";\n");
            }
            return this;
        }

        public Builder staticImports(String... fullyQualifiedMemberTypeNames) {
            for (String typeName : fullyQualifiedMemberTypeNames) {
                if (typeName.startsWith("import ") || typeName.startsWith("static ")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include an \"import \" or \"static \" prefix");
                } else if (typeName.endsWith(";") || typeName.endsWith("\n")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include a suffixed terminator");
                }
                this.imports.add("import static " + typeName + ";\n");
            }
            return this;
        }

        public Builder javaParser(Supplier<JavaParser> javaParser) {
            this.javaParser = javaParser;
            return this;
        }

        public Builder doAfterVariableSubstitution(Consumer<String> afterVariableSubstitution) {
            this.onAfterVariableSubstitution = afterVariableSubstitution;
            return this;
        }

        public Builder doBeforeParseTemplate(Consumer<String> beforeParseTemplate) {
            this.onBeforeParseTemplate = beforeParseTemplate;
            return this;
        }

        public JavaTemplate build() {
            return new JavaTemplate(parentScope, javaParser, code, imports,
                    onAfterVariableSubstitution, onBeforeParseTemplate);
        }
    }
}
