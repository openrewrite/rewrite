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
package org.openrewrite.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.BuildMetadata;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindBuildMetadata extends Recipe {

    @Option(displayName = "Build metadata key",
            description = "The key to search for in the build metadata.",
            example = "lstFormatVersion")
    String key;

    @Option(displayName = "Build metadata value",
            description = "The value to search for in the build metadata.",
            example = "2")
    String value;

    @Override
    public String getDisplayName() {
        return "Find build metadata";
    }

    @Override
    public String getDescription() {
        return "Find source files with matching build metadata.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    for (BuildMetadata buildMetadata : tree.getMarkers().findAll(BuildMetadata.class)) {
                        if (buildMetadata.getMetadata().containsKey(key)) {
                            if (buildMetadata.getMetadata().get(key).equals(value)) {
                                return SearchResult.found(tree, "Found build metadata");
                            }
                        }
                    }
                }
                return tree;
            }
        };
    }
}
