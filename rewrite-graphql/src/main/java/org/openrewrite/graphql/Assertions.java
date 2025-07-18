/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.graphql;

import org.jspecify.annotations.Nullable;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public class Assertions {
    private Assertions() {}

    public static SourceSpecs graphQl(@Nullable String before) {
        return graphQl(before, s -> {});
    }

    public static SourceSpecs graphQl(@Nullable String before, Consumer<SourceSpec<GraphQl.Document>> spec) {
        SourceSpec<GraphQl.Document> graphQl = new SourceSpec<>(GraphQl.Document.class, null, GraphQlParser.builder(), before, null);
        spec.accept(graphQl);
        return graphQl;
    }

    public static SourceSpecs graphQl(@Nullable String before, @Nullable String after) {
        return graphQl(before, after, s -> {});
    }

    public static SourceSpecs graphQl(@Nullable String before, @Nullable String after, 
                                     Consumer<SourceSpec<GraphQl.Document>> spec) {
        SourceSpec<GraphQl.Document> graphQl = new SourceSpec<>(GraphQl.Document.class, null, GraphQlParser.builder(), before, s -> after);
        spec.accept(graphQl);
        return graphQl;
    }
}