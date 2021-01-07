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

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.*;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.internal.*;
import org.openrewrite.java.search.FindType;
import org.openrewrite.marker.Markers;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("unused")
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface J extends Serializable, Tree {
    @Override
    default <R, P> R accept(TreeVisitor<R, P> v, P p) {
        return v instanceof JavaVisitor ?
                acceptJava((JavaVisitor<R, P>) v, p) : v.defaultValue(null, p);
    }

    default <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
        return v.defaultValue(this, p);
    }

    default String print(TreePrinter<?> printer) {
        return new JavaPrinter<>((TreePrinter<?>) printer).visit(this, null);
    }

    @Override
    default String print() {
        return new JavaPrinter<>(TreePrinter.identity()).visit(this, null);
    }

    <J2 extends J> J2 withPrefix(Space space);

    Space getPrefix();

    @JsonIgnore
    default List<Comment> getComments() {
        return getPrefix().getComments();
    }

    default <J2 extends J> J2 withComments(List<Comment> comments) {
        return withPrefix(getPrefix().withComments(comments));
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitAnnotatedType(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Annotation implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        NameTree annotationType;

        @Nullable
        @With
        JContainer<Expression> args;

        @JsonIgnore
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitAnnotation(this, p);
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
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
        public ArrayType withType(JavaType type) {
            if (type == getType()) {
                return this;
            }
            return withElementType(elementType.withType(type));
        }

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitAssert(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Assign implements J, Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression variable;

        @With
        JLeftPadded<Expression> assignment;

        @With
        @Nullable
        JavaType type;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitAssign(this, p);
        }

        @JsonIgnore
        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class AssignOp implements J, Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression variable;

        @With
        JLeftPadded<Type> operator;

        @With
        Expression assignment;

        @With
        @Nullable
        JavaType type;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitAssignOp(this, p);
        }

        @JsonIgnore
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Binary implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression left;

        @With
        JLeftPadded<Type> operator;

        @With
        Expression right;

        @With
        @Nullable
        JavaType type;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitBinary(this, p);
        }

        @JsonIgnore
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class Block implements J, Statement {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        /**
         * These comments and whitespace are AFTER the static keyword
         */
        @Nullable
        @With
        Space statik;

        @JsonProperty("statik")
        @Nullable
        public Space getStatic() {
            return statik;
        }

        public Block withStatic(@Nullable Space statik) {
            return new Block(id, prefix, markers, statik, statements, end);
        }

        @Getter
        @With
        List<JRightPadded<Statement>> statements;

        @Getter
        @With
        Space end;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitBlock(this, p);
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitBreak(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Case implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression pattern;

        @With
        JContainer<Statement> statements;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitCase(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class ClassDecl implements J, Statement {
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

        @With
        @Getter
        JLeftPadded<Kind> kind;

        @With
        @Getter
        Ident name;

        @With
        @Getter
        @Nullable
        JContainer<TypeParameter> typeParameters;

        @Nullable
        JLeftPadded<TypeTree> extendings;

        public ClassDecl withExtends(@Nullable JLeftPadded<TypeTree> extendings) {
            if (extendings == this.extendings) {
                return this;
            }
            return new ClassDecl(id, prefix, markers, annotations, modifiers, kind, name,
                    typeParameters, extendings, implementings, body, type);
        }

        @JsonProperty("extendings")
        @Nullable
        public JLeftPadded<TypeTree> getExtends() {
            return extendings;
        }

        @Nullable
        JContainer<TypeTree> implementings;

        public ClassDecl withImplements(@Nullable JContainer<TypeTree> implementings) {
            if (implementings == this.implementings) {
                return this;
            }
            return new ClassDecl(id, prefix, markers, annotations, modifiers, kind, name,
                    typeParameters, extendings, implementings, body, type);
        }

        @JsonProperty("implementings")
        @Nullable
        public JContainer<TypeTree> getImplements() {
            return implementings;
        }

        @With
        @Getter
        Block body;

        @With
        @Getter
        @Nullable
        JavaType.Class type;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitClassDecl(this, p);
        }

        @JsonIgnore
        public String getSimpleName() {
            return name.getSimpleName();
        }

        public enum Kind {
            Class,
            Enum,
            Interface,
            Annotation
        }

        @Nullable
        public EnumValueSet getEnumValues() {
            for (JRightPadded<Statement> stat : body.getStatements()) {
                if (stat.getElem() instanceof EnumValueSet) {
                    return (EnumValueSet) stat.getElem();
                }
            }
            return null;
        }

        @JsonIgnore
        public List<VariableDecls> getFields() {
            List<VariableDecls> list = new ArrayList<>();
            for (JRightPadded<Statement> stat : body.getStatements()) {
                if (stat.getElem() instanceof VariableDecls) {
                    VariableDecls variableDecls = (VariableDecls) stat.getElem();
                    list.add(variableDecls);
                }
            }
            return list;
        }

        @JsonIgnore
        public List<MethodDecl> getMethods() {
            List<MethodDecl> list = new ArrayList<>();
            for (JRightPadded<Statement> stat : body.getStatements()) {
                if (stat.getElem() instanceof MethodDecl) {
                    MethodDecl methodDecl = (MethodDecl) stat.getElem();
                    list.add(methodDecl);
                }
            }
            return list;
        }

        public boolean hasModifier(String modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        @Override
        public String toString() {
            return "ClassDecl(" + ClassDeclToString.toString(this) + ")";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class CompilationUnit implements J, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Path sourcePath;

        @With
        @Nullable
        JRightPadded<Package> packageDecl;

        @With
        List<JRightPadded<Import>> imports;

        @With
        List<ClassDecl> classes;

        @With
        Space eof;

        @With
        Collection<JavaStyle> styles;

        @Override
        public Collection<JavaStyle> getStyles() {
            return styles;
        }

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        public Set<NameTree> findType(String clazz) {
            return FindType.find(this, clazz);
        }

        @JsonIgnore
        public Path getSourceSet() {
            int packageLevelsUp = getPackageDecl() == null ? 0 :
                    (int) getPackageDecl().getElem().printTrimmed().chars().filter(c -> c == '.').count();
            // Jump over Java file name
            return sourcePath.getParent().resolve(IntStream.range(0, packageLevelsUp + 1)
                    .mapToObj(n -> "../")
                    .collect(joining(""))).normalize();
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitContinue(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class DoWhileLoop implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        JRightPadded<Statement> body;

        @With
        JLeftPadded<Parentheses<Expression>> whileCondition;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitDoWhileLoop(this, p);
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
        public Empty withType(JavaType type) {
            return this;
        }

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitEnumValue(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class EnumValueSet implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        List<JRightPadded<EnumValue>> enums;

        boolean terminatedWithSemicolon;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitEnumValueSet(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class FieldAccess implements J, TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression target;

        @With
        JLeftPadded<Ident> name;

        @With
        @Nullable
        JavaType type;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitFieldAccess(this, p);
        }

        @JsonIgnore
        public String getSimpleName() {
            return name.getElem().getSimpleName();
        }

        @JsonIgnore
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
            if (!fieldAccess.getName().getElem().getSimpleName().equals(className.substring(className.lastIndexOf('.') + 1))) {
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ForEachLoop implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Control control;

        @With
        JRightPadded<Statement> body;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitForEachLoop(this, p);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Control implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            JRightPadded<VariableDecls> variable;

            @With
            JRightPadded<Expression> iterable;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ForLoop implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Control control;

        @With
        JRightPadded<Statement> body;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitForLoop(this, p);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Control implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            JRightPadded<Statement> init;

            @With
            JRightPadded<Expression> condition;

            @With
            List<JRightPadded<Statement>> update;
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
        public Ident withType(JavaType type) {
            if (type == getType()) {
                return this;
            }
            return build(id, prefix, markers, getSimpleName(), type);
        }

        @JsonIgnore
        public String getSimpleName() {
            return ident.getSimpleName();
        }

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
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
        public static Ident build(@JsonProperty("id") UUID id,
                                  @JsonProperty("prefix") Space prefix,
                                  @JsonProperty("metadata") Markers markers,
                                  @JsonProperty("simpleName") String simpleName,
                                  @JsonProperty("type") @Nullable JavaType type) {
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

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class If implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Parentheses<Expression> ifCondition;

        @With
        JRightPadded<Statement> thenPart;

        @With
        @Nullable
        Else elsePart;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitIf(this, p);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Else implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            JRightPadded<Statement> body;

            @Override
            public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
                return v.visitElse(this, p);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class Import implements J, Comparable<Import> {
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
        @Nullable
        Space statik;

        @With
        @Getter
        FieldAccess qualid;

        public boolean isStatic() {
            return statik != null;
        }

        @JsonProperty("statik")
        @Nullable
        public Space getStatic() {
            return statik;
        }

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitImport(this, p);
        }

        @JsonIgnore
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
        @JsonIgnore
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class InstanceOf implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        JRightPadded<Expression> expr;

        @With
        J clazz;

        @With
        @Nullable
        JavaType type;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitInstanceOf(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Label implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        /**
         * Right padded before the ':'
         */
        @With
        JRightPadded<Ident> label;

        @With
        Statement statement;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitLabel(this, p);
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitLambda(this, p);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Parameters implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            boolean parenthesized;

            @With
            List<JRightPadded<J>> params;
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
        public Literal withType(JavaType type) {
            if (type == this.type) {
                return this;
            }
            if (type instanceof JavaType.Primitive) {
                return new Literal(id, prefix, markers, value, valueSource, (JavaType.Primitive) type);
            }
            return this;
        }

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class MemberReference implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression containing;

        @With
        @Nullable
        JContainer<Expression> typeParameters;

        @With
        JLeftPadded<Ident> reference;

        @With
        @Nullable
        JavaType type;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitMemberReference(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class MethodDecl implements J, Statement {
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

        @With
        @Getter
        @Nullable
        JContainer<TypeParameter> typeParameters;

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

        @With
        @Getter
        JContainer<Statement> params;

        @Nullable
        JContainer<NameTree> throwz;

        public MethodDecl withThrows(JContainer<NameTree> throwz) {
            if (throwz == this.throwz) {
                return this;
            }
            return new MethodDecl(id, prefix, markers, annotations, modifiers, typeParameters, returnTypeExpr,
                    name, params, throwz, body, defaultValue, type);
        }

        @JsonProperty("throwz")
        @Nullable
        public JContainer<NameTree> getThrows() {
            return throwz;
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
        @With
        @Getter
        @Nullable
        JLeftPadded<Expression> defaultValue;

        @With
        @Getter
        @Nullable
        JavaType.Method type;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitMethod(this, p);
        }

        @JsonIgnore
        public boolean isAbstract() {
            return body == null;
        }

        @JsonIgnore
        public boolean isConstructor() {
            return getReturnTypeExpr() == null;
        }

        @JsonIgnore
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class MethodInvocation implements J, Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        /**
         * Right padded before the '.'
         */
        @With
        @Nullable
        JRightPadded<Expression> select;

        @With
        @Nullable
        JContainer<TypeParameter> typeParameters;

        @With
        Ident name;

        @With
        JContainer<Expression> args;

        @Nullable
        JavaType.Method type;

        @SuppressWarnings("unchecked")
        @Override
        public MethodInvocation withType(JavaType type) {
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitMethodInvocation(this, p);
        }

        @JsonIgnore
        @Nullable
        public JavaType getReturnType() {
            return type == null ? null : type.getResolvedSignature() == null ? null :
                    type.getResolvedSignature().getReturnType();
        }

        @JsonIgnore
        public String getSimpleName() {
            return name.getSimpleName();
        }

        @JsonIgnore
        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
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

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class MultiCatch implements J, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        List<JRightPadded<NameTree>> alternatives;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitMultiCatch(this, p);
        }

        @SuppressWarnings("unchecked")
        @Override
        public MultiCatch withType(JavaType type) {
            // cannot overwrite type directly, perform this operation on each alternative separately
            return this;
        }

        @JsonIgnore
        @Override
        public JavaType getType() {
            return new JavaType.MultiCatch(alternatives.stream()
                    .filter(Objects::nonNull)
                    .map(alt -> alt.getElem().getType())
                    .collect(toList()));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class NewArray implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        TypeTree typeExpr;

        @With
        List<ArrayDimension> dimensions;

        @With
        @Nullable
        JContainer<Expression> initializer;

        @With
        @Nullable
        JavaType type;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitNewArray(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ArrayDimension implements J {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        JRightPadded<Expression> index;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitArrayDimension(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class NewClass implements J, Statement, Expression {
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
        @With
        @Getter
        JRightPadded<Expression> encl;

        Space nooh;

        public NewClass withNew(Space nooh) {
            if (nooh == this.nooh) {
                return this;
            }
            return new NewClass(id, prefix, markers, encl, nooh, clazz, args, body, type);
        }

        @JsonProperty("nooh")
        public Space getNew() {
            return nooh;
        }

        @Nullable
        @With
        @Getter
        TypeTree clazz;

        @Nullable
        @With
        @Getter
        JContainer<Expression> args;

        @With
        @Nullable
        @Getter
        Block body;

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitNewClass(this, p);
        }

        @JsonIgnore
        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitPackage(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ParameterizedType implements J, TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        NameTree clazz;

        @With
        @Nullable
        JContainer<Expression> typeParameters;

        @Override
        public JavaType getType() {
            return clazz.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ParameterizedType withType(JavaType type) {
            if (type == clazz.getType()) {
                return this;
            }
            return withClazz(clazz.withType(type));
        }

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitParameterizedType(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Parentheses<J2 extends J> implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        JRightPadded<J2> tree;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitParentheses(this, p);
        }

        @JsonIgnore
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
        public Parentheses<J2> withType(JavaType type) {
            return tree instanceof Expression ? ((Expression) tree).withType(type) :
                    tree instanceof NameTree ? ((NameTree) tree).withType(type) :
                            this;
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
        public Primitive withType(JavaType type) {
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
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
        Parentheses<Expression> selector;

        @With
        Block cases;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
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
        Parentheses<Expression> lock;

        @With
        Block body;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitSynchronized(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Ternary implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression condition;

        @With
        JLeftPadded<Expression> truePart;

        @With
        JLeftPadded<Expression> falsePart;

        @With
        @Nullable
        JavaType type;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitTernary(this, p);
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
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitThrow(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class Try implements J, Statement {
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
        @Nullable
        JContainer<Resource> resources;

        @With
        @Getter
        Block body;

        @With
        @Getter
        List<Catch> catches;

        @Nullable
        JLeftPadded<Block> finallie;

        public Try withFinally(JLeftPadded<Block> finallie) {
            if (finallie == this.finallie) {
                return this;
            }
            return new Try(id, prefix, markers, resources, body, catches, finallie);
        }

        @JsonProperty("finallie")
        @Nullable
        public JLeftPadded<Block> getFinally() {
            return finallie;
        }

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
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
            Parentheses<VariableDecls> param;

            @With
            Block body;

            @Override
            public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
                return v.visitCatch(this, p);
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
        Parentheses<TypeTree> clazz;

        @With
        Expression expr;

        @Override
        public JavaType getType() {
            return clazz.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public TypeCast withType(JavaType type) {
            return withClazz(clazz.withType(type));
        }

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitTypeCast(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class TypeParameter implements J {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        List<Annotation> annotations;

        /**
         * Will be either a {@link TypeTree} or {@link Wildcard}. Wildcards aren't possible in
         * every context where type parameters may be defined (e.g. not possible on new statements).
         */
        @With
        Expression name;

        @With
        @Nullable
        JContainer<TypeTree> bounds;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitTypeParameter(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Unary implements J, Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        JLeftPadded<Type> operator;

        @With
        Expression expr;

        @With
        @Nullable
        JavaType type;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitUnary(this, p);
        }

        @JsonIgnore
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class VariableDecls implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        List<Annotation> annotations;

        List<Modifier> modifiers;

        public VariableDecls withModifiers(List<Modifier> modifiers) {
            if (modifiers == this.modifiers) {
                return this;
            }
            return new VariableDecls(id, prefix, markers, annotations, modifiers, typeExpr, varargs,
                    dimensionsBeforeName, vars);
        }

        @With
        @Nullable
        TypeTree typeExpr;

        @With
        @Nullable
        Space varargs;

        @With
        List<JLeftPadded<Space>> dimensionsBeforeName;

        @With
        List<JRightPadded<NamedVar>> vars;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitMultiVariable(this, p);
        }

        @JsonIgnore
        public JavaType.Class getTypeAsClass() {
            return typeExpr == null ? null : TypeUtils.asClass(typeExpr.getType());
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class NamedVar implements J, NameTree {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            Ident name;

            @With
            List<JLeftPadded<Space>> dimensionsAfterName;

            @With
            @Nullable
            JLeftPadded<Expression> initializer;

            @With
            @Nullable
            JavaType type;

            @JsonIgnore
            public String getSimpleName() {
                return name.getSimpleName();
            }

            @Override
            public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
                return v.visitVariable(this, p);
            }

            @JsonIgnore
            public boolean isField(Cursor cursor) {
                return cursor
                        .getParentOrThrow() // J.VariableDecls
                        .getParentOrThrow() // J.Block
                        .getParentOrThrow() // maybe J.ClassDecl
                        .getTree() instanceof J.ClassDecl;
            }
        }

        public boolean hasModifier(String modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        @Override
        public String toString() {
            return "VariableDecls(" + VariableDeclsToString.toString(this) + ")";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class WhileLoop implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Parentheses<Expression> condition;

        @With
        JRightPadded<Statement> body;

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitWhileLoop(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Wildcard implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        JLeftPadded<Bound> bound;

        @With
        @Nullable
        NameTree boundedType;

        @Override
        public JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Wildcard withType(JavaType type) {
            return this;
        }

        @Override
        public <R, P> R acceptJava(JavaVisitor<R, P> v, P p) {
            return v.visitWildcard(this, p);
        }

        public enum Bound {
            Extends,
            Super
        }
    }
}
