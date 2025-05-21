/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.javascript.JavaScriptVisitor;
import org.openrewrite.marker.Markers;

import java.beans.Transient;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

/**
 * JSX elements for JavaScript AST.
 */
@SuppressWarnings("unused")
public interface JSX extends JS {

    /**
     * Represents a JSX tag. There are two variants:
     * 1. Self-closing tag: Has selfClosing property, no children or closingName
     * 2. Tag with children: Has children and closingName properties, no selfClosing
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Tag implements JS, Statement, Expression {

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

        JLeftPadded<String> openName;

        public String getOpenName() {
            return openName.getElement();
        }

        @Getter
        @With
        Space afterName;

        List<JRightPadded<JSX>> attributes;

        public List<JSX> getAttributes() {
            return JRightPadded.getElements(attributes);
        }

        public Tag withAttributes(List<JSX> attributes) {
            return getPadding().withAttributes(JRightPadded.withElements(this.attributes, attributes));
        }

        @Getter
        @With
        @Nullable
        Space selfClosing;

        @Nullable
        List<JRightPadded<J>> children;

        @Nullable
        public List<J> getChildren() {
            return children == null ? null : JRightPadded.getElements(children);
        }

        @Nullable
        JLeftPadded<String> closingName;

        @Nullable
        public String getClosingName() {
            return closingName == null ? null : closingName.getElement();
        }

        @With
        @Nullable
        JavaType type;

        @Override
        public @Nullable JavaType getType() {
            return type;
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJsxTag(this, p);
        }

        public boolean isSelfClosing() {
            return selfClosing != null;
        }

        public boolean hasChildren() {
            return children != null && closingName != null;
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
            private final JSX.Tag t;

            public JLeftPadded<String> getOpenName() {
                return t.openName;
            }

            public Tag withOpenName(JLeftPadded<String> openName) {
                return t.openName == openName ? t : new Tag(t.id, t.prefix, t.markers, openName, t.afterName, t.attributes, t.selfClosing, t.children, t.closingName, t.type);
            }

            public List<JRightPadded<JSX>> getAttributes() {
                return t.attributes;
            }

            public Tag withAttributes(List<JRightPadded<JSX>> attributes) {
                return t.attributes == attributes ? t : new Tag(t.id, t.prefix, t.markers, t.openName, t.afterName, attributes, t.selfClosing, t.children, t.closingName, t.type);
            }

            @Nullable
            public List<JRightPadded<J>> getChildren() {
                return t.children;
            }

            public Tag withChildren(@Nullable List<JRightPadded<J>> children) {
                return t.children == children ? t : new Tag(t.id, t.prefix, t.markers, t.openName, t.afterName, t.attributes, t.selfClosing, children, t.closingName, t.type);
            }

            @Nullable
            public JLeftPadded<String> getClosingName() {
                return t.closingName;
            }

            public Tag withClosingName(@Nullable JLeftPadded<String> closingName) {
                return t.closingName == closingName ? t : new Tag(t.id, t.prefix, t.markers, t.openName, t.afterName, t.attributes, t.selfClosing, t.children, closingName, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Attribute implements JSX, Statement {
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
        J.Identifier key;

        JLeftPadded<Expression> value;

        public Expression getValue() {
            return value.getElement();
        }

        public Attribute withValue(Expression value) {
            return getPadding().withValue(this.value.withElement(value));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJsxAttribute(this, p);
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
            private final JSX.Attribute t;

            public JLeftPadded<Expression> getValue() {
                return t.value;
            }

            public Attribute withValue(JLeftPadded<Expression> value) {
                return t.value == value ? t : new Attribute(t.id, t.prefix, t.markers, t.key, value);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class SpreadAttribute implements JSX, Statement {
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
        Space dots;

        JRightPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public SpreadAttribute withExpression(Expression expression) {
            return getPadding().withExpression(this.expression.withElement(expression));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJsxSpreadAttribute(this, p);
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
            private final JSX.SpreadAttribute t;

            public JRightPadded<Expression> getExpression() {
                return t.expression;
            }

            public SpreadAttribute withExpression(JRightPadded<Expression> expression) {
                return t.expression == expression ? t : new SpreadAttribute(t.id, t.prefix, t.markers, t.dots, expression);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class EmbeddedExpression implements JSX, Statement {
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

        public EmbeddedExpression withExpression(Expression expression) {
            return getPadding().withExpression(this.expression.withElement(expression));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJsxEmbeddedExpression(this, p);
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
            private final JSX.EmbeddedExpression t;

            public JRightPadded<Expression> getExpression() {
                return t.expression;
            }

            public EmbeddedExpression withExpression(JRightPadded<Expression> expression) {
                return t.expression == expression ? t : new EmbeddedExpression(t.id, t.prefix, t.markers, expression);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class NamespacedName implements JSX, Expression {
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
        J.Identifier namespace;

        JLeftPadded<J.Identifier> name;

        public J.Identifier getName() {
            return name.getElement();
        }

        public NamespacedName withName(J.Identifier name) {
            return getPadding().withName(this.name.withElement(name));
        }

        @With
        @Nullable
        JavaType type;

        @Override
        public @Nullable JavaType getType() {
            return type;
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJsxNamespacedName(this, p);
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
            private final JSX.NamespacedName t;

            public JLeftPadded<J.Identifier> getName() {
                return t.name;
            }

            public NamespacedName withName(JLeftPadded<J.Identifier> name) {
                return t.name == name ? t : new NamespacedName(t.id, t.prefix, t.markers, t.namespace, name, t.type);
            }
        }
    }
}
