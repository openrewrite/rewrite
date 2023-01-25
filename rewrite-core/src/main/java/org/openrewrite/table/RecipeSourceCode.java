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
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class RecipeSourceCode extends DataTable<RecipeSourceCode.Row> {

    public RecipeSourceCode(Recipe recipe) {
        super(recipe, Row.class, RecipeSourceCode.class.getName(),
                "Recipe source code",
                "The source code of a recipe along with its human-readable description.");
    }

    @Value
    public static class Row {
        String name;
        String displayName;
        String description;
        String sourceCode;
    }
}
