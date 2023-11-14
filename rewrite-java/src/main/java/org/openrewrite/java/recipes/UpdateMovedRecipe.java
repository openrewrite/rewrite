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
import org.openrewrite.Option;
import org.openrewrite.Recipe;

import java.util.Arrays;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateMovedRecipe extends Recipe {
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
        return "Update moved package recipe";
    }

    @Override
    public String getDescription() {
        return "If a recipe moved between packages, update the code reference places, declarative recipes, and `activeRecipes` in ppm.xml.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
            new UpdateMovedPackageClassName(oldRecipeFullyQualifiedClassName,newRecipeFullyQualifiedClassName),
            new UpdateMovedRecipeYaml(oldRecipeFullyQualifiedClassName, newRecipeFullyQualifiedClassName),
            new UpdateMovedRecipeXml(oldRecipeFullyQualifiedClassName, newRecipeFullyQualifiedClassName)
        );
    }
}