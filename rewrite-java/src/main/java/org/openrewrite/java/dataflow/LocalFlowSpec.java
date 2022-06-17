/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.controlflow.Guard;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Incubating(since = "7.24.0")
public abstract class LocalFlowSpec<Source extends Expression, Sink extends J> {
    protected final Type sourceType;
    protected final Type sinkType;
    protected LocalFlowSpec() {
        Type superClass = this.getClass().getGenericSuperclass();
        if (superClass instanceof Class) {
            throw new IllegalArgumentException("Internal error: LocalFlowSpec constructed without actual type information");
        } else {
            this.sourceType = ((ParameterizedType) superClass).getActualTypeArguments()[0];
            this.sinkType = ((ParameterizedType) superClass).getActualTypeArguments()[1];
        }
    }

    public Class<?> getSourceType() {
        return (Class<?>) sourceType;
    }

    public Class<?> getSinkType() {
        return (Class<?>) sinkType;
    }

    /**
     * The following is always true: {@code  source == cursor.getValue()}.
     *
     * @param source The {@link Expression} to check to determine if it should be considered flow-graph source.
     * @param cursor The current cursor position of the {@code source}.
     * @return {@code true} if {@code source} and {@code cursor} should be considered the source or root of a flow graph.
     */
    public abstract boolean isSource(Source source, Cursor cursor);

    /**
     * The following is always true: {@code  sink == cursor.getValue()}.
     *
     * @param sink The {@link Expression} to check to determine if it should be considered a flow-graph sink.
     * @param cursor The current cursor position of the {@code sink}.
     * @return {@code true} if {@code sink} and {@code cursor} should be considered the sink or leaf of a flow graph.
     */
    public abstract boolean isSink(Sink sink, Cursor cursor);

    public final boolean isFlowStep(
            Expression srcExpression,
            Cursor srcCursor,
            Expression sinkExpression,
            Cursor sinkCursor
    ) {
        return ExternalFlowModels.instance().isAdditionalFlowStep(
                srcExpression,
                srcCursor,
                sinkExpression,
                sinkCursor
        ) || isAdditionalFlowStep(
                srcExpression,
                srcCursor,
                sinkExpression,
                sinkCursor
        );
    }

    /**
     * takes an existing flow-step in the graph and offers a potential next flow step.
     * The method can then decide if the offered potential next flow step should be considered a valid next flow step
     * in the graph.
     *
     * Allows for ad-hoc taint tracking by allowing for additional, non-default flow steps to be added to the flow graph.
     *
     * The following is always true:
     * {@code  srcExpression == srcCursor.getValue() && sinkExpression == sinkCursor.getValue()}.
     */
    public boolean isAdditionalFlowStep(
            Expression srcExpression,
            Cursor srcCursor,
            Expression sinkExpression,
            Cursor sinkCursor
    ) {
        return false;
    }

    public boolean isBarrierGuard(Guard guard, boolean branch) {
        return false;
    }

    /**
     * Holds if flow through `expression` is prohibited.
     */
    public boolean isBarrier(Expression expression, Cursor cursor) {
        return false;
    }
}
