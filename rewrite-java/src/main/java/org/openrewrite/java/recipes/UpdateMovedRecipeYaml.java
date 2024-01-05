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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;
import org.openrewrite.yaml.tree.YamlKey;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateMovedRecipeYaml extends Recipe {
    @Option(displayName = "The fully qualified className of recipe moved from",
        description = "The fully qualified className of recipe moved from a old package.",
        example = "org.openrewrite.java.cleanup.UnnecessaryCatch")
    String oldRecipeFullyQualifiedClassName;

    @Option(displayName = "The fully qualified className of recipe moved to",
        description = "The fully qualified className of recipe moved to a new package.",
        example = "org.openrewrite.staticanalysis.UnnecessaryCatch")
    String newRecipeFullyQualifiedClassName;

    @Override
    public String getDisplayName() {
        return "Update moved package recipe in yaml file";
    }

    @Override
    public String getDescription() {
        return "Update moved package recipe in yaml file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext ctx) {
                List<String> keys = getCursor().getPathAsStream()
                    .filter(Yaml.Mapping.Entry.class::isInstance)
                    .map(Yaml.Mapping.Entry.class::cast)
                    .map(Yaml.Mapping.Entry::getKey)
                    .map(YamlKey::getValue)
                    .collect(Collectors.toList());
                Collections.reverse(keys);
                String prop = String.join(".", keys);

                if (prop.equals("recipeList")
                    && scalar.getValue().equals(oldRecipeFullyQualifiedClassName)) {
                    return scalar.withValue(newRecipeFullyQualifiedClassName);
                }
                return super.visitScalar(scalar, ctx);
            }
        };
    }
}
