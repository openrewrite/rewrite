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
package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.marker.SearchResult;

import java.util.Optional;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class IsInRepository extends Recipe {
    @Override
    public String getDisplayName() {
        return "Is in repository";
    }

    @Override
    public String getDescription() {
        return "A search recipe which marks files that are in a repository with one of the supplied names. " +
               "Intended for use as a precondition for other recipes being run over many different repositories.";
    }

    @Option(displayName = "Allowed repositories",
            description = "The names of the repositories that are allowed to be searched. " +
                          "Determines repository name according to git metadata recorded in the `GitProvenance` marker.",
            example = "rewrite")
    Set<String> allowedRepositories;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree == null) {
                    return null;
                }
                Optional<GitProvenance> maybeGp = tree.getMarkers().findFirst(GitProvenance.class);
                if (maybeGp.isPresent()) {
                    GitProvenance gp = maybeGp.get();
                    if (allowedRepositories.contains(gp.getRepositoryName())) {
                        return SearchResult.found(tree);
                    }
                }
                return tree;
            }
        };
    }
}
