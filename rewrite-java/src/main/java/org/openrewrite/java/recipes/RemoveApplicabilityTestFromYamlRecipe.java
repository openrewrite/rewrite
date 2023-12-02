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
package org.openrewrite.java.recipes;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.yaml.ChangePropertyKey;
import org.openrewrite.yaml.CommentOutProperty;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;
import org.openrewrite.yaml.tree.YamlKey;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class RemoveApplicabilityTestFromYamlRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove any-source applicability and migrate single-source applicability tests in YAML recipe";
    }

    @Override
    public String getDescription() {
        return "Removes any-source applicability tests from YAML recipes, as the are no " +
               "longer supported in Rewrite 8 and migrates single-source applicability tests to preconditions.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("Rewrite8 migration");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> yamlRecipeCheckVisitor = new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                List<String> keys = getCursor().getPathAsStream()
                        .filter(Yaml.Mapping.Entry.class::isInstance)
                        .map(Yaml.Mapping.Entry.class::cast)
                        .map(Yaml.Mapping.Entry::getKey)
                        .map(YamlKey::getValue)
                        .collect(Collectors.toList());
                Collections.reverse(keys);
                String prop = String.join(".", keys);

                if (prop.equals("applicability.anySource") || prop.equals("applicability.singleSource")) {
                    return SearchResult.found(entry);
                }
                return super.visitMappingEntry(entry, ctx);
            }
        };

        return Preconditions.check(yamlRecipeCheckVisitor, new YamlIsoVisitor<ExecutionContext>() {
                    @Override
                    public Yaml visit(@Nullable Tree tree, ExecutionContext ctx) {
                        Tree visited = new ChangePropertyKey(
                                "applicability.singleSource",
                                "preconditions",
                                false,
                                null
                        ).getVisitor().visit(tree, ctx);

                        String commentText = "Applicability tests are no longer supported for yaml recipes, please remove or require " +
                                             "migrating the recipe to Java code";
                        return (Yaml) new CommentOutProperty("applicability.anySource", commentText).getVisitor().visit(visited, ctx);
                    }
                }
        );
    }
}
