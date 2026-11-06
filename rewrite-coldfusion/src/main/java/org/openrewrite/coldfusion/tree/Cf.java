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
package org.openrewrite.coldfusion.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.coldfusion.ColdFusionVisitor;
import org.openrewrite.coldfusion.internal.ColdFusionPrinter;
import org.openrewrite.coldfusion.service.ColdFusionAutoFormatService;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.beans.Transient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

/**
 * The ColdFusion (CFML) LST. Script syntax reuses the {@link J} model; tag syntax, interpolation and
 * the operators and literals Java has no counterpart for are {@code Cf} types.
 */
@SuppressWarnings("unused")
public interface Cf extends J {

    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        //noinspection unchecked
        return (R) acceptColdFusion(v.adapt(ColdFusionVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(ColdFusionVisitor.class);
    }

    default <P> @Nullable J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    /**
     * A {@code .cfm} or {@code .cfc} file. A template's statements are its tags, text and
     * interpolations; a script component's are its imports and the {@link J.ClassDeclaration} the file
     * declares, which is named for the file. Comments, in either, are in the space before what follows
     * them, or in {@link #eof}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CompilationUnit implements Cf, JavaSourceFile, SourceFile {
        @Nullable
        @NonFinal
        transient SoftReference<TypesInUse> typesInUse;

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
        Path sourcePath;

        @Getter
        @With
        @Nullable
        FileAttributes fileAttributes;

        @Nullable // for backwards compatibility
        String charsetName;

        @Getter
        @With
        boolean charsetBomMarked;

        @Getter
        @With
        @Nullable
        Checksum checksum;

        List<JRightPadded<Statement>> statements;

        @Getter
        @With
        Space eof;

        /**
         * The type the file is: the component it declares, which its {@link J.ClassDeclaration} has too, or the
         * template itself, named for its path as {@code includes/udfs.cfm} is {@code includes.udfs}. Null when the
         * file is not typed.
         */
        @Getter
        @With
        JavaType.@Nullable FullyQualified type;

        public List<Statement> getStatements() {
            return JRightPadded.getElements(statements);
        }

        public Cf.CompilationUnit withStatements(List<Statement> statements) {
            return getPadding().withStatements(JRightPadded.withElements(this.statements, statements));
        }

        @Override
        public Charset getCharset() {
            return charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        public Cf.CompilationUnit withCharsetName(String charsetName) {
            return charsetName.equals(this.charsetName) ? this : new Cf.CompilationUnit(
                    typesInUse, padding, id, prefix, markers, sourcePath, fileAttributes,
                    charsetName, charsetBomMarked, checksum, statements, eof, type);
        }

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new ColdFusionPrinter<>();
        }

        /**
         * Whether the tree prints the file's bytes, its byte order mark among them, which comparing the text read with
         * the text printed cannot tell.
         */
        @Override
        public boolean printEqualsInput(Parser.Input input, ExecutionContext ctx) {
            ByteArrayOutputStream read = new ByteArrayOutputStream();
            try (EncodingDetectingInputStream is = input.getSource(ctx)) {
                byte[] buffer = new byte[8192];
                for (int n; (n = is.read(buffer, 0, buffer.length)) != -1; ) {
                    read.write(buffer, 0, n);
                }
                byte[] bytes = read.toByteArray();
                if (is.isCharsetBomMarked()) {
                    byte[] marked = new byte[bytes.length + 3];
                    marked[0] = (byte) 0xEF;
                    marked[1] = (byte) 0xBB;
                    marked[2] = (byte) 0xBF;
                    System.arraycopy(bytes, 0, marked, 3, bytes.length);
                    bytes = marked;
                }
                return Arrays.equals(printAllAsBytes(), bytes);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /**
         * CFML's own {@link AutoFormatService}, which {@code autoFormat} and {@code autoIndent} reach, since Java's
         * would rewrite CFML's whitespace into Java's conventions.
         */
        @Override
        @SuppressWarnings("unchecked")
        public <S1, T extends S1> T service(Class<S1> service) {
            String serviceName = service.getName();
            try {
                Class<S1> serviceClass;
                if (ColdFusionAutoFormatService.class.getName().equals(serviceName)) {
                    serviceClass = service;
                } else if (AutoFormatService.class.getName().equals(serviceName)) {
                    // loaded where the service asked for is, as a recipe's class loader may differ from this one
                    serviceClass = (Class<S1>) service.getClassLoader().loadClass(ColdFusionAutoFormatService.class.getName());
                } else {
                    return JavaSourceFile.super.service(service);
                }
                return (T) serviceClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
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
            return this;
        }

        @Override
        public List<Import> getImports() {
            return emptyList();
        }

        @Override
        public JavaSourceFile withImports(List<Import> imports) {
            return this;
        }

        @Override
        public List<ClassDeclaration> getClasses() {
            List<ClassDeclaration> classes = new ArrayList<>(1);
            for (Statement statement : getStatements()) {
                if (statement instanceof ClassDeclaration) {
                    classes.add((ClassDeclaration) statement);
                }
            }
            return classes;
        }

        @Override
        public JavaSourceFile withClasses(List<ClassDeclaration> classes) {
            Iterator<ClassDeclaration> replacements = classes.iterator();
            List<Statement> statements = ListUtils.map(getStatements(), statement -> !(statement instanceof ClassDeclaration) ?
                    statement : replacements.hasNext() ? replacements.next() : null);
            while (replacements.hasNext()) {
                statements = ListUtils.concat(statements, replacements.next());
            }
            return withStatements(statements);
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
        public static class Padding implements JavaSourceFile.Padding {
            private final Cf.CompilationUnit t;

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public Cf.CompilationUnit withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new Cf.CompilationUnit(t.typesInUse, t.padding, t.id,
                        t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked,
                        t.checksum, statements, t.eof, t.type);
            }

            @Override
            public List<JRightPadded<Import>> getImports() {
                return emptyList();
            }

            @Override
            public JavaSourceFile withImports(List<JRightPadded<Import>> imports) {
                return t;
            }
        }
    }

    /**
     * Template text between tags, written to the output as it stands, from its first character that
     * is not whitespace to its last. The whitespace and {@code <!--- --->} comments before it are its
     * prefix. In a {@code <cfoutput>} body, {@code ##} stays as written. HTML comments are text.
     */
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Text implements Cf, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String text;

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitText(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * A CFML tag such as {@code <cfquery name="q">...</cfquery>}. The tags that take an expression
     * rather than attributes ({@code cfset}, {@code cfif}, {@code cfelseif}, {@code cfreturn}) hold it
     * in {@link #expression}. A tag closed by a matching end tag has a {@link #body} written without
     * braces, and a {@link #closing}; {@code <cfelse>} and {@code <cfelseif>} are tags in the body of
     * their {@code <cfif>}. The space before the end tag is the end of the body.
     */
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Tag implements Cf, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The name as written, e.g. {@code cfquery}, {@code CFSET} or {@code cf_layout}.
         */
        String name;

        List<TagAttribute> attributes;

        /**
         * The expression of a {@code cfif}, {@code cfelseif} or {@code cfreturn}, or what a {@code cfset}
         * holds: an assignment, a call or a {@code var} declaration.
         */
        @Nullable
        J expression;

        /**
         * The space before {@code >} or {@code />}.
         */
        Space beforeEnd;

        boolean selfClosing;

        J.@Nullable Block body;

        @Nullable
        Closing closing;

        public boolean isNamed(String name) {
            return this.name.equalsIgnoreCase(name);
        }

        public @Nullable TagAttribute getAttribute(String name) {
            for (TagAttribute attribute : attributes) {
                if (attribute.getName().getSimpleName().equalsIgnoreCase(name)) {
                    return attribute;
                }
            }
            return null;
        }

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitTag(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        /**
         * The end tag: {@code </} the name as written, then the space before {@code >}.
         */
        @Value
        @With
        public static class Closing {
            String name;
            Space beforeEnd;
        }
    }

    /**
     * An attribute of a tag, a component, a property, a function or a tag written as a script
     * statement: {@code name="value"}, {@code name=value} or a bare {@code name}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TagAttribute implements Cf {
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
        J.Identifier name;

        @Nullable
        JLeftPadded<Expression> value;

        public @Nullable Expression getValue() {
            return value == null ? null : value.getElement();
        }

        public TagAttribute withValue(@Nullable Expression value) {
            if (value == null) {
                return getPadding().withValue(null);
            }
            return getPadding().withValue(this.value == null ? JLeftPadded.build(value) : this.value.withElement(value));
        }

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitTagAttribute(this, p);
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
            private final TagAttribute t;

            public @Nullable JLeftPadded<Expression> getValue() {
                return t.value;
            }

            public TagAttribute withValue(@Nullable JLeftPadded<Expression> value) {
                return t.value == value ? t : new TagAttribute(t.id, t.prefix, t.markers, t.name, value);
            }
        }
    }

    /**
     * An expression in pound signs: {@code #name#} in a {@code <cfoutput>} body, a string or an
     * attribute value, or around an expression in script. {@link #after} is the space before the
     * closing {@code #}.
     */
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Interpolation implements Cf, Expression, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Expression tree;
        Space after;

        @Override
        public @Nullable JavaType getType() {
            return tree.getType();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) withTree(tree.withType(type));
        }

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitInterpolation(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * A string with interpolated expressions, {@code "Hello #name#!"}. The strings are literal
     * segments ({@link J.Literal}, value source as written, {@code ""} and {@code ##} included) and
     * {@link Interpolation}s. An unquoted tag attribute value has an empty delimiter.
     */
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class StringTemplate implements Cf, Expression, TypedTree {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String delimiter;
        List<J> strings;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitStringTemplate(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * The binary operators Java has no counterpart for. CFML's spelled-out forms of Java's
     * operators ({@code EQ}, {@code GT}, {@code AND}, {@code MOD}) are {@link J.Binary} with an
     * {@code OperatorSpelling} marker.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Binary implements Cf, Expression, TypedTree {
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
        Expression left;

        JLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElement();
        }

        public Cf.Binary withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        @Getter
        @With
        Expression right;

        @Getter
        @With
        @Nullable
        JavaType type;

        public enum Type {
            /** {@code &} */
            Concat,
            /** {@code ^} */
            Power,
            /** {@code \} */
            IntegerDivision,
            /** {@code CONTAINS}, {@code CT} */
            Contains,
            /** {@code DOES NOT CONTAIN}, {@code NCT} */
            DoesNotContain,
            /** {@code EQV} */
            Equivalence,
            /** {@code IMP} */
            Implication,
            /** {@code XOR} */
            Xor,
            /** {@code ?:} */
            Elvis,
            /** {@code ===} */
            Identical,
            /** {@code !==} */
            NotIdentical
        }

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitBinary(this, p);
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
            private final Cf.Binary t;

            public JLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public Cf.Binary withOperator(JLeftPadded<Type> operator) {
                return t.operator == operator ? t : new Cf.Binary(t.id, t.prefix, t.markers, t.left, operator, t.right, t.type);
            }
        }
    }

    /**
     * {@code &=}, which {@link J.AssignmentOperation} would print as a bitwise and.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class AssignmentOperation implements Cf, Statement, Expression, TypedTree {
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
        Expression variable;

        JLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElement();
        }

        public Cf.AssignmentOperation withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        @Getter
        @With
        Expression assignment;

        @Getter
        @With
        @Nullable
        JavaType type;

        public enum Type {
            /** {@code &=} */
            Concat
        }

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitAssignmentOperation(this, p);
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
            private final Cf.AssignmentOperation t;

            public JLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public Cf.AssignmentOperation withOperator(JLeftPadded<Type> operator) {
                return t.operator == operator ? t : new Cf.AssignmentOperation(t.id, t.prefix, t.markers, t.variable, operator, t.assignment, t.type);
            }
        }
    }

    /**
     * A struct literal, {@code {a: 1, "b" = 2}}, or an ordered one, {@code [a: 1, b: 2]}. Its
     * entries are {@link KeyValue}s. An empty struct holds a single {@link J.Empty}; the empty
     * ordered struct {@code [:]} holds one {@link KeyValue} whose key and value are {@link J.Empty}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class StructLiteral implements Cf, Expression, TypedTree {
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
        boolean ordered;

        JContainer<Expression> entries;

        public List<Expression> getEntries() {
            return entries.getElements();
        }

        public Cf.StructLiteral withEntries(List<Expression> entries) {
            return getPadding().withEntries(JContainer.withElements(this.entries, entries));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitStructLiteral(this, p);
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
            private final Cf.StructLiteral t;

            public JContainer<Expression> getEntries() {
                return t.entries;
            }

            public Cf.StructLiteral withEntries(JContainer<Expression> entries) {
                return t.entries == entries ? t : new Cf.StructLiteral(t.id, t.prefix, t.markers, t.ordered, entries, t.type);
            }
        }
    }

    /**
     * A struct entry or a named argument: {@code key: value} or {@code key = value}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class KeyValue implements Cf, Expression {
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
        Expression key;

        JLeftPadded<Separator> separator;

        public Separator getSeparator() {
            return separator.getElement();
        }

        public Cf.KeyValue withSeparator(Separator separator) {
            return getPadding().withSeparator(this.separator.withElement(separator));
        }

        @Getter
        @With
        Expression value;

        public enum Separator {
            Colon,
            Equals
        }

        @Override
        public @Nullable JavaType getType() {
            return value.getType();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
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
            private final Cf.KeyValue t;

            public JLeftPadded<Separator> getSeparator() {
                return t.separator;
            }

            public Cf.KeyValue withSeparator(JLeftPadded<Separator> separator) {
                return t.separator == separator ? t : new Cf.KeyValue(t.id, t.prefix, t.markers, t.key, separator, t.value);
            }
        }
    }

    /**
     * A tag written as a script statement: {@code lock name="x" timeout=10 { ... }},
     * {@code param name="x" default="";}, {@code include "header.cfm";}, {@code abort;}. A tag that
     * takes an expression rather than attributes holds it in {@link #expression}.
     */
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class ScriptTag implements Cf, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        J.Identifier name;
        List<TagAttribute> attributes;

        @Nullable
        Expression expression;

        J.@Nullable Block body;

        public boolean isNamed(String name) {
            return this.name.getSimpleName().equalsIgnoreCase(name);
        }

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitScriptTag(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * A tag written as a call with a body, {@code cfloop(array=items, index="i") { ... }}. The same call
     * with no body is a plain {@link J.MethodInvocation}. The space before the call is this one's prefix.
     */
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class TagCall implements Cf, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        J.MethodInvocation call;
        J.Block body;

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitTagCall(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * A closure written with attributes after its parameter list, {@code function() localmode="classic" {}},
     * which {@link J.Lambda} has no place for. A closure without attributes is a plain {@link J.Lambda}.
     * The attributes are annotations, as a function's are. The space before the closure is this one's prefix.
     */
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Closure implements Cf, Expression, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        J.Lambda lambda;
        List<J.Annotation> attributes;

        @Override
        public @Nullable JavaType getType() {
            return lambda.getType();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) withLambda(lambda.withType(type));
        }

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitClosure(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * A type named by a string, which CFML allows wherever it allows a dotted name: the type of
     * {@code catch ("MyApp.NotFound" e)}, or the component of {@code new "models.#name#"()}. The name
     * is a {@link J.Literal} or a {@link StringTemplate}.
     */
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class TypeLiteral implements Cf, TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Expression name;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitTypeLiteral(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * A call of something other than a name: {@code handlers[key]()}, {@code getFactory()()}.
     * A call of a name or a member is a {@link J.MethodInvocation}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Invocation implements Cf, Expression, Statement, TypedTree {
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
        Expression target;

        JContainer<Expression> arguments;

        public List<Expression> getArguments() {
            return arguments.getElements();
        }

        public Cf.Invocation withArguments(List<Expression> arguments) {
            return getPadding().withArguments(JContainer.withElements(this.arguments, arguments));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitInvocation(this, p);
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
            private final Cf.Invocation t;

            public JContainer<Expression> getArguments() {
                return t.arguments;
            }

            public Cf.Invocation withArguments(JContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new Cf.Invocation(t.id, t.prefix, t.markers, t.target, arguments, t.type);
            }
        }
    }

    /**
     * An expression in statement position whose J type is not a {@link Statement}, such as a bare
     * literal or a binary expression.
     */
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class ExpressionStatement implements Cf, Expression, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Expression expression;

        @Override
        public Space getPrefix() {
            return expression.getPrefix();
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            //noinspection unchecked
            return (J2) withExpression(expression.withPrefix(space));
        }

        @Override
        public Markers getMarkers() {
            return expression.getMarkers();
        }

        @Override
        public <J2 extends Tree> J2 withMarkers(Markers markers) {
            //noinspection unchecked
            return (J2) withExpression(expression.withMarkers(markers));
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

        @Override
        public <P> J acceptColdFusion(ColdFusionVisitor<P> v, P p) {
            return v.visitExpressionStatement(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }
}
