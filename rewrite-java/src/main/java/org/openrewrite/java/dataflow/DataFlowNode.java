/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.dataflow;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.java.trait.util.TraitErrors;
import org.openrewrite.java.trait.variable.Parameter;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class DataFlowNode {
    final Cursor cursor;

    abstract Optional<Expression> asExpression();

    abstract Optional<Parameter> asParameter();

    abstract <T> T map(Function<Expression, T> whenExpression, Function<Parameter, T> whenParameter);

    public static DataFlowNode of(Cursor cursor) {
        if (cursor.getValue() instanceof Expression) {
            return new ExpressionDataFlowNode(cursor, cursor.getValue());
        } else if (cursor.getValue() instanceof J.VariableDeclarations.NamedVariable) {
            return new ParameterDataFlowNode(cursor, Parameter.viewOf(cursor).orSuccess(TraitErrors::doThrow));
        } else {
            throw new IllegalArgumentException("DataFlowNode can not be of type: " + cursor.getValue().getClass());
        }
    }
}

class ExpressionDataFlowNode extends DataFlowNode {
    private final Expression expression;
    ExpressionDataFlowNode(Cursor cursor, Expression expression) {
        super(cursor);
        this.expression = requireNonNull(expression, "expression");
    }

    @Override
    Optional<Expression> asExpression() {
        return Optional.of(expression);
    }

    @Override
    Optional<Parameter> asParameter() {
        return Optional.empty();
    }

    @Override
    <T> T map(Function<Expression, T> whenExpression, Function<Parameter, T> whenParameter) {
        requireNonNull(whenExpression, "whenExpression");
        requireNonNull(whenParameter, "whenParameter");
        return whenExpression.apply(expression);
    }
}

class ParameterDataFlowNode extends DataFlowNode {
    private final Parameter parameter;
    ParameterDataFlowNode(Cursor cursor, Parameter parameter) {
        super(cursor);
        this.parameter = requireNonNull(parameter, "parameter");
    }

    @Override
    Optional<Expression> asExpression() {
        return Optional.empty();
    }

    @Override
    Optional<Parameter> asParameter() {
        return Optional.of(parameter);
    }

    @Override
    <T> T map(Function<Expression, T> whenExpression, Function<Parameter, T> whenParameter) {
        requireNonNull(whenExpression, "whenExpression");
        requireNonNull(whenParameter, "whenParameter");
        return whenParameter.apply(parameter);
    }
}
