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

    public abstract boolean isSource(Source source, Cursor cursor);

    public abstract boolean isSink(Sink sink, Cursor cursor);

    public boolean isAdditionalFlowStep(Expression startE, Cursor startC, Expression endE, Cursor endC) {
        return false;
    }

    public boolean isBarrierGuard(Expression expr) {
        return false;
    }
}
