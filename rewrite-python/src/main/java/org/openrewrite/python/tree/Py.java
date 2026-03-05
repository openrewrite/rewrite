/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.python.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.python.PythonVisitor;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.rpc.request.Print;

import java.beans.Transient;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public interface Py extends J {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        final String visitorName = v.getClass().getCanonicalName();
        // FIXME HACK TO AVOID RUNTIME VISITOR-ADAPTING IN NATIVE IMAGE
        if (visitorName != null && visitorName.startsWith("io.moderne.serialization.")) {
            return (R) this;
        }
        return (R) acceptPython(v.adapt(PythonVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(PythonVisitor.class);
    }

    default <P> @Nullable J acceptPython(PythonVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    Space getPrefix();

    @Override
    default List<Comment> getComments() {
        return getPrefix().getComments();
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    final class Async implements Py, Statement {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Statement statement;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitAsync(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    final class Await implements Py, Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression expression;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitAwait(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Binary implements Py, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
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
            return operator.getElement();
        }

        public Py.Binary withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        @Nullable
        @With
        Space negation;

        @With
        Expression right;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitBinary(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public enum Type {
            In,
            Is,
            IsNot,
            NotIn,

            FloorDivision,
            MatrixMultiplication,
            Power,

            StringConcatenation,
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Py.Binary t;

            public JLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public Py.Binary withOperator(JLeftPadded<Type> operator) {
                return t.operator == operator ? t : new Py.Binary(t.id, t.prefix, t.markers, t.left, operator, t.negation, t.right, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class ChainedAssignment implements Py, Statement, TypedTree {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        List<JRightPadded<Expression>> variables;

        public List<Expression> getVariables() {
            return JRightPadded.getElements(variables);
        }

        public ChainedAssignment withVariables(List<Expression> variables) {
            return getPadding().withVariables(JRightPadded.withElements(this.variables, variables));
        }

        @With
        Expression assignment;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitChainedAssignment(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ChainedAssignment t;

            public List<JRightPadded<Expression>> getVariables() {
                return t.variables;
            }

            public ChainedAssignment withVariables(List<JRightPadded<Expression>> variables) {
                return t.variables == variables ? t : new ChainedAssignment(t.id, t.prefix, t.markers, variables, t.assignment, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @Data
    final class ExceptionType implements Py, TypeTree {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        JavaType type;

        @With
        boolean isExceptionGroup;

        @With
        Expression expression;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitExceptionType(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @Data
    final class LiteralType implements Py, Expression, TypeTree {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        // Not `J.Literal` so that also literals like `-1` are captured
        @With
        Expression literal;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitLiteralType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @Getter
    @With
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    final class TypeHint implements Py, TypeTree {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        // Using `Expression` to also cater for `None`
        Expression typeTree;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitTypeHint(this, p);
        }
    }


    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CompilationUnit implements Py, JavaSourceFile, SourceFile {
        @Nullable
        @NonFinal
        transient SoftReference<TypesInUse> typesInUse;

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @EqualsAndHashCode.Include
        @With
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

        @With
        @Getter
        @Nullable
        FileAttributes fileAttributes;

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        @With
        @Getter
        boolean charsetBomMarked;

        @With
        @Getter
        @Nullable
        Checksum checksum;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @SuppressWarnings("unchecked")
        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        List<JRightPadded<J.Import>> imports;

        @Override
        public List<J.Import> getImports() {
            return JRightPadded.getElements(imports);
        }

        @Override
        public Py.CompilationUnit withImports(List<J.Import> imports) {
            return getPadding().withImports(JRightPadded.withElements(this.imports, imports));
        }

        List<JRightPadded<Statement>> statements;

        public List<Statement> getStatements() {
            return JRightPadded.getElements(statements);
        }

        public Py.CompilationUnit withStatements(List<Statement> statements) {
            return getPadding().withStatements(JRightPadded.withElements(this.statements, statements));
        }

        @With
        @Getter
        Space eof;

        @Override
        @Transient
        public List<ClassDeclaration> getClasses() {
            return statements.stream()
                    .map(JRightPadded::getElement)
                    .filter(J.ClassDeclaration.class::isInstance)
                    .map(J.ClassDeclaration.class::cast)
                    .collect(toList());
        }

        @Override
        public JavaSourceFile withClasses(List<ClassDeclaration> classes) {
            // FIXME unsupported
            return this;
        }

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new TreeVisitor<Tree, PrintOutputCapture<P>>() {
                @Override
                public Tree preVisit(Tree tree, PrintOutputCapture<P> p) {
                    PythonRewriteRpc rpc = PythonRewriteRpc.getOrStart();
                    Print.MarkerPrinter mappedMarkerPrinter = Print.MarkerPrinter.from(p.getMarkerPrinter());
                    p.append(rpc.print(tree, cursor, mappedMarkerPrinter));
                    stopAfterPreVisit();
                    return tree;
                }
            };
        }

        @Override
        @Transient
        public TypesInUse getTypesInUse() {
            TypesInUse cache;
            if (this.typesInUse == null) {
                cache = TypesInUse.build(this);
                this.typesInUse = new SoftReference<>(cache);
            } else {
                cache = this.typesInUse.get();
                if (cache == null || cache.getCu() != this) {
                    cache = TypesInUse.build(this);
                    this.typesInUse = new SoftReference<>(cache);
                }
            }
            return cache;
        }

        @Override
        public @Nullable Package getPackageDeclaration() {
            return null;
        }

        @Override
        public JavaSourceFile withPackageDeclaration(Package pkg) {
            throw new IllegalStateException("Python does not support package declarations");
        }

        @Override
        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding implements JavaSourceFile.Padding {
            private final Py.CompilationUnit t;

            @Override
            public List<JRightPadded<J.Import>> getImports() {
                return t.imports;
            }

            @Override
            public Py.CompilationUnit withImports(List<JRightPadded<J.Import>> imports) {
                return t.imports == imports ? t : new Py.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, null,
                        imports, t.statements, t.eof);
            }

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public Py.CompilationUnit withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new Py.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath,
                        t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.imports, statements, t.eof);
            }
        }
    }

    @Getter
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor
    final class ExpressionStatement implements Py, Expression, Statement {
        @With
        UUID id;

        @With
        Expression expression;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitExpressionStatement(this, p);
        }

        @Override
        public <P2 extends J> P2 withPrefix(Space space) {
            return (P2) withExpression(expression.withPrefix(space));
        }

        @Override
        public Space getPrefix() {
            return expression.getPrefix();
        }

        @Override
        public <P2 extends Tree> P2 withMarkers(Markers markers) {
            return (P2) withExpression(expression.withMarkers(markers));
        }

        @Override
        public Markers getMarkers() {
            return expression.getMarkers();
        }

        @Override
        public @Nullable JavaType getType() {
            return expression.getType();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) withExpression(expression.withType(type));
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class ExpressionTypeTree implements Py, Expression, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        J reference;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitExpressionTypeTree(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            if (reference instanceof Expression) {
                return ((Expression) reference).getType();
            }
            if (reference instanceof TypedTree) {
                return ((TypedTree) reference).getType();
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ExpressionTypeTree withType(@Nullable JavaType type) {
            if (reference instanceof Expression) {
                return withReference(((Expression) reference).withType(type));
            }
            if (reference instanceof TypedTree) {
                return withReference(((TypedTree) reference).withType(type));
            }
            return this;
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @Getter
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor
    final class StatementExpression implements Py, Expression, Statement {
        @With
        UUID id;

        @With
        Statement statement;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitStatementExpression(this, p);
        }

        @Override
        public <P2 extends J> P2 withPrefix(Space space) {
            return (P2) withStatement(statement.withPrefix(space));
        }

        @Override
        public Space getPrefix() {
            return statement.getPrefix();
        }

        @Override
        public <P2 extends Tree> P2 withMarkers(Markers markers) {
            return (P2) withStatement(statement.withMarkers(markers));
        }

        @Override
        public Markers getMarkers() {
            return statement.getMarkers();
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            return (T) this;
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MultiImport implements Py, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        @Nullable
        JRightPadded<NameTree> from;

        public @Nullable NameTree getFrom() {
            return from == null ? null : from.getElement();
        }

        public MultiImport withFrom(NameTree from) {
            return getPadding().withFrom(JRightPadded.withElement(this.from, from));
        }

        @Getter
        @With
        boolean parenthesized;

        JContainer<J.Import> names;

        public List<J.Import> getNames() {
            return this.names.getElements();
        }

        public MultiImport withNames(List<J.Import> names) {
            return getPadding().withNames(JContainer.withElements(this.names, names));
        }

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitMultiImport(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final MultiImport t;

            public @Nullable JRightPadded<NameTree> getFrom() {
                return t.from;
            }

            public MultiImport withFrom(@Nullable JRightPadded<NameTree> from) {
                return t.from == from ? t : new MultiImport(t.id, t.prefix, t.markers, from, t.parenthesized, t.names);
            }

            public JContainer<Import> getNames() {
                return t.names;
            }

            public MultiImport withNames(JContainer<J.Import> names) {
                return t.names == names ? t : new MultiImport(t.id, t.prefix, t.markers, t.from, t.parenthesized, names);
            }
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class KeyValue implements Py, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @Getter
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        JRightPadded<Expression> key;

        public Expression getKey() {
            return key.getElement();
        }

        public KeyValue withKey(@Nullable Expression key) {
            return getPadding().withKey(JRightPadded.withElement(this.key, key));
        }

        @Getter
        @With
        Expression value;

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitKeyValue(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final KeyValue t;

            public @Nullable JRightPadded<Expression> getKey() {
                return t.key;
            }

            public KeyValue withKey(@Nullable JRightPadded<Expression> key) {
                return t.key == key ? t : new KeyValue(t.id, t.prefix, t.markers, key, t.value, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class DictLiteral implements Py, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @Getter
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        JContainer<Expression> elements;

        public List<Expression> getElements() {
            return elements.getElements();
        }

        public DictLiteral withElements(List<Expression> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitDictLiteral(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final DictLiteral t;

            public JContainer<Expression> getElements() {
                return t.elements;
            }

            public DictLiteral withElements(JContainer<Expression> elements) {
                return t.elements == elements ? t : new DictLiteral(t.id, t.prefix, t.markers, elements, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CollectionLiteral implements Py, Expression, TypedTree {

        public enum Kind {
            LIST, SET, TUPLE
        }

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @Getter
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        @Getter
        @With
        Kind kind;

        JContainer<Expression> elements;

        public List<Expression> getElements() {
            return elements.getElements();
        }

        public CollectionLiteral withElements(List<Expression> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitCollectionLiteral(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final CollectionLiteral t;

            public JContainer<Expression> getElements() {
                return t.elements;
            }

            public CollectionLiteral withElements(JContainer<Expression> elements) {
                return t.elements == elements ? t : new CollectionLiteral(t.id, t.prefix, t.markers, t.kind, elements, t.type);
            }
        }
    }

    @Getter
    @With
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    final class FormattedString implements Py, Expression, TypedTree {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        Markers markers;

        String delimiter;

        List<Expression> parts;

        @Override
        public JavaType getType() {
            return JavaType.Primitive.String;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitFormattedString(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Value implements Py, Expression, TypedTree {

            public enum Conversion {
                STR, REPR, ASCII
            }

            @Nullable
            @NonFinal
            transient WeakReference<Padding> padding;

            @Getter
            @With
            @EqualsAndHashCode.Include
            UUID id;

            @Getter
            @With
            Space prefix;

            @Getter
            @With
            Markers markers;

            JRightPadded<Expression> expression;

            public Expression getExpression() {
                return expression.getElement();
            }

            public Value withExpression(Expression expression) {
                return getPadding().withExpression(JRightPadded.withElement(this.expression, expression));
            }

            @Nullable
            JRightPadded<Boolean> debug;

            public boolean isDebug() {
                return debug != null && debug.getElement();
            }

            public Value withDebug(boolean debug) {
                return getPadding().withDebug(debug ? JRightPadded.withElement(this.debug, true) : null);
            }

            @Nullable
            @Getter
            @With
            Conversion conversion;

            @Nullable
            @Getter
            @With
            Expression format;

            @Override
            public JavaType getType() {
                return JavaType.Primitive.String;
            }

            @Override
            public <T extends J> T withType(@Nullable JavaType type) {
                //noinspection unchecked
                return (T) this;
            }

            @Override
            public <P> J acceptPython(PythonVisitor<P> v, P p) {
                return v.visitFormattedStringValue(this, p);
            }

            @Override
            @Transient
            public CoordinateBuilder.Expression getCoordinates() {
                return new CoordinateBuilder.Expression(this);
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final Value t;

                public JRightPadded<Expression> getExpression() {
                    return t.expression;
                }

                public Value withExpression(JRightPadded<Expression> expression) {
                    return t.expression == expression ? t : new Value(t.id, t.prefix, t.markers, expression, t.debug, t.conversion, t.format);
                }

                public @Nullable JRightPadded<Boolean> getDebug() {
                    return t.debug;
                }

                public Value withDebug(@Nullable JRightPadded<Boolean> debug) {
                    return t.debug == debug ? t : new Value(t.id, t.prefix, t.markers, t.expression, debug, t.conversion, t.format);
                }
            }
        }
    }

    @Getter
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor
    final class Pass implements Py, Statement {
        @With
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitPass(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TrailingElseWrapper implements Py, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
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
        Statement statement;

        JLeftPadded<Block> elseBlock;

        public Block getElseBlock() {
            return elseBlock.getElement();
        }

        public TrailingElseWrapper withElseBlock(Block elseBlock) {
            return this.getPadding().withElseBlock(JLeftPadded.withElement(this.elseBlock, elseBlock));
        }

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitTrailingElseWrapper(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TrailingElseWrapper t;

            public JLeftPadded<Block> getElseBlock() {
                return t.elseBlock;
            }

            public TrailingElseWrapper withElseBlock(JLeftPadded<Block> elseBlock) {
                return t.elseBlock == elseBlock ?
                        t :
                        new TrailingElseWrapper(t.padding, t.id, t.prefix, t.markers, t.statement, elseBlock);
            }
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    final class ComprehensionExpression implements Py, Expression {

        public enum Kind {
            LIST, SET, DICT, GENERATOR
        }

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Kind kind;

        @With
        Expression result;

        @With
        List<Clause> clauses;

        @With
        Space suffix;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitComprehensionExpression(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Getter
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false)
        @RequiredArgsConstructor
        public static final class Condition implements Py {
            @With
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            Expression expression;

            @Override
            public <P> J acceptPython(PythonVisitor<P> v, P p) {
                return v.visitComprehensionCondition(this, p);
            }

        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Clause implements Py {
            @Nullable
            @NonFinal
            transient WeakReference<Padding> padding;

            @With
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
            JRightPadded<Boolean> async;

            public boolean isAsync() {
                return async != null && async.getElement();
            }

            public Clause withAsync(boolean async) {
                return getPadding().withAsync(JRightPadded.withElement(this.async, async));
            }

            @With
            @Getter
            Expression iteratorVariable;

            JLeftPadded<Expression> iteratedList;

            @With
            @Getter
            @Nullable
            List<Condition> conditions;

            public Expression getIteratedList() {
                return this.iteratedList.getElement();
            }

            public Clause withIteratedList(Expression expression) {
                return this.getPadding().withIteratedList(this.iteratedList.withElement(expression));
            }

            @Override
            public <P> J acceptPython(PythonVisitor<P> v, P p) {
                return v.visitComprehensionClause(this, p);
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final Clause t;

                public JLeftPadded<Expression> getIteratedList() {
                    return t.iteratedList;
                }

                public Clause withIteratedList(JLeftPadded<Expression> iteratedList) {
                    return t.iteratedList == iteratedList ? t : new Clause(t.id, t.prefix, t.markers, t.async, t.iteratorVariable, iteratedList, t.conditions);
                }

                public @Nullable JRightPadded<Boolean> getAsync() {
                    return t.async;
                }

                public Clause withAsync(JRightPadded<Boolean> async) {
                    return t.async == async ? t : new Clause(t.id, t.prefix, t.markers, async, t.iteratorVariable, t.iteratedList, t.conditions);
                }
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TypeAlias implements Py, Statement, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
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
        J.Identifier name;

        JLeftPadded<J> value;

        public J getValue() {
            return value.getElement();
        }

        public TypeAlias withValue(J value) {
            return getPadding().withValue(this.value.withElement(value));
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitTypeAlias(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TypeAlias t;

            public JLeftPadded<J> getValue() {
                return t.value;
            }

            public TypeAlias withValue(JLeftPadded<J> assignment) {
                return t.value == assignment ? t : new TypeAlias(t.id, t.prefix, t.markers, t.name, assignment, t.type);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class YieldFrom implements Py, Expression {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Expression expression;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitYieldFrom(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class UnionType implements Py, Expression, TypeTree {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        List<JRightPadded<Expression>> types;

        public List<Expression> getTypes() {
            return JRightPadded.getElements(types);
        }

        public UnionType withTypes(List<Expression> types) {
            return getPadding().withTypes(JRightPadded.withElements(this.types, types));
        }

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitUnionType(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final UnionType t;

            public List<JRightPadded<Expression>> getTypes() {
                return t.types;
            }

            public UnionType withTypes(List<JRightPadded<Expression>> types) {
                return t.types == types ? t : new UnionType(t.id, t.prefix, t.markers, types, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class VariableScope implements Py, Statement {

        public enum Kind {
            GLOBAL,
            NONLOCAL,
        }

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
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
        Kind kind;

        List<JRightPadded<J.Identifier>> names;

        public List<J.Identifier> getNames() {
            return JRightPadded.getElements(names);
        }

        public VariableScope withNames(List<J.Identifier> names) {
            return this.getPadding().withNames(JRightPadded.withElements(this.names, names));
        }


        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitVariableScope(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final VariableScope t;

            public List<JRightPadded<J.Identifier>> getNames() {
                return t.names;
            }

            public VariableScope withNames(List<JRightPadded<J.Identifier>> names) {
                return names == t.names ?
                        t :
                        new VariableScope(t.id, t.prefix, t.markers, t.kind, names);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Del implements Py, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        List<JRightPadded<Expression>> targets;

        public List<Expression> getTargets() {
            return JRightPadded.getElements(targets);
        }

        public Del withTargets(List<Expression> expressions) {
            return this.getPadding().withTargets(JRightPadded.withElements(this.targets, expressions));
        }

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitDel(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Del t;

            public List<JRightPadded<Expression>> getTargets() {
                return t.targets;
            }

            public Del withTargets(List<JRightPadded<Expression>> expressions) {
                return expressions == t.targets ?
                        t :
                        new Del(t.id, t.prefix, t.markers, expressions);
            }
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    final class SpecialParameter implements Py, TypeTree {

        public enum Kind {
            KWARGS,
            ARGS,
        }

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Kind kind;

        @With
        @Nullable
        TypeHint typeHint;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitSpecialParameter(this, p);
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    final class Star implements Py, Expression, TypeTree {

        public enum Kind {
            LIST,
            DICT,
        }

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Kind kind;

        @With
        Expression expression;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitStar(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class NamedArgument implements Py, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
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
        J.Identifier name;

        JLeftPadded<Expression> value;

        public Expression getValue() {
            return value.getElement();
        }

        public NamedArgument withValue(Expression value) {
            return getPadding().withValue(JLeftPadded.withElement(this.value, value));
        }

        @With
        @Getter
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitNamedArgument(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final NamedArgument t;

            public JLeftPadded<Expression> getValue() {
                return t.value;
            }

            public NamedArgument withValue(JLeftPadded<Expression> value) {
                return value == t.value ?
                        t :
                        new NamedArgument(t.id, t.prefix, t.markers, t.name, value, t.type);
            }
        }
    }


    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    final class TypeHintedExpression implements Py, Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression expression;

        @With
        TypeHint typeHint;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitTypeHintedExpression(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ErrorFrom implements Py, Expression {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
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
        Expression error;

        JLeftPadded<Expression> from;

        @With
        @Getter
        @Nullable
        JavaType type;

        public Expression getFrom() {
            return from.getElement();
        }

        public ErrorFrom withFrom(Expression from) {
            return this.getPadding().withFrom(this.from.withElement(from));
        }


        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitErrorFrom(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ErrorFrom t;

            public JLeftPadded<Expression> getFrom() {
                return t.from;
            }

            public ErrorFrom withFrom(JLeftPadded<Expression> from) {
                return from == t.from ?
                        t :
                        new ErrorFrom(t.id, t.prefix, t.markers, t.error, from, t.type);
            }
        }

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MatchCase implements Py, Expression {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
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
        Pattern pattern;

        @Nullable
        JLeftPadded<Expression> guard;

        @With
        @Getter
        @Nullable
        JavaType type;

        public @Nullable Expression getGuard() {
            return guard == null ? null : guard.getElement();
        }

        public MatchCase withGuard(Expression guard) {
            return this.getPadding().withGuard(
                    JLeftPadded.withElement(this.guard, guard)
            );
        }

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitMatchCase(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        @RequiredArgsConstructor
        public static class Padding {
            private final MatchCase t;

            public @Nullable JLeftPadded<Expression> getGuard() {
                return t.guard;
            }

            public MatchCase withGuard(JLeftPadded<Expression> guard) {
                return guard == t.guard ?
                        t :
                        new MatchCase(t.id, t.prefix, t.markers, t.pattern, guard, null);
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public final static class Pattern implements Py, Expression {
            public enum Kind {
                AS,
                CAPTURE,
                CLASS,
                DOUBLE_STAR,
                GROUP,
                KEY_VALUE,
                KEYWORD,
                LITERAL,
                MAPPING,
                OR,
                SEQUENCE,
                SEQUENCE_LIST,
                SEQUENCE_TUPLE,
                STAR,
                VALUE,
                WILDCARD,
            }

            @Nullable
            @NonFinal
            transient WeakReference<Padding> padding;

            @With
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
            Kind kind;

            JContainer<J> children;

            @With
            @Getter
            @Nullable
            JavaType type;

            public List<J> getChildren() {
                return children.getElements();
            }

            public Pattern withChildren(List<J> children) {
                return this.getPadding().withChildren(
                        JContainer.withElements(this.children, children)
                );
            }

            @Override
            public <P> J acceptPython(PythonVisitor<P> v, P p) {
                return v.visitMatchCasePattern(this, p);
            }

            @Override
            public CoordinateBuilder.Expression getCoordinates() {
                return new CoordinateBuilder.Expression(this);
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }


            @RequiredArgsConstructor
            public static class Padding {
                private final Pattern t;

                public JContainer<J> getChildren() {
                    return t.children;
                }

                public Pattern withChildren(JContainer<J> children) {
                    return children == t.children ?
                            t :
                            new Pattern(t.id, t.prefix, t.markers, t.kind, children, t.type);
                }
            }

        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Slice implements Py, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @Getter
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        @Nullable
        JRightPadded<Expression> start;

        public @Nullable Expression getStart() {
            return start != null ? start.getElement() : null;
        }

        public Slice withStart(@Nullable Expression start) {
            return getPadding().withStart(JRightPadded.withElement(this.start, start));
        }

        @Nullable
        JRightPadded<Expression> stop;

        public @Nullable Expression getStop() {
            return stop != null ? stop.getElement() : null;
        }

        public Slice withStop(@Nullable Expression stop) {
            return getPadding().withStop(JRightPadded.withElement(this.stop, stop));
        }

        @Nullable
        JRightPadded<Expression> step;

        public @Nullable Expression getStep() {
            return step != null ? step.getElement() : null;
        }

        public Slice withStep(@Nullable Expression step) {
            return getPadding().withStep(JRightPadded.withElement(this.step, step));
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> J acceptPython(PythonVisitor<P> v, P p) {
            return v.visitSlice(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Slice t;

            public @Nullable JRightPadded<Expression> getStart() {
                return t.start;
            }

            public Slice withStart(@Nullable JRightPadded<Expression> start) {
                return t.start == start ? t : new Slice(t.id, t.prefix, t.markers, start, t.stop, t.step);
            }

            public @Nullable JRightPadded<Expression> getStop() {
                return t.stop;
            }

            public Slice withStop(@Nullable JRightPadded<Expression> stop) {
                return t.stop == stop ? t : new Slice(t.id, t.prefix, t.markers, t.start, stop, t.step);
            }

            public @Nullable JRightPadded<Expression> getStep() {
                return t.step;
            }

            public Slice withStep(@Nullable JRightPadded<Expression> step) {
                return t.step == step ? t : new Slice(t.id, t.prefix, t.markers, t.start, t.stop, step);
            }
        }
    }

}
