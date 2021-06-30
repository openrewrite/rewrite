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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.TreeVisitor;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.internal.HclPrinter;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface Hcl extends Serializable, Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return v instanceof HclVisitor ?
                (R) acceptHcl((HclVisitor<P>) v, p) : v.defaultValue(this, p);
    }

    @Nullable
    default <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof HclVisitor;
    }

    default <P> String print(TreePrinter<P> printer, P p) {
        return new HclPrinter<>(printer).print(this, p);
    }

    @Override
    default <P> String print(P p) {
        return print(TreePrinter.identity(), p);
    }

    Space getPrefix();

    Hcl withPrefix(Space prefix);

    <T extends Hcl> T withMarkers(Markers markers);

    Markers getMarkers();

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Attribute implements BodyContent, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        /**
         * When this is attribute of an object value,
         * can be a parenthetical expression or identifier.
         */
        @With
        Expression name;

        @With
        HclLeftPadded<Type> type;

        @With
        Expression value;

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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class AttributeAccess implements Expression, Label {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression attribute;

        @With
        HclLeftPadded<Identifier> name;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitAttributeAccess(this, p);
        }

        @Override
        public String toString() {
            return "AttributeAccess{" + attribute + "." + name + "}";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Binary implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression left;

        @With
        HclLeftPadded<Type> operator;

        @With
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Block implements BodyContent, Expression {
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
        Body body;

        @With
        Space close;

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
    @Data
    class Body implements Hcl {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        List<BodyContent> contents;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitBody(this, p);
        }

        @Override
        public String toString() {
            return "Body";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Conditional implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression condition;

        @With
        HclLeftPadded<Expression> truePart;

        @With
        HclLeftPadded<Expression> falsePart;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitConditional(this, p);
        }

        @Override
        public String toString() {
            return "Block";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Tuple implements CollectionValue {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        HclContainer<Expression> values;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitTuple(this, p);
        }

        @Override
        public String toString() {
            return "Tuple";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class ConfigFile implements Hcl, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Path sourcePath;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Body body;

        @With
        String eof;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitConfigFile(this, p);
        }

        @Override
        public String toString() {
            return "ConfigFile";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Empty implements Expression {
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
    @Data
    class ForIntro implements Hcl {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        HclContainer<Identifier> variables;

        @With
        Expression in;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitForIntro(this, p);
        }

        @Override
        public String toString() {
            return "ForTuple";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class ForObject implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        ForIntro intro;

        @With
        HclLeftPadded<Expression> updateName;

        @With
        HclLeftPadded<Expression> updateValue;

        @With
        @Nullable
        Empty ellipsis;

        @With
        @Nullable
        HclLeftPadded<Expression> condition;

        @With
        Space end;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitForObject(this, p);
        }

        @Override
        public String toString() {
            return "ForTuple";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class ForTuple implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        ForIntro intro;

        @With
        HclLeftPadded<Expression> update;

        @With
        @Nullable
        HclLeftPadded<Expression> condition;

        @With
        Space end;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitForTuple(this, p);
        }

        @Override
        public String toString() {
            return "ForTuple";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class FunctionCall implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Identifier name;

        @With
        HclContainer<Expression> arguments;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitFunctionCall(this, p);
        }

        @Override
        public String toString() {
            return "FunctionCall{" + name + ", argArity=" + arguments.getElements().size() + "}";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Identifier implements Expression, Label {
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
        @Data
        public static class Position implements Hcl {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            HclRightPadded<Expression> position;

            @Override
            public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
                return v.visitIndexPosition(this, p);
            }

            @Override
            public String toString() {
                return "IndexPosition";
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Literal implements Hcl, Expression, Label {
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
        @Nullable
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
    @Data
    class ObjectValue implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        /**
         * Can be an {@link Attribute} or {@link Empty}. {@link Empty} is the
         * type of a trailing comma at the end of the attribute list.
         */
        @With
        HclContainer<Expression> attributes;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitObjectValue(this, p);
        }

        @Override
        public String toString() {
            return "Parentheses";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Parentheses implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        HclRightPadded<Expression> expression;

        @Override
        public <P> Hcl acceptHcl(HclVisitor<P> v, P p) {
            return v.visitParentheses(this, p);
        }

        @Override
        public String toString() {
            return "Parentheses";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class QuotedTemplate implements Expression {
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
    class HeredocTemplate implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        String arrow;

        public HeredocTemplate withArrow(String arrow) {
            if (!arrow.equals("<<") && !arrow.equals("<<-")) {
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
    class Splat implements Expression {
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
    @Data
    class Unary implements Expression {
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
