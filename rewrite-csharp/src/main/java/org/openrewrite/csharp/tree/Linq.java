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
package org.openrewrite.csharp.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.csharp.CSharpVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Statement;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

public interface Linq extends Cs {

    interface SelectOrGroupClause extends Linq {
    }

    interface QueryClause extends Linq {
    }

    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class QueryExpression implements Linq, Expression {
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
        FromClause fromClause;

        @With
        @Getter
        QueryBody body;

        @Override
        public @Nullable JavaType getType() {
            return fromClause.getType();
        }

        @Override
        public QueryExpression withType(@Nullable JavaType type) {
            return withFromClause(fromClause.withType(type));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitQueryExpression(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class QueryBody implements Linq {
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
        List<QueryClause> clauses;

        @With
        @Getter
        @Nullable
        SelectOrGroupClause selectOrGroup;

        @With
        @Getter
        @Nullable
        QueryContinuation continuation;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitQueryBody(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class FromClause implements Linq, QueryClause, Expression {
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
        @Nullable
        TypeTree typeIdentifier;

        JRightPadded<J.Identifier> identifier;

        @With
        @Getter
        Expression expression;

        public Expression getIdentifier() {
            return identifier.getElement();
        }

        public FromClause withIdentifier(J.Identifier identifier) {
            return getPadding().withIdentifier(this.identifier.withElement(identifier));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitFromClause(this, p);
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
        public @Nullable JavaType getType() {
            return expression.getType();
        }

        @Override
        public FromClause withType(@Nullable JavaType type) {
            return this.withExpression(expression.withType(type));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final FromClause t;

            public JRightPadded<J.Identifier> getIdentifier() {
                return t.identifier;
            }

            public FromClause withIdentifier(JRightPadded<J.Identifier> identifier) {
                return t.identifier == identifier ? t : new FromClause(t.id, t.prefix, t.markers, t.typeIdentifier, identifier, t.expression);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class LetClause implements Linq, QueryClause {
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

        JRightPadded<J.Identifier> identifier;

        @With
        @Getter
        Expression expression;

        public J.Identifier getIdentifier() {
            return identifier.getElement();
        }

        public LetClause withIdentifier(J.Identifier identifier) {
            return getPadding().withIdentifier(JRightPadded.withElement(this.identifier, identifier));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitLetClause(this, p);
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
            private final LetClause t;

            public JRightPadded<J.Identifier> getIdentifier() {
                return t.identifier;
            }

            public LetClause withIdentifier(JRightPadded<J.Identifier> identifier) {
                return t.identifier == identifier ? t : new LetClause(t.id, t.prefix, t.markers, identifier, t.expression);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class JoinClause implements Linq, QueryClause {
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

        JRightPadded<J.Identifier> identifier;

        JRightPadded<Expression> inExpression;

        JRightPadded<Expression> leftExpression;

        @With
        @Getter
        Expression rightExpression;

        @Nullable
        JLeftPadded<JoinIntoClause> into;

        public J.Identifier getIdentifier() {
            return identifier.getElement();
        }

        public JoinClause withIdentifier(J.Identifier identifier) {
            return getPadding().withIdentifier(this.identifier.withElement(identifier));
        }

        public Expression getInExpression() {
            return inExpression.getElement();
        }

        public JoinClause withInExpression(Expression inExpression) {
            return getPadding().withInExpression(this.inExpression.withElement(inExpression));
        }

        public Expression getLeftExpression() {
            return leftExpression.getElement();
        }

        public JoinClause withLeftExpression(Expression leftExpression) {
            return getPadding().withLeftExpression(this.leftExpression.withElement(leftExpression));
        }

        public @Nullable JoinIntoClause getInto() {
            return into == null ? null : into.getElement();
        }

        public JoinClause withInto(@Nullable JoinIntoClause into) {
            return getPadding().withInto(this.into.withElement(into));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitJoinClause(this, p);
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
            private final JoinClause t;

            public JRightPadded<J.Identifier> getIdentifier() {
                return t.identifier;
            }

            public JoinClause withIdentifier(JRightPadded<J.Identifier> identifier) {
                return t.identifier == identifier ? t :
                        new JoinClause(t.id, t.prefix, t.markers, identifier, t.inExpression,
                                t.leftExpression, t.rightExpression, t.into);
            }

            public JRightPadded<Expression> getInExpression() {
                return t.inExpression;
            }

            public JoinClause withInExpression(JRightPadded<Expression> inExpression) {
                return t.inExpression == inExpression ? t :
                        new JoinClause(t.id, t.prefix, t.markers, t.identifier, inExpression,
                                t.leftExpression, t.rightExpression, t.into);
            }

            public JRightPadded<Expression> getLeftExpression() {
                return t.leftExpression;
            }

            public JoinClause withLeftExpression(JRightPadded<Expression> leftExpression) {
                return t.leftExpression == leftExpression ? t :
                        new JoinClause(t.id, t.prefix, t.markers, t.identifier, t.inExpression,
                                leftExpression, t.rightExpression, t.into);
            }

            public @Nullable JLeftPadded<JoinIntoClause> getInto() {
                return t.into;
            }

            public JoinClause withInto(@Nullable JLeftPadded<JoinIntoClause> into) {
                return t.into == into ? t :
                        new JoinClause(t.id, t.prefix, t.markers, t.identifier, t.inExpression,
                                t.leftExpression, t.rightExpression, into);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class JoinIntoClause implements Linq, QueryClause {
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
        J.Identifier identifier;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitJoinIntoClause(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class WhereClause implements Linq, QueryClause {

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
        Expression condition;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitWhereClause(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class OrderByClause implements Linq, QueryClause {
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

        List<JRightPadded<Ordering>> orderings;

        public List<Ordering> getOrderings() {
            return JRightPadded.getElements(orderings);
        }

        public OrderByClause withOrderings(List<Ordering> orderings) {
            return getPadding().withOrderings(JRightPadded.withElements(this.orderings, orderings));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitOrderByClause(this, p);
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
            private final OrderByClause t;

            public List<JRightPadded<Ordering>> getOrderings() {
                return t.orderings;
            }

            public OrderByClause withOrderings(List<JRightPadded<Ordering>> orderings) {
                return t.orderings == orderings ? t : new OrderByClause(t.id, t.prefix, t.markers, orderings);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class QueryContinuation implements Linq {
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
        J.Identifier identifier;

        @With
        @Getter
        QueryBody body;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitQueryContinuation(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Ordering implements Linq {
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

        JRightPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public Ordering withExpression(Expression expression) {
            return getPadding().withExpression(this.expression.withElement(expression));
        }

        @With
        @Getter
        @Nullable
        DirectionKind direction;

        public enum DirectionKind {
            Ascending,
            Descending
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitOrdering(this, p);
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
            private final Ordering t;

            public JRightPadded<Expression> getExpression() {
                return t.expression;
            }

            public Ordering withExpression(JRightPadded<Expression> expression) {
                return t.expression == expression ? t : new Ordering(t.id, t.prefix, t.markers, expression, t.direction);
            }
        }
    }

    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class SelectClause implements Linq, SelectOrGroupClause {

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
        Expression expression;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitSelectClause(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class GroupClause implements Linq, SelectOrGroupClause {
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

        JRightPadded<Expression> groupExpression;

        public Expression getGroupExpression() {
            return groupExpression.getElement();
        }

        public GroupClause withGroupExpression(Expression groupExpression) {
            return getPadding().withGroupExpression(this.groupExpression.withElement(groupExpression));
        }

        @With
        @Getter
        Expression key;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitGroupClause(this, p);
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
            private final GroupClause t;

            public JRightPadded<Expression> getGroupExpression() {
                return t.groupExpression;
            }

            public GroupClause withGroupExpression(JRightPadded<Expression> groupExpression) {
                return t.groupExpression == groupExpression ? t : new GroupClause(t.id, t.prefix, t.markers, groupExpression, t.key);
            }
        }
    }
}
