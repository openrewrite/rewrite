/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {ChangeText} from "./change-text";
import {Recipe} from "@openrewrite/rewrite";

export class RecipeWithRecipeList extends Recipe {
    name = "org.openrewrite.example.text.with-recipe-list"
    displayName = "A recipe that has a recipe list";
    description = "To verify that it is possible for a recipe list to be called over RPC.";

    async recipeList() {
        return [new ChangeText({text: "hello"})];
    }
}
