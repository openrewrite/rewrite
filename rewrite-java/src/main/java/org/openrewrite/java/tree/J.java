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
package org.openrewrite.java.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.internal.*;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.java.tree.Coordinates.ClassDeclCoordinates;
import org.openrewrite.java.tree.Coordinates.MethodDeclCoordinates;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("unused")
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface J extends Serializable, Tree {
    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return v instanceof JavaVisitor ?
                (R) acceptJava((JavaVisitor<P>) v, p) :
                v.defaultValue(null, p);
    }

    @Nullable
    default <P> J acceptJava(JavaVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    default <P> String print(TreePrinter<P> printer, P p) {
        return new JavaPrinter<>(printer).print(this, p);
    }

    @Override
    default <P> String print(P p) {
        return print(TreePrinter.identity(), p);
    }

    <J2 extends J> J2 withPrefix(Space space);

    Space getPrefix();

    default List<Comment> getComments() {
        return getPrefix().getComments();
    }

    default <J2 extends J> J2 withComments(List<Comment> comments) {
        return withPrefix(getPrefix().withComments(comments));
    }

    /**
     * Find all subtrees marked with a particular marker rooted at this tree.
     *
     * @param markerType The marker type to look for
     * @param <J2>       The expected supertype common to all subtrees that could be found.
     * @return The set of matching subtrees.
     */
    default <J2 extends J> Set<J2> findMarkedWith(Class<? extends Marker> markerType) {
        Set<J2> trees = new HashSet<>();
        new JavaListMarkersVisitor<J2>(markerType).visit(this, trees);
        return trees;
    }

    default Coordinates coordinates() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @SuppressWarnings("unchecked")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class AnnotatedType implements J, Expression, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        List<J.Annotation> annotations;

        @With
        TypeTree typeExpr;

        @Override
        public JavaType getType() {
            return typeExpr.getType();
        }

        @Override
        public AnnotatedType withType(@Nullable JavaType type) {
            return withTypeExpr(typeExpr.withType(type));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitAnnotatedType(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Annotation implements J, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        NameTree annotationType;

        @Nullable
        JContainer<Expression> args;

        @Nullable
        public List<Expression> getArgs() {
            return args == null ? null : args.getElems();
        }

        public Annotation withArgs(@Nullable List<Expression> args) {
            return getPadding().withArgs(JContainer.withElems(this.args, args));
        }

        @Override
        public JavaType getType() {
            return annotationType.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Annotation withType(@Nullable JavaType type) {
            return withAnnotationType(annotationType.withType(type));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitAnnotation(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Annotation t;

            @Nullable
            public JContainer<Expression> getArgs() {
                return t.args;
            }

            public Annotation withArgs(@Nullable JContainer<Expression> args) {
                return t.args == args ? t : new Annotation(t.id, t.prefix, t.markers, t.annotationType, args);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ArrayAccess implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression indexed;

        @With
        ArrayDimension dimension;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitArrayAccess(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ArrayType implements J, TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        TypeTree elementType;

        @With
        List<JRightPadded<Space>> dimensions;

        @Override
        public JavaType getType() {
            return elementType.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ArrayType withType(@Nullable JavaType type) {
            return type == getType() ? this : withElementType(elementType.withType(type));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitArrayType(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Assert implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression condition;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitAssert(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Assign implements J, Statement, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Expression variable;

        JLeftPadded<Expression> assignment;

        public Expression getAssignment() {
            return assignment.getElem();
        }

        public Assign withAssignment(Expression assignment) {
            return getPadding().withAssignment(this.assignment.withElem(assignment));
        }

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitAssign(this, p);
        }

        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Assign t;

            public JLeftPadded<Expression> getAssignment() {
                return t.assignment;
            }

            public Assign withAssignment(JLeftPadded<Expression> assignment) {
                return t.assignment == assignment ? t : new Assign(t.id, t.prefix, t.markers, t.variable, assignment, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class AssignOp implements J, Statement, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Expression variable;

        JLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElem();
        }

        public AssignOp withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElem(operator));
        }

        @With
        @Getter
        Expression assignment;

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitAssignOp(this, p);
        }

        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }

        public enum Type {
            Addition,
            Subtraction,
            Multiplication,
            Division,
            Modulo,
            BitAnd,
            BitOr,
            BitXor,
            LeftShift,
            RightShift,
            UnsignedRightShift
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final AssignOp t;

            public JLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public AssignOp withOperator(JLeftPadded<Type> operator) {
                return t.operator == operator ? t : new AssignOp(t.id, t.prefix, t.markers, t.variable, operator, t.assignment, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Binary implements J, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression left;

        JLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElem();
        }

        public Binary withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElem(operator));
        }

        @With
        Expression right;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitBinary(this, p);
        }

        @Override
        public List<Tree> getSideEffects() {
            List<Tree> sideEffects = new ArrayList<>(2);
            sideEffects.addAll(left.getSideEffects());
            sideEffects.addAll(right.getSideEffects());
            return sideEffects;
        }

        public enum Type {
            Addition,
            Subtraction,
            Multiplication,
            Division,
            Modulo,
            LessThan,
            GreaterThan,
            LessThanOrEqual,
            GreaterThanOrEqual,
            Equal,
            NotEqual,
            BitAnd,
            BitOr,
            BitXor,
            LeftShift,
            RightShift,
            UnsignedRightShift,
            Or,
            And
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Binary t;

            public JLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public Binary withOperator(JLeftPadded<Type> operator) {
                return t.operator == operator ? t : new Binary(t.id, t.prefix, t.markers, t.left, operator, t.right, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Block implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        JRightPadded<Boolean> statik;

        public boolean isStatic() {
            return statik.getElem();
        }

        public Block withStatic(boolean statik) {
            return getPadding().withStatic(this.statik.withElem(statik));
        }

        List<JRightPadded<Statement>> statements;

        public List<Statement> getStatements() {
            return JRightPadded.getElems(statements);
        }

        public Block withStatements(List<Statement> statements) {
            return getPadding().withStatements(JRightPadded.withElems(this.statements, statements));
        }

        @Getter
        @With
        Space end;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitBlock(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Block t;

            public JRightPadded<Boolean> getStatic() {
                return t.statik;
            }

            public Block withStatic(JRightPadded<Boolean> statik) {
                return t.statik == statik ? t : new Block(t.id, t.prefix, t.markers, statik, t.statements, t.end);
            }

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public Block withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new Block(t.id, t.prefix, t.markers, t.statik, statements, t.end);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Break implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        Ident label;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitBreak(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Case implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Expression pattern;

        JContainer<Statement> statements;

        public List<Statement> getStatements() {
            return statements.getElems();
        }

        public Case withStatements(List<Statement> statements) {
            return getPadding().withStatements(this.statements.getPadding().withElems(JRightPadded.withElems(
                    this.statements.getPadding().getElems(), statements)));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitCase(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Case t;

            public JContainer<Statement> getStatements() {
                return t.statements;
            }

            public Case withStatements(JContainer<Statement> statements) {
                return t.statements == statements ? t : new Case(t.id, t.prefix, t.markers, t.pattern, statements);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ClassDecl implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        List<Annotation> annotations;

        @With
        @Getter
        List<Modifier> modifiers;

        JLeftPadded<Kind> kind;

        public Kind getKind() {
            return kind.getElem();
        }

        public ClassDecl withKind(Kind kind) {
            return getPadding().withKind(this.kind.withElem(kind));
        }

        @With
        @Getter
        Ident name;

        @Nullable
        JContainer<TypeParameter> typeParameters;

        @Nullable
        public List<TypeParameter> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElems();
        }

        public ClassDecl withTypeParameters(@Nullable List<TypeParameter> typeParameters) {
            return getPadding().withTypeParameters(JContainer.withElems(this.typeParameters, typeParameters));
        }

        @Nullable
        JLeftPadded<TypeTree> extendings;

        @Nullable
        public TypeTree getExtends() {
            return extendings == null ? null : extendings.getElem();
        }

        public ClassDecl withExtends(@Nullable TypeTree extendings) {
            return getPadding().withExtends(JLeftPadded.withElem(this.extendings, extendings));
        }

        @Nullable
        JContainer<TypeTree> implementings;

        @Nullable
        public List<TypeTree> getImplements() {
            return implementings == null ? null : implementings.getElems();
        }

        public ClassDecl withImplements(@Nullable List<TypeTree> implementings) {
            return getPadding().withImplements(JContainer.withElems(this.implementings, implementings));
        }

        @With
        @Getter
        Block body;

        @With
        @Getter
        @Nullable
        JavaType.Class type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitClassDecl(this, p);
        }

        public String getSimpleName() {
            return name.getSimpleName();
        }

        public enum Kind {
            Class,
            Enum,
            Interface,
            Annotation
        }

        public boolean hasModifier(String modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        @Override
        public String toString() {
            return "ClassDecl(" + ClassDeclToString.toString(this) + ")";
        }

        public ClassDeclCoordinates coordinates() {
            return new ClassDeclCoordinates(this);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ClassDecl t;

            public JLeftPadded<Kind> getKind() {
                return t.kind;
            }

            public ClassDecl withKind(JLeftPadded<Kind> kind) {
                return t.kind == kind ? t : new ClassDecl(t.id, t.prefix, t.markers, t.annotations, t.modifiers, kind, t.name, t.typeParameters, t.extendings, t.implementings, t.body, t.type);
            }

            @Nullable
            public JLeftPadded<TypeTree> getExtends() {
                return t.extendings;
            }

            public ClassDecl withExtends(@Nullable JLeftPadded<TypeTree> extendings) {
                return t.extendings == extendings ? t : new ClassDecl(t.id, t.prefix, t.markers, t.annotations, t.modifiers, t.kind, t.name, t.typeParameters, extendings, t.implementings, t.body, t.type);
            }

            @Nullable
            public JContainer<TypeTree> getImplements() {
                return t.implementings;
            }

            public ClassDecl withImplements(@Nullable JContainer<TypeTree> implementings) {
                return t.implementings == implementings ? t : new ClassDecl(t.id, t.prefix, t.markers, t.annotations, t.modifiers, t.kind, t.name, t.typeParameters, t.extendings, implementings, t.body, t.type);
            }

            @Nullable
            public JContainer<TypeParameter> getTypeParameters() {
                return t.typeParameters;
            }

            public ClassDecl withTypeParameters(@Nullable JContainer<TypeParameter> typeParameters) {
                return t.typeParameters == typeParameters ? t : new ClassDecl(t.id, t.prefix, t.markers, t.annotations, t.modifiers, t.kind, t.name, typeParameters, t.extendings, t.implementings, t.body, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CompilationUnit implements J, SourceFile {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Path sourcePath;

        @Nullable
        JRightPadded<Package> packageDecl;

        @Nullable
        public Package getPackageDecl() {
            return packageDecl == null ? null : packageDecl.getElem();
        }

        public CompilationUnit withPackageDecl(Package packageDecl) {
            return getPadding().withPackageDecl(JRightPadded.withElem(this.packageDecl, packageDecl));
        }

        List<JRightPadded<Import>> imports;

        public List<Import> getImports() {
            return JRightPadded.getElems(imports);
        }

        public CompilationUnit withImports(List<Import> imports) {
            return getPadding().withImports(JRightPadded.withElems(this.imports, imports));
        }

        @With
        @Getter
        List<ClassDecl> classes;

        @With
        @Getter
        Space eof;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        public Set<NameTree> findType(String clazz) {
            return FindTypes.find(this, clazz);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final CompilationUnit t;

            @Nullable
            public JRightPadded<Package> getPackageDecl() {
                return t.packageDecl;
            }

            public CompilationUnit withPackageDecl(@Nullable JRightPadded<Package> packageDecl) {
                return t.packageDecl == packageDecl ? t : new CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, packageDecl, t.imports, t.classes, t.eof);
            }

            public List<JRightPadded<Import>> getImports() {
                return t.imports;
            }

            public CompilationUnit withImports(List<JRightPadded<Import>> imports) {
                return t.imports == imports ? t : new CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, t.packageDecl, imports, t.classes, t.eof);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Continue implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        Ident label;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitContinue(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class DoWhileLoop implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        JRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElem();
        }

        public DoWhileLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElem(body));
        }

        JLeftPadded<ControlParentheses<Expression>> whileCondition;

        public ControlParentheses<Expression> getWhileCondition() {
            return whileCondition.getElem();
        }

        public DoWhileLoop withWhileCondition(ControlParentheses<Expression> whileCondition) {
            return getPadding().withWhileCondition(this.whileCondition.withElem(whileCondition));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitDoWhileLoop(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final DoWhileLoop t;

            public JRightPadded<Statement> getBody() {
                return t.body;
            }

            public DoWhileLoop withBody(JRightPadded<Statement> body) {
                return t.body == body ? t : new DoWhileLoop(t.id, t.prefix, t.markers, body, t.whileCondition);
            }

            public JLeftPadded<ControlParentheses<Expression>> getWhileCondition() {
                return t.whileCondition;
            }

            public DoWhileLoop withWhileCondition(JLeftPadded<ControlParentheses<Expression>> whileCondition) {
                return t.whileCondition == whileCondition ? t : new DoWhileLoop(t.id, t.prefix, t.markers, t.body, whileCondition);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Empty implements J, Statement, Expression, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @Override
        public JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Empty withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitEmpty(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class EnumValue implements J {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Ident name;

        @With
        @Nullable
        NewClass initializer;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitEnumValue(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class EnumValueSet implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        List<JRightPadded<EnumValue>> enums;

        public List<EnumValue> getEnums() {
            return JRightPadded.getElems(enums);
        }

        public EnumValueSet withEnums(List<EnumValue> enums) {
            return getPadding().withEnums(JRightPadded.withElems(this.enums, enums));
        }

        @With
        @Getter
        boolean terminatedWithSemicolon;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitEnumValueSet(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final EnumValueSet t;

            public List<JRightPadded<EnumValue>> getEnums() {
                return t.enums;
            }

            public EnumValueSet withEnums(List<JRightPadded<EnumValue>> enums) {
                return t.enums == enums ? t : new EnumValueSet(t.id, t.prefix, t.markers, enums, t.terminatedWithSemicolon);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class FieldAccess implements J, TypeTree, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Expression target;

        JLeftPadded<Ident> name;

        public Ident getName() {
            return name.getElem();
        }

        public FieldAccess withName(Ident name) {
            return getPadding().withName(this.name.withElem(name));
        }

        @With
        @Getter
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitFieldAccess(this, p);
        }

        public String getSimpleName() {
            return name.getElem().getSimpleName();
        }

        @Override
        public List<Tree> getSideEffects() {
            return target.getSideEffects();
        }

        /**
         * Make debugging a bit easier
         */
        public String toString() {
            return "FieldAccess(" + printTrimmed() + ")";
        }

        /**
         * @return For expressions like {@code String.class}, this casts target expression to a {@link NameTree}.
         * If the field access is not a reference to a class type, returns null.
         */
        @Nullable
        public NameTree asClassReference() {
            if (target instanceof NameTree) {
                String fqn = null;
                if (type instanceof JavaType.Class) {
                    fqn = ((JavaType.Class) type).getFullyQualifiedName();
                } else if (type instanceof JavaType.ShallowClass) {
                    fqn = ((JavaType.ShallowClass) type).getFullyQualifiedName();
                }

                return "java.lang.Class".equals(fqn) ? (NameTree) target : null;
            }
            return null;
        }

        public boolean isFullyQualifiedClassReference(String className) {
            return isFullyQualifiedClassReference(this, className);
        }

        /**
         * Evaluate whether the specified MethodMatcher and this FieldAccess are describing the same type or not.
         * Known limitation/bug: MethodMatchers can have patterns/wildcards like "com.*.Bar" instead of something
         * concrete like "com.foo.Bar". This limitation is not desirable or intentional and should be fixed.
         * If a methodMatcher is passed that includes wildcards the result will always be "false"
         *
         * @param methodMatcher a methodMatcher whose internal pattern is fully concrete (no wildcards)
         */
        public boolean isFullyQualifiedClassReference(MethodMatcher methodMatcher) {
            String hopefullyFullyQualifiedMethod = methodMatcher.getTargetTypePattern().pattern() + "." + methodMatcher.getMethodNamePattern().pattern();
            return isFullyQualifiedClassReference(this, hopefullyFullyQualifiedMethod);
        }

        private boolean isFullyQualifiedClassReference(J.FieldAccess fieldAccess, String className) {
            if (!className.contains(".")) {
                return false;
            }
            if (!fieldAccess.getName().getSimpleName().equals(className.substring(className.lastIndexOf('.') + 1))) {
                return false;
            }
            if (fieldAccess.getTarget() instanceof J.FieldAccess) {
                return isFullyQualifiedClassReference((J.FieldAccess) fieldAccess.getTarget(), className.substring(0, className.lastIndexOf('.')));
            }
            if (fieldAccess.getTarget() instanceof J.Ident) {
                return ((J.Ident) fieldAccess.getTarget()).getSimpleName().equals(className.substring(0, className.lastIndexOf('.')));
            }
            return false;
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final FieldAccess t;

            public JLeftPadded<Ident> getName() {
                return t.name;
            }

            public FieldAccess withName(JLeftPadded<Ident> name) {
                return t.name == name ? t : new FieldAccess(t.id, t.prefix, t.markers, t.target, name, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ForEachLoop implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Control control;

        JRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElem();
        }

        public ForEachLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElem(body));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitForEachLoop(this, p);
        }

        @ToString
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Control implements J {
            @Nullable
            @NonFinal
            transient Padding padding;

            @EqualsAndHashCode.Include
            @Getter
            UUID id;

            @With
            @Getter
            Space prefix;

            @With
            @Getter
            Markers markers;

            JRightPadded<VariableDecls> variable;

            public VariableDecls getVariable() {
                return variable.getElem();
            }

            public Control withVariable(VariableDecls variable) {
                return getPadding().withVariable(this.variable.withElem(variable));
            }

            JRightPadded<Expression> iterable;

            public Expression getIterable() {
                return iterable.getElem();
            }

            public Control withIterable(Expression iterable) {
                return getPadding().withIterable(this.iterable.withElem(iterable));
            }

            @Override
            public <P> J acceptJava(JavaVisitor<P> v, P p) {
                return v.visitForEachControl(this, p);
            }

            public Padding getPadding() {
                if (padding == null || padding.t != this) {
                    this.padding = new Padding(this);
                }
                return padding;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final Control t;

                public JRightPadded<VariableDecls> getVariable() {
                    return t.variable;
                }

                public Control withVariable(JRightPadded<VariableDecls> variable) {
                    return t.variable == variable ? t : new Control(t.id, t.prefix, t.markers, variable, t.iterable);
                }

                public JRightPadded<Expression> getIterable() {
                    return t.iterable;
                }

                public Control withIterable(JRightPadded<Expression> iterable) {
                    return t.iterable == iterable ? t : new Control(t.id, t.prefix, t.markers, t.variable, iterable);
                }
            }
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ForEachLoop t;

            public JRightPadded<Statement> getBody() {
                return t.body;
            }

            public ForEachLoop withBody(JRightPadded<Statement> body) {
                return t.body == body ? t : new ForEachLoop(t.id, t.prefix, t.markers, t.control, body);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ForLoop implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Control control;

        JRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElem();
        }

        public ForLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElem(body));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitForLoop(this, p);
        }

        @ToString
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Control implements J {
            @Nullable
            @NonFinal
            transient Padding padding;

            @EqualsAndHashCode.Include
            @Getter
            UUID id;

            @With
            @Getter
            Space prefix;

            @With
            @Getter
            Markers markers;

            JRightPadded<Statement> init;

            public Statement getInit() {
                return init.getElem();
            }

            public Control withInit(Statement init) {
                return getPadding().withInit(this.init.withElem(init));
            }

            JRightPadded<Expression> condition;

            public Expression getCondition() {
                return condition.getElem();
            }

            public Control withCondition(Expression condition) {
                return getPadding().withCondition(this.condition.withElem(condition));
            }

            List<JRightPadded<Statement>> update;

            public List<Statement> getUpdate() {
                return JRightPadded.getElems(update);
            }

            public Control withUpdate(List<Statement> update) {
                return getPadding().withUpdate(JRightPadded.withElems(this.update, update));
            }

            @Override
            public <P> J acceptJava(JavaVisitor<P> v, P p) {
                return v.visitForControl(this, p);
            }

            public Padding getPadding() {
                if (padding == null || padding.t != this) {
                    this.padding = new Padding(this);
                }
                return padding;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final Control t;

                public JRightPadded<Statement> getInit() {
                    return t.init;
                }

                public ForLoop.Control withInit(JRightPadded<Statement> init) {
                    return t.init == init ? t : new ForLoop.Control(t.id, t.prefix, t.markers, init, t.condition, t.update);
                }

                public JRightPadded<Expression> getCondition() {
                    return t.condition;
                }

                public ForLoop.Control withCondition(JRightPadded<Expression> condition) {
                    return t.condition == condition ? t : new ForLoop.Control(t.id, t.prefix, t.markers, t.init, condition, t.update);
                }

                public List<JRightPadded<Statement>> getUpdate() {
                    return t.update;
                }

                public ForLoop.Control withUpdate(List<JRightPadded<Statement>> update) {
                    return t.update == update ? t : new ForLoop.Control(t.id, t.prefix, t.markers, t.init, t.condition, update);
                }
            }
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ForLoop t;

            public JRightPadded<Statement> getBody() {
                return t.body;
            }

            public ForLoop withBody(JRightPadded<Statement> body) {
                return t.body == body ? t : new ForLoop(t.id, t.prefix, t.markers, t.control, body);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Getter
    final class Ident implements J, TypeTree, Expression {
        private static final Map<String, Map<JavaType, IdentFlyweight>> flyweights = new HashMap<>();

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        Markers markers;

        IdentFlyweight ident;

        private Ident(UUID id, IdentFlyweight ident, Space prefix, Markers markers) {
            this.id = id;
            this.ident = ident;
            this.prefix = prefix;
            this.markers = markers;
        }

        @Override
        public JavaType getType() {
            return ident.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Ident withType(@Nullable JavaType type) {
            if (type == getType()) {
                return this;
            }
            return build(id, prefix, markers, getSimpleName(), type);
        }

        public String getSimpleName() {
            return ident.getSimpleName();
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitIdentifier(this, p);
        }

        public Ident withName(String name) {
            if (name.equals(ident.getSimpleName())) {
                return this;
            }
            return build(id, prefix, markers, name, getType());
        }

        @SuppressWarnings("unchecked")
        public Ident withMarkers(Markers markers) {
            if (markers == this.markers) {
                return this;
            }
            return build(id, prefix, markers, ident.getSimpleName(), getType());
        }

        @SuppressWarnings("unchecked")
        public Ident withPrefix(Space prefix) {
            if (prefix == this.prefix) {
                return this;
            }
            return build(id, prefix, markers, ident.getSimpleName(), getType());
        }

        @JsonCreator
        public static Ident build(UUID id,
                                  Space prefix,
                                  Markers markers,
                                  String simpleName,
                                  @Nullable JavaType type) {
            synchronized (flyweights) {
                return new Ident(
                        id,
                        flyweights
                                .computeIfAbsent(simpleName, n -> new HashMap<>())
                                .computeIfAbsent(type, t -> new IdentFlyweight(simpleName, t)),
                        prefix,
                        markers
                );
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @Data
        public static final class IdentFlyweight implements Serializable {
            String simpleName;

            @Nullable
            JavaType type;
        }

        /**
         * Making debugging a bit easier
         */
        public String toString() {
            return "Ident(" + printTrimmed() + ")";
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class If implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        ControlParentheses<Expression> ifCondition;

        JRightPadded<Statement> thenPart;

        public Statement getThenPart() {
            return thenPart.getElem();
        }

        public If withThenPart(Statement thenPart) {
            return getPadding().withThenPart(this.thenPart.withElem(thenPart));
        }

        @With
        @Nullable
        @Getter
        Else elsePart;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitIf(this, p);
        }

        @ToString
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Else implements J {
            @Nullable
            @NonFinal
            transient Padding padding;

            @EqualsAndHashCode.Include
            @Getter
            UUID id;

            @With
            @Getter
            Space prefix;

            @With
            @Getter
            Markers markers;

            JRightPadded<Statement> body;

            public Statement getBody() {
                return body.getElem();
            }

            public Else withBody(Statement body) {
                return getPadding().withBody(this.body.withElem(body));
            }

            @Override
            public <P> J acceptJava(JavaVisitor<P> v, P p) {
                return v.visitElse(this, p);
            }

            public Padding getPadding() {
                if (padding == null || padding.t != this) {
                    this.padding = new Padding(this);
                }
                return padding;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final Else t;

                public JRightPadded<Statement> getBody() {
                    return t.body;
                }

                public Else withBody(JRightPadded<Statement> body) {
                    return t.body == body ? t : new Else(t.id, t.prefix, t.markers, body);
                }
            }
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final If t;

            public JRightPadded<Statement> getThenPart() {
                return t.thenPart;
            }

            public If withThenPart(JRightPadded<Statement> thenPart) {
                return t.thenPart == thenPart ? t : new If(t.id, t.prefix, t.markers, t.ifCondition, thenPart, t.elsePart);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Import implements J, Comparable<Import> {
        @Nullable
        @NonFinal
        transient Padding padding;

        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        @With
        JLeftPadded<Boolean> statik;

        @With
        @Getter
        FieldAccess qualid;

        public boolean isStatic() {
            return statik.getElem();
        }

        public Import withStatic(boolean statik) {
            return getPadding().withStatic(this.statik.withElem(statik));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitImport(this, p);
        }

        public boolean isFromType(String clazz) {
            if ("*".equals(qualid.getSimpleName())) {
                return qualid.target.printTrimmed().equals(Arrays.stream(clazz.split("\\."))
                        .filter(pkgOrNam -> Character.isLowerCase(pkgOrNam.charAt(0)))
                        .collect(Collectors.joining("."))
                );
            }
            return (isStatic() ? qualid.getTarget().printTrimmed() : qualid.printTrimmed()).equals(clazz);
        }

        public String getTypeName() {
            return isStatic() ? qualid.getTarget().printTrimmed() : qualid.printTrimmed();
        }

        /**
         * Retrieve just the package from the import.
         * e.g.:
         * import org.foo.A;            -> "org.foo"
         * import static org.foo.A.bar; -> "org.foo"
         * import org.foo.*;            -> "org.foo"
         */

        public String getPackageName() {
            JavaType.Class importType = TypeUtils.asClass(qualid.getType());
            if (importType != null) {
                return importType.getPackageName();
            }

            AtomicBoolean takeWhile = new AtomicBoolean(true);
            return stream(qualid.getTarget().printTrimmed().split("\\."))
                    .filter(pkg -> {
                        takeWhile.set(takeWhile.get() && !pkg.isEmpty() && Character.isLowerCase(pkg.charAt(0)));
                        return takeWhile.get();
                    })
                    .collect(joining("."));
        }

        @Override
        public int compareTo(Import o) {
            String p1 = this.getPackageName();
            String p2 = o.getPackageName();

            String[] p1s = p1.split("\\.");
            String[] p2s = p2.split("\\.");

            for (int i = 0; i < p1s.length; i++) {
                String s = p1s[i];
                if (p2s.length < i + 1) {
                    return 1;
                }
                if (!s.equals(p2s[i])) {
                    return s.compareTo(p2s[i]);
                }
            }

            return p1s.length < p2s.length ? -1 :
                    this.getQualid().getSimpleName().compareTo(o.getQualid().getSimpleName());
        }

        /**
         * Make debugging a bit easier
         */
        public String toString() {
            return "Import(" + ImportToString.toString(this) + ")";
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Import t;

            public JLeftPadded<Boolean> getStatic() {
                return t.statik;
            }

            public Import withStatic(JLeftPadded<Boolean> statik) {
                return t.statik == statik ? t : new Import(t.id, t.prefix, t.markers, statik, t.qualid);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class InstanceOf implements J, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        JRightPadded<Expression> expr;

        public Expression getExpr() {
            return expr.getElem();
        }

        public InstanceOf withExpr(Expression expr) {
            return getPadding().withExpr(this.expr.withElem(expr));
        }

        @With
        @Getter
        J clazz;

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitInstanceOf(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final InstanceOf t;

            public JRightPadded<Expression> getExpr() {
                return t.expr;
            }

            public InstanceOf withExpr(JRightPadded<Expression> expr) {
                return t.expr == expr ? t : new InstanceOf(t.id, t.prefix, t.markers, expr, t.clazz, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Label implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        /**
         * Right padded before the ':'
         */
        JRightPadded<Ident> label;

        public Ident getLabel() {
            return label.getElem();
        }

        public Label withLabel(Ident label) {
            return getPadding().withLabel(this.label.withElem(label));
        }

        @With
        @Getter
        Statement statement;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitLabel(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Label t;

            public JRightPadded<Ident> getLabel() {
                return t.label;
            }

            public Label withLabel(JRightPadded<Ident> label) {
                return t.label == label ? t : new Label(t.id, t.prefix, t.markers, label, t.statement);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Lambda implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Parameters parameters;

        @With
        Space arrow;

        @With
        J body;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitLambda(this, p);
        }

        @ToString
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Parameters implements J {
            @Nullable
            @NonFinal
            transient Padding padding;

            @EqualsAndHashCode.Include
            @Getter
            UUID id;

            @With
            @Getter
            Space prefix;

            @With
            @Getter
            Markers markers;

            @With
            @Getter
            boolean parenthesized;

            List<JRightPadded<J>> params;

            public List<J> getParams() {
                return JRightPadded.getElems(params);
            }

            public Parameters withParams(List<J> params) {
                return getPadding().withParams(JRightPadded.withElems(this.params, params));
            }

            public Padding getPadding() {
                if (padding == null || padding.t != this) {
                    this.padding = new Padding(this);
                }
                return padding;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final Parameters t;

                public List<JRightPadded<J>> getParams() {
                    return t.params;
                }

                public Parameters withParams(List<JRightPadded<J>> params) {
                    return t.params == params ? t : new Parameters(t.id, t.prefix, t.markers, t.parenthesized, params);
                }
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Literal implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        Object value;

        @With
        String valueSource;

        /**
         * Including String literals
         */
        JavaType.Primitive type;

        @SuppressWarnings("unchecked")
        @Override
        public Literal withType(@Nullable JavaType type) {
            if (type == this.type) {
                return this;
            }
            if (type instanceof JavaType.Primitive) {
                return new Literal(id, prefix, markers, value, valueSource, (JavaType.Primitive) type);
            }
            return this;
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }

        public <T> String transformValue(Function<T, Object> transform) {
            Matcher valueMatcher = Pattern.compile("(.*)" + Pattern.quote(value == null ? "null" : value.toString()) + "(.*)")
                    .matcher(printTrimmed().replace("\\", ""));
            if (valueMatcher.find()) {
                String prefix = valueMatcher.group(1);
                String suffix = valueMatcher.group(2);

                //noinspection unchecked
                return prefix + transform.apply((T) value) + suffix;
            }
            throw new IllegalStateException("Encountered a literal `" + this + "` that could not be transformed");
        }

        @Override
        public String toString() {
            return "Literal(" + LiteralToString.toString(this) + ")";
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MemberReference implements J, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Expression containing;

        @Nullable
        JContainer<Expression> typeParameters;

        @Nullable
        public List<Expression> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElems();
        }

        public MemberReference withTypeParameters(@Nullable List<Expression> typeParameters) {
            return getPadding().withTypeParameters(JContainer.withElems(this.typeParameters, typeParameters));
        }

        JLeftPadded<Ident> reference;

        public Ident getReference() {
            return reference.getElem();
        }

        public MemberReference withReference(Ident reference) {
            return getPadding().withReference(this.reference.withElem(reference));
        }

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitMemberReference(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final MemberReference t;

            @Nullable
            public JContainer<Expression> getTypeParameters() {
                return t.typeParameters;
            }

            public MemberReference withTypeParameters(@Nullable JContainer<Expression> typeParameters) {
                return t.typeParameters == typeParameters ? t : new MemberReference(t.id, t.prefix, t.markers, t.containing, typeParameters, t.reference, t.type);
            }

            public JLeftPadded<Ident> getReference() {
                return t.reference;
            }

            public MemberReference withReference(JLeftPadded<Ident> reference) {
                return t.reference == reference ? t : new MemberReference(t.id, t.prefix, t.markers, t.containing, t.typeParameters, reference, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MethodDecl implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        List<Annotation> annotations;

        @With
        @Getter
        List<Modifier> modifiers;

        @Nullable
        JContainer<TypeParameter> typeParameters;

        @Nullable
        public List<TypeParameter> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElems();
        }

        public MethodDecl withTypeParameters(@Nullable List<TypeParameter> typeParameters) {
            return getPadding().withTypeParameters(JContainer.withElems(this.typeParameters, typeParameters));
        }

        /**
         * Null for constructor declarations.
         */
        @With
        @Getter
        @Nullable
        TypeTree returnTypeExpr;

        @With
        @Getter
        Ident name;

        JContainer<Statement> params;

        public List<Statement> getParams() {
            return params.getElems();
        }

        public MethodDecl withParams(List<Statement> params) {
            return getPadding().withParams(JContainer.withElems(this.params, params));
        }

        @Nullable
        JContainer<NameTree> throwz;

        @Nullable
        public List<NameTree> getThrows() {
            return throwz == null ? null : throwz.getElems();
        }

        public MethodDecl withThrows(@Nullable List<NameTree> throwz) {
            return getPadding().withThrows(JContainer.withElems(this.throwz, throwz));
        }

        /**
         * Null for abstract method declarations and interface method declarations.
         */
        @With
        @Getter
        @Nullable
        Block body;

        /**
         * For default values on definitions of annotation parameters.
         */
        @Nullable
        JLeftPadded<Expression> defaultValue;

        @Nullable
        public Expression getDefaultValue() {
            return defaultValue == null ? null : defaultValue.getElem();
        }

        public MethodDecl withDefaultValue(@Nullable Expression defaultValue) {
            return getPadding().withDefaultValue(JLeftPadded.withElem(this.defaultValue, defaultValue));
        }

        @With
        @Getter
        @Nullable
        JavaType.Method type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitMethod(this, p);
        }

        public boolean isAbstract() {
            return body == null;
        }

        public boolean isConstructor() {
            return getReturnTypeExpr() == null;
        }

        public String getSimpleName() {
            return name.getSimpleName();
        }

        public boolean hasModifier(String modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        @Override
        public String toString() {
            return "MethodDecl(" + MethodDeclToString.toString(this) + ")";
        }

        @Override
        public MethodDeclCoordinates coordinates() {
            return new MethodDeclCoordinates(this);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final MethodDecl t;

            @Nullable
            public JContainer<TypeParameter> getTypeParameters() {
                return t.typeParameters;
            }

            public MethodDecl withTypeParameters(@Nullable JContainer<TypeParameter> typeParameters) {
                return t.typeParameters == typeParameters ? t : new MethodDecl(t.id, t.prefix, t.markers, t.annotations, t.modifiers, typeParameters, t.returnTypeExpr, t.name, t.params, t.throwz, t.body, t.defaultValue, t.type);
            }

            public JContainer<Statement> getParams() {
                return t.params;
            }

            public MethodDecl withParams(JContainer<Statement> params) {
                return t.params == params ? t : new MethodDecl(t.id, t.prefix, t.markers, t.annotations, t.modifiers, t.typeParameters, t.returnTypeExpr, t.name, params, t.throwz, t.body, t.defaultValue, t.type);
            }

            @Nullable
            public JContainer<NameTree> getThrows() {
                return t.throwz;
            }

            public MethodDecl withThrows(@Nullable JContainer<NameTree> throwz) {
                return t.throwz == throwz ? t : new MethodDecl(t.id, t.prefix, t.markers, t.annotations, t.modifiers, t.typeParameters, t.returnTypeExpr, t.name, t.params, throwz, t.body, t.defaultValue, t.type);
            }

            @Nullable
            public JLeftPadded<Expression> getDefaultValue() {
                return t.defaultValue;
            }

            public MethodDecl withDefaultValue(@Nullable JLeftPadded<Expression> defaultValue) {
                return t.defaultValue == defaultValue ? t : new MethodDecl(t.id, t.prefix, t.markers, t.annotations, t.modifiers, t.typeParameters, t.returnTypeExpr, t.name, t.params, t.throwz, t.body, defaultValue, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MethodInvocation implements J, Statement, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        /**
         * Right padded before the '.'
         */
        @Nullable
        JRightPadded<Expression> select;

        @Nullable
        public Expression getSelect() {
            return select == null ? null : select.getElem();
        }

        public MethodInvocation withSelect(@Nullable Expression select) {
            return getPadding().withSelect(JRightPadded.withElem(this.select, select));
        }

        @Nullable
        JContainer<Expression> typeParameters;

        @Nullable
        public List<Expression> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElems();
        }

        @With
        @Getter
        Ident name;

        JContainer<Expression> args;

        public List<Expression> getArgs() {
            return args.getElems();
        }

        public MethodInvocation withArgs(List<Expression> args) {
            return getPadding().withArgs(JContainer.withElems(this.args, args));
        }

        @Nullable
        @Getter
        JavaType.Method type;

        @SuppressWarnings("unchecked")
        @Override
        public MethodInvocation withType(@Nullable JavaType type) {
            if (type == this.type) {
                return this;
            }
            if (type instanceof JavaType.Method) {
                return new MethodInvocation(id, prefix, markers, select, typeParameters, name, args, (JavaType.Method) type);
            }
            return this;
        }

        public MethodInvocation withDeclaringType(JavaType.FullyQualified type) {
            if (this.type == null) {
                return this;
            } else {
                return withType(this.type.withDeclaringType(type));
            }
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitMethodInvocation(this, p);
        }

        @Nullable
        public JavaType getReturnType() {
            return type == null ? null : type.getResolvedSignature() == null ? null :
                    type.getResolvedSignature().getReturnType();
        }

        public String getSimpleName() {
            return name.getSimpleName();
        }

        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }

        @Override
        public String toString() {
            return "MethodInvocation(" + MethodInvocationToString.toString(this) + ")";
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final MethodInvocation t;

            @Nullable
            public JRightPadded<Expression> getSelect() {
                return t.select;
            }

            public MethodInvocation withSelect(@Nullable JRightPadded<Expression> select) {
                return t.select == select ? t : new MethodInvocation(t.id, t.prefix, t.markers, select, t.typeParameters, t.name, t.args, t.type);
            }

            @Nullable
            public JContainer<Expression> getTypeParameters() {
                return t.typeParameters;
            }

            public MethodInvocation withTypeParameters(@Nullable JContainer<Expression> typeParameters) {
                return t.typeParameters == typeParameters ? t : new MethodInvocation(t.id, t.prefix, t.markers, t.select, typeParameters, t.name, t.args, t.type);
            }

            public JContainer<Expression> getArgs() {
                return t.args;
            }

            public MethodInvocation withArgs(JContainer<Expression> args) {
                return t.args == args ? t : new MethodInvocation(t.id, t.prefix, t.markers, t.select, t.typeParameters, t.name, args, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Modifier implements J {
        public static boolean hasModifier(Collection<Modifier> modifiers, String modifier) {
            return modifiers.stream().anyMatch(m -> m.getClass().getSimpleName()
                    .toLowerCase().equals(modifier));
        }

        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Type type;

        public enum Type {
            Default,
            Public,
            Protected,
            Private,
            Abstract,
            Static,
            Final,
            Native,
            Strictfp,
            Synchronized,
            Transient,
            Volatile,
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MultiCatch implements J, TypeTree {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        List<JRightPadded<NameTree>> alternatives;

        public List<NameTree> getAlternatives() {
            return JRightPadded.getElems(alternatives);
        }

        public MultiCatch withAlternatives(List<NameTree> alternatives) {
            return getPadding().withAlternatives(JRightPadded.withElems(this.alternatives, alternatives));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitMultiCatch(this, p);
        }

        @SuppressWarnings("unchecked")
        @Override
        public MultiCatch withType(@Nullable JavaType type) {
            // cannot overwrite type directly, perform this operation on each alternative separately
            return this;
        }

        @Override
        public JavaType getType() {
            return new JavaType.MultiCatch(alternatives.stream()
                    .filter(Objects::nonNull)
                    .map(alt -> alt.getElem().getType())
                    .collect(toList()));
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final MultiCatch t;

            public List<JRightPadded<NameTree>> getAlternatives() {
                return t.alternatives;
            }

            public MultiCatch withAlternatives(List<JRightPadded<NameTree>> alternatives) {
                return t.alternatives == alternatives ? t : new MultiCatch(t.id, t.prefix, t.markers, alternatives);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class NewArray implements J, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Nullable
        @Getter
        TypeTree typeExpr;

        @With
        @Getter
        List<ArrayDimension> dimensions;

        @Nullable
        JContainer<Expression> initializer;

        @Nullable
        public List<Expression> getInitializer() {
            return initializer == null ? null : initializer.getElems();
        }

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitNewArray(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final NewArray t;

            @Nullable
            public JContainer<Expression> getInitializer() {
                return t.initializer;
            }

            public NewArray withInitializer(@Nullable JContainer<Expression> initializer) {
                return t.initializer == initializer ? t : new NewArray(t.id, t.prefix, t.markers, t.typeExpr, t.dimensions, initializer, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ArrayDimension implements J {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        JRightPadded<Expression> index;

        public Expression getIndex() {
            return index.getElem();
        }

        public ArrayDimension withIndex(Expression index) {
            return getPadding().withIndex(this.index.withElem(index));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitArrayDimension(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ArrayDimension t;

            public JRightPadded<Expression> getIndex() {
                return t.index;
            }

            public ArrayDimension withIndex(JRightPadded<Expression> index) {
                return t.index == index ? t : new ArrayDimension(t.id, t.prefix, t.markers, index);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class NewClass implements J, Statement, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        /**
         * For situations like <code>this.new A()</code>.
         * Right padded before the '.'
         */
        @Nullable
        JRightPadded<Expression> encl;

        @Nullable
        public Expression getEncl() {
            return encl == null ? null : encl.getElem();
        }

        public NewClass withEncl(Expression encl) {
            return getPadding().withEncl(JRightPadded.withElem(this.encl, encl));
        }

        Space nooh;

        public Space getNew() {
            return nooh;
        }

        public NewClass withNew(Space nooh) {
            if (nooh == this.nooh) {
                return this;
            }
            return new NewClass(id, prefix, markers, encl, nooh, clazz, args, body, type);
        }

        @Nullable
        @With
        @Getter
        TypeTree clazz;

        @Nullable
        JContainer<Expression> args;

        @Nullable
        public JContainer<Expression> getArgs() {
            return args;
        }

        public NewClass withArgs(@Nullable List<Expression> args) {
            return getPadding().withArgs(JContainer.withElems(this.args, args));
        }

        @With
        @Nullable
        @Getter
        Block body;

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitNewClass(this, p);
        }

        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final NewClass t;

            @Nullable
            public JRightPadded<Expression> getEncl() {
                return t.encl;
            }

            public NewClass withEncl(@Nullable JRightPadded<Expression> encl) {
                return t.encl == encl ? t : new NewClass(t.id, t.prefix, t.markers, encl, t.nooh, t.clazz, t.args, t.body, t.type);
            }

            @Nullable
            public JContainer<Expression> getArgs() {
                return t.args;
            }

            public NewClass withArgs(@Nullable JContainer<Expression> args) {
                return t.args == args ? t : new NewClass(t.id, t.prefix, t.markers, t.encl, t.nooh, t.clazz, args, t.body, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Package implements J {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression expr;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitPackage(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ParameterizedType implements J, TypeTree, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        NameTree clazz;

        @Nullable
        JContainer<Expression> typeParameters;

        @Nullable
        public List<Expression> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElems();
        }

        public ParameterizedType withTypeParameters(@Nullable List<Expression> typeParameters) {
            return getPadding().withTypeParameters(JContainer.withElems(this.typeParameters, typeParameters));
        }

        @Override
        public JavaType getType() {
            return clazz.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ParameterizedType withType(@Nullable JavaType type) {
            if (type == clazz.getType()) {
                return this;
            }
            return withClazz(clazz.withType(type));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitParameterizedType(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ParameterizedType t;

            @Nullable
            public JContainer<Expression> getTypeParameters() {
                return t.typeParameters;
            }

            public ParameterizedType withTypeParameters(@Nullable JContainer<Expression> typeParameters) {
                return t.typeParameters == typeParameters ? t : new ParameterizedType(t.id, t.prefix, t.markers, t.clazz, typeParameters);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Parentheses<J2 extends J> implements J, Expression {
        @Nullable
        @NonFinal
        transient Padding<J2> padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        JRightPadded<J2> tree;

        public J2 getTree() {
            return tree.getElem();
        }

        public Parentheses<J2> withTree(J2 tree) {
            return getPadding().withTree(this.tree.withElem(tree));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitParentheses(this, p);
        }

        @Override
        public List<Tree> getSideEffects() {
            return tree instanceof Expression ? ((Expression) tree).getSideEffects() : emptyList();
        }

        @Override
        public JavaType getType() {
            return tree instanceof Expression ? ((Expression) tree).getType() :
                    tree instanceof NameTree ? ((NameTree) tree).getType() :
                            null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Parentheses<J2> withType(@Nullable JavaType type) {
            return tree instanceof Expression ? ((Expression) tree).withType(type) :
                    tree instanceof NameTree ? ((NameTree) tree).withType(type) :
                            this;
        }

        public Padding<J2> getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding<>(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding<J2 extends J> {
            private final Parentheses<J2> t;

            public JRightPadded<J2> getTree() {
                return t.tree;
            }

            public Parentheses<J2> withTree(JRightPadded<J2> tree) {
                return t.tree == tree ? t : new Parentheses<>(t.id, t.prefix, t.markers, tree);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ControlParentheses<J2 extends J> implements J, Expression {
        @Nullable
        @NonFinal
        transient Padding<J2> padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        JRightPadded<J2> tree;

        public J2 getTree() {
            return tree.getElem();
        }

        public ControlParentheses<J2> withTree(J2 tree) {
            return getPadding().withTree(this.tree.withElem(tree));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitControlParentheses(this, p);
        }

        @Override
        public List<Tree> getSideEffects() {
            return tree instanceof Expression ? ((Expression) tree).getSideEffects() : emptyList();
        }

        @Override
        public JavaType getType() {
            return tree instanceof Expression ? ((Expression) tree).getType() :
                    tree instanceof NameTree ? ((NameTree) tree).getType() :
                            null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ControlParentheses<J2> withType(@Nullable JavaType type) {
            return tree instanceof Expression ? ((Expression) tree).withType(type) :
                    tree instanceof NameTree ? ((NameTree) tree).withType(type) :
                            this;
        }

        public Padding<J2> getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding<>(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding<J2 extends J> {
            private final ControlParentheses<J2> t;

            public JRightPadded<J2> getTree() {
                return t.tree;
            }

            public ControlParentheses<J2> withTree(JRightPadded<J2> tree) {
                return t.tree == tree ? t : new ControlParentheses<>(t.id, t.prefix, t.markers, tree);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class Primitive implements J, TypeTree, Expression {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        JavaType.Primitive type;

        @SuppressWarnings("unchecked")
        @Override
        public Primitive withType(@Nullable JavaType type) {
            if (type == this.type) {
                return this;
            }
            if (!(type instanceof JavaType.Primitive)) {
                throw new IllegalArgumentException("Cannot apply a non-primitive type to Primitive");
            }
            return new Primitive(id, prefix, markers, (JavaType.Primitive) type);
        }

        @Override
        @NonNull
        public JavaType.Primitive getType() {
            return type;
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitPrimitive(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Return implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        Expression expr;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitReturn(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Switch implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        ControlParentheses<Expression> selector;

        @With
        Block cases;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitSwitch(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Synchronized implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        ControlParentheses<Expression> lock;

        @With
        Block body;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitSynchronized(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Ternary implements J, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Expression condition;

        JLeftPadded<Expression> truePart;

        public Expression getTruePart() {
            return truePart.getElem();
        }

        public Ternary withTruePart(Expression truePart) {
            return getPadding().withTruePart(this.truePart.withElem(truePart));
        }

        JLeftPadded<Expression> falsePart;

        public Expression getFalsePart() {
            return falsePart.getElem();
        }

        public Ternary withFalsePart(Expression falsePart) {
            return getPadding().withFalsePart(this.falsePart.withElem(falsePart));
        }

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitTernary(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Ternary t;

            public JLeftPadded<Expression> getTruePart() {
                return t.truePart;
            }

            public Ternary withTruePart(JLeftPadded<Expression> truePart) {
                return t.truePart == truePart ? t : new Ternary(t.id, t.prefix, t.markers, t.condition, truePart, t.falsePart, t.type);
            }

            public JLeftPadded<Expression> getFalsePart() {
                return t.falsePart;
            }

            public Ternary withFalsePart(JLeftPadded<Expression> falsePart) {
                return t.falsePart == falsePart ? t : new Ternary(t.id, t.prefix, t.markers, t.condition, t.truePart, falsePart, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Throw implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression exception;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitThrow(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Try implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @Nullable
        JContainer<Resource> resources;

        @Nullable
        public List<Resource> getResources() {
            return resources == null ? null : resources.getElems();
        }

        public Try withResources(List<Resource> resources) {
            return getPadding().withResources(JContainer.withElems(this.resources, resources));
        }

        @With
        @Getter
        Block body;

        @With
        @Getter
        List<Catch> catches;

        @Nullable
        JLeftPadded<Block> finallie;

        @Nullable
        public Block getFinally() {
            return finallie == null ? null : finallie.getElem();
        }

        public Try withFinally(Block finallie) {
            return getPadding().withFinally(JLeftPadded.withElem(this.finallie, finallie));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitTry(this, p);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Resource implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            VariableDecls variableDecls;

            /**
             * Only honored on the last resource in a collection of resources.
             */
            @With
            boolean terminatedWithSemicolon;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Catch implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            ControlParentheses<VariableDecls> param;

            @With
            Block body;

            @Override
            public <P> J acceptJava(JavaVisitor<P> v, P p) {
                return v.visitCatch(this, p);
            }
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Try t;

            @Nullable
            public JContainer<Resource> getResources() {
                return t.resources;
            }

            public Try withResources(@Nullable JContainer<Resource> resources) {
                return t.resources == resources ? t : new Try(t.id, t.prefix, t.markers, resources, t.body, t.catches, t.finallie);
            }

            @Nullable
            public JLeftPadded<Block> getFinally() {
                return t.finallie;
            }

            public Try withFinally(@Nullable JLeftPadded<Block> finallie) {
                return t.finallie == finallie ? t : new Try(t.id, t.prefix, t.markers, t.resources, t.body, t.catches, finallie);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class TypeCast implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        ControlParentheses<TypeTree> clazz;

        @With
        Expression expr;

        @Override
        public JavaType getType() {
            return clazz.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public TypeCast withType(@Nullable JavaType type) {
            return withClazz(clazz.withType(type));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitTypeCast(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TypeParameter implements J {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        List<Annotation> annotations;

        /**
         * Will be either a {@link TypeTree} or {@link Wildcard}. Wildcards aren't possible in
         * every context where type parameters may be defined (e.g. not possible on new statements).
         */
        @With
        @Getter
        Expression name;

        @Nullable
        JContainer<TypeTree> bounds;

        @Nullable
        public List<TypeTree> getBounds() {
            return bounds == null ? null : bounds.getElems();
        }

        public TypeParameter withBounds(@Nullable List<TypeTree> bounds) {
            return getPadding().withBounds(JContainer.withElems(this.bounds, bounds));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitTypeParameter(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TypeParameter t;

            @Nullable
            public JContainer<TypeTree> getBounds() {
                return t.bounds;
            }

            public TypeParameter withBounds(@Nullable JContainer<TypeTree> bounds) {
                return t.bounds == bounds ? t : new TypeParameter(t.id, t.prefix, t.markers, t.annotations, t.name, bounds);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Unary implements J, Statement, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        JLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElem();
        }

        public Unary withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElem(operator));
        }

        @With
        @Getter
        Expression expr;

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitUnary(this, p);
        }

        @Override
        public List<Tree> getSideEffects() {
            return expr.getSideEffects();
        }

        public enum Type {
            PreIncrement,
            PreDecrement,
            PostIncrement,
            PostDecrement,
            Positive,
            Negative,
            Complement,
            Not
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Unary t;

            public JLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public Unary withOperator(JLeftPadded<Type> operator) {
                return t.operator == operator ? t : new Unary(t.id, t.prefix, t.markers, operator, t.expr, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class VariableDecls implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        List<Annotation> annotations;

        @With
        @Getter
        List<Modifier> modifiers;

        @With
        @Nullable
        @Getter
        TypeTree typeExpr;

        @With
        @Nullable
        @Getter
        Space varargs;

        @With
        @Getter
        List<JLeftPadded<Space>> dimensionsBeforeName;

        List<JRightPadded<NamedVar>> vars;

        public List<NamedVar> getVars() {
            return JRightPadded.getElems(vars);
        }

        public VariableDecls withVars(List<NamedVar> vars) {
            return getPadding().withVars(JRightPadded.withElems(this.vars, vars));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitMultiVariable(this, p);
        }

        @Nullable
        public JavaType.Class getTypeAsClass() {
            return typeExpr == null ? null : TypeUtils.asClass(typeExpr.getType());
        }

        @ToString
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class NamedVar implements J, NameTree {
            @Nullable
            @NonFinal
            transient Padding padding;

            @EqualsAndHashCode.Include
            @Getter
            UUID id;

            @With
            @Getter
            Space prefix;

            @With
            @Getter
            Markers markers;

            @With
            @Getter
            Ident name;

            @With
            @Getter
            List<JLeftPadded<Space>> dimensionsAfterName;

            @Nullable
            JLeftPadded<Expression> initializer;

            @Nullable
            public Expression getInitializer() {
                return initializer == null ? null : initializer.getElem();
            }

            public NamedVar withInitializer(@Nullable Expression initializer) {
                return getPadding().withInitializer(JLeftPadded.withElem(this.initializer, initializer));
            }

            @With
            @Nullable
            @Getter
            JavaType type;

            public String getSimpleName() {
                return name.getSimpleName();
            }

            @Override
            public <P> J acceptJava(JavaVisitor<P> v, P p) {
                return v.visitVariable(this, p);
            }

            public boolean isField(Cursor cursor) {
                return cursor
                        .getParentOrThrow() // JRightPadded
                        .getParentOrThrow() // J.VariableDecls
                        .getParentOrThrow() // JRightPadded
                        .getParentOrThrow() // J.Block
                        .getParentOrThrow() // maybe J.ClassDecl
                        .getValue() instanceof J.ClassDecl;
            }

            public Padding getPadding() {
                if (padding == null || padding.t != this) {
                    this.padding = new Padding(this);
                }
                return padding;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final NamedVar t;

                @Nullable
                public JLeftPadded<Expression> getInitializer() {
                    return t.initializer;
                }

                public NamedVar withInitializer(@Nullable JLeftPadded<Expression> initializer) {
                    return t.initializer == initializer ? t : new NamedVar(t.id, t.prefix, t.markers, t.name, t.dimensionsAfterName, initializer, t.type);
                }
            }
        }

        public boolean hasModifier(String modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        @Override
        public String toString() {
            return "VariableDecls(" + VariableDeclsToString.toString(this) + ")";
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final VariableDecls t;

            public List<JRightPadded<NamedVar>> getVars() {
                return t.vars;
            }

            public VariableDecls withVars(List<JRightPadded<NamedVar>> vars) {
                return t.vars == vars ? t : new VariableDecls(t.id, t.prefix, t.markers, t.annotations, t.modifiers, t.typeExpr, t.varargs, t.dimensionsBeforeName, vars);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class WhileLoop implements J, Statement {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        ControlParentheses<Expression> condition;

        JRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElem();
        }

        public WhileLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElem(body));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitWhileLoop(this, p);
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final WhileLoop t;

            public JRightPadded<Statement> getBody() {
                return t.body;
            }

            public WhileLoop withBody(JRightPadded<Statement> body) {
                return t.body == body ? t : new WhileLoop(t.id, t.prefix, t.markers, t.condition, body);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Wildcard implements J, Expression {
        @Nullable
        @NonFinal
        transient Padding padding;

        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @Nullable
        JLeftPadded<Bound> bound;

        @Nullable
        public Bound getBound() {
            return bound == null ? null : bound.getElem();
        }

        public Wildcard withBound(@Nullable Bound bound) {
            return getPadding().withBound(JLeftPadded.withElem(this.bound, bound));
        }

        @With
        @Nullable
        @Getter
        NameTree boundedType;

        @Override
        public JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Wildcard withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitWildcard(this, p);
        }

        public enum Bound {
            Extends,
            Super
        }

        public Padding getPadding() {
            if (padding == null || padding.t != this) {
                this.padding = new Padding(this);
            }
            return padding;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Wildcard t;

            @Nullable
            public JLeftPadded<Bound> getBound() {
                return t.bound;
            }

            public Wildcard withBound(@Nullable JLeftPadded<Bound> bound) {
                return t.bound == bound ? t : new Wildcard(t.id, t.prefix, t.markers, bound, t.boundedType);
            }
        }
    }
}
