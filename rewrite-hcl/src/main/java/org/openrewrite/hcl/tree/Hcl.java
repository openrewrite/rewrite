/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.hcl.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.openrewrite.hcl.HclParser;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.internal.HclPrinter;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.template.SourceTemplate;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;

@SuppressWarnings("unused")
public interface Hcl extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptHcl(v.adapt(HclVisitor.class), p);
    }

    @Nullable
    default <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(HclVisitor.class);
    }

    Space getPrefix();

    <H extends Hcl> H withPrefix(Space prefix);

    default <H extends Hcl> H withTemplate(SourceTemplate<Hcl, HclCoordinates> template, HclCoordinates coordinates, Object... parameters) {
        return template.withTemplate(this, coordinates, parameters);
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Attribute implements BodyContent, Expression {
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

        /**
         * When this is attribute of an object value,
         * can be a parenthetical expression or identifier.
         */
        @With
        @Getter
        Expression name;

        @JsonIgnore
        public String getSimpleName() {
            return getSimpleName(name);
        }

        private String getSimpleName(Expression e) {
            return e instanceof Parentheses ? getSimpleName(((Parentheses) e).getExpression()) : ((Identifier) e).getName();
        }

        HclLeftPadded<Type> type;

        public Type getType() {
            return type.getElement();
        }

        public Attribute withType(Type type) {
            return getPadding().withType(this.type.withElement(type));
        }

        @With
        @Getter
        Expression value;

        @With
        @Getter
        @Nullable
        Empty comma;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitAttribute(this, p);
        }

        @Override
        public String toString() {
            return "Attribute{" + name + "}";
        }

        public enum Type {
            ObjectElement,
            Assignment
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
            private final Attribute t;

            public HclLeftPadded<Type> getType() {
                return t.type;
            }

            public Attribute withType(HclLeftPadded<Type> type) {
                return t.type == type ? t : new Attribute(t.id, t.prefix, t.markers, t.name, type, t.value, t.comma);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class AttributeAccess implements Expression, Label {
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
        Expression attribute;

        HclLeftPadded<Identifier> name;

        public Identifier getType() {
            return name.getElement();
        }

        public AttributeAccess withType(Identifier name) {
            return getPadding().withName(this.name.withElement(name));
        }

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitAttributeAccess(this, p);
        }

        @Override
        public String toString() {
            return "AttributeAccess{" + attribute + "." + name + "}";
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
            private final AttributeAccess t;

            public HclLeftPadded<Identifier> getName() {
                return t.name;
            }

            public AttributeAccess withName(HclLeftPadded<Identifier> name) {
                return t.name == name ? t : new AttributeAccess(t.id, t.prefix, t.markers, t.attribute, name);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Binary implements Expression {
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
        Expression left;

        HclLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElement();
        }

        public Binary withOperator(Type type) {
            return getPadding().withOperator(this.operator.withElement(type));
        }

        @With
        @Getter
        Expression right;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitBinary(this, p);
        }

        @Override
        public String toString() {
            return "Binary";
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
            Or,
            And
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
            private final Binary t;

            public HclLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public Binary withOperator(HclLeftPadded<Type> operator) {
                return t.operator == operator ? t : new Binary(t.id, t.prefix, t.markers, t.left, operator, t.right);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Block implements BodyContent, Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        /**
         * Nullable for anonymous block expressions
         */
        @With
        @Nullable
        Identifier type;

        @With
        List<Label> labels;

        @With
        Space open;

        @With
        List<BodyContent> body;

        @Incubating(since = "7.27.0")
        @Nullable
        public Attribute getAttribute(String attrName) {
            for (BodyContent t : body) {
                if (t instanceof Attribute) {
                    Attribute attribute = (Attribute) t;
                    if (attribute.getSimpleName().equals(attrName)) {
                        return attribute;
                    }
                }
            }
            return null;
        }

        /**
         * Locate an attribute with the given name and set its value.
         *
         * @param attrName The attribute to locate. This assumes there is one and only one.
         * @param value    The value to set.
         * @return This block.
         */
        @Incubating(since = "7.27.0")
        public Hcl.Block withAttributeValue(String attrName, Object value) {
            Attribute attr = getAttribute(attrName);
            if (attr == null || getAttributeValue(attrName).equals(value)) {
                return this;
            }

            return withBody(ListUtils.map(body, b -> {
                if (b == attr) {
                    if (attr.getValue() instanceof Literal) {
                        Literal l = (Literal) attr.getValue();
                        return attr.withValue(l.withValue(value.toString()).withValueSource("\"" + value + "\""));
                    } else if (attr.getValue() instanceof QuotedTemplate) {
                        QuotedTemplate q = (QuotedTemplate) attr.getValue();
                        return attr.withValue(q.withExpressions(singletonList(new Literal(Tree.randomId(),
                                Space.EMPTY, Markers.EMPTY, value, value.toString()))));
                    }
                }
                return b;
            }));
        }

        /**
         * @param attrName The name of the attribute to look for.
         * @return The text value of the attribute matching the provided name, if any.
         */
        @Incubating(since = "7.27.0")
        @Nullable
        public <T> T getAttributeValue(String attrName) {
            Attribute attr = getAttribute(attrName);
            if (attr == null) {
                return null;
            }

            Object value = attr.getValue() instanceof Literal ?
                    ((Literal) attr.getValue()).getValueSource() : null;
            if (attr.getValue() instanceof QuotedTemplate) {
                Cursor root = new Cursor(null, HclParser.builder().build().parse("")
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Could not parse as HCL")));
                StringBuilder valueBuilder = new StringBuilder();
                for (Expression expr : ((QuotedTemplate) attr.getValue()).getExpressions()) {
                    valueBuilder.append(expr.print(root));
                }
                value = valueBuilder.toString();
            }
            //noinspection unchecked
            return (T) value;
        }

        @With
        Space end;

        public CoordinateBuilder.Block getCoordinates() {
            return new CoordinateBuilder.Block(this);
        }

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitBlock(this, p);
        }

        @Override
        public String toString() {
            return "Block";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Conditional implements Expression {
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
        Expression condition;

        HclLeftPadded<Expression> truePart;

        public Expression getTruePart() {
            return truePart.getElement();
        }

        public Conditional withTruePart(Expression truePart) {
            return getPadding().withTruePart(this.truePart.withElement(truePart));
        }

        HclLeftPadded<Expression> falsePart;

        public Expression getFalsePart() {
            return falsePart.getElement();
        }

        public Conditional withFalsePart(Expression falsePart) {
            return getPadding().withFalsePart(this.falsePart.withElement(falsePart));
        }

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitConditional(this, p);
        }

        @Override
        public String toString() {
            return "Block";
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
            private final Conditional t;

            public HclLeftPadded<Expression> getTruePart() {
                return t.truePart;
            }

            public Conditional withTruePart(HclLeftPadded<Expression> truePart) {
                return t.truePart == truePart ? t : new Conditional(t.id, t.prefix, t.markers, t.condition, truePart, t.falsePart);
            }

            public HclLeftPadded<Expression> getFalsePart() {
                return t.falsePart;
            }

            public Conditional withFalsePart(HclLeftPadded<Expression> falsePart) {
                return t.falsePart == falsePart ? t : new Conditional(t.id, t.prefix, t.markers, t.condition, t.truePart, falsePart);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class ConfigFile implements Hcl, SourceFile {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Path sourcePath;

        @With
        @Nullable
        FileAttributes fileAttributes;

        @With
        Space prefix;

        @With
        Markers markers;

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        @With
        @Getter
        boolean charsetBomMarked;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @With
        @Getter
        @Nullable
        Checksum checksum;

        @With
        List<BodyContent> body;

        @With
        Space eof;

        public CoordinateBuilder.ConfigFile getCoordinates() {
            return new CoordinateBuilder.ConfigFile(this);
        }

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitConfigFile(this, p);
        }

        @Override
        public String toString() {
            return "ConfigFile";
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new HclPrinter<>();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Empty implements Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitEmpty(this, p);
        }

        @Override
        public String toString() {
            return "Empty";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ForIntro implements Hcl {
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

        HclContainer<Identifier> variables;

        public List<Identifier> getVariables() {
            return variables.getElements();
        }

        public ForIntro withVariables(List<Identifier> variables) {
            return getPadding().withVariables(HclContainer.withElements(this.variables, variables));
        }

        @With
        @Getter
        Expression in;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitForIntro(this, p);
        }

        @Override
        public String toString() {
            return "ForIntro";
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
            private final ForIntro t;

            public HclContainer<Identifier> getVariables() {
                return t.variables;
            }

            public ForIntro withVariables(HclContainer<Identifier> variables) {
                return t.variables == variables ? t : new ForIntro(t.id, t.prefix, t.markers, variables, t.in);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ForObject implements Expression {
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
        ForIntro intro;

        HclLeftPadded<Expression> updateName;

        public Expression getUpdateName() {
            return updateName.getElement();
        }

        public ForObject withUpdateName(Expression updateName) {
            return getPadding().withUpdateName(this.updateName.withElement(updateName));
        }

        HclLeftPadded<Expression> updateValue;

        public Expression getUpdateValue() {
            return updateValue.getElement();
        }

        public ForObject withUpdateValue(Expression updateValue) {
            return getPadding().withUpdateValue(this.updateValue.withElement(updateValue));
        }

        @With
        @Getter
        @Nullable
        Empty ellipsis;

        @Nullable
        HclLeftPadded<Expression> condition;

        @Nullable
        public Expression getCondition() {
            return condition == null ? null : condition.getElement();
        }

        public ForObject withCondition(Expression condition) {
            return getPadding().withCondition(HclLeftPadded.withElement(this.condition, condition));
        }

        @With
        @Getter
        Space end;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitForObject(this, p);
        }

        @Override
        public String toString() {
            return "ForTuple";
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
            private final ForObject t;

            public HclLeftPadded<Expression> getUpdateName() {
                return t.updateName;
            }

            public ForObject withUpdateName(HclLeftPadded<Expression> updateName) {
                return t.updateName == updateName ? t : new ForObject(t.id, t.prefix, t.markers, t.intro, updateName, t.updateValue, t.ellipsis, t.condition, t.end);
            }

            public HclLeftPadded<Expression> getUpdateValue() {
                return t.updateValue;
            }

            public ForObject withUpdateValue(HclLeftPadded<Expression> updateValue) {
                return t.updateValue == updateValue ? t : new ForObject(t.id, t.prefix, t.markers, t.intro, t.updateName, updateValue, t.ellipsis, t.condition, t.end);
            }

            @Nullable
            public HclLeftPadded<Expression> getCondition() {
                return t.condition;
            }

            public ForObject withCondition(@Nullable HclLeftPadded<Expression> condition) {
                return t.condition == condition ? t : new ForObject(t.id, t.prefix, t.markers, t.intro, t.updateName, t.updateValue, t.ellipsis, condition, t.end);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ForTuple implements Expression {
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
        ForIntro intro;

        HclLeftPadded<Expression> update;

        public Expression getUpdateName() {
            return update.getElement();
        }

        public ForTuple withUpdateName(Expression updateName) {
            return getPadding().withUpdate(this.update.withElement(updateName));
        }

        @Nullable
        HclLeftPadded<Expression> condition;

        @Nullable
        public Expression getCondition() {
            return condition == null ? null : condition.getElement();
        }

        public ForTuple withCondition(Expression condition) {
            return getPadding().withCondition(HclLeftPadded.withElement(this.condition, condition));
        }

        @With
        @Getter
        Space end;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitForTuple(this, p);
        }

        @Override
        public String toString() {
            return "ForTuple";
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
            private final ForTuple t;

            public HclLeftPadded<Expression> getUpdate() {
                return t.update;
            }

            public ForTuple withUpdate(HclLeftPadded<Expression> update) {
                return t.update == update ? t : new ForTuple(t.id, t.prefix, t.markers, t.intro, update, t.condition, t.end);
            }

            @Nullable
            public HclLeftPadded<Expression> getCondition() {
                return t.condition;
            }

            public ForTuple withCondition(@Nullable HclLeftPadded<Expression> condition) {
                return t.condition == condition ? t : new ForTuple(t.id, t.prefix, t.markers, t.intro, t.update, condition, t.end);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class FunctionCall implements Expression {
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
        Identifier name;

        HclContainer<Expression> arguments;

        public List<Expression> getVariables() {
            return arguments.getElements();
        }

        public FunctionCall withArguments(List<Expression> arguments) {
            return getPadding().withArguments(HclContainer.withElements(this.arguments, arguments));
        }

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitFunctionCall(this, p);
        }

        @Override
        public String toString() {
            return "FunctionCall{" + name + ", argArity=" + arguments.getElements().size() + "}";
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
            private final FunctionCall t;

            public HclContainer<Expression> getArguments() {
                return t.arguments;
            }

            public FunctionCall withArguments(HclContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new FunctionCall(t.id, t.prefix, t.markers, t.name, arguments);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class HeredocTemplate implements Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        String arrow;

        public HeredocTemplate withArrow(String arrow) {
            if (!"<<".equals(arrow) && !"<<-".equals(arrow)) {
                throw new IllegalArgumentException("Heredoc arrow must be one of << or <<-");
            }
            return this.arrow.equals(arrow) ? this :
                    new HeredocTemplate(id, prefix, markers, arrow, delimiter, expressions, end);
        }

        @With
        Identifier delimiter;

        @With
        List<Expression> expressions;

        @With
        Space end;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitHeredocTemplate(this, p);
        }

        @Override
        public String toString() {
            return "QuotedTemplate";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Identifier implements Expression, Label {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        String name;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitIdentifier(this, p);
        }

        @Override
        public String toString() {
            return "Identifier{" + name + "}";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Index implements Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression indexed;

        @With
        Position position;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitIndex(this, p);
        }

        @Override
        public String toString() {
            return "Index";
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Position implements Hcl {
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

            HclRightPadded<Expression> position;

            public Expression getPosition() {
                return position.getElement();
            }

            public Position withPosition(Expression position) {
                return getPadding().withPosition(this.position.withElement(position));
            }

            @Override
            public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
                return v.visitIndexPosition(this, p);
            }

            @Override
            public String toString() {
                return "IndexPosition";
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
                private final Position t;

                public HclRightPadded<Expression> getPosition() {
                    return t.position;
                }

                public Position withPosition(HclRightPadded<Expression> position) {
                    return t.position == position ? t : new Position(t.id, t.prefix, t.markers, position);
                }
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Literal implements Hcl, Expression, Label {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Object value;

        @With
        String valueSource;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }

        @Override
        public String toString() {
            return "Literal{" + valueSource + "}";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ObjectValue implements Expression {
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

        /**
         * Can be an {@link Attribute} or {@link Empty}. {@link Empty} is the
         * type of a trailing comma at the end of the attribute list.
         */
        HclContainer<Expression> attributes;

        public List<Expression> getAttributes() {
            return attributes.getElements();
        }

        public ObjectValue withArguments(List<Expression> attributes) {
            return getPadding().withAttributes(HclContainer.withElements(this.attributes, attributes));
        }

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitObjectValue(this, p);
        }

        @Override
        public String toString() {
            return "ObjectValue";
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
            private final ObjectValue t;

            public HclContainer<Expression> getAttributes() {
                return t.attributes;
            }

            public ObjectValue withAttributes(HclContainer<Expression> attributes) {
                return t.attributes == attributes ? t : new ObjectValue(t.id, t.prefix, t.markers, attributes);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Parentheses implements Expression {
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

        HclRightPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public Parentheses withExpression(Expression expression) {
            return getPadding().withExpression(this.expression.withElement(expression));
        }

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitParentheses(this, p);
        }

        @Override
        public String toString() {
            return "Parentheses";
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
            private final Parentheses t;

            public HclRightPadded<Expression> getExpression() {
                return t.expression;
            }

            public Parentheses withExpression(HclRightPadded<Expression> expression) {
                return t.expression == expression ? t : new Parentheses(t.id, t.prefix, t.markers, expression);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class QuotedTemplate implements Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        List<Expression> expressions;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitQuotedTemplate(this, p);
        }

        @Override
        public String toString() {
            return "QuotedTemplate";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Splat implements Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression select;

        @With
        Operator operator;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitSplat(this, p);
        }

        @Override
        public String toString() {
            return "Splat{" + select + "}";
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Operator implements Hcl {
            @With
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            Type type;

            /**
             * The {@link Empty} here is a placeholder for the fixed '*' token.
             * <p>
             * Left padding between the '*' and the preceding '.' or '[', depending on whether it
             * is an attribute splat or full splat.
             * <p>
             * Right padding possible for full splats (between the '*' and the ']').
             */
            @With
            HclRightPadded<Empty> splat;

            @Override
            public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
                return v.visitSplatOperator(this, p);
            }

            @Override
            public String toString() {
                return "SplatOperator";
            }

            public enum Type {
                Attribute,
                Full
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class TemplateInterpolation implements Hcl, Expression {
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
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitTemplateInterpolation(this, p);
        }

        @Override
        public String toString() {
            return "TemplateInterpolation";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Tuple implements CollectionValue {
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

        HclContainer<Expression> values;

        public List<Expression> getValues() {
            return values.getElements();
        }

        public Tuple withValues(List<Expression> values) {
            return getPadding().withValues(HclContainer.withElements(this.values, values));
        }

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitTuple(this, p);
        }

        @Override
        public String toString() {
            return "Tuple";
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
            private final Tuple t;

            public HclContainer<Expression> getValues() {
                return t.values;
            }

            public Tuple withValues(HclContainer<Expression> values) {
                return t.values == values ? t : new Tuple(t.id, t.prefix, t.markers, values);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Unary implements Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Type operator;

        @With
        Expression expression;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitUnary(this, p);
        }

        @Override
        public String toString() {
            return "Unary";
        }

        public enum Type {
            Negative,
            Not
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class VariableExpression implements Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Identifier name;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitVariableExpression(this, p);
        }

        @Override
        public String toString() {
            return "Variable{" + name + "}";
        }
    }
}
