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
package org.openrewrite.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class RewriteRecipeSource extends DataTable<RewriteRecipeSource.Row> {

    public RewriteRecipeSource(Recipe recipe) {
        super(recipe,
                "Rewrite recipe source code",
                "This table contains the source code of recipes along with their metadata " +
                "for use in an experiment fine-tuning large language models to produce more recipes."
        );
    }

    @Value
    public static class Row {
        @Column(displayName = "Recipe name", description = "The name of the recipe.")
        String displayName;

        @Column(displayName = "Recipe description", description = "The description of the recipe.")
        String description;

        @Column(displayName = "Recipe type", description = "Differentiate between Java and YAML recipes, as they may be " +
                                                           "two independent data sets used in LLM fine-tuning.")
        RecipeType recipeType;

        @Column(displayName = "Recipe source code", description = "The full source code of the recipe.")
        String sourceCode;

        @Column(displayName = "Recipe options", description = "JSON format of recipe options.")
        String options;
    }

    public enum RecipeType {
        Java,
        Yaml
    }
}
