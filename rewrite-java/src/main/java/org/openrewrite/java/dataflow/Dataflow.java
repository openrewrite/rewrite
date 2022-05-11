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

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.tree.Expression;

import java.util.Collections;
import java.util.List;

@Incubating(since = "7.24.0")
@RequiredArgsConstructor
public class Dataflow {
    final Cursor cursor;

    public <E extends Expression> List<Flow<?, E>> findSinks(LocalFlowSpec<?, E> spec) {
        return Collections.emptyList();
    }

    public <E extends Expression> List<Flow<E, ?>> findSources(LocalFlowSpec<E, ?> spec) {
        return Collections.emptyList();
    }

    @Value
    public static class Flow<Source extends Expression, Sink extends Expression> {
        List<Expression> path;

        public Source getSource() {
            //noinspection unchecked
            return (Source) path.get(0);
        }

        public Sink getSink() {
            //noinspection unchecked
            return (Sink) path.get(path.size() - 1);
        }
    }
}
